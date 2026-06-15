package com.warungdata.pos.features.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warungdata.pos.core.database.entity.ProductEntity
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierScreen(
    viewModel: CashierViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val format = remember { NumberFormat.getNumberInstance(Locale("id", "ID")) }

    // Success dialog
    if (state.showSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSuccess,
            title = { Text("Transaksi Berhasil") },
            text = {
                Column {
                    Text("Invoice: ${state.invoiceNumber}")
                    Text("Total: Rp ${format.format(state.total)}")
                    Text("Pembayaran: ${state.paymentMethod}")
                }
            },
            confirmButton = {
                Button(onClick = viewModel::dismissSuccess) {
                    Text("OK")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with search
        TopAppBar(
            title = { Text("Kasir") }
        )

        // Search
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

        // Main content: products grid + cart side by side (or stacked)
        Row(modifier = Modifier.weight(1f)) {
            // Products grid (left/main)
            Column(modifier = Modifier.weight(0.6f)) {
                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.products, key = { it.id }) { product ->
                            ProductGridItem(
                                product = product,
                                onClick = { viewModel.addToCart(product) }
                            )
                        }
                    }
                }
            }

            // Cart panel (right)
            Surface(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Cart header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cart", style = MaterialTheme.typography.titleSmall)
                        if (state.cart.isNotEmpty()) {
                            TextButton(onClick = {
                                viewModel.removeFromCart(state.cart.first().product.id)
                            }) {
                                Text("Hapus", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Cart items
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.cart, key = { it.product.id }) { item ->
                            CartItemRow(
                                item = item,
                                onIncrease = { viewModel.addToCart(item.product) },
                                onDecrease = { viewModel.updateQty(item.product.id, item.qty - 1) },
                                onRemove = { viewModel.removeFromCart(item.product.id) }
                            )
                        }
                    }

                    // Cart totals
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal")
                            Text("Rp ${format.format(state.subtotal)}")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Diskon")
                            OutlinedTextField(
                                value = if (state.discount > 0) state.discount.toString() else "",
                                onValueChange = { viewModel.updateDiscount(it.toLongOrNull() ?: 0) },
                                modifier = Modifier.width(100.dp),
                                singleLine = true,
                                placeholder = { Text("0") }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Text(
                                "Rp ${format.format(state.total)}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = viewModel::showPayment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = state.cart.isNotEmpty()
                        ) {
                            Text("Bayar (${state.cart.size} item)")
                        }
                    }
                }
            }
        }
    }

    // Payment dialog
    if (state.showPaymentDialog) {
        PaymentDialog(
            total = state.total,
            format = format,
            onDismiss = viewModel::hidePayment,
            onConfirm = viewModel::checkout,
            onPaymentMethodChange = viewModel::setPaymentMethod,
            selectedMethod = state.paymentMethod
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductGridItem(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column {
                Text(
                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(product.sellingPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Stok: ${product.stock}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.stock <= product.minimumStock)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.product.sellingPrice)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(text = "${item.qty}", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onIncrease, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.subtotal)}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentDialog(
    total: Long,
    format: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    selectedMethod: String
) {
    var selectedMethodLocal by remember { mutableStateOf(selectedMethod) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Total: Rp ${format.format(total)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text("Metode Pembayaran", style = MaterialTheme.typography.labelLarge)

                val methods = listOf(
                    "cash" to "Tunai",
                    "qris" to "QRIS",
                    "transfer" to "Transfer",
                    "utang" to "Utang"
                )
                methods.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedMethodLocal == key,
                            onClick = { selectedMethodLocal = key }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onPaymentMethodChange(selectedMethodLocal)
                onConfirm()
            }) {
                Text("Bayar Rp ${format.format(total)}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
