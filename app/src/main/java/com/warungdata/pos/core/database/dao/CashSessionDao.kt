package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.CashSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashSessionDao {
    @Query("SELECT * FROM cash_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<CashSessionEntity>>

    @Query("SELECT * FROM cash_sessions WHERE date = :date LIMIT 1")
    suspend fun getSessionByDateOnce(date: String): CashSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashSessionEntity): Long

    @Update
    suspend fun updateSession(session: CashSessionEntity)
}
