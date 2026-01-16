package com.denysshulhin.pulsetorch.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.denysshulhin.pulsetorch.core.permissions.AudioPermission
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchServiceController
import com.denysshulhin.pulsetorch.data.settings.SettingsRepository
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Effect
import com.denysshulhin.pulsetorch.domain.model.Mode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val repo = SettingsRepository(ctx)

    val uiState: StateFlow<AppUiState> =
        combine(
            repo.settingsFlow,
            PulseTorchRuntime.isRunning,
            PulseTorchRuntime.statusText,
            PulseTorchRuntime.signalLevel01
        ) { settings, running, status, sig ->
            AppUiState(
                settings = settings,
                isRunning = running,
                statusText = status,
                signalLevel01 = sig
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState())

    fun setMode(mode: Mode) {
        // You can allow changing mode while running, pipeline manager will restart
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
        PulseTorchServiceController.stop(ctx)
    }

    fun toggleRunning() {
        val running = uiState.value.isRunning
        if (running) {
            PulseTorchServiceController.stop(ctx)
            return
        }

        // mic permission check only for MIC mode
        val mode = uiState.value.settings.mode
        if (mode == Mode.MIC && !AudioPermission.hasRecordAudio(ctx)) {
            PulseTorchRuntime.setStatus("MIC PERMISSION REQUIRED")
            return
        }

        PulseTorchServiceController.start(ctx)
    }
}
