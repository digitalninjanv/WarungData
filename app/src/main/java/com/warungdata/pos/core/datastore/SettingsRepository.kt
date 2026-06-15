package com.warungdata.pos.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "warungdata_settings")

class SettingsRepository(private val context: Context) {

    private object PrefKeys {
        val STORE_NAME = stringPreferencesKey(SettingsKeys.STORE_NAME)
        val OWNER_NAME = stringPreferencesKey(SettingsKeys.OWNER_NAME)
        val BUSINESS_TYPE = stringPreferencesKey(SettingsKeys.BUSINESS_TYPE)
        val CURRENCY = stringPreferencesKey(SettingsKeys.CURRENCY)
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey(SettingsKeys.HAS_COMPLETED_ONBOARDING)
        val PIN_HASH = stringPreferencesKey(SettingsKeys.PIN_HASH)
        val PIN_LENGTH = intPreferencesKey(SettingsKeys.PIN_LENGTH)
        val IS_PIN_ENABLED = booleanPreferencesKey(SettingsKeys.IS_PIN_ENABLED)
        val THEME_MODE = stringPreferencesKey(SettingsKeys.THEME_MODE)
        val PRINTER_ENABLED = booleanPreferencesKey(SettingsKeys.PRINTER_ENABLED)
        val PRINTER_ADDRESS = stringPreferencesKey(SettingsKeys.PRINTER_ADDRESS)
        val BACKUP_INTERVAL = intPreferencesKey(SettingsKeys.BACKUP_INTERVAL)
        val LAST_BACKUP_TIME = longPreferencesKey(SettingsKeys.LAST_BACKUP_TIME)
        val DEFAULT_SELLING_MARGIN = intPreferencesKey(SettingsKeys.DEFAULT_SELLING_MARGIN)
    }

    val storeName: Flow<String> = context.dataStore.data.map { it[PrefKeys.STORE_NAME] ?: "" }
    val ownerName: Flow<String> = context.dataStore.data.map { it[PrefKeys.OWNER_NAME] ?: "" }
    val businessType: Flow<String> = context.dataStore.data.map { it[PrefKeys.BUSINESS_TYPE] ?: "" }
    val currency: Flow<String> = context.dataStore.data.map { it[PrefKeys.CURRENCY] ?: "IDR" }
    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.HAS_COMPLETED_ONBOARDING] ?: false }
    val pinHash: Flow<String> = context.dataStore.data.map { it[PrefKeys.PIN_HASH] ?: "" }
    val pinLength: Flow<Int> = context.dataStore.data.map { it[PrefKeys.PIN_LENGTH] ?: 4 }
    val isPinEnabled: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.IS_PIN_ENABLED] ?: false }
    val themeMode: Flow<String> = context.dataStore.data.map { it[PrefKeys.THEME_MODE] ?: "system" }
    val printerEnabled: Flow<Boolean> = context.dataStore.data.map { it[PrefKeys.PRINTER_ENABLED] ?: false }
    val printerAddress: Flow<String> = context.dataStore.data.map { it[PrefKeys.PRINTER_ADDRESS] ?: "" }
    val backupInterval: Flow<Int> = context.dataStore.data.map { it[PrefKeys.BACKUP_INTERVAL] ?: 0 }
    val lastBackupTime: Flow<Long> = context.dataStore.data.map { it[PrefKeys.LAST_BACKUP_TIME] ?: 0L }
    val defaultSellingMargin: Flow<Int> = context.dataStore.data.map { it[PrefKeys.DEFAULT_SELLING_MARGIN] ?: 30 }

    suspend fun isOnboardingCompleted(): Boolean = hasCompletedOnboarding.first()

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun saveStoreInfo(name: String, owner: String, businessType: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.STORE_NAME] = name.trim()
            prefs[PrefKeys.OWNER_NAME] = owner.trim()
            prefs[PrefKeys.BUSINESS_TYPE] = businessType
        }
    }

    suspend fun setPin(pin: String) {
        val normalizedPin = normalizePin(pin)
        require(normalizedPin.length in 4..6) { "PIN must be 4-6 digits" }

        context.dataStore.edit { prefs ->
            prefs[PrefKeys.PIN_HASH] = hashPin(normalizedPin)
            prefs[PrefKeys.PIN_LENGTH] = normalizedPin.length
            prefs[PrefKeys.IS_PIN_ENABLED] = true
        }
    }

    /**
     * Backward-compatible method. Older code passed the raw PIN here despite the old name.
     */
    suspend fun setPinHash(hash: String) {
        val rawPin = normalizePin(hash)
        if (rawPin.length in 4..6 && rawPin == hash) {
            setPin(rawPin)
            return
        }

        context.dataStore.edit { prefs ->
            val savedLength = prefs[PrefKeys.PIN_LENGTH]
            prefs[PrefKeys.PIN_HASH] = hash
            prefs[PrefKeys.IS_PIN_ENABLED] = hash.isNotEmpty()
            if (savedLength == null || savedLength !in 4..6) {
                prefs[PrefKeys.PIN_LENGTH] = 4
            }
        }
    }

    suspend fun isPinSet(): Boolean = pinHash.first().isNotEmpty()

    suspend fun getPinLength(): Int {
        val prefs = context.dataStore.data.first()
        val savedLength = prefs[PrefKeys.PIN_LENGTH]
        if (savedLength != null && savedLength in 4..6) return savedLength

        val storedPin = prefs[PrefKeys.PIN_HASH].orEmpty()
        return if (storedPin.length in 4..6 && storedPin.all { it.isDigit() }) {
            storedPin.length
        } else {
            4
        }
    }

    suspend fun verifyPin(input: String): Boolean {
        val normalizedInput = normalizePin(input)
        if (normalizedInput.length !in 4..6) return false

        val stored = pinHash.first()
        if (stored.isEmpty()) return false

        val hashedInput = hashPin(normalizedInput)
        if (stored == hashedInput) return true

        // Migration path for APK versions that accidentally stored the raw PIN.
        if (stored == normalizedInput) {
            setPin(normalizedInput)
            return true
        }

        return false
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[PrefKeys.THEME_MODE] = mode }
    }

    suspend fun setPrinterConfig(enabled: Boolean, address: String = "") {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.PRINTER_ENABLED] = enabled
            prefs[PrefKeys.PRINTER_ADDRESS] = address
        }
    }

    suspend fun setLastBackupTime(time: Long) {
        context.dataStore.edit { prefs -> prefs[PrefKeys.LAST_BACKUP_TIME] = time }
    }

    private fun normalizePin(pin: String): String = pin.filter { it.isDigit() }

    /**
     * Simple hash for local offline PIN protection.
     */
    private fun hashPin(pin: String): String {
        val salt = "WarungData2026"
        return (pin + salt).hashCode().toString()
    }
}
