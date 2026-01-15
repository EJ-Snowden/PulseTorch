package com.denysshulhin.pulsetorch.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val isRunning = MutableStateFlow(false)
    private val statusText = MutableStateFlow("IDLE")

    val uiState: StateFlow<AppUiState> =
        combine(repo.settingsFlow, isRunning, statusText) { settings, running, status ->
            AppUiState(settings = settings, isRunning = running, statusText = status)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    fun setMode(mode: Mode) {
        // 1) always stop torch when changing mode
        forceStop()

        // 2) persist mode so UI updates selected tab everywhere
        viewModelScope.launch {
            repo.setMode(mode)
        }
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
        torch.shutdown()
    }

    fun toggleRunning() {
        val willRun = !isRunning.value
        isRunning.value = willRun

        if (!willRun) {
            statusText.value = "IDLE"
            torch.shutdown()
            return
        }

        if (!torch.isTorchAvailable()) {
            isRunning.value = false
            statusText.value = "NO TORCH"
            torch.shutdown()
            return
        }

        val s = uiState.value.settings
        val level = if (s.autoBrightness) 1f else 0.7f

        torch.setLevel(level)
        torch.setEnabled(true)
        statusText.value = "RUNNING"
    }

    override fun onCleared() {
        torch.shutdown()
        super.onCleared()
    }
}
