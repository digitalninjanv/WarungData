package com.warungdata.pos.features.stock

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.ProductEntity
import com.warungdata.pos.core.database.entity.StockMovementEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StockUiState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val showLowStockOnly: Boolean = false,
    val isLoading: Boolean = true
)

data class StockAdjustmentState(
    val isVisible: Boolean = false,
    val product: ProductEntity? = null,
    val type: String = "IN",
    val qty: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class StockViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val productDao = db.productDao()
    private val stockMovementDao = db.stockMovementDao()

    private val _uiState = MutableStateFlow(StockUiState())
    val uiState: StateFlow<StockUiState> = _uiState.asStateFlow()

    private val _adjustmentState = MutableStateFlow(StockAdjustmentState())
    val adjustmentState: StateFlow<StockAdjustmentState> = _adjustmentState.asStateFlow()

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

    fun toggleLowStockFilter() {
        _uiState.value = _uiState.value.copy(
            showLowStockOnly = !_uiState.value.showLowStockOnly
        )
    }

    fun showAdjustment(product: ProductEntity) {
        _adjustmentState.value = StockAdjustmentState(
            isVisible = true,
            product = product,
            type = "IN"
        )
    }

    fun hideAdjustment() {
        _adjustmentState.value = StockAdjustmentState()
    }

    fun updateAdjustmentType(type: String) {
        _adjustmentState.value = _adjustmentState.value.copy(type = type, error = null)
    }

    fun updateAdjustmentQty(qty: String) {
        _adjustmentState.value = _adjustmentState.value.copy(qty = qty, error = null)
    }

    fun updateAdjustmentNote(note: String) {
        _adjustmentState.value = _adjustmentState.value.copy(note = note)
    }

    fun saveAdjustment() {
        val state = _adjustmentState.value
        val product = state.product ?: return
        val qty = state.qty.toIntOrNull()

        if (qty == null || qty <= 0) {
            _adjustmentState.value = state.copy(error = "Jumlah harus lebih dari 0")
            return
        }

        _adjustmentState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val currentProduct = productDao.getProductById(product.id) ?: return@launch
            val beforeStock = currentProduct.stock
            val afterStock: Int

            when (state.type) {
                "IN" -> {
                    productDao.addStock(product.id, qty)
                    afterStock = beforeStock + qty
                }
                "ADJUSTMENT" -> {
                    productDao.updateProduct(currentProduct.copy(stock = qty))
                    afterStock = qty
                }
                else -> {
                    val result = productDao.reduceStock(product.id, qty)
                    if (result == 0) {
                        _adjustmentState.value = _adjustmentState.value.copy(
                            isSaving = false,
                            error = "Stok tidak mencukupi"
                        )
                        return@launch
                    }
                    afterStock = beforeStock - qty
                }
            }

            stockMovementDao.insertMovement(
                StockMovementEntity(
                    productId = product.id,
                    type = state.type,
                    qty = qty,
                    beforeStock = beforeStock,
                    afterStock = afterStock,
                    note = state.note
                )
            )

            _adjustmentState.value = StockAdjustmentState()
        }
    }
}
