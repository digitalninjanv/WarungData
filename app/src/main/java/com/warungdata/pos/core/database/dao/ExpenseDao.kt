package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Int): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getExpensesByDateRange(start: Long, end: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getExpensesByDateRangeOnce(start: Long, end: Long): List<ExpenseEntity>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date BETWEEN :start AND :end")
    suspend fun getTotalExpenses(start: Long, end: Long): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}
