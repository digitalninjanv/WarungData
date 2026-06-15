package com.warungdata.pos.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat { CSV, JSON, PDF }
enum class ExportType { PRODUCTS, SALES, CUSTOMERS, EXPENSES, STOCK, DEBTS, SUPPLIERS }

data class ExportResult(val file: File, val format: ExportFormat, val type: ExportType, val size: Long)

class ExportService(private val context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val settingsRepo = SettingsRepository(context)
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID"))
    private val numFmt = java.text.NumberFormat.getNumberInstance(Locale("id", "ID"))

    suspend fun export(type: ExportType, format: ExportFormat, startDate: Long = 0, endDate: Long = Long.MAX_VALUE): ExportResult = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), "exports")
        dir.mkdirs()
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${type.name.lowercase()}_$ts.${format.name.lowercase()}"
        val file = File(dir, fileName)

        when (format) {
            ExportFormat.CSV -> exportCsv(type, file, startDate, endDate)
            ExportFormat.JSON -> exportJson(type, file, startDate, endDate)
            ExportFormat.PDF -> exportPdf(type, file, startDate, endDate)
        }
        ExportResult(file, format, type, file.length())
    }

    private fun writeCsvLine(w: Writer, vararg vals: String) {
        w.write(vals.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n")
    }

    private suspend fun exportCsv(type: ExportType, file: File, start: Long, end: Long) {
        file.bufferedWriter(Charsets.UTF_8).use { w ->
            when (type) {
                ExportType.PRODUCTS -> {
                    writeCsvLine(w, "ID", "Nama", "Kategori", "Barcode", "Satuan", "Harga Beli", "Harga Jual", "Stok", "Min Stok")
                    db.productDao().getAllProductsOnce().forEach { p ->
                        val cat = p.categoryId?.let { runCatching { db.categoryDao().getCategoryById(it) }.getOrNull() }?.name ?: ""
                        writeCsvLine(w, p.id.toString(), p.name, cat, p.barcode, p.unit, p.purchasePrice.toString(), p.sellingPrice.toString(), p.stock.toString(), p.minimumStock.toString())
                    }
                }
                ExportType.SALES -> {
                    writeCsvLine(w, "Invoice", "Tanggal", "Subtotal", "Diskon", "Total", "Metode", "Status", "Pelanggan", "Kasir")
                    db.saleDao().getSalesByDateRangeOnce(start, end).forEach { s ->
                        val cust = s.customerId?.let { runCatching { db.customerDao().getCustomerById(it) }.getOrNull() }?.name ?: ""
                        writeCsvLine(w, s.invoiceNumber, dateFmt.format(Date(s.date)), s.subtotal.toString(), s.discount.toString(), s.total.toString(), s.paymentMethod, s.paymentStatus, cust, s.cashierName)
                    }
                }
                ExportType.CUSTOMERS -> {
                    writeCsvLine(w, "ID", "Nama", "Telepon", "Alamat", "Total Piutang", "Catatan")
                    db.customerDao().getAllCustomersOnce().forEach { c ->
                        writeCsvLine(w, c.id.toString(), c.name, c.phone, c.address, c.totalDebt.toString(), c.note)
                    }
                }
                ExportType.EXPENSES -> {
                    writeCsvLine(w, "ID", "Kategori", "Jumlah", "Tanggal", "Catatan")
                    db.expenseDao().getAllExpensesOnce().forEach { e ->
                        writeCsvLine(w, e.id.toString(), e.category, e.amount.toString(), dateFmt.format(Date(e.date)), e.note)
                    }
                }
                ExportType.STOCK -> {
                    writeCsvLine(w, "ID", "Produk", "Tipe", "Qty", "Stok Sebelum", "Stok Sesudah", "Tanggal", "Catatan")
                    db.stockMovementDao().getAllMovementsOnce().forEach { m ->
                        val prod = runCatching { db.productDao().getProductById(m.productId) }.getOrNull()?.name ?: "ID:${m.productId}"
                        writeCsvLine(w, m.id.toString(), prod, m.type, m.qty.toString(), m.beforeStock.toString(), m.afterStock.toString(), dateFmt.format(Date(m.createdAt)), m.note)
                    }
                }
                ExportType.DEBTS -> {
                    writeCsvLine(w, "ID", "Pelanggan", "Jumlah", "Terbayar", "Sisa", "Jatuh Tempo", "Status")
                    db.customerDebtDao().getAllDebtsOnce().forEach { d ->
                        val cust = runCatching { db.customerDao().getCustomerById(d.customerId) }.getOrNull()?.name ?: "ID:${d.customerId}"
                        val due = d.dueDate?.let { dateFmt.format(Date(it)) } ?: ""
                        writeCsvLine(w, d.id.toString(), cust, d.amount.toString(), d.paidAmount.toString(), d.remainingAmount.toString(), due, d.status)
                    }
                }
                ExportType.SUPPLIERS -> {
                    writeCsvLine(w, "ID", "Nama", "Telepon", "Alamat", "Total Utang", "Catatan")
                    db.supplierDao().getAllSuppliersOnce().forEach { s ->
                        writeCsvLine(w, s.id.toString(), s.name, s.phone, s.address, s.totalDebt.toString(), s.note)
                    }
                }
            }
        }
    }

    private suspend fun exportJson(type: ExportType, file: File, start: Long, end: Long) {
        val root = JSONObject()
        val arr = JSONArray()
        root.put(type.name.lowercase(), arr)

        when (type) {
            ExportType.PRODUCTS -> {
                db.productDao().getAllProductsOnce().forEach { p ->
                    arr.put(JSONObject().apply {
                        put("id", p.id); put("name", p.name); put("categoryId", p.categoryId)
                        put("barcode", p.barcode); put("unit", p.unit)
                        put("purchasePrice", p.purchasePrice); put("sellingPrice", p.sellingPrice)
                        put("stock", p.stock); put("minimumStock", p.minimumStock)
                        put("isActive", p.isActive)
                    })
                }
            }
            ExportType.SALES -> {
                db.saleDao().getSalesByDateRangeOnce(start, end).forEach { s ->
                    arr.put(JSONObject().apply {
                        put("invoice", s.invoiceNumber); put("date", s.date)
                        put("subtotal", s.subtotal); put("discount", s.discount)
                        put("total", s.total); put("paymentMethod", s.paymentMethod)
                        put("paymentStatus", s.paymentStatus); put("cashierName", s.cashierName)
                    })
                }
            }
            ExportType.CUSTOMERS -> {
                db.customerDao().getAllCustomersOnce().forEach { c ->
                    arr.put(JSONObject().apply {
                        put("id", c.id); put("name", c.name); put("phone", c.phone)
                        put("address", c.address); put("totalDebt", c.totalDebt); put("note", c.note)
                    })
                }
            }
            ExportType.EXPENSES -> {
                db.expenseDao().getAllExpensesOnce().forEach { e ->
                    arr.put(JSONObject().apply {
                        put("id", e.id); put("category", e.category); put("amount", e.amount)
                        put("date", e.date); put("note", e.note)
                    })
                }
            }
            else -> {
                exportCsv(type, File(file.parentFile, "temp.csv"), start, end)
                file.delete()
            }
        }

        file.writeText(root.toString(2), Charsets.UTF_8)
    }

    private suspend fun exportPdf(type: ExportType, file: File, start: Long, end: Long) {
        val doc = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 10f }

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        var y = 40f

        val storeName = runCatching { settingsRepo.storeName.first() }.getOrNull().let { if (it.isNullOrBlank()) "WarungData POS" else it }
        canvas.drawText(storeName, 40f, y, titlePaint); y += 25f
        canvas.drawText("Laporan ${type.name.lowercase().replaceFirstChar { it.uppercase() }}", 40f, y, headerPaint); y += 15f
        canvas.drawText("Tanggal cetak: ${dateFmt.format(Date())}", 40f, y, textPaint); y += 20f

        val data = mutableListOf<List<String>>()
        val headers: List<String>

        when (type) {
            ExportType.PRODUCTS -> {
                headers = listOf("Nama", "Harga Jual", "Stok", "Min")
                db.productDao().getAllProductsOnce().forEach { p ->
                    data.add(listOf(p.name, numFmt.format(p.sellingPrice), p.stock.toString(), p.minimumStock.toString()))
                }
            }
            ExportType.SALES -> {
                headers = listOf("Invoice", "Total", "Metode", "Status")
                db.saleDao().getSalesByDateRangeOnce(start, end).forEach { s ->
                    data.add(listOf(s.invoiceNumber, numFmt.format(s.total), s.paymentMethod, s.paymentStatus))
                }
            }
            ExportType.CUSTOMERS -> {
                headers = listOf("Nama", "Telepon", "Piutang")
                db.customerDao().getAllCustomersOnce().forEach { c ->
                    data.add(listOf(c.name, c.phone, numFmt.format(c.totalDebt)))
                }
            }
            ExportType.EXPENSES -> {
                headers = listOf("Kategori", "Jumlah", "Tanggal")
                db.expenseDao().getAllExpensesOnce().forEach { e ->
                    data.add(listOf(e.category, numFmt.format(e.amount), dateFmt.format(Date(e.date))))
                }
            }
            else -> {
                headers = listOf("Data", "Tidak tersedia")
                data.add(listOf("PDF hanya untuk Produk, Penjualan, Pelanggan, Pengeluaran", ""))
            }
        }

        canvas.drawLine(40f, y, 555f, y, Paint()); y += 5f
        val colW = 515f / headers.size
        headers.forEachIndexed { i, h ->
            canvas.drawText(h, 40f + i * colW, y, headerPaint)
        }
        y += 15f
        canvas.drawLine(40f, y, 555f, y, Paint()); y += 8f

        for (row in data) {
            row.forEachIndexed { i, v ->
                canvas.drawText(v, 40f + i * colW, y, textPaint)
            }
            y += 14f
            if (y > 800f) { break }
        }

        canvas.drawText("-- WarungData POS --", 200f, y + 20f, textPaint)

        doc.finishPage(page)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    companion object {
        fun getExportDir(context: Context): File {
            val dir = File(context.getExternalFilesDir(null), "exports")
            dir.mkdirs(); return dir
        }
    }
}
