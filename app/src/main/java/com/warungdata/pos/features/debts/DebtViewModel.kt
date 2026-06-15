package com.warungdata.pos.features.debts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.CustomerDebtEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DebtItem(
    val debt: CustomerDebtEntity,
    val customerName: String,
    val customerPhone: String
)

data class DebtUiState(
    val activeDebts: List<DebtItem> = emptyList(),
    val paidDebts: List<DebtItem> = emptyList(),
    val selectedTab: Int = 0,
    val isLoading: Boolean = true
)

data class PaymentFormState(
    val debtId: Int = 0,
    val customerName: String = "",
    val totalAmount: Long = 0,
    val remainingAmount: Long = 0,
    val amount: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class DebtViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val debtDao = db.customerDebtDao()
    private val customerDao = db.customerDao()

    private val _uiState = MutableStateFlow(DebtUiState())
    val uiState: StateFlow<DebtUiState> = _uiState.asStateFlow()

    private val _paymentForm = MutableStateFlow(PaymentFormState())
    val paymentForm: StateFlow<PaymentFormState> = _paymentForm.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                debtDao.getAllDebts(),
                customerDao.getAllCustomers()
            ) { debts, customers ->
                val customerMap = customers.associateBy { it.id }
                val items = debts.map { debt ->
                    DebtItem(
                        debt = debt,
                        customerName = customerMap[debt.customerId]?.name ?: "Unknown",
                        customerPhone = customerMap[debt.customerId]?.phone ?: ""
                    )
                }
                DebtUiState(
                    activeDebts = items.filter { it.debt.status == "active" },
                    paidDebts = items.filter { it.debt.status == "paid" },
                    selectedTab = _uiState.value.selectedTab,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun showPaymentDialog(debtItem: DebtItem) {
        _paymentForm.value = PaymentFormState(
            debtId = debtItem.debt.id,
            customerName = debtItem.customerName,
            totalAmount = debtItem.debt.amount,
            remainingAmount = debtItem.debt.remainingAmount
        )
    }

    fun hidePaymentDialog() {
        _paymentForm.value = PaymentFormState()
    }

    fun updatePaymentAmount(value: String) {
        _paymentForm.value = _paymentForm.value.copy(amount = value, error = null)
    }

    fun payDebt() {
        val f = _paymentForm.value
        val amount = f.amount.toLongOrNull()
        if (amount == null || amount <= 0) {
            _paymentForm.value = f.copy(error = "Jumlah pembayaran tidak valid")
            return
        }
        if (amount > f.remainingAmount) {
            _paymentForm.value = f.copy(error = "Jumlah melebihi sisa utang")
            return
        }

        _paymentForm.value = f.copy(isSaving = true, error = null)

        viewModelScope.launch {
            val updated = debtDao.payDebt(f.debtId, amount)
            if (updated > 0) {
                val debt = debtDao.getDebtById(f.debtId)
                if (debt != null) {
                    if (debt.remainingAmount <= 0) {
                        debtDao.settleDebt(f.debtId)
                    }
                    customerDao.refreshDebtTotal(debt.customerId)
                }
            }
            _paymentForm.value = PaymentFormState()
        }
    }

    fun settleDebt(debtId: Int) {
        viewModelScope.launch {
            val debt = debtDao.getDebtById(debtId)
            debtDao.settleDebt(debtId)
            if (debt != null) {
                customerDao.refreshDebtTotal(debt.customerId)
            }
        }
    }

    fun getDebtsByCustomer(customerId: Int): Flow<List<CustomerDebtEntity>> {
        return debtDao.getDebtsByCustomer(customerId)
    }
}
