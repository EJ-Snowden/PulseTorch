package com.denysshulhin.pulsetorch.data.engine

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import com.denysshulhin.pulsetorch.domain.model.Effect
import kotlin.math.PI
import kotlin.math.sin

class EffectEngine {
    private var phase = 0f
    private var emaOut = 0f

    fun reset() {
        phase = 0f
        emaOut = 0f
    }

    /**
     * signal: 0..1 (после Analyzer)
     * dtSec: delta time в секундах
     * settings: твои настройки (effect, maxStrobeHz, sensitivity, smoothness)
     */
    fun nextLevel(signal: Float, dtSec: Float, settings: AppSettings): Float {
        val s = signal.coerceIn(0f, 1f)

        val sensitivity = settings.sensitivity.coerceIn(0.1f, 2.5f)
        val smoothnessUi = settings.smoothness.coerceIn(0f, 1f)

        val maxHz = settings.maxStrobeHz.coerceIn(1f, 20f)
        val base = (s * sensitivity).coerceIn(0f, 1f)

        val rawOut = when (settings.effect) {
            Effect.SMOOTH -> smooth(base, smoothnessUi)
            Effect.PULSE -> pulse(base, dtSec, maxHz)
            Effect.STROBE -> strobe(base, dtSec, maxHz)
        }

        // немного сгладим выход, чтобы не дергалось (особенно на Smooth)
        val out = ema(emaOut, rawOut, 0.25f)
        emaOut = out

        return out.coerceIn(0f, 1f)
    }

    private fun smooth(level: Float, smoothness: Float): Float {
        // smoothness 0..1: 0 = без сглаживания, 1 = очень плавно
        val a = (0.15f + smoothness * 0.55f).coerceIn(0.05f, 0.80f)
        emaOut = ema(emaOut, level, a)
        return emaOut
    }

    private fun pulse(level: Float, dtSec: Float, maxHz: Float): Float {
        // частота зависит от сигнала, но не превышает maxHz
        val hz = (1f + level * (maxHz - 1f)).coerceIn(0.8f, maxHz)
        phase = (phase + hz * dtSec) % 1f

        // duty зависит от уровня (чем больше сигнал, тем шире импульс)
        val duty = (0.18f + level * 0.22f).coerceIn(0.10f, 0.45f)
        val on = if (phase < duty) 1f else 0f

        // амплитудная модуляция
        return (on * (0.25f + 0.75f * level)).coerceIn(0f, 1f)
    }

    private fun strobe(level: Float, dtSec: Float, maxHz: Float): Float {
        // фиксированный строб на maxHz, но не выше 10 по умолчанию обычно
        val hz = maxHz.coerceIn(1f, 20f)
        phase = (phase + hz * dtSec) % 1f

        // 50% duty
        val on = if (phase < 0.5f) 1f else 0f
        return (on * (0.35f + 0.65f * level)).coerceIn(0f, 1f)
    }

    private fun ema(prev: Float, next: Float, a: Float): Float = prev + (next - prev) * a
}
