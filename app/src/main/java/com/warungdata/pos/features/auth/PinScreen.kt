package com.warungdata.pos.features.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun PinScreen(
    viewModel: PinViewModel = viewModel(),
    onVerified: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isVerified) {
        if (state.isVerified) onVerified()
    }

    if (state.isNewUser) {
        onVerified()
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WarungData POS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Masukkan PIN",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN dots display
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(6) { index ->
                    val filled = index < state.enteredPin.length
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric keypad
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { digit ->
                            KeypadButton(
                                text = digit,
                                onClick = { viewModel.enterDigit(digit) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    KeypadButton(
                        text = "0",
                        onClick = { viewModel.enterDigit("0") },
                        modifier = Modifier.weight(1f)
                    )
                    KeypadButton(
                        text = "⌫",
                        onClick = viewModel::deleteDigit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}
