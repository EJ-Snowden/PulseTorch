package com.denysshulhin.pulsetorch.core.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PulseTorchRuntime {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _statusText = MutableStateFlow("IDLE")
    val statusText: StateFlow<String> = _statusText

    private val _signalLevel01 = MutableStateFlow(0f)
    val signalLevel01: StateFlow<Float> = _signalLevel01

    fun setRunning(v: Boolean) {
        _isRunning.value = v
    }

    fun setStatus(v: String) {
        _statusText.value = v
    }

    fun setSignal(v: Float) {
        _signalLevel01.value = v.coerceIn(0f, 1f)
    }

    fun resetUi() {
        _isRunning.value = false
        _statusText.value = "IDLE"
        _signalLevel01.value = 0f
    }
}
