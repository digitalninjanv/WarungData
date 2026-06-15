package com.warungdata.pos.features.stock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.database.entity.ProductEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockScreen(
    viewModel: StockViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val adjustmentState by viewModel.adjustmentState.collectAsState()

    val filteredProducts = remember(state.products, state.searchQuery, state.showLowStockOnly) {
        state.products.filter { product ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    product.name.contains(state.searchQuery, ignoreCase = true) ||
                    product.barcode.contains(state.searchQuery, ignoreCase = false)
            val matchesLowStock = !state.showLowStockOnly || product.stock <= product.minimumStock
            matchesSearch && matchesLowStock
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Stok") })
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
                placeholder = { Text("Cari produk...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.showLowStockOnly,
                    onClick = { if (state.showLowStockOnly) viewModel.toggleLowStockFilter() },
                    label = { Text("Semua") }
                )
                FilterChip(
                    selected = state.showLowStockOnly,
                    onClick = { if (!state.showLowStockOnly) viewModel.toggleLowStockFilter() },
                    label = { Text("Stok Menipis") }
                )
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
                        StockProductCard(
                            product = product,
                            onClick = { viewModel.showAdjustment(product) }
                        )
                    }
                }
            }
        }
    }

    if (adjustmentState.isVisible && adjustmentState.product != null) {
        StockAdjustmentDialog(
            adjustmentState = adjustmentState,
            onTypeChange = viewModel::updateAdjustmentType,
            onQtyChange = viewModel::updateAdjustmentQty,
            onNoteChange = viewModel::updateAdjustmentNote,
            onSave = viewModel::saveAdjustment,
            onDismiss = viewModel::hideAdjustment
        )
    }
}

@Composable
private fun StockProductCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
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
                Text(
                    text = "Min. Stok: ${product.minimumStock} ${product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val stockColor = if (product.stock <= product.minimumStock)
                MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
            Text(
                text = "Stok: ${product.stock}",
                style = MaterialTheme.typography.bodyMedium,
                color = stockColor
            )
        }
    }
}

@Composable
private fun StockAdjustmentDialog(
    adjustmentState: StockAdjustmentState,
    onTypeChange: (String) -> Unit,
    onQtyChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val product = adjustmentState.product ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Penyesuaian Stok") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Stok saat ini: ${product.stock}",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                Text("Tipe", style = MaterialTheme.typography.labelLarge)
                val types = listOf(
                    "IN" to "Masuk",
                    "OUT" to "Keluar",
                    "DAMAGED" to "Rusak",
                    "LOST" to "Hilang",
                    "ADJUSTMENT" to "Penyesuaian"
                )
                types.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = adjustmentState.type == key,
                            onClick = { onTypeChange(key) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }

                OutlinedTextField(
                    value = adjustmentState.qty,
                    onValueChange = onQtyChange,
                    label = { Text("Jumlah") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = adjustmentState.note,
                    onValueChange = onNoteChange,
                    label = { Text("Catatan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (adjustmentState.error != null) {
                    Text(
                        text = adjustmentState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !adjustmentState.isSaving
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
