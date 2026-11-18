package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.AspectRatioOption
import com.example.model.TextScaleOption
import com.example.model.ThemeMode
import com.example.model.VisualStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "storyboardku_prefs")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val ACTIVE_PROJECT_ID = longPreferencesKey("active_project_id")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val TEXT_SCALE = stringPreferencesKey("text_scale")
        val DEFAULT_STYLE = stringPreferencesKey("default_style")
        val DEFAULT_RATIO = stringPreferencesKey("default_ratio")
        val DEFAULT_SCENE_DURATION = intPreferencesKey("default_scene_duration")
        val ONBOARDING_DISMISSED = booleanPreferencesKey("onboarding_dismissed")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            activeProjectId = prefs[Keys.ACTIVE_PROJECT_ID] ?: -1L,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.DARK,
            textScale = prefs[Keys.TEXT_SCALE]?.let { runCatching { TextScaleOption.valueOf(it) }.getOrNull() }
                ?: TextScaleOption.MEDIUM,
            defaultStyle = prefs[Keys.DEFAULT_STYLE]?.let { runCatching { VisualStyle.valueOf(it) }.getOrNull() }
                ?: VisualStyle.CARTOON_2D_IN_3D_ENV,
            defaultRatio = prefs[Keys.DEFAULT_RATIO]?.let { runCatching { AspectRatioOption.valueOf(it) }.getOrNull() }
                ?: AspectRatioOption.RATIO_9_16,
            defaultSceneDurationSec = prefs[Keys.DEFAULT_SCENE_DURATION] ?: 10,
            onboardingDismissed = prefs[Keys.ONBOARDING_DISMISSED] ?: false
        )
    }

    suspend fun setActiveProjectId(projectId: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_PROJECT_ID] = projectId }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setTextScale(scale: TextScaleOption) {
        context.dataStore.edit { it[Keys.TEXT_SCALE] = scale.name }
    }

    suspend fun setDefaultStyle(style: VisualStyle) {
        context.dataStore.edit { it[Keys.DEFAULT_STYLE] = style.name }
    }

    suspend fun setDefaultRatio(ratio: AspectRatioOption) {
        context.dataStore.edit { it[Keys.DEFAULT_RATIO] = ratio.name }
    }

    suspend fun setDefaultSceneDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SCENE_DURATION] = seconds.coerceIn(2, 120) }
    }

    suspend fun setOnboardingDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DISMISSED] = dismissed }
    }
}
