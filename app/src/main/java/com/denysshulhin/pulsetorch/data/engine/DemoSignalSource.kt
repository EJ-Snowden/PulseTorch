package com.denysshulhin.pulsetorch.data.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Временный источник сигнала (без аудио).
 * Дает уровень 0..1 примерно как музыка: синус + шум + "удары".
 */
class DemoSignalSource {

    fun levels(fps: Int = 60): Flow<Float> = flow {
        val dtMs = (1000 / fps).coerceAtLeast(8)
        var t = 0f

        while (currentCoroutineContext().isActive) {
            t += dtMs / 1000f

            val wave = (sin(2f * PI.toFloat() * 0.9f * t) * 0.5f + 0.5f) // 0..1
            val beat = if ((t % 0.55f) < 0.06f) 1f else 0f
            val noise = Random.nextFloat() * 0.08f

            val level = (0.15f + wave * 0.45f + beat * 0.55f + noise).coerceIn(0f, 1f)
            emit(level)

            delay(dtMs.toLong())
        }
    }
}
