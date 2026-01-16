package com.denysshulhin.pulsetorch.data.pipeline

import kotlinx.coroutines.delay
import kotlin.math.sin

class DemoAudioSource(
    private val fps: Int = 60
) : AudioSource {

    private var t = 0f
    private var running = false
    private var last: Float? = null

    override suspend fun start() {
        running = true
        t = 0f
        last = 0f
    }

    override suspend fun stop() {
        running = false
        last = null
    }

    override fun readAmplitude01(): Float? = last

    // optional helper for pipeline (we’ll run it from pipeline loop)
    suspend fun tick() {
        if (!running) return
        val w = (sin(t) * 0.5f + 0.5f) // 0..1
        val wobble = (sin(t * 0.33f) * 0.15f + 0.85f).coerceIn(0f, 1f)
        last = (w * wobble).coerceIn(0f, 1f)
        t += 0.18f
        delay((1000 / fps).toLong())
    }
}
