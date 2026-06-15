package com.warungdata.pos.features.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.database.entity.CustomerEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showFormDialog by remember { mutableStateOf(false) }
    var detailCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val filteredCustomers = remember(state.customers, state.searchQuery) {
        if (state.searchQuery.isBlank()) state.customers
        else state.customers.filter { customer ->
            customer.name.contains(state.searchQuery, ignoreCase = true) ||
                    customer.phone.contains(state.searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Pelanggan") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.startAddCustomer()
                showFormDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pelanggan")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::search,
                placeholder = { Text("Cari pelanggan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada pelanggan", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            onClick = { detailCustomer = customer }
                        )
                    }
                }
            }
        }
    }

    detailCustomer?.let { customer ->
        CustomerDetailDialog(
            customer = customer,
            onDismiss = { detailCustomer = null },
            onEdit = {
                detailCustomer = null
                viewModel.startEditCustomer(customer)
                showFormDialog = true
            }
        )
    }

    if (showFormDialog) {
        CustomerFormDialog(
            formState = viewModel.formState.collectAsState().value,
            onUpdateName = viewModel::updateName,
            onUpdatePhone = viewModel::updatePhone,
            onUpdateAddress = viewModel::updateAddress,
            onSave = {
                viewModel.saveCustomer()
                if (!viewModel.formState.value.isSaving) {
                    showFormDialog = false
                }
            },
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    onClick: () -> Unit
) {
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (customer.phone.isNotEmpty()) {
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (customer.totalDebt > 0) {
                Text(
                    text = "Rp ${format.format(customer.totalDebt)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CustomerDetailDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(customer.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (customer.phone.isNotEmpty()) {
                    Text("Telepon: ${customer.phone}", style = MaterialTheme.typography.bodyMedium)
                }
                if (customer.address.isNotEmpty()) {
                    Text("Alamat: ${customer.address}", style = MaterialTheme.typography.bodyMedium)
                }
                if (customer.note.isNotEmpty()) {
                    Text("Catatan: ${customer.note}", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                Text(
                    text = "Total Utang: Rp ${format.format(customer.totalDebt)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (customer.totalDebt > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(onClick = onEdit) {
                Text("Edit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
private fun CustomerFormDialog(
    formState: CustomerFormState,
    onUpdateName: (String) -> Unit,
    onUpdatePhone: (String) -> Unit,
    onUpdateAddress: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (formState.isEditing) "Edit Pelanggan" else "Tambah Pelanggan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onUpdateName,
                    label = { Text("Nama *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.phone,
                    onValueChange = onUpdatePhone,
                    label = { Text("Telepon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = formState.address,
                    onValueChange = onUpdateAddress,
                    label = { Text("Alamat") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
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
