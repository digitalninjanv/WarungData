package com.warungdata.pos

import com.warungdata.pos.core.database.entity.*
import org.junit.Test
import org.junit.Assert.*

class DatabaseUnitTest {
    @Test
    fun productEntity_margin_calculatedCorrectly() {
        val p = ProductEntity(name = "Test", purchasePrice = 1000, sellingPrice = 1500)
        assertEquals(50, p.margin)
    }

    @Test
    fun productEntity_zeroPurchasePrice_returnsZeroMargin() {
        val p = ProductEntity(name = "Test", purchasePrice = 0, sellingPrice = 1500)
        assertEquals(0, p.margin)
    }

    @Test
    fun productEntity_defaultValues() {
        val p = ProductEntity(name = "Test Barang")
        assertEquals("pcs", p.unit)
        assertEquals(0, p.stock)
        assertEquals(0, p.minimumStock)
        assertTrue(p.isActive)
    }

    @Test
    fun saleEntity_defaultValues() {
        val s = SaleEntity(invoiceNumber = "INV-001")
        assertEquals("cash", s.paymentMethod)
        assertEquals("paid", s.paymentStatus)
        assertEquals(0, s.total)
    }

    @Test
    fun customerDebtEntity_defaultStatus_isActive() {
        val d = CustomerDebtEntity(customerId = 1, amount = 100000)
        assertEquals("active", d.status)
        assertEquals(0, d.remainingAmount)
    }

    @Test
    fun storeEntity_hasFixedId() {
        val s = StoreEntity(name = "Toko", ownerName = "Owner")
        assertEquals(1, s.id)
    }

    @Test
    fun backupHistoryEntity_creation() {
        val b = BackupHistoryEntity(filePath = "/tmp/test.zip", fileSize = 1024, type = "manual")
        assertEquals("manual", b.type)
        assertTrue(b.createdAt > 0)
    }

    @Test
    fun stockMovementEntity_types() {
        val types = listOf("IN", "OUT", "DAMAGED", "LOST", "ADJUSTMENT")
        types.forEach { type ->
            val m = StockMovementEntity(productId = 1, type = type, qty = 5)
            assertEquals(type, m.type)
        }
    }

    @Test
    fun userEntity_defaultRole_isOwner() {
        val u = UserEntity(name = "Admin")
        assertEquals("owner", u.role)
        assertTrue(u.isActive)
    }

    @Test
    fun expenseEntity_defaultValues() {
        val e = ExpenseEntity(category = "Listrik", amount = 50000)
        assertTrue(e.createdAt > 0)
        assertEquals("", e.note)
    }

    @Test
    fun supplierEntity_creation() {
        val s = SupplierEntity(name = "PT Supplier")
        assertEquals(0, s.totalDebt)
        assertEquals("", s.phone)
    }

    @Test
    fun saleItemEntity_profitCalculation() {
        val item = SaleItemEntity(saleId = 1, productId = 1, productName = "Test",
            qty = 2, purchasePrice = 1000, sellingPrice = 1500, subtotal = 3000)
        assertEquals(2, item.qty)
        assertEquals(1500, item.sellingPrice)
    }

    @Test
    fun categoryEntity_creation() {
        val c = CategoryEntity(name = "Minuman")
        assertEquals("Minuman", c.name)
        assertTrue(c.createdAt > 0)
    }
}
