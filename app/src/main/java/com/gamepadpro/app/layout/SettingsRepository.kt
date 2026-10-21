package com.gamepadpro.app.layout

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val vibration: Boolean = true,
    val autoReconnect: Boolean = true,
    /** Stick dead zone, 0f..0.30f. */
    val deadZone: Float = 0.08f,
    /** Show pressed buttons + raw HID report bytes on the gamepad screen. */
    val debugOverlay: Boolean = false,
    /** Bluetooth MAC of the last connected PC/TV (for auto reconnect). */
    val lastHostAddress: String = ""
)

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext

    val settings: Flow<Settings> = appContext.dataStore.data.map { prefs ->
        Settings(
            vibration = prefs[KEY_VIBRATION] ?: true,
            autoReconnect = prefs[KEY_AUTO_RECONNECT] ?: true,
            deadZone = (prefs[KEY_DEAD_ZONE] ?: 0.08f).coerceIn(0f, 0.30f),
            debugOverlay = prefs[KEY_DEBUG] ?: false,
            lastHostAddress = prefs[KEY_LAST_HOST] ?: ""
        )
    }

    suspend fun setVibration(on: Boolean) =
        appContext.dataStore.edit { it[KEY_VIBRATION] = on }

    suspend fun setAutoReconnect(on: Boolean) =
        appContext.dataStore.edit { it[KEY_AUTO_RECONNECT] = on }

    suspend fun setDeadZone(value: Float) =
        appContext.dataStore.edit { it[KEY_DEAD_ZONE] = value.coerceIn(0f, 0.30f) }

    suspend fun setDebugOverlay(on: Boolean) =
        appContext.dataStore.edit { it[KEY_DEBUG] = on }

    suspend fun setLastHostAddress(address: String) =
        appContext.dataStore.edit { it[KEY_LAST_HOST] = address }

    companion object {
        private val KEY_VIBRATION = booleanPreferencesKey("vibration")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_DEAD_ZONE = floatPreferencesKey("dead_zone")
        private val KEY_DEBUG = booleanPreferencesKey("debug_overlay")
        private val KEY_LAST_HOST = stringPreferencesKey("last_host")
    }
}
