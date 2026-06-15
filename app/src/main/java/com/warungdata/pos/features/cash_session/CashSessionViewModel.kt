package com.warungdata.pos.features.cash_session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.CashSessionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CashSessionUiState(
    val currentSession: CashSessionEntity? = null,
    val isLoading: Boolean = true,
    val openingCashInput: String = "",
    val closingCashInput: String = "",
    val expectedCash: Long = 0,
    val error: String? = null
)

class CashSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val cashSessionDao = db.cashSessionDao()
    private val saleDao = db.saleDao()

    private val _uiState = MutableStateFlow(CashSessionUiState())
    val uiState: StateFlow<CashSessionUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadSession()
    }

    private fun loadSession() {
        val today = dateFormat.format(Date())
        viewModelScope.launch {
            val session = cashSessionDao.getSessionByDateOnce(today)

            if (session != null && session.status == "open") {
                // Calculate expected cash
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val endOfDay = cal.timeInMillis

                val cashRevenue = saleDao.getSalesByDateRangeOnce(startOfDay, endOfDay)
                    .filter { it.paymentMethod == "cash" && it.paymentStatus == "paid" }
                    .sumOf { it.total }

                val expected = session.openingBalance + cashRevenue

                _uiState.value = _uiState.value.copy(
                    currentSession = session,
                    expectedCash = expected,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    currentSession = session,
                    isLoading = false
                )
            }
        }
    }

    fun updateOpeningCash(amount: String) {
        _uiState.value = _uiState.value.copy(openingCashInput = amount, error = null)
    }

    fun updateClosingCash(amount: String) {
        _uiState.value = _uiState.value.copy(closingCashInput = amount, error = null)
    }

    fun openSession() {
        val state = _uiState.value
        val amount = state.openingCashInput.toLongOrNull()

        if (amount == null) {
            _uiState.value = state.copy(error = "Masukkan jumlah yang valid")
            return
        }

        val today = dateFormat.format(Date())

        viewModelScope.launch {
            val session = CashSessionEntity(
                date = today,
                openingBalance = amount,
                status = "open"
            )
            cashSessionDao.insertSession(session)
            loadSession()
        }
    }

    fun closeSession() {
        val state = _uiState.value
        val amount = state.closingCashInput.toLongOrNull()
        val session = state.currentSession

        if (amount == null || session == null) {
            _uiState.value = state.copy(error = "Masukkan jumlah yang valid")
            return
        }

        viewModelScope.launch {
            val difference = amount - state.expectedCash
            val updatedSession = session.copy(
                closingBalance = amount,
                expectedCash = state.expectedCash,
                difference = difference,
                status = "closed"
            )
            cashSessionDao.updateSession(updatedSession)
            loadSession()
        }
    }
}
