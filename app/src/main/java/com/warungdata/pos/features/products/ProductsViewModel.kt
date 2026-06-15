package com.warungdata.pos.features.products

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.CategoryEntity
import com.warungdata.pos.core.database.entity.ProductEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProductsUiState(
    val products: List<ProductEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val isLoading: Boolean = true
)

data class ProductFormState(
    val id: Int = 0,
    val name: String = "",
    val categoryId: Int? = null,
    val barcode: String = "",
    val unit: String = "pcs",
    val purchasePrice: String = "",
    val sellingPrice: String = "",
    val stock: String = "0",
    val minimumStock: String = "0",
    val notes: String = "",
    val isActive: Boolean = true,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class ProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val productDao = db.productDao()
    private val categoryDao = db.categoryDao()

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ProductFormState())
    val formState: StateFlow<ProductFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                productDao.getActiveProducts(),
                categoryDao.getAllCategories()
            ) { products, categories ->
                ProductsUiState(
                    products = products,
                    categories = categories,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun filterByCategory(categoryId: Int?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun startAddProduct() {
        _formState.value = ProductFormState()
    }

    fun startEditProduct(product: ProductEntity) {
        _formState.value = ProductFormState(
            id = product.id,
            name = product.name,
            categoryId = product.categoryId,
            barcode = product.barcode,
            unit = product.unit,
            purchasePrice = if (product.purchasePrice > 0) product.purchasePrice.toString() else "",
            sellingPrice = if (product.sellingPrice > 0) product.sellingPrice.toString() else "",
            stock = product.stock.toString(),
            minimumStock = product.minimumStock.toString(),
            notes = product.notes,
            isActive = product.isActive,
            isEditing = true
        )
    }

    fun updateName(value: String) { _formState.value = _formState.value.copy(name = value, error = null) }
    fun updateCategory(value: Int?) { _formState.value = _formState.value.copy(categoryId = value) }
    fun updateBarcode(value: String) { _formState.value = _formState.value.copy(barcode = value) }
    fun updateUnit(value: String) { _formState.value = _formState.value.copy(unit = value) }
    fun updatePurchasePrice(value: String) { _formState.value = _formState.value.copy(purchasePrice = value) }
    fun updateSellingPrice(value: String) { _formState.value = _formState.value.copy(sellingPrice = value) }
    fun updateStock(value: String) { _formState.value = _formState.value.copy(stock = value) }
    fun updateMinStock(value: String) { _formState.value = _formState.value.copy(minimumStock = value) }
    fun updateNotes(value: String) { _formState.value = _formState.value.copy(notes = value) }

    fun saveProduct() {
        val f = _formState.value
        if (f.name.isBlank()) {
            _formState.value = f.copy(error = "Nama produk harus diisi")
            return
        }
        val purchase = f.purchasePrice.toLongOrNull() ?: 0
        val selling = f.sellingPrice.toLongOrNull() ?: 0
        if (selling <= 0) {
            _formState.value = f.copy(error = "Harga jual harus diisi")
            return
        }

        _formState.value = f.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val product = ProductEntity(
                id = if (f.isEditing) f.id else 0,
                name = f.name,
                categoryId = f.categoryId,
                barcode = f.barcode,
                unit = f.unit,
                purchasePrice = purchase,
                sellingPrice = selling,
                stock = f.stock.toIntOrNull() ?: 0,
                minimumStock = f.minimumStock.toIntOrNull() ?: 0,
                notes = f.notes,
                isActive = f.isActive
            )
            if (f.isEditing) {
                productDao.updateProduct(product)
            } else {
                productDao.insertProduct(product)
            }
            _formState.value = ProductFormState()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            productDao.deleteProduct(product)
        }
    }

    fun toggleActive(product: ProductEntity) {
        viewModelScope.launch {
            productDao.updateProduct(product.copy(isActive = !product.isActive))
        }
    }
}
