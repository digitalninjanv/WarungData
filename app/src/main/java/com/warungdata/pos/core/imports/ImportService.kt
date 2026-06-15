package com.warungdata.pos.core.imports

import android.content.Context
import android.net.Uri
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.CategoryEntity
import com.warungdata.pos.core.database.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportResult(val total: Int, val imported: Int, val errors: List<String>)

class ImportService(private val context: Context) {
    private val db = AppDatabase.getInstance(context)

    suspend fun importProducts(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        var total = 0
        var imported = 0

        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val content = stream.bufferedReader(Charsets.UTF_8).readText()
                val lines = content.lines().filter { it.isNotBlank() }
                total = lines.size - 1 // minus header

                for ((i, line) in lines.withIndex()) {
                    if (i == 0) continue // skip header
                    try {
                        val cols = parseCsvLine(line)
                        if (cols.size < 3) {
                            errors.add("Baris ${i + 1}: format salah")
                            continue
                        }

                        val name = cols[0].trim()
                        var categoryId: Int? = null
                        if (cols.size > 1 && cols[1].isNotBlank()) {
                            val catName = cols[1].trim()
                            var cat = db.categoryDao().getCategoryByName(catName)
                            if (cat == null) {
                                val id = db.categoryDao().insertCategory(CategoryEntity(name = catName))
                                cat = CategoryEntity(id = id.toInt(), name = catName)
                            }
                            categoryId = cat.id
                        }

                        val purchasePrice = cols.getOrNull(2)?.trim()?.replace("\\D".toRegex(), "")?.toLongOrNull() ?: 0
                        val sellingPrice = cols.getOrNull(3)?.trim()?.replace("\\D".toRegex(), "")?.toLongOrNull() ?: 0
                        val stock = cols.getOrNull(4)?.trim()?.toIntOrNull() ?: 0
                        val minStock = cols.getOrNull(5)?.trim()?.toIntOrNull() ?: 0
                        val barcode = cols.getOrNull(6)?.trim() ?: ""
                        val unit = cols.getOrNull(7)?.trim() ?: "pcs"

                        db.productDao().insertProduct(ProductEntity(
                            name = name,
                            categoryId = categoryId,
                            purchasePrice = purchasePrice,
                            sellingPrice = sellingPrice,
                            stock = stock,
                            minimumStock = minStock,
                            barcode = barcode,
                            unit = unit
                        ))
                        imported++
                    } catch (e: Exception) {
                        errors.add("Baris ${i + 1}: ${e.message}")
                    }
                }
            } ?: run {
                errors.add("Tidak dapat membaca file")
            }
        } catch (e: Exception) {
            errors.add("Gagal membaca file: ${e.message}")
        }

        ImportResult(total, imported, errors)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }
}
