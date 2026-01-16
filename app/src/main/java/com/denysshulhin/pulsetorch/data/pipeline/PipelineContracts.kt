package com.denysshulhin.pulsetorch.data.pipeline

import kotlinx.coroutines.flow.StateFlow
import com.denysshulhin.pulsetorch.domain.model.AppUiState

interface AudioSource {
    suspend fun start()
    suspend fun stop()
    fun amplitudeFlow(): kotlinx.coroutines.flow.Flow<Float>
}

data class AnalyzerOutput(
    val raw: Float,
    val gated: Float,
    val smoothed: Float,
    val normalized: Float
)

interface Analyzer {
    fun process(amplitude: Float, settings: com.denysshulhin.pulsetorch.domain.model.AppSettings): AnalyzerOutput
    fun reset()
}

data class EffectOutput(
    val level: Float,        // 0..1 torch intensity target
    val hz: Float? = null     // for debug (strobe/pulse)
)

interface EffectEngine {
    fun process(input: AnalyzerOutput, settings: com.denysshulhin.pulsetorch.domain.model.AppSettings): EffectOutput
    fun reset()
}

interface TorchController {
    fun isTorchAvailable(): Boolean
    fun shutdown()
    fun setEnabled(enabled: Boolean)
    fun setLevel(level: Float) // 0..1
}

interface Pipeline {
    fun start(scope: kotlinx.coroutines.CoroutineScope, ui: StateFlow<AppUiState>)
    fun stop()
}
