package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Int): SaleEntity?

    @Query("SELECT * FROM sales WHERE invoiceNumber = :invoice")
    suspend fun getSaleByInvoice(invoice: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getSalesByDateRange(start: Long, end: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getSalesByDateRangeOnce(start: Long, end: Long): List<SaleEntity>

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE date BETWEEN :start AND :end AND paymentStatus = 'paid'")
    suspend fun getTotalRevenue(start: Long, end: Long): Long

    @Query("SELECT COALESCE(COUNT(*), 0) FROM sales WHERE date BETWEEN :start AND :end")
    suspend fun getTransactionCount(start: Long, end: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Delete
    suspend fun deleteSale(sale: SaleEntity)

    @Query("SELECT COALESCE(MAX(id), 0) FROM sales")
    suspend fun getLastSaleId(): Int
}
