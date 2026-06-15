package com.warungdata.pos.features.transactions

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
import com.warungdata.pos.core.database.entity.SaleEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    onBack: () -> Unit = {},
    viewModel: TransactionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Riwayat Transaksi") })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada transaksi", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.transactions, key = { it.id }) { sale ->
                    TransactionCard(sale, format, dateFormat) {
                        viewModel.showDetails(sale)
                    }
                }
            }
        }
    }

    if (state.showDetailsDialog && state.selectedTransaction != null) {
        val sale = state.selectedTransaction!!
        AlertDialog(
            onDismissRequest = viewModel::hideDetails,
            title = { Text("Detail Transaksi - ${sale.invoiceNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tanggal: ${dateFormat.format(Date(sale.date))}")
                    Text("Metode: ${sale.paymentMethod.uppercase()} - ${sale.paymentStatus.uppercase()}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    state.selectedTransactionItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.productName} x${item.qty}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Rp ${format.format(item.subtotal)}")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (sale.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diskon")
                            Text("- Rp ${format.format(sale.discount)}")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text("Rp ${format.format(sale.total)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::hideDetails) {
                    Text("Tutup")
                }
            },
            dismissButton = {
                Button(
                    onClick = viewModel::initiateVoid,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Batalkan Transaksi")
                }
            }
        )
    }

    if (state.showVoidConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelVoid,
            title = { Text("Batalkan Transaksi") },
            text = { Text("Yakin ingin membatalkan transaksi ini? Stok akan dikembalikan dan transaksi dihapus dari laporan (tindakan ini akan dicatat di audit log).") },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmVoid,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Batalkan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelVoid) {
                    Text("Kembali")
                }
            }
        )
    }
}

@Composable
private fun TransactionCard(
    sale: SaleEntity,
    format: NumberFormat,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = sale.invoiceNumber, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = dateFormat.format(Date(sale.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rp ${format.format(sale.total)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = sale.paymentStatus.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sale.paymentStatus == "paid") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
