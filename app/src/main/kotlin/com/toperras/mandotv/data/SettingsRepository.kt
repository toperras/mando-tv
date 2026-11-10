package com.toperras.mandotv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Ajustes de la app (DataStore). Aquí NO se guardan tokens ni claves:
// esos van al almacén seguro de Android (AndroidKeyStore) por separado.

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val hapticsEnabled: Boolean = true,
)

private val Context.settingsStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val hapticsKey = booleanPreferencesKey("haptics")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            theme = prefs[themeKey]
                ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                ?: ThemePreference.SYSTEM,
            hapticsEnabled = prefs[hapticsKey] ?: true,
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.settingsStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[hapticsKey] = enabled }
    }
}
