package com.denysshulhin.pulsetorch.data.pipeline

import android.os.SystemClock
import com.denysshulhin.pulsetorch.domain.model.AppSettings
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class BasicAudioAnalyzer(
    // fps оставляем, но используем как fallback
    private val fps: Int = 60
) : Analyzer {

    private var tMs: Long = 0L
    private var lastWallMs: Long = 0L

    // Envelope (после AGC)
    private var env = 0f

    // AGC
    private var envMean = 0.12f   // “типичная громкость”
    private var envPeak = 0.25f   // для компрессии/нормализации

    // Flux
    private var fluxEma = 0f
    private var fluxMean = 0.05f

    // Beat tracking
    private var lastBeatMs: Long = -1L
    private var ioiMs: Float = 600f // ~100 bpm
    private var beatConf = 0f

    private var onsetHoldMs: Long = 0L

    // Breathe
    private var breathePhase = 0f

    // extra: fallback flux from energy deltas (helps file tracks)
    private var prevEnergy = 0f

    override fun reset() {
        tMs = 0L
        lastWallMs = 0L

        env = 0f
        envMean = 0.12f
        envPeak = 0.25f

        fluxEma = 0f
        fluxMean = 0.05f

        lastBeatMs = -1L
        ioiMs = 600f
        beatConf = 0f

        onsetHoldMs = 0L
        breathePhase = 0f
        prevEnergy = 0f
    }

    override fun process(f: AudioFeatures, settings: AppSettings): AnalyzerOutput {
        // --- real dt (fixes mismatch with pipeline tick) ---
        val now = SystemClock.elapsedRealtime()
        val dtMs = if (lastWallMs == 0L) {
            (1000f / fps).toLong().coerceAtLeast(10L)
        } else {
            (now - lastWallMs).coerceIn(10L, 60L)
        }
        lastWallMs = now
        tMs += dtMs

        val smoothing = settings.smoothing.coerceIn(0f, 1f)
        val sens = settings.sensitivity.coerceIn(0f, 1f)

        val energyIn = f.energy01.coerceIn(0f, 1f)
        val fluxIn = f.flux01.coerceIn(0f, 1f)

        // --- Silence detection (more tolerant) ---
        val isSilence = energyIn < 0.010f && fluxIn < 0.015f

        // --- AGC / normalization ---
        // mean follows slowly, peak a bit faster
        envMean = lerp(envMean, energyIn, if (isSilence) 0.02f else 0.008f).coerceIn(0.02f, 0.40f)
        envPeak = max(envPeak * 0.995f, energyIn).coerceIn(0.05f, 0.90f)

        // target makes quiet songs visible, loud songs not always maxed
        val target = lerp(0.18f, 0.30f, sens)
        val gain = (target / max(0.03f, envMean)).coerceIn(0.7f, 3.2f)

        val energy = (energyIn * gain).coerceIn(0f, 1f)

        // --- Envelope follower (attack/release) ---
        val attack = lerp(0.40f, 0.80f, 1f - smoothing)   // sharper when smoothing low
        val release = if (isSilence) 0.32f else lerp(0.05f, 0.16f, smoothing)
        env = if (energy >= env) lerp(env, energy, attack) else lerp(env, energy, release)
        env = env.coerceIn(0f, 1f)

        // --- Flux: combine provided flux + energy delta (stabilizes reactions) ---
        val dE = abs(energy - prevEnergy)
        prevEnergy = energy

        val fluxRaw = max(fluxIn, (dE * 1.6f).coerceIn(0f, 1f))
        val fluxAlpha = if (isSilence) 0.35f else 0.22f
        fluxEma = lerp(fluxEma, fluxRaw, fluxAlpha).coerceIn(0f, 1f)
        fluxMean = lerp(fluxMean, fluxEma, if (isSilence) 0.02f else 0.01f).coerceIn(0.01f, 0.28f)

        // --- Onset threshold (more sensitive) ---
        val mul = lerp(1.55f, 1.05f, sens)
        val th = (fluxMean * mul + lerp(0.012f, 0.006f, sens)).coerceIn(0.020f, 0.32f)

        val canTrigger = tMs >= onsetHoldMs
        val onset = !isSilence && canTrigger && (fluxEma > th) && (fluxEma > fluxMean + 0.012f)

        if (onset) {
            // keep double-triggers away, but allow 200+ bpm
            val hold = lerp(95f, 35f, sens).toLong()
            onsetHoldMs = tMs + hold
            registerBeatCandidate()
        } else {
            val dec = if (isSilence) 0.030f else 0.010f
            beatConf = (beatConf - dec).coerceIn(0f, 1f)
        }

        val phase01 = computePhase01()

        // --- Breathe only when beat weak ---
        val breatheHz = (0.30f + 0.90f * env).coerceIn(0.25f, 1.35f)
        breathePhase = (breathePhase + (breatheHz * dtMs.toFloat() / 1000f)) % 1f
        val breathe01 = smoothSin01(breathePhase)

        val tempo = bpmFromIoi(ioiMs)

        return AnalyzerOutput(
            energy01 = energy,
            flux01 = fluxEma,
            env01 = env,
            beat01 = beatConf,
            tempoBpm = tempo,
            phase01 = phase01,
            breathe01 = breathe01
        )
    }

    private fun registerBeatCandidate() {
        val now = tMs

        if (lastBeatMs > 0) {
            val diff = (now - lastBeatMs).toFloat()

            // accept wider bpm: 35..220 (helps club tracks)
            if (diff in 273f..1715f) {
                val expected = ioiMs
                val ratio = diff / max(1f, expected)

                val corrected = when {
                    ratio > 1.85f -> diff * 0.5f
                    ratio < 0.60f -> diff * 2.0f
                    else -> diff
                }

                ioiMs = lerp(ioiMs, corrected, 0.18f).coerceIn(273f, 1715f)
                beatConf = (beatConf + 0.22f).coerceIn(0f, 1f)
            } else {
                beatConf = (beatConf - 0.05f).coerceIn(0f, 1f)
            }
        } else {
            beatConf = (beatConf + 0.12f).coerceIn(0f, 1f)
        }

        lastBeatMs = now
    }

    private fun computePhase01(): Float {
        val lb = lastBeatMs
        if (lb <= 0) return 0.5f

        val dt = (tMs - lb).toFloat()
        val period = ioiMs.coerceIn(273f, 1715f)
        val p = (dt / period).coerceIn(0f, 4f)
        return (p % 1f).coerceIn(0f, 1f)
    }

    private fun bpmFromIoi(ioiMs: Float): Float? {
        if (ioiMs <= 0f) return null
        val bpm = 60000f / ioiMs
        return bpm.coerceIn(35f, 220f)
    }

    private fun smoothSin01(p: Float): Float {
        val x = (sin(2f * Math.PI.toFloat() * p) * 0.5f + 0.5f)
        return x * x * (3f - 2f * x)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
