package com.denysshulhin.pulsetorch.data.pipeline

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

class DemoAudioSource : AudioSource {

    private val out = MutableSharedFlow<Float>(replay = 0, extraBufferCapacity = 64)

    override suspend fun start() {
        // nothing, generator runs in pipeline loop
    }

    override suspend fun stop() {
        // nothing
    }

    override fun amplitudeFlow(): Flow<Float> = out

    suspend fun emitLoop(isActive: () -> Boolean) {
        var t = 0f
        while (isActive()) {
            // 0..1 pseudo amplitude (music-like)
            val a = (sin(2f * PI.toFloat() * 1.2f * t) * 0.5f + 0.5f)
            val b = (sin(2f * PI.toFloat() * 0.33f * t) * 0.5f + 0.5f)
            val amp = (0.15f + 0.85f * (0.7f * a + 0.3f * b)).coerceIn(0f, 1f)

            out.tryEmit(amp)

            t += 0.016f
            delay(16)
        }
    }
}
