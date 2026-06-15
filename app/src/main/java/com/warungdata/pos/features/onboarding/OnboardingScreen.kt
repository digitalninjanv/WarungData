package com.warungdata.pos.features.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = viewModel(),
    onComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val complete by viewModel.onboardingComplete.collectAsState()

    LaunchedEffect(complete) {
        if (complete) onComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Setup Toko") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Selamat datang di WarungData POS!",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Isi data toko kamu untuk memulai.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.storeName,
                onValueChange = viewModel::updateStoreName,
                label = { Text("Nama Toko") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = state.ownerName,
                onValueChange = viewModel::updateOwnerName,
                label = { Text("Nama Pemilik") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Business type selector
            Text("Jenis Usaha", style = MaterialTheme.typography.labelLarge)
            val businessTypes = listOf(
                "warung" to "Warung Sembako",
                "konter" to "Konter Pulsa",
                "toko" to "Toko Kelontong",
                "laundry" to "Laundry",
                "bengkel" to "Bengkel",
                "makanan" to "Warung Makan"
            )
            businessTypes.forEach { (key, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = state.businessType == key,
                        onClick = { viewModel.updateBusinessType(key) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(label)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Buat PIN Keamanan", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                value = state.pin,
                onValueChange = { if (it.length <= 6) viewModel.updatePin(it) },
                label = { Text("PIN (4-6 digit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                )
            )

            OutlinedTextField(
                value = state.pinConfirm,
                onValueChange = { if (it.length <= 6) viewModel.updatePinConfirm(it) },
                label = { Text("Konfirmasi PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                )
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isSaving
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Mulai", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
