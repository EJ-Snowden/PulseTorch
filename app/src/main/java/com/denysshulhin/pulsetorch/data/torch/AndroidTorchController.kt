package com.denysshulhin.pulsetorch.data.torch

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.denysshulhin.pulsetorch.domain.contracts.TorchController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AndroidTorchController(
    context: Context,
    private val scope: CoroutineScope
) : TorchController {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraId: String? = null
    private var maxStrengthLevel: Int = 1

    private var enabled: Boolean = false
    private var level01: Float = 1f

    private var pwmJob: Job? = null

    override fun isTorchAvailable(): Boolean = resolveTorchCameraId() != null

    override fun supportsStrengthControl(): Boolean {
        resolveTorchCameraId() ?: return false
        return Build.VERSION.SDK_INT >= 33 && maxStrengthLevel > 1
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        apply()
    }

    override fun setLevel(level01: Float) {
        this.level01 = level01.coerceIn(0f, 1f)
        if (enabled) apply()
    }

    override fun shutdown() {
        enabled = false
        stopPwm()
        safeSetTorch(false)
    }

    private fun apply() {
        val id = resolveTorchCameraId() ?: return

        if (!enabled) {
            stopPwm()
            safeSetTorch(false)
            return
        }

        val lvl = level01.coerceIn(0f, 1f)
        if (lvl <= 0f) {
            stopPwm()
            safeSetTorch(false)
            return
        }

        // Prefer native strength control on Android 13+ if supported
        if (Build.VERSION.SDK_INT >= 33 && maxStrengthLevel > 1) {
            stopPwm()
            val strength = (lvl * maxStrengthLevel.toFloat()).roundToInt().coerceIn(1, maxStrengthLevel)
            safeTurnOnWithStrength(id, strength)
            return
        }

        // Fallback 1: full on
        if (lvl >= 0.999f) {
            stopPwm()
            safeSetTorch(true)
            return
        }

        // Fallback 2: duty-cycle (PWM) to imitate brightness
        startPwm(level = lvl)
    }

    private fun resolveTorchCameraId(): String? {
        if (cameraId != null) return cameraId

        val ids = runCatching { cameraManager.cameraIdList }.getOrNull() ?: return null
        val best = ids.firstOrNull { id ->
            val c = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull() ?: return@firstOrNull false
            val hasFlash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val facing = c.get(CameraCharacteristics.LENS_FACING)
            hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: ids.firstOrNull { id ->
            val c = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull() ?: return@firstOrNull false
            c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }

        cameraId = best
        if (best != null) {
            maxStrengthLevel = readMaxStrength(best)
        }
        return cameraId
    }

    private fun readMaxStrength(id: String): Int {
        if (Build.VERSION.SDK_INT < 33) return 1
        val c = runCatching { cameraManager.getCameraCharacteristics(id) }.getOrNull() ?: return 1
        val max = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
        return max.coerceAtLeast(1)
    }

    private fun safeSetTorch(on: Boolean) {
        val id = resolveTorchCameraId() ?: return
        runCatching { cameraManager.setTorchMode(id, on) }
    }

    private fun safeTurnOnWithStrength(id: String, strength: Int) {
        if (Build.VERSION.SDK_INT < 33) {
            safeSetTorch(true)
            return
        }
        runCatching {
            cameraManager.turnOnTorchWithStrengthLevel(id, strength)
        }.onFailure {
            // fallback if device/API rejects strength call
            safeSetTorch(true)
        }
    }

    private fun startPwm(level: Float) {
        stopPwm()

        val id = resolveTorchCameraId() ?: return
        val periodMs = 20L // 50Hz. If hardware is slow you can raise to 30-40ms.
        val onMs = (periodMs * level).toLong().coerceIn(1L, periodMs - 1L)
        val offMs = (periodMs - onMs).coerceAtLeast(1L)

        pwmJob = scope.launch(Dispatchers.Default) {
            while (isActive && enabled) {
                runCatching { cameraManager.setTorchMode(id, true) }
                delay(onMs)
                runCatching { cameraManager.setTorchMode(id, false) }
                delay(offMs)
            }
            // safety off
            runCatching { cameraManager.setTorchMode(id, false) }
        }
    }

    private fun stopPwm() {
        pwmJob?.cancel()
        pwmJob = null
    }
}
