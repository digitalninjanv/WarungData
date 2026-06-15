package com.warungdata.pos.features.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.ExpenseEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val expenses: List<ExpenseEntity> = emptyList(),
    val totalExpenses: Long = 0,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val isLoading: Boolean = true
)

data class ExpenseFormState(
    val id: Int = 0,
    val category: String = "",
    val amount: String = "",
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

val expenseCategories = listOf(
    "Belanja Stok",
    "Sewa",
    "Listrik",
    "Gaji",
    "Transport",
    "Plastik/Kemasan",
    "Biaya Admin",
    "Lain-lain"
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val expenseDao = db.expenseDao()

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ExpenseFormState())
    val formState: StateFlow<ExpenseFormState> = _formState.asStateFlow()

    init {
        loadExpenses()
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.startDate != null && state.endDate != null) {
                expenseDao.getExpensesByDateRange(state.startDate, state.endDate).collect { expenses ->
                    val total = expenseDao.getTotalExpenses(state.startDate, state.endDate)
                    _uiState.value = ExpensesUiState(
                        expenses = expenses,
                        totalExpenses = total,
                        startDate = state.startDate,
                        endDate = state.endDate,
                        isLoading = false
                    )
                }
            } else {
                expenseDao.getAllExpenses().collect { expenses ->
                    val total = expenses.sumOf { it.amount }
                    _uiState.value = ExpensesUiState(
                        expenses = expenses,
                        totalExpenses = total,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setDateRange(start: Long, end: Long) {
        _uiState.value = _uiState.value.copy(startDate = start, endDate = end)
        loadExpenses()
    }

    fun clearDateRange() {
        _uiState.value = _uiState.value.copy(startDate = null, endDate = null)
        loadExpenses()
    }

    fun startAddExpense() {
        _formState.value = ExpenseFormState()
    }

    fun startEditExpense(expense: ExpenseEntity) {
        _formState.value = ExpenseFormState(
            id = expense.id,
            category = expense.category,
            amount = expense.amount.toString(),
            note = expense.note,
            date = expense.date,
            isEditing = true
        )
    }

    fun updateCategory(value: String) { _formState.value = _formState.value.copy(category = value, error = null) }
    fun updateAmount(value: String) { _formState.value = _formState.value.copy(amount = value, error = null) }
    fun updateNote(value: String) { _formState.value = _formState.value.copy(note = value) }

    fun saveExpense() {
        val f = _formState.value
        if (f.category.isBlank()) {
            _formState.value = f.copy(error = "Kategori harus dipilih")
            return
        }
        val amount = f.amount.toLongOrNull()
        if (amount == null || amount <= 0) {
            _formState.value = f.copy(error = "Jumlah harus diisi dengan angka")
            return
        }

        _formState.value = f.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val expense = ExpenseEntity(
                id = if (f.isEditing) f.id else 0,
                category = f.category,
                amount = amount,
                note = f.note,
                date = f.date
            )
            if (f.isEditing) {
                expenseDao.updateExpense(expense)
            } else {
                expenseDao.insertExpense(expense)
            }
            _formState.value = ExpenseFormState()
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseDao.deleteExpense(expense)
        }
    }
}
