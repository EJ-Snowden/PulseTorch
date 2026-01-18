package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.contracts.TorchController
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class AudioPipelineManager(
    private val torch: TorchController,
    private val sourceFactory: (Mode) -> AudioSource,
    private val analyzer: Analyzer,
    private val engine: EffectEngine,
    private val onSignal: (Float) -> Unit
) : Pipeline {

    private var pipelineScope: CoroutineScope? = null
    private var uiRef: StateFlow<AppUiState>? = null

    private var job: Job? = null
    private var currentSourceKey: String? = null
    private var currentSource: AudioSource? = null
    private var restarting = false

    private val tickMs = 20L

    override fun start(scope: CoroutineScope, ui: StateFlow<AppUiState>) {
        if (job != null) return
        pipelineScope = scope
        uiRef = ui
        startInternal(scope, ui, ui.value.settings.mode)
    }

    override fun stop() {
        val scope = pipelineScope
        if (scope == null) {
            hardShutdown()
            return
        }
        scope.launch { stopInternal() }
    }

    private fun sourceKeyFor(ui: AppUiState): String {
        val s = ui.settings
        return when (s.mode) {
            Mode.FILE -> "FILE:${s.fileUri ?: ""}"
            Mode.SYSTEM -> "SYSTEM"
            Mode.MIC -> "MIC"
        }
    }

    private fun startInternal(scope: CoroutineScope, ui: StateFlow<AppUiState>, mode: Mode) {
        currentSourceKey = sourceKeyFor(ui.value)
        analyzer.reset()
        engine.reset()

        val src = sourceFactory(mode)
        currentSource = src

        job = scope.launch {
            src.start()

            var lastLevel = 0f

            while (isActive) {
                val state = ui.value
                val settings = state.settings

                val newKey = sourceKeyFor(state)
                if (newKey != currentSourceKey && !restarting) {
                    restarting = true
                    restart(settings.mode)
                    break
                }

                if (!torch.isTorchAvailable()) {
                    hardShutdown()
                    break
                }

                delay(tickMs)

                val feat = src.readFeatures()
                if (feat == null) {
                    // hard fall to zero if source not producing data
                    lastLevel = fallToZero(lastLevel, 0.25f)
                    onSignal(0f)
                    torch.setLevel(lastLevel)
                    torch.setEnabled(lastLevel > 0.02f)
                    continue
                }

                val a = analyzer.process(feat, settings)
                val e = engine.process(a, settings)

                onSignal(a.env01)

                val level = e.level.coerceIn(0f, 1f)
                lastLevel = level

                torch.setLevel(level)
                torch.setEnabled(level > 0.02f)
            }
        }
    }

    private fun fallToZero(x: Float, speed: Float): Float {
        val s = speed.coerceIn(0.05f, 0.7f)
        return max(0f, x - s)
    }

    private fun restart(newMode: Mode) {
        val scope = pipelineScope ?: return
        val ui = uiRef ?: return

        scope.launch {
            stopInternal()
            startInternal(scope, ui, newMode)
            restarting = false
        }
    }

    private suspend fun stopInternal() {
        val j = job
        job = null
        if (j != null) j.cancelAndJoin()

        val src = currentSource
        currentSource = null
        currentSourceKey = null

        if (src != null) runCatching { src.stop() }

        onSignal(0f)
        torch.shutdown()
    }

    private fun hardShutdown() {
        job?.cancel()
        job = null
        currentSource = null
        currentSourceKey = null
        onSignal(0f)
        torch.shutdown()
    }
}
