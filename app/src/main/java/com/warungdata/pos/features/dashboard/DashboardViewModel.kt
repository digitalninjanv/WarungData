package com.warungdata.pos.features.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DashboardUiState(
    val storeName: String = "",
    val ownerName: String = "",
    val todayRevenue: Long = 0,
    val todayTransactions: Int = 0,
    val todayProfit: Long = 0,
    val todayExpenses: Long = 0,
    val outstandingDebt: Long = 0,
    val lowStockCount: Int = 0,
    val isLoading: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val settingsRepo = SettingsRepository(application)
    private val cal = Calendar.getInstance()

    init {
        viewModelScope.launch {
            val startOfDay = getStartOfDay()
            val endOfDay = getEndOfDay()

            val revenue = db.saleDao().getTotalRevenue(startOfDay, endOfDay)
            val count = db.saleDao().getTransactionCount(startOfDay, endOfDay)
            val profit = db.saleItemDao().getTotalProfit(startOfDay, endOfDay)
            val expenses = db.expenseDao().getTotalExpenses(startOfDay, endOfDay)
            val debt = db.customerDebtDao().getTotalOutstandingDebt().first()
            val lowStock = db.productDao().getLowStockCount().first()
            val storeName = settingsRepo.storeName.first()
            val ownerName = settingsRepo.ownerName.first()

            _uiState.value = DashboardUiState(
                storeName = storeName,
                ownerName = ownerName,
                todayRevenue = revenue,
                todayTransactions = count,
                todayProfit = profit,
                todayExpenses = expenses,
                outstandingDebt = debt,
                lowStockCount = lowStock,
                isLoading = false
            )
        }
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val startOfDay = getStartOfDay()
            val endOfDay = getEndOfDay()

            _uiState.value = _uiState.value.copy(
                todayRevenue = db.saleDao().getTotalRevenue(startOfDay, endOfDay),
                todayTransactions = db.saleDao().getTransactionCount(startOfDay, endOfDay),
                todayProfit = db.saleItemDao().getTotalProfit(startOfDay, endOfDay),
                todayExpenses = db.expenseDao().getTotalExpenses(startOfDay, endOfDay),
                outstandingDebt = db.customerDebtDao().getTotalOutstandingDebt().first(),
                lowStockCount = db.productDao().getLowStockCount().first(),
                isLoading = false
            )
        }
    }

    private fun getStartOfDay(): Long {
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(): Long {
        cal.timeInMillis = System.currentTimeMillis()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
