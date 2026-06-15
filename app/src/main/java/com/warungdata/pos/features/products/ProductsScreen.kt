package com.warungdata.pos.features.products

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.database.entity.ProductEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: ProductsViewModel = viewModel(),
    onAddProduct: () -> Unit = {},
    onEditProduct: (ProductEntity) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showFormDialog by remember { mutableStateOf(false) }

    // Filter products by search and category
    val filteredProducts = remember(state.products, state.searchQuery, state.selectedCategoryId) {
        state.products.filter { product ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    product.name.contains(state.searchQuery, ignoreCase = true) ||
                    product.barcode.contains(state.searchQuery, ignoreCase = false)
            val matchesCategory = state.selectedCategoryId == null ||
                    product.categoryId == state.selectedCategoryId
            matchesSearch && matchesCategory
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Produk") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.startAddProduct()
                showFormDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::search,
                placeholder = { Text("Cari produk...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category filter chips
            if (state.categories.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { viewModel.filterByCategory(null) },
                        label = { Text("Semua") }
                    )
                    state.categories.forEach { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.filterByCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada produk", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                viewModel.startEditProduct(product)
                                showFormDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        ProductFormDialog(
            formState = viewModel.formState.collectAsState().value,
            categories = state.categories,
            onFormAction = { action ->
                when (action) {
                    is FormAction.UpdateName -> viewModel.updateName(action.value)
                    is FormAction.UpdateCategory -> viewModel.updateCategory(action.value)
                    is FormAction.UpdateBarcode -> viewModel.updateBarcode(action.value)
                    is FormAction.UpdateUnit -> viewModel.updateUnit(action.value)
                    is FormAction.UpdatePurchasePrice -> viewModel.updatePurchasePrice(action.value)
                    is FormAction.UpdateSellingPrice -> viewModel.updateSellingPrice(action.value)
                    is FormAction.UpdateStock -> viewModel.updateStock(action.value)
                    is FormAction.UpdateMinStock -> viewModel.updateMinStock(action.value)
                    is FormAction.UpdateNotes -> viewModel.updateNotes(action.value)
                    FormAction.Save -> viewModel.saveProduct()
                    FormAction.Cancel -> { showFormDialog = false }
                }
                if (action is FormAction.Save && !viewModel.formState.value.isSaving) {
                    showFormDialog = false
                }
            },
            onDismiss = { showFormDialog = false }
        )
    }
}

@Composable
private fun ProductCard(
    product: ProductEntity,
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
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.barcode.isNotEmpty()) {
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Rp ${format.format(product.sellingPrice)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Stok: ${product.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (product.stock <= product.minimumStock)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = product.unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

sealed class FormAction {
    data class UpdateName(val value: String) : FormAction()
    data class UpdateCategory(val value: Int?) : FormAction()
    data class UpdateBarcode(val value: String) : FormAction()
    data class UpdateUnit(val value: String) : FormAction()
    data class UpdatePurchasePrice(val value: String) : FormAction()
    data class UpdateSellingPrice(val value: String) : FormAction()
    data class UpdateStock(val value: String) : FormAction()
    data class UpdateMinStock(val value: String) : FormAction()
    data class UpdateNotes(val value: String) : FormAction()
    data object Save : FormAction()
    data object Cancel : FormAction()
}

@Composable
fun ProductFormDialog(
    formState: ProductFormState,
    categories: List<com.warungdata.pos.core.database.entity.CategoryEntity>,
    onFormAction: (FormAction) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (formState.isEditing) "Edit Produk" else "Tambah Produk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = { onFormAction(FormAction.UpdateName(it)) },
                    label = { Text("Nama Produk *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.purchasePrice,
                        onValueChange = { onFormAction(FormAction.UpdatePurchasePrice(it)) },
                        label = { Text("Harga Beli") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = formState.sellingPrice,
                        onValueChange = { onFormAction(FormAction.UpdateSellingPrice(it)) },
                        label = { Text("Harga Jual *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.stock,
                        onValueChange = { onFormAction(FormAction.UpdateStock(it)) },
                        label = { Text("Stok") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = formState.minimumStock,
                        onValueChange = { onFormAction(FormAction.UpdateMinStock(it)) },
                        label = { Text("Stok Min") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = formState.barcode,
                    onValueChange = { onFormAction(FormAction.UpdateBarcode(it)) },
                    label = { Text("Barcode") },
                    singleLine = true,
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
                onClick = { onFormAction(FormAction.Save) },
                enabled = !formState.isSaving
            ) {
                Text(if (formState.isEditing) "Simpan" else "Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = { onFormAction(FormAction.Cancel) }) {
                Text("Batal")
            }
        }
    )
}
