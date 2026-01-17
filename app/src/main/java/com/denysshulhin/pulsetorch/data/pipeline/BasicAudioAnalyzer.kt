package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import kotlin.math.max
import kotlin.math.sqrt

class BasicAudioAnalyzer(
    private val sampleRateFps: Int = 50,
    private val rmsWindowMs: Int = 90,
) : Analyzer {

    private val windowSize = max(6, (sampleRateFps * rmsWindowMs / 1000f).toInt())
    private val sq = FloatArray(windowSize)
    private var idx = 0
    private var sumSq = 0f

    private var noiseFloor = 0.02f
    private var gateOpen = false

    private var env = 0f
    private var agcGain = 1f
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

        // RMS
        val x2 = raw * raw
        sumSq -= sq[idx]
        sq[idx] = x2
        sumSq += x2
        idx++
        if (idx >= windowSize) idx = 0
        val rms = sqrt(max(0f, sumSq / windowSize.toFloat())).coerceIn(0f, 1f)

        // Noise floor learn (slow)
        val learn = rms < noiseFloor * 1.35f + 0.02f
        if (learn) {
            noiseFloor = lerp(noiseFloor, rms, 0.012f).coerceIn(0.0f, 0.25f)
        }

        // Gate
        val sensitivity = settings.sensitivity.coerceIn(0f, 1f)
        val gateBase = (0.075f - 0.055f * sensitivity).coerceIn(0.015f, 0.10f)
        val openTh = max(gateBase, noiseFloor * 1.6f)
        val closeTh = max(gateBase * 0.75f, noiseFloor * 1.25f)

        gateOpen = if (gateOpen) rms > closeTh else rms > openTh
        val gated = if (gateOpen) rms else 0f

        // Envelope follower
        val smoothing = settings.smoothing.coerceIn(0f, 1f)
        val attack = (0.55f - 0.25f * smoothing).coerceIn(0.25f, 0.65f)
        val release = (0.10f + 0.18f * smoothing).coerceIn(0.08f, 0.30f)
        env = if (gated >= env) lerp(env, gated, attack) else lerp(env, gated, release)
        val envClamped = env.coerceIn(0f, 1f)

        // Bass focus (simple shaping)
        val shaped = if (settings.bassFocus) {
            val t = envClamped
            (t * t * (2.1f - 1.1f * t)).coerceIn(0f, 1f)
        } else envClamped

        // AGC
        val target = 0.50f
        val measured = max(0.001f, shaped)
        val desiredGain = (target / measured).coerceIn(0.6f, 6.0f)
        agcGain = lerp(agcGain, desiredGain, 0.015f).coerceIn(0.6f, 6.0f)

        var normalized = (shaped * agcGain).coerceIn(0f, 1f)
        normalized = softClip(normalized, knee = 0.84f)

        // final EMA
        val emaAlpha = (0.14f + 0.28f * smoothing).coerceIn(0.12f, 0.42f)
        ema = lerp(ema, normalized, emaAlpha)
        val out = ema.coerceIn(0f, 1f)

        return AnalyzerOutput(
            raw = raw,
            gated = gated,
            smoothed = out,
            normalized = out
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun softClip(x: Float, knee: Float): Float {
        val v = x.coerceIn(0f, 1f)
        if (v <= knee) return v
        val t = (v - knee) / max(0.0001f, (1f - knee))
        return (knee + (1f - knee) * (t / (1f + t))).coerceIn(0f, 1f)
    }
}
