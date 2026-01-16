package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import com.denysshulhin.pulsetorch.domain.model.Effect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class BasicEffectEngine : EffectEngine {

    private var phase = 0f
    private var lastLevel = 0f

    override fun reset() {
        phase = 0f
        lastLevel = 0f
    }

    override fun process(input: AnalyzerOutput, settings: AppSettings): EffectOutput {
        val x = input.normalized.coerceIn(0f, 1f)

        return when (settings.effect) {
            Effect.SMOOTH -> {
                // direct mapped intensity with light smoothing
                val level = lerp(lastLevel, x, 0.25f)
                lastLevel = level
                EffectOutput(level = level, hz = null)
            }

            Effect.PULSE -> {
                // pulse frequency depends on intensity; clamp
                val hz = (1.0f + 7.0f * x).coerceIn(1f, minHzLimit(settings))
                phase = advance(phase, hz)
                val wave = (sin(2f * PI.toFloat() * phase) * 0.5f + 0.5f) // 0..1
                val level = (0.15f + 0.85f * wave * x).coerceIn(0f, 1f)
                lastLevel = level
                EffectOutput(level = level, hz = hz)
            }

            Effect.STROBE -> {
                val maxHz = minHzLimit(settings)
                val hz = (2.0f + (maxHz - 2.0f) * x).coerceIn(2f, maxHz)
                phase = advance(phase, hz)
                // square-ish strobe
                val square = if (phase < 0.5f) 1f else 0f
                val level = (square * x).coerceIn(0f, 1f)
                lastLevel = level
                EffectOutput(level = level, hz = hz)
            }
        }
    }

    private fun minHzLimit(settings: AppSettings): Float {
        return settings.maxStrobeHz.coerceIn(2f, 20f)
    }

    private fun advance(p: Float, hz: Float): Float {
        // pipeline ticks ~60fps, dt=1/60
        val dt = 1f / 60f
        var np = p + hz * dt
        if (np >= 1f) np -= np.toInt()
        return np
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
