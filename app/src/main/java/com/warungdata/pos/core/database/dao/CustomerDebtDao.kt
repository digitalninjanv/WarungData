package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.CustomerDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDebtDao {
    @Query("SELECT * FROM customer_debts WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getDebtsByCustomer(customerId: Int): Flow<List<CustomerDebtEntity>>

    @Query("SELECT * FROM customer_debts WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveDebts(): Flow<List<CustomerDebtEntity>>

    @Query("SELECT * FROM customer_debts ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<CustomerDebtEntity>>

    @Query("SELECT * FROM customer_debts ORDER BY createdAt DESC")
    suspend fun getAllDebtsOnce(): List<CustomerDebtEntity>

    @Query("SELECT * FROM customer_debts WHERE id = :id")
    suspend fun getDebtById(id: Int): CustomerDebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: CustomerDebtEntity): Long

    @Update
    suspend fun updateDebt(debt: CustomerDebtEntity)

    @Query("UPDATE customer_debts SET paidAmount = paidAmount + :amount, remainingAmount = remainingAmount - :amount WHERE id = :debtId AND remainingAmount >= :amount")
    suspend fun payDebt(debtId: Int, amount: Long): Int

    @Query("UPDATE customer_debts SET status = 'paid', remainingAmount = 0 WHERE id = :debtId")
    suspend fun settleDebt(debtId: Int)

    @Query("SELECT COALESCE(SUM(remainingAmount), 0) FROM customer_debts WHERE status = 'active'")
    fun getTotalOutstandingDebt(): Flow<Long>

    @Query("SELECT COALESCE(COUNT(*), 0) FROM customer_debts WHERE status = 'active'")
    fun getActiveDebtCount(): Flow<Int>
}
