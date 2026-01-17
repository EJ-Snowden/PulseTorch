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

    // File playback UI
    private val _fileIsPlaying = MutableStateFlow(false)
    val fileIsPlaying: StateFlow<Boolean> = _fileIsPlaying

    private val _filePosMs = MutableStateFlow(0L)
    val filePosMs: StateFlow<Long> = _filePosMs

    private val _fileDurMs = MutableStateFlow(0L)
    val fileDurMs: StateFlow<Long> = _fileDurMs

    fun setRunning(v: Boolean) { _isRunning.value = v }
    fun setStatus(v: String) { _statusText.value = v }
    fun setSignal(v: Float) { _signalLevel01.value = v.coerceIn(0f, 1f) }

    fun setFileIsPlaying(v: Boolean) { _fileIsPlaying.value = v }
    fun setFilePosMs(v: Long) { _filePosMs.value = v.coerceAtLeast(0L) }
    fun setFileDurMs(v: Long) { _fileDurMs.value = v.coerceAtLeast(0L) }

    fun resetUi() {
        _isRunning.value = false
        _statusText.value = "IDLE"
        _signalLevel01.value = 0f

        _fileIsPlaying.value = false
        _filePosMs.value = 0L
        _fileDurMs.value = 0L
    }
}
