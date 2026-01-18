package com.denysshulhin.pulsetorch.data.pipeline.mic

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresPermission
import com.denysshulhin.pulsetorch.data.pipeline.AudioFeatures
import com.denysshulhin.pulsetorch.data.pipeline.AudioSource
import kotlin.math.abs
import kotlin.math.sqrt

class MicAudioSource(
    private val sampleRate: Int = 44100,
    private val chunkMs: Int = 20
) : AudioSource {

    private var record: AudioRecord? = null
    private var buffer: ShortArray = ShortArray(0)

    private var lastEnergy = 0f
    private var lastOut: AudioFeatures? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun start() {
        if (record != null) return

        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
            throw IllegalStateException("AudioRecord: bad min buffer size")
        }

        val chunkSamples = ((sampleRate * chunkMs) / 1000).coerceIn(256, 4096)
        val wantedBytes = chunkSamples * 2
        val finalBytes = maxOf(minBuf, wantedBytes * 2)

        val r = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channel,
            encoding,
            finalBytes
        )

        if (r.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { r.release() }
            throw IllegalStateException("AudioRecord init failed")
        }

        record = r
        buffer = ShortArray(chunkSamples)
        lastEnergy = 0f
        lastOut = null

        r.startRecording()
    }

    override suspend fun stop() {
        val r = record ?: return
        record = null
        runCatching { r.stop() }
        runCatching { r.release() }
        lastOut = null
    }

    override fun readFeatures(): AudioFeatures? {
        val r = record ?: return null
        if (Build.VERSION.SDK_INT >= 24 && r.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return null
        }

        val n = r.read(buffer, 0, buffer.size)
        if (n <= 0) return null

        var sum = 0.0
        for (i in 0 until n) {
            val v = buffer[i].toDouble() / 32768.0
            sum += v * v
        }
        val rms = sqrt(sum / n).toFloat()

        val energy = (rms * 2.6f).coerceIn(0f, 1f)
        val flux = abs(energy - lastEnergy).coerceIn(0f, 1f)
        lastEnergy = energy

        val out = AudioFeatures(energy01 = energy, flux01 = flux)
        lastOut = out
        return out
    }
}
