package com.warungdata.pos.features.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.warungdata.pos.core.datastore.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PinUiState(
    val enteredPin: String = "",
    val error: String? = null,
    val isVerified: Boolean = false,
    val isNewUser: Boolean = false
)

class PinViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasOnboarded = settingsRepo.isOnboardingCompleted()
            _uiState.value = _uiState.value.copy(isNewUser = !hasOnboarded)
        }
    }

    fun enterDigit(digit: String) {
        val current = _uiState.value.enteredPin
        if (current.length >= 6) return
        val newPin = current + digit
        _uiState.value = _uiState.value.copy(enteredPin = newPin, error = null)

        if (newPin.length >= 4) {
            verifyPin(newPin)
        }
    }

    fun deleteDigit() {
        val current = _uiState.value.enteredPin
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                enteredPin = current.dropLast(1),
                error = null
            )
        }
    }

    fun clearPin() {
        _uiState.value = _uiState.value.copy(enteredPin = "", error = null)
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val valid = settingsRepo.verifyPin(pin)
            if (valid) {
                _uiState.value = _uiState.value.copy(isVerified = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "PIN salah",
                    enteredPin = ""
                )
            }
        }
    }
}
