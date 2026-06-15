package com.warungdata.pos.core.database.dao

import androidx.room.*
import com.warungdata.pos.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock <= minimumStock AND isActive = 1 ORDER BY stock ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE stock <= 0 AND isActive = 1")
    fun getOutOfStockProducts(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE stock <= minimumStock AND isActive = 1")
    fun getLowStockCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stock = stock + :qty WHERE id = :productId")
    suspend fun addStock(productId: Int, qty: Int)

    @Query("UPDATE products SET stock = stock - :qty WHERE id = :productId AND stock >= :qty")
    suspend fun reduceStock(productId: Int, qty: Int): Int

    @Query("SELECT SUM(stock * sellingPrice) FROM products WHERE isActive = 1")
    suspend fun getTotalInventoryValue(): Long

    @Query("SELECT * FROM products WHERE expiredDate IS NOT NULL AND expiredDate <= :maxDate AND isActive = 1")
    fun getExpiringProducts(maxDate: Long): Flow<List<ProductEntity>>
}
