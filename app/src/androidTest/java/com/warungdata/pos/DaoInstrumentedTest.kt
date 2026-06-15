package com.warungdata.pos

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoInstrumentedTest {
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun productDao_insertAndRetrieve() = runBlocking {
        val id = db.productDao().insertProduct(ProductEntity(name = "Kopi", sellingPrice = 5000))
        assertTrue(id > 0)

        val product = db.productDao().getProductById(id.toInt())
        assertNotNull(product)
        assertEquals("Kopi", product?.name)
    }

    @Test
    fun productDao_updateStock() = runBlocking {
        val id = db.productDao().insertProduct(ProductEntity(name = "Gula", stock = 10)).toInt()

        val affected = db.productDao().reduceStock(id, 3)
        assertEquals(1, affected)

        val product = db.productDao().getProductById(id)
        assertEquals(7, product?.stock)
    }

    @Test
    fun productDao_lowStockDetection() = runBlocking {
        db.productDao().insertProduct(ProductEntity(name = "A", stock = 1, minimumStock = 5))
        db.productDao().insertProduct(ProductEntity(name = "B", stock = 10, minimumStock = 5))

        val lowStock = db.productDao().getLowStockCount().first()
        assertEquals(1, lowStock)
    }

    @Test
    fun categoryDao_insertAndGet() = runBlocking {
        val id = db.categoryDao().insertCategory(CategoryEntity(name = "Minuman")).toInt()
        val cat = db.categoryDao().getCategoryById(id)
        assertNotNull(cat)
        assertEquals("Minuman", cat?.name)
    }

    @Test
    fun saleDao_insertAndRevenue() = runBlocking {
        val now = System.currentTimeMillis()
        db.saleDao().insertSale(SaleEntity(
            invoiceNumber = "INV-001", date = now, total = 50000, paymentStatus = "paid"
        ))
        db.saleDao().insertSale(SaleEntity(
            invoiceNumber = "INV-002", date = now, total = 30000, paymentStatus = "paid"
        ))

        val revenue = db.saleDao().getTotalRevenue(now - 1000, now + 1000)
        assertEquals(80000, revenue)
    }

    @Test
    fun customerDao_insertAndDebtTracking() = runBlocking {
        val cid = db.customerDao().insertCustomer(CustomerEntity(name = "Budi")).toInt()
        val customer = db.customerDao().getCustomerById(cid)
        assertNotNull(customer)
        assertEquals("Budi", customer?.name)
    }

    @Test
    fun expenseDao_insertAndCalculateTotal() = runBlocking {
        val now = System.currentTimeMillis()
        db.expenseDao().insertExpense(ExpenseEntity(category = "Listrik", amount = 100000, date = now))
        db.expenseDao().insertExpense(ExpenseEntity(category = "Air", amount = 50000, date = now))

        val total = db.expenseDao().getTotalExpenses(now - 1000, now + 1000)
        assertEquals(150000, total)
    }

    @Test
    fun stockMovementDao_insertAndList() = runBlocking {
        db.stockMovementDao().insertMovement(StockMovementEntity(productId = 1, type = "IN", qty = 10))
        db.stockMovementDao().insertMovement(StockMovementEntity(productId = 1, type = "OUT", qty = 3))

        val movements = db.stockMovementDao().getRecentMovements(10).first()
        assertEquals(2, movements.size)
    }

    @Test
    fun supplierDao_insert() = runBlocking {
        val id = db.supplierDao().insertSupplier(SupplierEntity(name = "PT ABC")).toInt()
        val s = db.supplierDao().getSupplierById(id)
        assertEquals("PT ABC", s?.name)
    }

    @Test
    fun debtDao_fullCycle() = runBlocking {
        db.customerDebtDao().insertDebt(CustomerDebtEntity(customerId = 1, amount = 100000, remainingAmount = 100000))
        db.customerDebtDao().insertDebt(CustomerDebtEntity(customerId = 1, amount = 50000, remainingAmount = 50000))

        val outstanding = db.customerDebtDao().getTotalOutstandingDebt().first()
        assertEquals(150000, outstanding)
    }
}
