package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.SupplierDebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDebtDao {
    @Query("SELECT * FROM supplier_debts WHERE supplierId = :supplierId ORDER BY createdAt DESC")
    fun getDebtsBySupplier(supplierId: Int): Flow<List<SupplierDebtEntity>>

    @Query("SELECT * FROM supplier_debts WHERE status = 'active' ORDER BY createdAt DESC")
    fun getActiveDebts(): Flow<List<SupplierDebtEntity>>

    @Query("SELECT * FROM supplier_debts WHERE id = :id")
    suspend fun getDebtById(id: Int): SupplierDebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: SupplierDebtEntity): Long

    @Update
    suspend fun updateDebt(debt: SupplierDebtEntity)

    @Query("UPDATE supplier_debts SET paidAmount = paidAmount + :amount, remainingAmount = remainingAmount - :amount WHERE id = :debtId AND remainingAmount >= :amount")
    suspend fun payDebt(debtId: Int, amount: Long): Int

    @Query("UPDATE supplier_debts SET status = 'paid', remainingAmount = 0 WHERE id = :debtId")
    suspend fun settleDebt(debtId: Int)

    @Query("SELECT COALESCE(SUM(remainingAmount), 0) FROM supplier_debts WHERE status = 'active'")
    fun getTotalOutstandingDebt(): Flow<Long>
}
