package com.warungdata.pos.features.backup

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.backup.BackupResult
import com.warungdata.pos.core.backup.BackupService
import com.warungdata.pos.core.database.entity.BackupHistoryEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BackupUiState(
    val isCreating: Boolean = false,
    val isRestoring: Boolean = false,
    val lastResult: BackupResult? = null,
    val history: List<BackupHistoryEntity> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val backupService = BackupService(application)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init { loadHistory() }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(history = backupService.getBackupHistory())
        }
    }

    fun createBackup() {
        _uiState.value = _uiState.value.copy(isCreating = true, error = null, successMessage = null)
        viewModelScope.launch {
            try {
                val result = backupService.createBackup()
                _uiState.value = _uiState.value.copy(isCreating = false, lastResult = result, successMessage = "Backup berhasil dibuat")
                loadHistory()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isCreating = false, error = e.message ?: "Gagal membuat backup")
            }
        }
    }

    fun restoreBackup(filePath: String) {
        _uiState.value = _uiState.value.copy(isRestoring = true, error = null, successMessage = null)
        viewModelScope.launch {
            try {
                val success = backupService.restoreBackup(File(filePath))
                if (success) {
                    _uiState.value = _uiState.value.copy(isRestoring = false, successMessage = "Restore berhasil! Restart aplikasi.")
                } else {
                    _uiState.value = _uiState.value.copy(isRestoring = false, error = "Gagal merestore backup")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isRestoring = false, error = e.message ?: "Gagal merestore backup")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Buat Backup", style = MaterialTheme.typography.titleMedium)
                    Text("Membuat file ZIP berisi database dan file ekspor", style = MaterialTheme.typography.bodySmall)

                    Button(
                        onClick = viewModel::createBackup,
                        enabled = !state.isCreating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isCreating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Buat Backup Sekarang")
                    }
                }
            }

            state.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Text("Riwayat Backup", style = MaterialTheme.typography.titleMedium)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.history) { backup ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dateFmt.format(Date(backup.createdAt)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${backup.type} • ${backup.fileSize} bytes", style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalButton(onClick = {
                                viewModel.restoreBackup(backup.filePath)
                            }, enabled = !state.isRestoring) {
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }
    }
}
