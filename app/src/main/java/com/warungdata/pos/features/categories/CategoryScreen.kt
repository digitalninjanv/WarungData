package com.warungdata.pos.features.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.database.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit = {},
    viewModel: CategoryViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var deleteConfirmationCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategori Produk") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::showAddDialog) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Kategori")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada kategori", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryCard(
                        category = category,
                        onEdit = { viewModel.showEditDialog(category) },
                        onDelete = { deleteConfirmationCategory = category }
                    )
                }
            }
        }
    }

    if (state.showDialog) {
        AlertDialog(
            onDismissRequest = viewModel::hideDialog,
            title = { Text(if (state.editingCategory == null) "Tambah Kategori" else "Edit Kategori") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.nameInput,
                        onValueChange = viewModel::updateNameInput,
                        label = { Text("Nama Kategori") },
                        isError = state.nameError != null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.nameError != null) {
                        Text(
                            text = state.nameError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::saveCategory) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::hideDialog) {
                    Text("Batal")
                }
            }
        )
    }

    if (deleteConfirmationCategory != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmationCategory = null },
            title = { Text("Hapus Kategori") },
            text = { Text("Apakah Anda yakin ingin menghapus kategori '${deleteConfirmationCategory?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(deleteConfirmationCategory!!)
                        deleteConfirmationCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationCategory = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: CategoryEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
