package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Insert
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_logs WHERE createdAt < :before")
    suspend fun deleteOldLogs(before: Long)
}
