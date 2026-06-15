package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_sessions")
data class CashSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val openingBalance: Long = 0,
    val closingBalance: Long? = null,
    val expectedCash: Long? = null,
    val difference: Long? = null,
    val status: String = "open",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
