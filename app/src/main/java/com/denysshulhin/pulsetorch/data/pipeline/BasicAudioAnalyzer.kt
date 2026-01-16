package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BasicAudioAnalyzer(
    private val sampleRateFps: Int = 60,
    private val rmsWindowMs: Int = 180,
) : Analyzer {

    // RMS window
    private val windowSize = max(8, (sampleRateFps * rmsWindowMs / 1000f).toInt())
    private val sq = FloatArray(windowSize)
    private var idx = 0
    private var sumSq = 0f

    // Noise floor estimate + gate
    private var noiseFloor = 0.02f
    private var gateOpen = false

    // Envelope follower (attack/release)
    private var env = 0f

    // AGC
    private var agcGain = 1f

    // Extra smoothing
    private var ema = 0f

    override fun reset() {
        for (i in sq.indices) sq[i] = 0f
        idx = 0
        sumSq = 0f

        noiseFloor = 0.02f
        gateOpen = false

        env = 0f
        agcGain = 1f
        ema = 0f
    }

    override fun process(amplitude: Float, settings: AppSettings): AnalyzerOutput {
        val raw = amplitude.coerceIn(0f, 1f)

        // 1) RMS over window
        val x = raw
        val x2 = x * x
        sumSq -= sq[idx]
        sq[idx] = x2
        sumSq += x2
        idx++
        if (idx >= windowSize) idx = 0
        val rms = sqrt(max(0f, sumSq / windowSize.toFloat())).coerceIn(0f, 1f)

        // 2) Update noise floor when signal is low (slow learning)
        // Learn only from small rms to avoid "learning the music"
        val noiseLearn = if (rms < noiseFloor * 1.35f + 0.02f) 1f else 0f
        if (noiseLearn > 0f) {
            noiseFloor = lerp(noiseFloor, rms, 0.01f).coerceIn(0.0f, 0.25f)
        }

        // 3) Gate with hysteresis (open/close thresholds)
        val sensitivity = settings.sensitivity.coerceIn(0f, 1f)
        val gateBase = (0.08f - 0.06f * sensitivity).coerceIn(0.015f, 0.10f)
        val openTh = max(gateBase, noiseFloor * 1.6f)
        val closeTh = max(gateBase * 0.75f, noiseFloor * 1.25f)

        gateOpen = if (gateOpen) {
            rms > closeTh
        } else {
            rms > openTh
        }

        val gated = if (gateOpen) rms else 0f

        // 4) Envelope follower with attack/release
        // Faster attack, slower release. Release slightly depends on smoothing setting.
        val smoothing = settings.smoothing.coerceIn(0f, 1f)
        val attack = (0.35f + 0.35f * (1f - smoothing)).coerceIn(0.15f, 0.70f)
        val release = (0.06f + 0.18f * smoothing).coerceIn(0.04f, 0.28f)
        env = if (gated >= env) lerp(env, gated, attack) else lerp(env, gated, release)
        val envClamped = env.coerceIn(0f, 1f)

        // 5) "Bass focus" placeholder
        // Without real spectrum, we can emulate by emphasizing envelope and reducing tiny spikes
        val shaped = if (settings.bassFocus) {
            // compress small changes, emphasize stronger beats
            val t = envClamped
            (t * t * (2.1f - 1.1f * t)).coerceIn(0f, 1f) // smoothstep-ish
        } else {
            envClamped
        }

        // 6) AGC normalization (adapt to quiet/loud music)
        // Target level depends on effect: strobe works better with a bit higher target
        val target = when (settings.effect) {
            com.denysshulhin.pulsetorch.domain.model.Effect.STROBE -> 0.55f
            com.denysshulhin.pulsetorch.domain.model.Effect.PULSE -> 0.50f
            com.denysshulhin.pulsetorch.domain.model.Effect.SMOOTH -> 0.45f
        }

        val measured = max(0.001f, shaped)
        val desiredGain = (target / measured).coerceIn(0.6f, 6.0f)

        // slow adaptation for stability
        agcGain = lerp(agcGain, desiredGain, 0.01f).coerceIn(0.6f, 6.0f)

        var normalized = (shaped * agcGain).coerceIn(0f, 1f)

        // 7) Limiter + extra EMA (polish)
        // Soft clip near top to avoid harsh flicker
        normalized = softClip(normalized, knee = 0.82f)

        val emaAlpha = (0.10f + 0.25f * smoothing).coerceIn(0.08f, 0.40f)
        ema = lerp(ema, normalized, emaAlpha)
        val smoothed = ema.coerceIn(0f, 1f)

        return AnalyzerOutput(
            raw = raw,
            gated = if (gateOpen) gated else 0f,
            smoothed = smoothed,
            normalized = smoothed
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun softClip(x: Float, knee: Float): Float {
        val v = x.coerceIn(0f, 1f)
        if (v <= knee) return v
        val t = (v - knee) / max(0.0001f, (1f - knee))
        // compress 1.0 smoothly
        return (knee + (1f - knee) * (t / (1f + t))).coerceIn(0f, 1f)
    }
}
