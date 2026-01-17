package com.denysshulhin.pulsetorch.data.pipeline.file

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

@UnstableApi
class PcmTapAudioProcessor(
    private val amplitudeOut: AtomicReference<Float>
) : AudioProcessor {

    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    private var outputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET

    private var isActive = false
    private var buffer: ByteBuffer = EMPTY_BUFFER
    private var ended = false

    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        this.inputAudioFormat = inputAudioFormat
        outputAudioFormat = inputAudioFormat
        isActive = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
        return outputAudioFormat
    }

    override fun isActive(): Boolean = isActive

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        // Copy through unchanged
        val out = ByteBuffer.allocateDirect(inputBuffer.remaining()).order(ByteOrder.nativeOrder())
        out.put(inputBuffer)
        out.flip()
        buffer = out

        // Compute RMS from PCM16 if possible
        if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            computeRmsPcm16(out.duplicate())
        }
    }

    override fun queueEndOfStream() {
        ended = true
    }

    override fun getOutput(): ByteBuffer {
        val out = buffer
        buffer = EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = ended && buffer === EMPTY_BUFFER

    override fun flush() {
        buffer = EMPTY_BUFFER
        ended = false
        amplitudeOut.set(0f)
    }

    @OptIn(UnstableApi::class)
    override fun reset() {
        flush()
        inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        outputAudioFormat = AudioProcessor.AudioFormat.NOT_SET
        isActive = false
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

        // map to 0..1 (analyzer will normalize)
        val amp = (rms * 2.2f).coerceIn(0f, 1f)
        amplitudeOut.set(amp)
    }

    companion object {
        private val EMPTY_BUFFER: ByteBuffer = ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())
    }
}