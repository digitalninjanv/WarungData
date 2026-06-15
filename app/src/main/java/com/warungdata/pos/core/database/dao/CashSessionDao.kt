package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.CashSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashSessionDao {
    @Query("SELECT * FROM cash_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<CashSessionEntity>>

    @Query("SELECT * FROM cash_sessions WHERE date = :date AND status = 'open' LIMIT 1")
    suspend fun getOpenSessionByDate(date: String): CashSessionEntity?

    @Query("SELECT * FROM cash_sessions WHERE id = :id")
    suspend fun getSessionById(id: Int): CashSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CashSessionEntity): Long

    @Update
    suspend fun updateSession(session: CashSessionEntity)
}
