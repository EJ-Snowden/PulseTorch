package com.denysshulhin.pulsetorch.feature.mic

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MicSignalViewModel {
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level

    fun update(level01: Float) {
        _level.value = level01.coerceIn(0f, 1f)
    }
}
