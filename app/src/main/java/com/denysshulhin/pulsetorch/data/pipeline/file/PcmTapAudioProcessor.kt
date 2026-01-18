package com.denysshulhin.pulsetorch.data.pipeline.file

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

@UnstableApi
class PcmTapAudioProcessor(
    private val energyOut: AtomicReference<Float>
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        // Пропускаем только PCM16, иначе просто "пасстру" (без анализа).
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            inputAudioFormat
        } else {
            // Оставим формат как есть, но анализ будет пропущен.
            inputAudioFormat
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        // Важно: output buffer должен быть новым/валидным, не reuse-им один и тот же кусок.
        val out = replaceOutputBuffer(inputBuffer.remaining())
        out.put(inputBuffer)
        out.flip()

        // Анализируем только PCM16
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            computeRmsPcm16(out.duplicate())
        }
    }

    override fun onQueueEndOfStream() {
        // nothing
    }

    override fun onFlush() {
        energyOut.set(0f)
    }

    override fun onReset() {
        energyOut.set(0f)
    }

    private fun computeRmsPcm16(bb: ByteBuffer) {
        bb.order(ByteOrder.LITTLE_ENDIAN)

        var sum = 0.0
        var n = 0

        while (bb.remaining() >= 2) {
            val s = bb.short.toInt()
            val v = s / 32768.0
            sum += v * v
            n++
        }
        if (n <= 0) return

        val rms = sqrt(sum / n).toFloat()
        // маппинг 0..1
        val energy = (rms * 2.4f).coerceIn(0f, 1f)
        energyOut.set(energy)
    }
}
