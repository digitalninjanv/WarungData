package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String = "owner",
    val pinHash: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
