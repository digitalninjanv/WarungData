package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val categoryId: Int? = null,
    val barcode: String = "",
    val unit: String = "pcs",
    val purchasePrice: Long = 0,
    val sellingPrice: Long = 0,
    val stock: Int = 0,
    val minimumStock: Int = 0,
    val expiredDate: Long? = null,
    val imagePath: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val margin: Long get() = if (purchasePrice > 0) ((sellingPrice - purchasePrice) * 100 / purchasePrice) else 0
}
