package com.warungdata.pos.features.cash_session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashSessionScreen(
    onBack: () -> Unit = {},
    viewModel: CashSessionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kasir Shift") })
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val session = state.currentSession

                if (session == null || session.status == "closed") {
                    // Open Shift
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Buka Shift Kasir", style = MaterialTheme.typography.titleLarge)
                            if (session?.status == "closed") {
                                Text(
                                    "Shift hari ini sudah ditutup. Anda bisa membuka shift baru atau mengedit yang ada.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedTextField(
                                value = state.openingCashInput,
                                onValueChange = viewModel::updateOpeningCash,
                                label = { Text("Uang Modal Awal (Kas)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.error != null
                            )
                            if (state.error != null) {
                                Text(
                                    text = state.error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = viewModel::openSession,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Buka Shift")
                            }
                        }
                    }
                } else {
                    // Close Shift
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Tutup Shift Kasir", style = MaterialTheme.typography.titleLarge)

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Modal Awal:")
                                Text("Rp ${format.format(session.openingBalance)}")
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Estimasi Kas di Laci:")
                                Text(
                                    "Rp ${format.format(state.expectedCash)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            OutlinedTextField(
                                value = state.closingCashInput,
                                onValueChange = viewModel::updateClosingCash,
                                label = { Text("Uang Fisik Aktual") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                isError = state.error != null
                            )

                            val closingNum = state.closingCashInput.toLongOrNull()
                            if (closingNum != null) {
                                val diff = closingNum - state.expectedCash
                                val diffColor = if (diff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Selisih:")
                                    Text("Rp ${format.format(diff)}", color = diffColor, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (state.error != null) {
                                Text(
                                    text = state.error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = viewModel::closeSession,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Tutup Shift")
                            }
                        }
                    }
                }
            }
        }
    }
}
