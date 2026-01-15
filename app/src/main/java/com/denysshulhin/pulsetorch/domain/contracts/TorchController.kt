package com.denysshulhin.pulsetorch.domain.contracts

interface TorchController {
    fun isTorchAvailable(): Boolean
    fun supportsStrengthControl(): Boolean

    fun setEnabled(enabled: Boolean)
    fun setLevel(level01: Float)

    fun shutdown()
}
