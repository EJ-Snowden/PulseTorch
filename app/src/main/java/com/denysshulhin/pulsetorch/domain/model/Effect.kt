package com.denysshulhin.pulsetorch.domain.model

enum class Effect { SMOOTH, PULSE, STROBE }

fun Effect.toChipIndex(): Int = when (this) {
    Effect.SMOOTH -> 0
    Effect.PULSE -> 1
    Effect.STROBE -> 2
}
