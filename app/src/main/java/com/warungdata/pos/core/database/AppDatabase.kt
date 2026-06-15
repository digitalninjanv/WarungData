package com.warungdata.pos.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.warungdata.pos.core.database.dao.*
import com.warungdata.pos.core.database.entity.*

@Database(
    entities = [
        StoreEntity::class,
        UserEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        CustomerEntity::class,
        CustomerDebtEntity::class,
        SupplierEntity::class,
        SupplierDebtEntity::class,
        StockMovementEntity::class,
        ExpenseEntity::class,
        CashSessionEntity::class,
        BackupHistoryEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerDebtDao(): CustomerDebtDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierDebtDao(): SupplierDebtDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun cashSessionDao(): CashSessionDao
    abstract fun backupHistoryDao(): BackupHistoryDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        private const val DATABASE_NAME = "warungdata.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
