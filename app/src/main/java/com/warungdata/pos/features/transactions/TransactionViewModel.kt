package com.warungdata.pos.features.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.database.AppDatabase
import com.warungdata.pos.core.database.entity.AuditLogEntity
import com.warungdata.pos.core.database.entity.SaleEntity
import com.warungdata.pos.core.database.entity.SaleItemEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionUiState(
    val transactions: List<SaleEntity> = emptyList(),
    val isLoading: Boolean = true,
    val selectedTransaction: SaleEntity? = null,
    val selectedTransactionItems: List<SaleItemEntity> = emptyList(),
    val showDetailsDialog: Boolean = false,
    val showVoidConfirm: Boolean = false
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val saleDao = db.saleDao()
    private val saleItemDao = db.saleItemDao()
    private val productDao = db.productDao()
    private val auditLogDao = db.auditLogDao()

    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            saleDao.getAllSales().collect { sales ->
                _uiState.value = _uiState.value.copy(
                    transactions = sales,
                    isLoading = false
                )
            }
        }
    }

    fun showDetails(sale: SaleEntity) {
        viewModelScope.launch {
            val items = saleItemDao.getItemsBySaleIdOnce(sale.id)
            _uiState.value = _uiState.value.copy(
                selectedTransaction = sale,
                selectedTransactionItems = items,
                showDetailsDialog = true
            )
        }
    }

    fun hideDetails() {
        _uiState.value = _uiState.value.copy(
            showDetailsDialog = false,
            selectedTransaction = null,
            selectedTransactionItems = emptyList()
        )
    }

    fun initiateVoid() {
        _uiState.value = _uiState.value.copy(showVoidConfirm = true, showDetailsDialog = false)
    }

    fun cancelVoid() {
        _uiState.value = _uiState.value.copy(showVoidConfirm = false, showDetailsDialog = true)
    }

    fun confirmVoid() {
        val sale = _uiState.value.selectedTransaction ?: return
        val items = _uiState.value.selectedTransactionItems

        viewModelScope.launch {
            // Restore stock
            items.forEach { item ->
                productDao.addStock(item.productId, item.qty)
            }

            // Delete sale and log
            saleDao.deleteSale(sale)

            auditLogDao.insertLog(
                AuditLogEntity(
                    action = "VOID_TRANSACTION",
                    entityType = "SaleEntity",
                    entityId = sale.id,
                    detail = "Voided invoice: ${sale.invoiceNumber}, Restored stock for ${items.size} items."
                )
            )

            _uiState.value = _uiState.value.copy(
                showVoidConfirm = false,
                selectedTransaction = null,
                selectedTransactionItems = emptyList()
            )
        }
    }
}
