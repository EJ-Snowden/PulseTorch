package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import kotlin.math.max
import kotlin.math.min

class BasicEffectEngine : EffectEngine {

    private var last = 0f

    override fun reset() {
        last = 0f
    }

    override fun process(a: AnalyzerOutput, settings: AppSettings): EffectOutput {
        val env = a.env01.coerceIn(0f, 1f)
        val beat = a.beat01.coerceIn(0f, 1f)
        val phase = a.phase01.coerceIn(0f, 1f)
        val breathe = a.breathe01.coerceIn(0f, 1f)

        // If almost silence - truly off (fix "always on")
        val silence = env < 0.012f && beat < 0.10f
        if (silence) {
            last = lerp(last, 0f, 0.35f)
            return EffectOutput(level = last)
        }

        // 1) Visible smooth brightness from env (compress quiet parts up)
        // gamma < 1 makes quiet tracks still move
        val envShaped = powApprox(env, 0.62f)
        val base = envShaped.coerceIn(0f, 1f)

        // 2) When beat is weak, add gentle motion (multiply so silence stays off)
        val beatWeak = (1f - beat).coerceIn(0f, 1f)
        val motion = lerp(0.78f, 1.00f, breathe)  // 0.78..1
        val baseWithMotion = base * lerp(1.00f, motion, beatWeak)

        // 3) Pulse (adds on top of base around beat)
        val pulse = pulseFromPhase(phase) // 0..1
        val pulseStrength = smooth01((beat - 0.08f) / 0.60f) // starts earlier
        val pulseAdd = pulse * pulseStrength * (0.35f + 0.65f * baseWithMotion)
        val pulsed = clamp01(baseWithMotion + pulseAdd * (1f - baseWithMotion))

        // 4) Strobe gate only when beat confident
        // Keep envelope always visible in parallel
        val strobeStrength = smooth01((beat - 0.22f) / 0.55f)
        val duty = lerp(0.60f, 0.14f, strobeStrength) // strong beat -> narrower flashes
        val gate = if (phase < duty) 1f else 0f

        // keep-part ensures you still see smooth brightness even during strobe
        val keep = lerp(0.85f, 0.30f, strobeStrength)
        val strobed = if (strobeStrength > 0f) {
            pulsed * (keep + (1f - keep) * gate)
        } else {
            pulsed
        }

        // 5) Low-latency smoothing
        val smoothing = settings.smoothing.coerceIn(0f, 1f)
        val t = lerp(0.40f, 0.18f, smoothing)
        val out = lerp(last, strobed, t).coerceIn(0f, 1f)
        last = out

        return EffectOutput(level = out)
    }

    private fun pulseFromPhase(phase01: Float): Float {
        val x = phase01.coerceIn(0f, 1f)
        val k = 7.5f
        val v = 1f / (1f + k * x)
        return (v * v).coerceIn(0f, 1f)
    }

    private fun smooth01(x: Float): Float {
        val v = x.coerceIn(0f, 1f)
        return v * v * (3f - 2f * v)
    }

    private fun clamp01(x: Float): Float = min(1f, max(0f, x))

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    // fast-ish pow approximation for gamma in 0.5..0.9
    private fun powApprox(x: Float, g: Float): Float {
        val v = x.coerceIn(0f, 1f)
        // simple 2-step curve: mix sqrt and linear
        val s = kotlin.math.sqrt(v)
        return lerp(v, s, ((1f - g) / 0.5f).coerceIn(0f, 1f))
    }
}
