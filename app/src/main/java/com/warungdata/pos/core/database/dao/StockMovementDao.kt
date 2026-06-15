package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsByProduct(productId: Int): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentMovements(limit: Int = 50): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC")
    suspend fun getAllMovementsOnce(): List<StockMovementEntity>

    @Insert
    suspend fun insertMovement(movement: StockMovementEntity): Long
}
