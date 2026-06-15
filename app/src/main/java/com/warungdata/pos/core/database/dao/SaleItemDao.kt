package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.SaleItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsBySaleId(saleId: Int): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsBySaleIdOnce(saleId: Int): List<SaleItemEntity>

    @Insert
    suspend fun insertItem(item: SaleItemEntity): Long

    @Insert
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Query("SELECT productId, SUM(qty) as qty, SUM(profit) as profit FROM sale_items GROUP BY productId ORDER BY qty DESC LIMIT :limit")
    suspend fun getBestSellers(limit: Int = 10): List<BestSellerResult>

    @Query("SELECT COALESCE(SUM(profit), 0) FROM sale_items WHERE saleId IN (SELECT id FROM sales WHERE date BETWEEN :start AND :end)")
    suspend fun getTotalProfit(start: Long, end: Long): Long

    data class BestSellerResult(
        val productId: Int,
        val qty: Int,
        val profit: Long
    )
}
