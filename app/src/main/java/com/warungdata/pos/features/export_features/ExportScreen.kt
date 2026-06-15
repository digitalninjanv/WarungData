package com.warungdata.pos.features.export_features

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.export.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class ExportUiState(
    val isLoading: Boolean = false,
    val result: ExportResult? = null,
    val error: String? = null
)

class ExportViewModel(application: Application) : AndroidViewModel(application) {
    private val exportService = ExportService(application)

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    val exportTypes = listOf(
        ExportType.PRODUCTS to "Produk",
        ExportType.SALES to "Penjualan",
        ExportType.CUSTOMERS to "Pelanggan",
        ExportType.EXPENSES to "Pengeluaran",
        ExportType.STOCK to "Stok",
        ExportType.DEBTS to "Piutang",
        ExportType.SUPPLIERS to "Supplier"
    )

    fun export(type: ExportType, format: ExportFormat) {
        _uiState.value = ExportUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val result = exportService.export(type, format)
                _uiState.value = ExportUiState(result = result)
            } catch (e: Exception) {
                _uiState.value = ExportUiState(error = e.message ?: "Gagal mengekspor")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    onBack: () -> Unit,
    viewModel: ExportViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ekspor Data") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Pilih data yang akan diekspor:", style = MaterialTheme.typography.bodyLarge)

            viewModel.exportTypes.forEach { (type, label) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.export(type, ExportFormat.CSV) },
                                enabled = !state.isLoading
                            ) { Text("CSV") }
                            OutlinedButton(
                                onClick = { viewModel.export(type, ExportFormat.JSON) },
                                enabled = !state.isLoading
                            ) { Text("JSON") }
                            OutlinedButton(
                                onClick = { viewModel.export(type, ExportFormat.PDF) },
                                enabled = !state.isLoading
                            ) { Text("PDF") }
                        }
                    }
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Mengekspor data...", style = MaterialTheme.typography.bodySmall)
            }

            state.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(err, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            state.result?.let { res ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ekspor Berhasil!", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text("File: ${res.file.name}")
                        Text("Ukuran: ${res.size} bytes")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", res.file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, when (res.format) {
                                        ExportFormat.CSV -> "text/csv"
                                        ExportFormat.JSON -> "application/json"
                                        ExportFormat.PDF -> "application/pdf"
                                    })
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            }) { Text("Buka File") }
                        }
                    }
                }
            }
        }
    }
}
