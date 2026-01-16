package com.denysshulhin.pulsetorch.data.engine

import com.denysshulhin.pulsetorch.data.analyzer.AudioAnalyzer
import com.denysshulhin.pulsetorch.data.torch.AndroidTorchController
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.os.SystemClock

class PulseTorchRunner(
    private val torch: AndroidTorchController,
    private val analyzer: AudioAnalyzer = AudioAnalyzer(),
    private val engine: EffectEngine = EffectEngine(),
    private val source: DemoSignalSource = DemoSignalSource()
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope, uiState: StateFlow<AppUiState>) {
        stop()

        analyzer.reset()
        engine.reset()

        job = scope.launch {
            var lastNs = SystemClock.elapsedRealtimeNanos()

            source.levels(fps = 60).collect { level ->
                val nowNs = SystemClock.elapsedRealtimeNanos()
                val dtSec = ((nowNs - lastNs).coerceAtLeast(1L)) / 1_000_000_000f
                lastNs = nowNs

                val s = uiState.value.settings

                if (!torch.isTorchAvailable()) {
                    torch.shutdown()
                    return@collect
                }

                // DSP
                val analyzed = analyzer.processLevel(
                    inputLevel = level,
                    gain = s.micGain,
                    smoothingOverride = s.smoothing
                )

                // Effect -> torch level
                val outLevel = engine.nextLevel(
                    signal = analyzed.normalized,
                    dtSec = dtSec,
                    settings = s
                )

                // apply
                if (outLevel <= 0.01f) {
                    torch.setEnabled(false)
                } else {
                    torch.setLevel(outLevel)
                    torch.setEnabled(true)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        torch.shutdown()
    }
}
