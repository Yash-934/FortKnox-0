package com.example.data.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "fortknox_theme_prefs")

enum class CyberpunkTemplate(
    val templateId: String,
    val title: String,
    val accentName: String,
    val accentHex: String,
    val accentColor: Color,
    val description: String
) {
    JARVIS_CYAN(
        templateId = "JARVIS_CYAN",
        title = "J.A.R.V.I.S",
        accentName = "CYAN",
        accentHex = "#00FFE1",
        accentColor = Color(0xFF00FFE1),
        description = "Electric cyan accent with deep cyber navy background"
    ),
    STARK_IND_AMBER(
        templateId = "STARK_IND_AMBER",
        title = "STARK IND",
        accentName = "AMBER",
        accentHex = "#FFB300",
        accentColor = Color(0xFFFFB300),
        description = "Industrial amber-gold accent with charcoal black chassis"
    ),
    VERONICA_CRIMSON(
        templateId = "VERONICA_CRIMSON",
        title = "VERONICA",
        accentName = "CRIMSON",
        accentHex = "#FF0033",
        accentColor = Color(0xFFFF0033),
        description = "Aggressive crimson neon accent with pitch void contrast"
    ),
    CYBER_MATRIX_EMERALD(
        templateId = "CYBER_MATRIX_EMERALD",
        title = "CYBER MATRIX",
        accentName = "EMERALD",
        accentHex = "#00FF88",
        accentColor = Color(0xFF00FF88),
        description = "Terminal emerald glow with dark matrix void background"
    );

    companion object {
        fun fromId(id: String?): CyberpunkTemplate {
            return values().firstOrNull { it.templateId.equals(id, ignoreCase = true) } ?: JARVIS_CYAN
        }
    }
}

data class ThemeConfig(
    val isCyberpunkModeEnabled: Boolean = false,
    val selectedTemplate: CyberpunkTemplate = CyberpunkTemplate.JARVIS_CYAN
)

class ThemePreferences(private val context: Context) {

    companion object {
        val KEY_CYBERPUNK_ENABLED = booleanPreferencesKey("is_cyberpunk_mode_enabled")
        val KEY_CYBERPUNK_TEMPLATE = stringPreferencesKey("selected_cyberpunk_template")
    }

    val themeConfigFlow: Flow<ThemeConfig> = context.themeDataStore.data.map { prefs ->
        val isEnabled = prefs[KEY_CYBERPUNK_ENABLED] ?: false
        val templateId = prefs[KEY_CYBERPUNK_TEMPLATE] ?: CyberpunkTemplate.JARVIS_CYAN.templateId
        ThemeConfig(
            isCyberpunkModeEnabled = isEnabled,
            selectedTemplate = CyberpunkTemplate.fromId(templateId)
        )
    }

    suspend fun setCyberpunkModeEnabled(enabled: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_CYBERPUNK_ENABLED] = enabled
        }
    }

    suspend fun setSelectedCyberpunkTemplate(template: CyberpunkTemplate) {
        context.themeDataStore.edit { prefs ->
            prefs[KEY_CYBERPUNK_TEMPLATE] = template.templateId
        }
    }

    suspend fun setSelectedCyberpunkTemplateById(templateId: String) {
        val template = CyberpunkTemplate.fromId(templateId)
        setSelectedCyberpunkTemplate(template)
    }
}
