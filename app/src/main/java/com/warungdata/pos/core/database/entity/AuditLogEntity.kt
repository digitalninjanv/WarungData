package com.warungdata.pos.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val entityType: String = "",
    val entityId: Int? = null,
    val userId: Int? = null,
    val userName: String = "",
    val detail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
