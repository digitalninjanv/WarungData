package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val date: Long = System.currentTimeMillis(),
    val subtotal: Long = 0,
    val discount: Long = 0,
    val total: Long = 0,
    val paymentMethod: String = "cash",
    val paymentStatus: String = "paid",
    val customerId: Int? = null,
    val cashierName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
