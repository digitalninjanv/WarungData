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
            prefs[PrefKeys.STORE_NAME] = name
            prefs[PrefKeys.OWNER_NAME] = owner
            prefs[PrefKeys.BUSINESS_TYPE] = businessType
        }
    }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.PIN_HASH] = hash
            prefs[PrefKeys.IS_PIN_ENABLED] = hash.isNotEmpty()
        }
    }

    suspend fun isPinSet(): Boolean = pinHash.first().isNotEmpty()

    suspend fun verifyPin(input: String): Boolean {
        val stored = pinHash.first()
        return stored.isNotEmpty() && stored == hashPin(input)
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

    /**
     * Simple hash for PIN (not cryptographic, adequate for offline local use)
     */
    private fun hashPin(pin: String): String {
        val salt = "WarungData2026"
        return (pin + salt).hashCode().toString()
    }
}
