package com.warungdata.pos.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSettings: () -> Unit = {},
    onExport: () -> Unit = {},
    onBackup: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.storeName.ifEmpty { "WarungData POS" },
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (state.ownerName.isNotEmpty()) {
                            Text(
                                state.ownerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text("Pengaturan") }, onClick = { showMenu = false; onSettings() }, leadingIcon = { Icon(Icons.Default.Settings, null) })
                            DropdownMenuItem(text = { Text("Ekspor Data") }, onClick = { showMenu = false; onExport() }, leadingIcon = { Icon(Icons.Default.FileDownload, null) })
                            DropdownMenuItem(text = { Text("Backup & Restore") }, onClick = { showMenu = false; onBackup() }, leadingIcon = { Icon(Icons.Default.Backup, null) })
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = "Omzet",
                        value = "Rp ${format.format(state.todayRevenue)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        icon = Icons.Default.AccountBalance,
                        label = "Laba Kotor",
                        value = "Rp ${format.format(state.todayProfit)}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        icon = Icons.Default.Receipt,
                        label = "Transaksi",
                        value = "${state.todayTransactions}",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        icon = Icons.Default.MoneyOff,
                        label = "Pengeluaran",
                        value = "Rp ${format.format(state.todayExpenses)}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Informasi Lainnya",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardCard(
                        icon = Icons.Default.Warning,
                        label = "Stok Menipis",
                        value = "${state.lowStockCount} Produk",
                        color = if (state.lowStockCount > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardCard(
                        icon = Icons.Default.Groups,
                        label = "Piutang",
                        value = "Rp ${format.format(state.outstandingDebt)}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Laba Bersih Estimasi: Rp ${format.format(state.todayProfit - state.todayExpenses)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DashboardCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
