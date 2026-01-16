package com.denysshulhin.pulsetorch.data.analyzer

import kotlin.math.max
import kotlin.math.min

data class AnalyzerConfig(
    val noiseGate: Float = 0.04f,      // порог “тишины” (0..1)
    val emaSignal: Float = 0.35f,       // EMA сглаживание сигнала (0..1)
    val emaNoise: Float = 0.02f,        // EMA для оценки noise floor
    val peakRise: Float = 0.25f,        // как быстро растет peak
    val peakFall: Float = 0.01f         // как быстро падает peak
)

data class AnalyzerOut(
    val raw: Float,            // вход 0..1
    val gated: Float,          // после noise gate
    val smoothed: Float,       // после EMA
    val normalized: Float,     // 0..1 адаптивная нормализация
    val noiseFloor: Float,     // оценка шума
    val peak: Float            // оценка пика
)

class AudioAnalyzer(
    private val cfg: AnalyzerConfig = AnalyzerConfig()
) {
    private var noiseFloor = 0f
    private var ema = 0f
    private var peak = 0.15f

    fun reset() {
        noiseFloor = 0f
        ema = 0f
        peak = 0.15f
    }

    /**
     * inputLevel: 0..1 (пока без источников аудио)
     * gain: множитель (например micGain)
     * smoothingOverride: 0..1 (если хочешь рулить сглаживанием из настроек)
     */
    fun processLevel(
        inputLevel: Float,
        gain: Float = 1f,
        smoothingOverride: Float? = null
    ): AnalyzerOut {
        val raw = (inputLevel * gain).coerceIn(0f, 1f)

        // noise floor учим по "низким" значениям
        val noiseLearn = if (raw < cfg.noiseGate * 1.5f) 1f else 0f
        if (noiseLearn > 0f) {
            noiseFloor = lerp(noiseFloor, raw, cfg.emaNoise)
        }

        // noise gate
        val gated = if (raw <= max(cfg.noiseGate, noiseFloor * 1.15f)) 0f else raw

        // EMA smoothing
        val a = (smoothingOverride ?: cfg.emaSignal).coerceIn(0f, 1f)
        ema = lerp(ema, gated, a)
        val smoothed = ema

        // adaptive peak (нормализация под тихую/громкую музыку)
        if (smoothed > peak) {
            peak = lerp(peak, smoothed, cfg.peakRise)
        } else {
            peak = lerp(peak, smoothed, cfg.peakFall)
        }
        peak = peak.coerceIn(0.10f, 1f)

        // normalize: (smoothed - floor) / (peak - floor)
        val floor = min(noiseFloor, 0.20f)
        val denom = max(0.001f, peak - floor)
        val normalized = ((smoothed - floor) / denom).coerceIn(0f, 1f)

        return AnalyzerOut(
            raw = raw,
            gated = gated,
            smoothed = smoothed,
            normalized = normalized,
            noiseFloor = noiseFloor,
            peak = peak
        )
    }

    private fun lerp(from: Float, to: Float, a: Float): Float = from + (to - from) * a
}
