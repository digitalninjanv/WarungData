package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    suspend fun getAllSuppliersOnce(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getSupplierById(id: Int): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("UPDATE suppliers SET totalDebt = (SELECT COALESCE(SUM(remainingAmount), 0) FROM supplier_debts WHERE supplierId = :supplierId AND status = 'active') WHERE id = :supplierId")
    suspend fun refreshDebtTotal(supplierId: Int)
}
