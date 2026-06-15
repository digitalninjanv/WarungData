package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_history")
data class BackupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filePath: String,
    val fileSize: Long = 0,
    val type: String = "manual",
    val checksum: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
