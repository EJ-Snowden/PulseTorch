package com.denysshulhin.pulsetorch.domain.model

data class AppSettings(
    val mode: Mode = Mode.MIC,
    val effect: Effect = Effect.STROBE,

    // Home
    val sensitivity: Float = 0.75f,
    val smoothness: Float = 0.40f,

    // Settings
    val autoBrightness: Boolean = true,
    val maxStrobeHz: Float = 10f,    // 1..20
    val micGain: Float = 1.4f,       // 0.5..2.0
    val smoothing: Float = 0.40f,    // 0..1
    val bassFocus: Boolean = true,
    val strobeWarning: Boolean = true
)
