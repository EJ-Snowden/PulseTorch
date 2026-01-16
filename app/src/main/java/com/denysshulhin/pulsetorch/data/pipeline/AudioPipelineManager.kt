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
    private var currentMode: Mode? = null
    private var currentSource: AudioSource? = null

    private var restarting = false

    // stable tick (20-50ms)
    private val tickMs = 30L

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

    private fun startInternal(scope: CoroutineScope, ui: StateFlow<AppUiState>, mode: Mode) {
        currentMode = mode
        analyzer.reset()
        engine.reset()

        val src = sourceFactory(mode)
        currentSource = src

        job = scope.launch {
            // start source
            src.start()

            // main loop
            while (isActive) {
                val settings = ui.value.settings

                // mode changed while running -> restart cleanly
                if (settings.mode != currentMode && !restarting) {
                    restarting = true
                    restart(settings.mode)
                    break
                }

                if (!torch.isTorchAvailable()) {
                    hardShutdown()
                    break
                }

                // Demo source can advance itself
                if (src is DemoAudioSource) {
                    src.tick()
                } else {
                    delay(tickMs)
                }

                val amp = src.readAmplitude01() ?: continue

                val a = analyzer.process(amp, settings)
                val e = engine.process(a, settings)

                // push live signal to UI (use normalized)
                onSignal(a.normalized)

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

        if (j != null) j.cancelAndJoin()

        val src = currentSource
        currentSource = null
        currentMode = null

        if (src != null) runCatching { src.stop() }

        onSignal(0f)
        torch.shutdown()
    }

    private fun hardShutdown() {
        job?.cancel()
        job = null
        currentSource = null
        currentMode = null
        onSignal(0f)
        torch.shutdown()
    }
}
