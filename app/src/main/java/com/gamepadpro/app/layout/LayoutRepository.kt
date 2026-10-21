package com.gamepadpro.app.layout

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Single DataStore instance for the whole app. */
internal val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gamepadpro")

internal val layoutJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** Saves / loads the customizable control layout as JSON in DataStore. */
class LayoutRepository(context: Context) {

    private val appContext = context.applicationContext

    val layout: Flow<List<ControlSpec>> = appContext.dataStore.data.map { prefs ->
        val raw = prefs[KEY_LAYOUT] ?: return@map DefaultLayout.controls
        try {
            val decoded = layoutJson.decodeFromString<List<ControlSpec>>(raw)
            if (decoded.isEmpty()) DefaultLayout.controls else decoded
        } catch (e: Exception) {
            DefaultLayout.controls
        }
    }

    suspend fun save(controls: List<ControlSpec>) {
        appContext.dataStore.edit { prefs ->
            prefs[KEY_LAYOUT] = layoutJson.encodeToString(controls)
        }
    }

    suspend fun reset() = save(DefaultLayout.controls)

    companion object {
        private val KEY_LAYOUT = stringPreferencesKey("layout_json")
    }
}
