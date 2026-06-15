package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val type: String,
    val qty: Int,
    val beforeStock: Int = 0,
    val afterStock: Int = 0,
    val referenceId: Int? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
