package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val totalDebt: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
