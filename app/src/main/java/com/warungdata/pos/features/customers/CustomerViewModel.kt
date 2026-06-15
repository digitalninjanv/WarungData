package com.warungdata.pos.features.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.CustomerEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomersUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

data class CustomerFormState(
    val id: Int = 0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val note: String = "",
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class CustomersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val customerDao = db.customerDao()

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(CustomerFormState())
    val formState: StateFlow<CustomerFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            customerDao.getAllCustomers().collect { customers ->
                _uiState.value = _uiState.value.copy(
                    customers = customers,
                    isLoading = false
                )
            }
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun startAddCustomer() {
        _formState.value = CustomerFormState()
    }

    fun startEditCustomer(customer: CustomerEntity) {
        _formState.value = CustomerFormState(
            id = customer.id,
            name = customer.name,
            phone = customer.phone,
            address = customer.address,
            note = customer.note,
            isEditing = true
        )
    }

    fun updateName(value: String) {
        _formState.value = _formState.value.copy(name = value, error = null)
    }

    fun updatePhone(value: String) {
        _formState.value = _formState.value.copy(phone = value)
    }

    fun updateAddress(value: String) {
        _formState.value = _formState.value.copy(address = value)
    }

    fun updateNote(value: String) {
        _formState.value = _formState.value.copy(note = value)
    }

    fun saveCustomer() {
        val f = _formState.value
        if (f.name.isBlank()) {
            _formState.value = f.copy(error = "Nama harus diisi")
            return
        }

        _formState.value = f.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val customer = CustomerEntity(
                id = if (f.isEditing) f.id else 0,
                name = f.name,
                phone = f.phone,
                address = f.address,
                note = f.note
            )
            if (f.isEditing) {
                customerDao.updateCustomer(customer)
            } else {
                customerDao.insertCustomer(customer)
            }
            _formState.value = CustomerFormState()
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            customerDao.deleteCustomer(customer)
        }
    }

    fun refreshDebtTotal(customerId: Int) {
        viewModelScope.launch {
            customerDao.refreshDebtTotal(customerId)
        }
    }
}
