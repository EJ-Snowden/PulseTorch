package com.denysshulhin.pulsetorch.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.denysshulhin.pulsetorch.domain.model.AppSettings
import com.denysshulhin.pulsetorch.domain.model.Effect
import com.denysshulhin.pulsetorch.domain.model.Mode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

    private object K {
        val MODE = stringPreferencesKey("mode")
        val EFFECT = stringPreferencesKey("effect")

        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val SMOOTHNESS = floatPreferencesKey("smoothness")

        val AUTO_BRIGHTNESS = booleanPreferencesKey("auto_brightness")
        val MAX_STROBE_HZ = floatPreferencesKey("max_strobe_hz")
        val MIC_GAIN = floatPreferencesKey("mic_gain")
        val SMOOTHING = floatPreferencesKey("smoothing")
        val BASS_FOCUS = booleanPreferencesKey("bass_focus")
        val STROBE_WARNING = booleanPreferencesKey("strobe_warning")
    }

    val settingsFlow: Flow<AppSettings> =
        context.ptDataStore.data.map { p ->
            AppSettings(
                mode = p[K.MODE]?.let { runCatching { Mode.valueOf(it) }.getOrNull() } ?: Mode.MIC,
                effect = p[K.EFFECT]?.let { runCatching { Effect.valueOf(it) }.getOrNull() } ?: Effect.STROBE,

                sensitivity = p[K.SENSITIVITY] ?: 0.75f,
                smoothness = p[K.SMOOTHNESS] ?: 0.40f,

                autoBrightness = p[K.AUTO_BRIGHTNESS] ?: true,
                maxStrobeHz = p[K.MAX_STROBE_HZ] ?: 10f,
                micGain = p[K.MIC_GAIN] ?: 1.4f,
                smoothing = p[K.SMOOTHING] ?: 0.40f,
                bassFocus = p[K.BASS_FOCUS] ?: true,
                strobeWarning = p[K.STROBE_WARNING] ?: true,
            )
        }

    suspend fun setMode(mode: Mode) = context.ptDataStore.edit { it[K.MODE] = mode.name }
    suspend fun setEffect(effect: Effect) = context.ptDataStore.edit { it[K.EFFECT] = effect.name }

    suspend fun setSensitivity(v: Float) = context.ptDataStore.edit { it[K.SENSITIVITY] = v.coerceIn(0f, 1f) }
    suspend fun setSmoothness(v: Float) = context.ptDataStore.edit { it[K.SMOOTHNESS] = v.coerceIn(0f, 1f) }

    suspend fun setAutoBrightness(v: Boolean) = context.ptDataStore.edit { it[K.AUTO_BRIGHTNESS] = v }
    suspend fun setMaxStrobeHz(v: Float) = context.ptDataStore.edit { it[K.MAX_STROBE_HZ] = v.coerceIn(1f, 20f) }
    suspend fun setMicGain(v: Float) = context.ptDataStore.edit { it[K.MIC_GAIN] = v.coerceIn(0.5f, 2.0f) }
    suspend fun setSmoothing(v: Float) = context.ptDataStore.edit { it[K.SMOOTHING] = v.coerceIn(0f, 1f) }
    suspend fun setBassFocus(v: Boolean) = context.ptDataStore.edit { it[K.BASS_FOCUS] = v }
    suspend fun setStrobeWarning(v: Boolean) = context.ptDataStore.edit { it[K.STROBE_WARNING] = v }
}
