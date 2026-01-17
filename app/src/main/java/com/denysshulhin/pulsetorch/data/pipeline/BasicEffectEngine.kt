package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

class BasicEffectEngine : EffectEngine {

    private var lastX = 0f
    private var lastLevel = 0f

    private var peakEma = 0f
    private var phase = 0f

    override fun reset() {
        lastX = 0f
        lastLevel = 0f
        peakEma = 0f
        phase = 0f
    }

    override fun process(input: AnalyzerOutput, settings: AppSettings): EffectOutput {
        val x = input.normalized.coerceIn(0f, 1f)

        // detect "beatiness" from slope
        val dx = (x - lastX)
        lastX = x

        val peak = max(0f, dx * 3.2f).coerceIn(0f, 1f)
        peakEma = lerp(peakEma, peak, 0.22f)
        val beatiness = peakEma.coerceIn(0f, 1f)

        // base smooth brightness
        val smoothness = settings.smoothness.coerceIn(0f, 1f)
        val alpha = (0.55f - 0.40f * smoothness).coerceIn(0.12f, 0.60f)
        val base = lerp(lastLevel, x, alpha)

        // pulse and strobe parts (blend)
        val maxHz = settings.maxStrobeHz.coerceIn(2f, 20f)
        val hz = (2.0f + (maxHz - 2.0f) * beatiness).coerceIn(2f, maxHz)

        // assume 50fps loop
        phase = advance(phase, hz, dt = 1f / 50f)

        val sine01 = (sin(2f * PI.toFloat() * phase) * 0.5f + 0.5f)

        val pulse = (0.10f + 0.90f * sine01) * x
        val strobe = if (phase < 0.18f) x else 0f

        // weights
        val strobeW = ((beatiness - 0.55f) / 0.35f).coerceIn(0f, 1f) * x
        val pulseW = ((beatiness - 0.18f) / 0.40f).coerceIn(0f, 1f) * (1f - strobeW)
        val baseW = (1f - strobeW - pulseW).coerceIn(0f, 1f)

        val out = (base * baseW + pulse * pulseW + strobe * strobeW).coerceIn(0f, 1f)
        lastLevel = out

        return EffectOutput(level = out, hz = hz)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun advance(p: Float, hz: Float, dt: Float): Float {
        var np = p + hz * dt
        if (np >= 1f) np -= np.toInt()
        return np
    }
}
