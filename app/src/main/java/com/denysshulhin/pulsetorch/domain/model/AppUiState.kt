package com.denysshulhin.pulsetorch.domain.model

data class AppUiState(
    val settings: AppSettings = AppSettings(),
    val isRunning: Boolean = false,
    val statusText: String = "IDLE"
)
