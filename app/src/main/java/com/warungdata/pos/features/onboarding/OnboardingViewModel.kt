package com.warungdata.pos.features.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MAX_PIN_LENGTH = 6

data class OnboardingUiState(
    val storeName: String = "",
    val ownerName: String = "",
    val businessType: String = "warung",
    val pin: String = "",
    val pinConfirm: String = "",
    val isSaving: Boolean = false,
    val error: String? = null
)

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    fun updateStoreName(value: String) {
        _uiState.value = _uiState.value.copy(storeName = value, error = null)
    }

    fun updateOwnerName(value: String) {
        _uiState.value = _uiState.value.copy(ownerName = value, error = null)
    }

    fun updateBusinessType(value: String) {
        _uiState.value = _uiState.value.copy(businessType = value, error = null)
    }

    fun updatePin(value: String) {
        _uiState.value = _uiState.value.copy(
            pin = value.onlyDigits().take(MAX_PIN_LENGTH),
            error = null
        )
    }

    fun updatePinConfirm(value: String) {
        _uiState.value = _uiState.value.copy(
            pinConfirm = value.onlyDigits().take(MAX_PIN_LENGTH),
            error = null
        )
    }

    fun save() {
        val state = _uiState.value
        val pin = state.pin.onlyDigits()
        val pinConfirm = state.pinConfirm.onlyDigits()

        if (state.storeName.isBlank()) {
            _uiState.value = state.copy(error = "Nama toko harus diisi")
            return
        }
        if (state.ownerName.isBlank()) {
            _uiState.value = state.copy(error = "Nama pemilik harus diisi")
            return
        }
        if (pin.length !in 4..6) {
            _uiState.value = state.copy(error = "PIN harus 4-6 digit angka")
            return
        }
        if (pin != pinConfirm) {
            _uiState.value = state.copy(error = "PIN tidak cocok")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        viewModelScope.launch {
            settingsRepo.saveStoreInfo(state.storeName, state.ownerName, state.businessType)
            settingsRepo.setPin(pin)
            settingsRepo.setOnboardingCompleted()
            _onboardingComplete.value = true
        }
    }

    private fun String.onlyDigits(): String = filter { it.isDigit() }
}
