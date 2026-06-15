package com.warungdata.pos.features.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
        _uiState.value = _uiState.value.copy(pin = value, error = null)
    }

    fun updatePinConfirm(value: String) {
        _uiState.value = _uiState.value.copy(pinConfirm = value, error = null)
    }

    fun save() {
        val state = _uiState.value
        if (state.storeName.isBlank()) {
            _uiState.value = state.copy(error = "Nama toko harus diisi")
            return
        }
        if (state.ownerName.isBlank()) {
            _uiState.value = state.copy(error = "Nama pemilik harus diisi")
            return
        }
        if (state.pin.length < 4) {
            _uiState.value = state.copy(error = "PIN minimal 4 digit")
            return
        }
        if (state.pin != state.pinConfirm) {
            _uiState.value = state.copy(error = "PIN tidak cocok")
            return
        }

        _uiState.value = state.copy(isSaving = true)

        viewModelScope.launch {
            settingsRepo.saveStoreInfo(state.storeName, state.ownerName, state.businessType)
            settingsRepo.setPinHash(state.pin)
            settingsRepo.setOnboardingCompleted()
            _onboardingComplete.value = true
        }
    }
}
