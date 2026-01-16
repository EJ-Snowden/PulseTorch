package com.denysshulhin.pulsetorch.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denysshulhin.pulsetorch.core.permissions.AudioPermission
import com.denysshulhin.pulsetorch.data.pipeline.AudioPipelineManager
import com.denysshulhin.pulsetorch.data.pipeline.BasicAudioAnalyzer
import com.denysshulhin.pulsetorch.data.pipeline.BasicEffectEngine
import com.denysshulhin.pulsetorch.data.pipeline.DemoAudioSource
import com.denysshulhin.pulsetorch.data.pipeline.AudioSource
import com.denysshulhin.pulsetorch.data.pipeline.mic.MicAudioSource
import com.denysshulhin.pulsetorch.data.settings.SettingsRepository
import com.denysshulhin.pulsetorch.data.torch.AndroidTorchController
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Effect
import com.denysshulhin.pulsetorch.domain.model.Mode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext

    private val repo = SettingsRepository(ctx)
    private val torch = AndroidTorchController(ctx, viewModelScope)

    private val isRunning = MutableStateFlow(false)
    private val statusText = MutableStateFlow("IDLE")
    private val signalLevel01 = MutableStateFlow(0f)

    private val pipeline = AudioPipelineManager(
        torch = torch,
        sourceFactory = { mode: Mode ->
            when (mode) {
                Mode.MIC -> MicAudioSource(sampleRate = 44100, chunkMs = 30)
                // пока остальные режимы оставим demo, позже заменим на file/system sources
                Mode.FILE -> DemoAudioSource()
                Mode.SYSTEM -> DemoAudioSource()
            }
        },
        analyzer = BasicAudioAnalyzer(sampleRateFps = 60, rmsWindowMs = 180),
        engine = BasicEffectEngine(),
        onSignal = { signalLevel01.value = it }
    )

    val uiState: StateFlow<AppUiState> =
        combine(repo.settingsFlow, isRunning, statusText, signalLevel01) { settings, running, status, sig ->
            AppUiState(
                settings = settings,
                isRunning = running,
                statusText = status,
                signalLevel01 = sig
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    fun setMode(mode: Mode) {
        forceStop()
        viewModelScope.launch { repo.setMode(mode) }
    }

    fun setEffect(effect: Effect) = viewModelScope.launch { repo.setEffect(effect) }

    fun setSensitivity(v: Float) = viewModelScope.launch { repo.setSensitivity(v) }
    fun setSmoothness(v: Float) = viewModelScope.launch { repo.setSmoothness(v) }

    fun setAutoBrightness(v: Boolean) = viewModelScope.launch { repo.setAutoBrightness(v) }
    fun setMaxStrobeHz(v: Float) = viewModelScope.launch { repo.setMaxStrobeHz(v) }
    fun setMicGain(v: Float) = viewModelScope.launch { repo.setMicGain(v) }
    fun setSmoothing(v: Float) = viewModelScope.launch { repo.setSmoothing(v) }
    fun setBassFocus(v: Boolean) = viewModelScope.launch { repo.setBassFocus(v) }
    fun setStrobeWarning(v: Boolean) = viewModelScope.launch { repo.setStrobeWarning(v) }

    fun forceStop() {
        isRunning.value = false
        statusText.value = "IDLE"
        signalLevel01.value = 0f
        pipeline.stop()
    }

    fun toggleRunning() {
        if (isRunning.value) {
            forceStop()
            return
        }

        // torch check
        if (!torch.isTorchAvailable()) {
            statusText.value = "NO TORCH"
            pipeline.stop()
            return
        }

        // mic permission check only for MIC mode
        val mode = uiState.value.settings.mode
        if (mode == Mode.MIC && !AudioPermission.hasRecordAudio(ctx)) {
            statusText.value = "MIC PERMISSION REQUIRED"
            isRunning.value = false
            pipeline.stop()
            return
        }

        isRunning.value = true
        statusText.value = "RUNNING"
        pipeline.start(viewModelScope, uiState)
    }

    override fun onCleared() {
        pipeline.stop()
        super.onCleared()
    }
}
