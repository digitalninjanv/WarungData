package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_debts")
data class SupplierDebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val supplierId: Int,
    val amount: Long = 0,
    val paidAmount: Long = 0,
    val remainingAmount: Long = 0,
    val dueDate: Long? = null,
    val status: String = "active",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
