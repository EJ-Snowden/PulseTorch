package com.denysshulhin.pulsetorch.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denysshulhin.pulsetorch.data.pipeline.AudioPipelineManager
import com.denysshulhin.pulsetorch.data.pipeline.BasicAudioAnalyzer
import com.denysshulhin.pulsetorch.data.pipeline.BasicEffectEngine
import com.denysshulhin.pulsetorch.data.pipeline.DemoAudioSource
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

    private val repo = SettingsRepository(app.applicationContext)
    private val torch = AndroidTorchController(app.applicationContext, viewModelScope)

    private val pipeline = AudioPipelineManager(
        torch = torch,
        sourceFactory = { _ -> DemoAudioSource() }, // пока demo
        analyzer = BasicAudioAnalyzer(sampleRateFps = 60, rmsWindowMs = 180),
        engine = BasicEffectEngine()
    )

    private val isRunning = MutableStateFlow(false)
    private val statusText = MutableStateFlow("IDLE")

    val uiState: StateFlow<AppUiState> =
        combine(repo.settingsFlow, isRunning, statusText) { settings, running, status ->
            AppUiState(settings = settings, isRunning = running, statusText = status)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    fun setMode(mode: Mode) {
        // гарантия: при смене режима всегда OFF
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
        pipeline.stop()
    }

    fun toggleRunning() {
        if (isRunning.value) {
            // stop
            isRunning.value = false
            statusText.value = "IDLE"
            pipeline.stop()
            return
        }

        // start
        if (!torch.isTorchAvailable()) {
            isRunning.value = false
            statusText.value = "NO TORCH"
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
