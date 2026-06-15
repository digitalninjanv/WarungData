package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.BackupHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupHistoryDao {
    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC")
    fun getAllBackups(): Flow<List<BackupHistoryEntity>>

    @Query("SELECT * FROM backup_history ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestBackup(): BackupHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupHistoryEntity): Long

    @Delete
    suspend fun deleteBackup(backup: BackupHistoryEntity)
}
