package com.warungdata.pos.features.settings

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val storeName: String = "",
    val ownerName: String = "",
    val businessType: String = "",
    val themeMode: String = "system",
    val isPinEnabled: Boolean = false,
    val printerEnabled: Boolean = false,
    val printerAddress: String = "",
    val defaultMargin: Int = 30,
    val saved: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.storeName, repo.ownerName, repo.businessType,
                repo.themeMode, repo.isPinEnabled, repo.printerEnabled,
                repo.printerAddress, repo.defaultSellingMargin
            ) { args ->
                SettingsUiState(
                    storeName = args[0] as String,
                    ownerName = args[1] as String,
                    businessType = args[2] as String,
                    themeMode = args[3] as String,
                    isPinEnabled = args[4] as Boolean,
                    printerEnabled = args[5] as Boolean,
                    printerAddress = args[6] as String,
                    defaultMargin = args[7] as Int
                )
            }.collect { _uiState.value = it }
        }
    }

    fun updateStoreName(v: String) { _uiState.value = _uiState.value.copy(storeName = v, saved = false) }
    fun updateOwnerName(v: String) { _uiState.value = _uiState.value.copy(ownerName = v, saved = false) }
    fun updateBusinessType(v: String) { _uiState.value = _uiState.value.copy(businessType = v, saved = false) }
    fun updatePinEnabled(v: Boolean) { _uiState.value = _uiState.value.copy(isPinEnabled = v, saved = false) }
    fun updatePrinterEnabled(v: Boolean) { _uiState.value = _uiState.value.copy(printerEnabled = v, saved = false) }
    fun updatePrinterAddress(v: String) { _uiState.value = _uiState.value.copy(printerAddress = v, saved = false) }
    fun updateDefaultMargin(v: Int) { _uiState.value = _uiState.value.copy(defaultMargin = v, saved = false) }

    fun save() {
        viewModelScope.launch {
            val s = _uiState.value
            repo.saveStoreInfo(s.storeName, s.ownerName, s.businessType)
            repo.setThemeMode(s.themeMode)
            repo.setPrinterConfig(s.printerEnabled, s.printerAddress)
            repo.setPinHash(if (s.isPinEnabled) "dummy" else "")
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }

    fun setThemeMode(mode: String) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        viewModelScope.launch { repo.setThemeMode(mode) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbar.showSnackbar("Pengaturan disimpan")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profil Toko", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = state.storeName, onValueChange = viewModel::updateStoreName, label = { Text("Nama Toko") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.ownerName, onValueChange = viewModel::updateOwnerName, label = { Text("Nama Pemilik") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = state.businessType, onValueChange = viewModel::updateBusinessType, label = { Text("Jenis Usaha") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tampilan", style = MaterialTheme.typography.titleMedium)
                    Text("Mode Tema", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("system" to "System", "light" to "Terang", "dark" to "Gelap").forEach { (k, l) ->
                            FilterChip(selected = state.themeMode == k, onClick = { viewModel.setThemeMode(k) }, label = { Text(l) })
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Keamanan", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Aktifkan PIN", modifier = Modifier.weight(1f))
                        Switch(checked = state.isPinEnabled, onCheckedChange = viewModel::updatePinEnabled)
                    }
                    Text("PIN digunakan untuk akses aplikasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Printer Bluetooth", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Aktifkan Printer", modifier = Modifier.weight(1f))
                        Switch(checked = state.printerEnabled, onCheckedChange = viewModel::updatePrinterEnabled)
                    }
                    if (state.printerEnabled) {
                        OutlinedTextField(value = state.printerAddress, onValueChange = viewModel::updatePrinterAddress, label = { Text("Alamat MAC Printer") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("00:11:22:33:44:55") })
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pengaturan Lain", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = state.defaultMargin.toString(),
                        onValueChange = { viewModel.updateDefaultMargin(it.toIntOrNull() ?: 30) },
                        label = { Text("Margin Default (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Pengaturan")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
