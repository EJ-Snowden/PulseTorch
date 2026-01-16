package com.denysshulhin.pulsetorch.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denysshulhin.pulsetorch.data.engine.PulseTorchRunner
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
    private val torch = AndroidTorchController(app, viewModelScope)

    // DSP runner (пока DemoSignalSource, без реального аудио)
    private val runner = PulseTorchRunner(torch)

    private val isRunning = MutableStateFlow(false)
    private val statusText = MutableStateFlow("IDLE")

    val uiState: StateFlow<AppUiState> =
        combine(repo.settingsFlow, isRunning, statusText) { settings, running, status ->
            AppUiState(settings = settings, isRunning = running, statusText = status)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    fun setMode(mode: Mode) {
        // always stop torch + runner when changing mode
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
        runner.stop() // includes torch.shutdown()
    }

    fun toggleRunning() {
        val willRun = !isRunning.value
        isRunning.value = willRun

        if (!willRun) {
            statusText.value = "IDLE"
            runner.stop()
            return
        }

        if (!torch.isTorchAvailable()) {
            isRunning.value = false
            statusText.value = "NO TORCH"
            runner.stop()
            return
        }

        statusText.value = "RUNNING"
        runner.start(viewModelScope, uiState)
    }

    override fun onCleared() {
        runner.stop()
        super.onCleared()
    }
}
