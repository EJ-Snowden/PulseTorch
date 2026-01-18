package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import kotlinx.coroutines.flow.StateFlow

data class AudioFeatures(
    val energy01: Float, // envelope-ish loudness 0..1
    val flux01: Float,   // change amount 0..1 (onsets)
)

interface AudioSource {
    suspend fun start()
    suspend fun stop()
    fun readFeatures(): AudioFeatures?
}

data class AnalyzerOutput(
    val energy01: Float,
    val flux01: Float,
    val env01: Float,          // smooth envelope
    val beat01: Float,         // beat confidence 0..1
    val tempoBpm: Float?,      // estimated bpm or null
    val phase01: Float,        // 0..1 beat phase (0 right after beat)
    val breathe01: Float       // slow modulation when no beat
)

interface Analyzer {
    fun process(f: AudioFeatures, settings: AppSettings): AnalyzerOutput
    fun reset()
}

data class EffectOutput(
    val level: Float, // 0..1 torch target
)

interface EffectEngine {
    fun process(a: AnalyzerOutput, settings: AppSettings): EffectOutput
    fun reset()
}

interface Pipeline {
    fun start(scope: kotlinx.coroutines.CoroutineScope, ui: StateFlow<AppUiState>)
    fun stop()
}
