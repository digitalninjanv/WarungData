package com.warungdata.pos.features.suppliers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.SupplierEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SuppliersUiState(
    val suppliers: List<SupplierEntity> = emptyList(),
    val isLoading: Boolean = true
)

data class SupplierFormState(
    val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class SupplierViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val supplierDao = db.supplierDao()

    private val _uiState = MutableStateFlow(SuppliersUiState())
    val uiState: StateFlow<SuppliersUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(SupplierFormState())
    val formState: StateFlow<SupplierFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            supplierDao.getAllSuppliers().collect { suppliers ->
                _uiState.value = SuppliersUiState(
                    suppliers = suppliers,
                    isLoading = false
                )
            }
        }
    }

    fun startAddSupplier() {
        _formState.value = SupplierFormState()
    }

    fun startEditSupplier(supplier: SupplierEntity) {
        _formState.value = SupplierFormState(
            id = supplier.id,
            name = supplier.name,
            phone = supplier.phone,
            address = supplier.address,
            note = supplier.note,
            isEditing = true
        )
    }

    fun updateName(value: String) { _formState.value = _formState.value.copy(name = value, error = null) }
    fun updatePhone(value: String) { _formState.value = _formState.value.copy(phone = value) }
    fun updateAddress(value: String) { _formState.value = _formState.value.copy(address = value) }
    fun updateNote(value: String) { _formState.value = _formState.value.copy(note = value) }

    fun saveSupplier() {
        val f = _formState.value
        if (f.name.isBlank()) {
            _formState.value = f.copy(error = "Nama supplier harus diisi")
            return
        }

        _formState.value = f.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val supplier = SupplierEntity(
                id = if (f.isEditing) f.id else 0,
                name = f.name,
                phone = f.phone,
                address = f.address,
                note = f.note,
                totalDebt = 0
            )
            if (f.isEditing) {
                supplierDao.updateSupplier(supplier)
            } else {
                supplierDao.insertSupplier(supplier)
            }
            _formState.value = SupplierFormState()
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            supplierDao.deleteSupplier(supplier)
        }
    }
}
