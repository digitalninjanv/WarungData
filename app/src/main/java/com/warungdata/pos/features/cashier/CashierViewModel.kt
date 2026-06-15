package com.warungdata.pos.features.cashier

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: ProductEntity,
    val qty: Int = 1,
    val subtotal: Long = product.sellingPrice
)

data class CashierUiState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val cart: List<CartItem> = emptyList(),
    val subtotal: Long = 0,
    val discount: Long = 0,
    val total: Long = 0,
    val paymentMethod: String = "cash",
    val paymentStatus: String = "paid",
    val customerId: Int? = null,
    val note: String = "",
    val showPaymentDialog: Boolean = false,
    val showSuccess: Boolean = false,
    val isLoading: Boolean = true,
    val invoiceNumber: String = ""
)

class CashierViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val productDao = db.productDao()
    private val saleDao = db.saleDao()
    private val saleItemDao = db.saleItemDao()

    private val _uiState = MutableStateFlow(CashierUiState())
    val uiState: StateFlow<CashierUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            productDao.getActiveProducts().collect { products ->
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false
                )
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun addToCart(product: ProductEntity) {
        val currentCart = _uiState.value.cart.toMutableList()
        val existingIndex = currentCart.indexOfFirst { it.product.id == product.id }

        if (existingIndex >= 0) {
            val item = currentCart[existingIndex]
            currentCart[existingIndex] = item.copy(qty = item.qty + 1)
        } else {
            currentCart.add(CartItem(product = product))
        }
        updateCartTotals(currentCart)
    }

    fun updateQty(productId: Int, qty: Int) {
        if (qty <= 0) return
        val currentCart = _uiState.value.cart.toMutableList()
        val index = currentCart.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentCart[index] = currentCart[index].copy(qty = qty)
            updateCartTotals(currentCart)
        }
    }

    fun removeFromCart(productId: Int) {
        val currentCart = _uiState.value.cart.filter { it.product.id != productId }
        _uiState.value = _uiState.value.copy(cart = currentCart)
        updateCartTotals(currentCart)
    }

    fun updateDiscount(discount: Long) {
        updateCartTotals(_uiState.value.cart, discount)
    }

    fun setPaymentMethod(method: String) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }

    fun setCustomerId(id: Int?) {
        _uiState.value = _uiState.value.copy(customerId = id)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun showPayment() {
        _uiState.value = _uiState.value.copy(showPaymentDialog = true)
    }

    fun hidePayment() {
        _uiState.value = _uiState.value.copy(showPaymentDialog = false)
    }

    fun checkout() {
        val state = _uiState.value
        if (state.cart.isEmpty()) return

        viewModelScope.launch {
            val lastId = saleDao.getLastSaleId()
            val invoiceNumber = generateInvoice(lastId + 1)
            val now = System.currentTimeMillis()

            val sale = SaleEntity(
                invoiceNumber = invoiceNumber,
                date = now,
                subtotal = state.subtotal,
                discount = state.discount,
                total = state.total,
                paymentMethod = state.paymentMethod,
                paymentStatus = if (state.paymentMethod == "utang") "unpaid" else "paid",
                customerId = state.customerId,
                note = state.note,
                createdAt = now
            )

            val saleId = saleDao.insertSale(sale).toInt()

            val items = state.cart.map { cartItem ->
                SaleItemEntity(
                    saleId = saleId,
                    productId = cartItem.product.id,
                    productName = cartItem.product.name,
                    qty = cartItem.qty,
                    purchasePrice = cartItem.product.purchasePrice,
                    sellingPrice = cartItem.product.sellingPrice,
                    subtotal = cartItem.subtotal,
                    profit = (cartItem.product.sellingPrice - cartItem.product.purchasePrice) * cartItem.qty
                )
            }
            saleItemDao.insertItems(items)

            // Reduce stock for each item
            state.cart.forEach { cartItem ->
                productDao.addStock(cartItem.product.id, -cartItem.qty)
            }

            _uiState.value = _uiState.value.copy(
                cart = emptyList(),
                subtotal = 0,
                discount = 0,
                total = 0,
                showPaymentDialog = false,
                showSuccess = true,
                invoiceNumber = invoiceNumber
            )
        }
    }

    fun dismissSuccess() {
        _uiState.value = _uiState.value.copy(showSuccess = false, invoiceNumber = "")
    }

    private fun updateCartTotals(cart: List<CartItem>, discount: Long = _uiState.value.discount) {
        val subtotal = cart.sumOf { it.product.sellingPrice * it.qty }
        val total = maxOf(0, subtotal - discount)
        _uiState.value = _uiState.value.copy(
            cart = cart,
            subtotal = subtotal,
            discount = discount,
            total = total
        )
    }

    private fun generateInvoice(lastId: Int): String {
        val datePart = java.text.SimpleDateFormat("yyMMdd", java.util.Locale("id", "ID"))
            .format(java.util.Date())
        return "WD-$datePart-${lastId.toString().padStart(4, '0')}"
    }
}
