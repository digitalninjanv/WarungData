package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val productId: Int,
    val productName: String,
    val qty: Int = 1,
    val purchasePrice: Long = 0,
    val sellingPrice: Long = 0,
    val subtotal: Long = 0,
    val profit: Long = 0
)
