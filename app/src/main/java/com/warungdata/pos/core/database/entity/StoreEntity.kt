package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stores")
data class StoreEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val ownerName: String,
    val businessType: String = "",
    val currency: String = "IDR",
    val address: String = "",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
