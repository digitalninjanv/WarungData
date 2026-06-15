package com.warungdata.pos.features.debts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtScreen(
    viewModel: DebtViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val paymentForm by viewModel.paymentForm.collectAsState()
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    val tabs = listOf("Aktif", "Lunas")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Utang") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) }
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val items = if (state.selectedTab == 0) state.activeDebts else state.paidDebts

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.selectedTab == 0) "Tidak ada utang aktif"
                            else "Belum ada utang lunas",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items, key = { it.debt.id }) { debtItem ->
                            DebtCard(
                                debtItem = debtItem,
                                format = format,
                                onClick = {
                                    if (debtItem.debt.status == "active") {
                                        viewModel.showPaymentDialog(debtItem)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (paymentForm.debtId > 0) {
        PaymentDialog(
            paymentForm = paymentForm,
            format = format,
            onUpdateAmount = viewModel::updatePaymentAmount,
            onPay = viewModel::payDebt,
            onSettle = { viewModel.settleDebt(paymentForm.debtId) },
            onDismiss = viewModel::hidePaymentDialog
        )
    }
}

@Composable
private fun DebtCard(
    debtItem: DebtItem,
    format: NumberFormat,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = debtItem.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Rp ${format.format(debtItem.debt.remainingAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (debtItem.debt.status == "active")
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: Rp ${format.format(debtItem.debt.amount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (debtItem.debt.dueDate != null) {
                    Text(
                        text = "Jatuh tempo: ${dateFormat.format(Date(debtItem.debt.dueDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    paymentForm: PaymentFormState,
    format: NumberFormat,
    onUpdateAmount: (String) -> Unit,
    onPay: () -> Unit,
    onSettle: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bayar Utang") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pelanggan: ${paymentForm.customerName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Total Utang: Rp ${format.format(paymentForm.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Sisa: Rp ${format.format(paymentForm.remainingAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                HorizontalDivider()
                OutlinedTextField(
                    value = paymentForm.amount,
                    onValueChange = onUpdateAmount,
                    label = { Text("Jumlah Bayar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (paymentForm.error != null) {
                    Text(
                        text = paymentForm.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSettle,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lunaskan")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onPay,
                enabled = !paymentForm.isSaving
            ) {
                Text("Bayar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
