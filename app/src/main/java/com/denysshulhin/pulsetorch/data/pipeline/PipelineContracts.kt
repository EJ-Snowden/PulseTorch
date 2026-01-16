package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.model.AppSettings
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import kotlinx.coroutines.flow.StateFlow

interface AudioSource {
    suspend fun start()
    suspend fun stop()

    // 1 tick = one fresh amplitude sample in 0..1, or null if not ready
    fun readAmplitude01(): Float?
}

data class AnalyzerOutput(
    val raw: Float,
    val gated: Float,
    val smoothed: Float,
    val normalized: Float
)

interface Analyzer {
    fun process(amplitude01: Float, settings: AppSettings): AnalyzerOutput
    fun reset()
}

data class EffectOutput(
    val level: Float, // 0..1 torch target
    val hz: Float? = null
)

interface EffectEngine {
    fun process(input: AnalyzerOutput, settings: AppSettings): EffectOutput
    fun reset()
}

interface Pipeline {
    fun start(scope: kotlinx.coroutines.CoroutineScope, ui: StateFlow<AppUiState>)
    fun stop()
}
