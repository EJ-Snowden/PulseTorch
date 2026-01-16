package com.denysshulhin.pulsetorch.data.pipeline

import com.denysshulhin.pulsetorch.domain.contracts.TorchController
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class AudioPipelineManager(
    private val torch: TorchController,
    private val sourceFactory: (Mode) -> AudioSource,
    private val analyzer: Analyzer,
    private val engine: EffectEngine
) : Pipeline {

    private var pipelineScope: CoroutineScope? = null
    private var uiRef: StateFlow<AppUiState>? = null

    private var job: Job? = null
    private var currentMode: Mode? = null
    private var currentSource: AudioSource? = null

    // restart guard (avoid multiple restarts)
    private var restarting = false

    override fun start(scope: CoroutineScope, ui: StateFlow<AppUiState>) {
        if (job != null) return

        pipelineScope = scope
        uiRef = ui

        startInternal(scope, ui, ui.value.settings.mode)
    }

    override fun stop() {
        val scope = pipelineScope
        val ui = uiRef

        if (scope == null || ui == null) {
            hardShutdown()
            return
        }

        scope.launch {
            stopInternal()
        }
    }

    private fun startInternal(scope: CoroutineScope, ui: StateFlow<AppUiState>, mode: Mode) {
        currentMode = mode
        analyzer.reset()
        engine.reset()

        val src = sourceFactory(mode)
        currentSource = src

        job = scope.launch {
            src.start()

            if (src is DemoAudioSource) {
                launch { src.emitLoop { this@launch.isActive } }
            }

            src.amplitudeFlow().collect { amp ->
                val settings = ui.value.settings

                // mode changed while running -> restart cleanly
                if (settings.mode != currentMode && !restarting) {
                    restarting = true
                    restart(settings.mode)
                    return@collect
                }

                if (!torch.isTorchAvailable()) {
                    hardShutdown()
                    return@collect
                }

                val a = analyzer.process(amp, settings)
                val e = engine.process(a, settings)

                val level = e.level.coerceIn(0f, 1f)
                torch.setLevel(level)
                torch.setEnabled(level > 0.01f)
            }
        }
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

        if (j != null) {
            j.cancelAndJoin()
        }

        val src = currentSource
        currentSource = null
        currentMode = null

        if (src != null) {
            runCatching { src.stop() }
        }

        // ALWAYS off
        torch.shutdown()
    }

    private fun hardShutdown() {
        // best effort, non-suspending emergency shutdown
        job?.cancel()
        job = null
        currentSource = null
        currentMode = null
        torch.shutdown()
    }
}
