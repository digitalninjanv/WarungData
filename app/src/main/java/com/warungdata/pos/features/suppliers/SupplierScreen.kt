package com.warungdata.pos.features.suppliers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.database.entity.SupplierEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    viewModel: SupplierViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showFormDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Supplier") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.startAddSupplier()
                showFormDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Supplier")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.suppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada supplier", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.suppliers, key = { it.id }) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            onClick = {
                                viewModel.startEditSupplier(supplier)
                                showFormDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        SupplierFormDialog(
            formState = viewModel.formState.collectAsState().value,
            onNameChange = viewModel::updateName,
            onPhoneChange = viewModel::updatePhone,
            onAddressChange = viewModel::updateAddress,
            onNoteChange = viewModel::updateNote,
            onSave = viewModel::saveSupplier,
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
private fun SupplierCard(
    supplier: SupplierEntity,
    onClick: () -> Unit
) {
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

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
            Text(
                text = supplier.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supplier.phone.isNotEmpty()) {
                Text(
                    text = supplier.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (supplier.address.isNotEmpty()) {
                Text(
                    text = supplier.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Utang: Rp ${format.format(supplier.totalDebt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (supplier.totalDebt > 0)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierFormDialog(
    formState: SupplierFormState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (formState.isEditing) "Edit Supplier" else "Tambah Supplier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onNameChange,
                    label = { Text("Nama Supplier *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = onPhoneChange,
                    label = { Text("No. Telepon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.address,
                    onValueChange = onAddressChange,
                    label = { Text("Alamat") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.note,
                    onValueChange = onNoteChange,
                    label = { Text("Catatan") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (formState.error != null) {
                    Text(
                        text = formState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !formState.isSaving
            ) {
                Text(if (formState.isEditing) "Simpan" else "Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
