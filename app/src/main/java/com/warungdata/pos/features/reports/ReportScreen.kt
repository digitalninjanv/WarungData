package com.warungdata.pos.features.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }
    val dateFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Laporan") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period selector
            Text("Periode", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("daily" to "Harian", "weekly" to "Mingguan", "monthly" to "Bulanan").forEach { (key, label) ->
                    FilterChip(
                        selected = state.period == key,
                        onClick = { viewModel.setPeriod(key) },
                        label = { Text(label) }
                    )
                }
            }

            Text(
                text = dateFormat.format(Date(state.startDate)) + " - " + dateFormat.format(Date(state.endDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Summary cards
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportRow("Omzet", "Rp ${format.format(state.revenue)}", MaterialTheme.colorScheme.primary)
                        ReportRow("Laba Kotor", "Rp ${format.format(state.profit)}", MaterialTheme.colorScheme.tertiary)
                        ReportRow("Pengeluaran", "Rp ${format.format(state.expenses)}", MaterialTheme.colorScheme.error)
                        HorizontalDivider()
                        ReportRow(
                            "Laba Bersih",
                            "Rp ${format.format(state.profit - state.expenses)}",
                            MaterialTheme.colorScheme.primary,
                            bold = true
                        )
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReportRow("Total Transaksi", "${state.transactionCount}", MaterialTheme.colorScheme.secondary)
                        ReportRow("Rata-rata per Transaksi", "Rp ${format.format(if (state.transactionCount > 0) state.revenue / state.transactionCount else 0)}", MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = color
        )
    }
}
