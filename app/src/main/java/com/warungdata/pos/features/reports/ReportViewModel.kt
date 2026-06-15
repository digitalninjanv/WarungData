package com.warungdata.pos.features.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class ReportUiState(
    val period: String = "daily",
    val revenue: Long = 0,
    val profit: Long = 0,
    val expenses: Long = 0,
    val transactionCount: Int = 0,
    val debtCollected: Long = 0,
    val isLoading: Boolean = true,
    val startDate: Long = 0,
    val endDate: Long = 0
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        setPeriod("daily")
    }

    fun setPeriod(period: String) {
        val cal = Calendar.getInstance()
        val now = System.currentTimeMillis()
        var start: Long
        var end: Long

        when (period) {
            "daily" -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                start = cal.timeInMillis
                end = start + 86400000 - 1
            }
            "weekly" -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                start = cal.timeInMillis
                end = start + 7 * 86400000 - 1
            }
            "monthly" -> {
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                cal.add(Calendar.DAY_OF_MONTH, -1)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                end = cal.timeInMillis
            }
            else -> {
                start = 0; end = now
            }
        }

        _uiState.value = _uiState.value.copy(period = period, startDate = start, endDate = end, isLoading = true)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                revenue = db.saleDao().getTotalRevenue(start, end),
                transactionCount = db.saleDao().getTransactionCount(start, end),
                profit = db.saleItemDao().getTotalProfit(start, end),
                expenses = db.expenseDao().getTotalExpenses(start, end),
                debtCollected = 0,
                isLoading = false
            )
        }
    }
}
