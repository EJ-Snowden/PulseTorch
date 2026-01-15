package com.denysshulhin.pulsetorch.domain.model

enum class Mode { FILE, SYSTEM, MIC }

fun Mode.toTabIndex(): Int = when (this) {
    Mode.FILE -> 0
    Mode.SYSTEM -> 1
    Mode.MIC -> 2
}
