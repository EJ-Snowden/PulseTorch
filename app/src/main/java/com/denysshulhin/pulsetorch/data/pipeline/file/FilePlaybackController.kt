package com.denysshulhin.pulsetorch.data.pipeline.file

import android.content.Context
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@UnstableApi
class FilePlaybackController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var player: ExoPlayer? = null
    private var visualizer: Visualizer? = null
    private var tickerJob: Job? = null

    @Volatile private var latestAmp01: Float = 0f

    private suspend fun getOrCreatePlayer(): ExoPlayer = withContext(Dispatchers.Main.immediate) {
        player?.let { return@withContext it }

        val p = ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    PulseTorchRuntime.setFileIsPlaying(isPlaying)
                    if (!isPlaying) {
                        latestAmp01 = 0f
                    }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    attachVisualizer(audioSessionId)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    // duration becomes known around READY
                    val dur = duration.takeIf { it > 0 } ?: 0L
                    PulseTorchRuntime.setFileDurMs(dur)

                    if (playbackState == Player.STATE_ENDED) {
                        PulseTorchRuntime.setFileIsPlaying(false)
                        latestAmp01 = 0f
                    }
                }
            })
        }

        player = p
        startTicker()
        return@withContext p
    }

    suspend fun ensurePlayer() {
        getOrCreatePlayer()
    }

    suspend fun prepare(uri: Uri) = withContext(Dispatchers.Main.immediate) {
        val p = getOrCreatePlayer()
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()

        // reset UI immediately
        PulseTorchRuntime.setFilePosMs(0L)
        PulseTorchRuntime.setFileDurMs(p.duration.takeIf { it > 0 } ?: 0L)
    }

    suspend fun play() = withContext(Dispatchers.Main.immediate) { player?.play() }
    suspend fun pause() = withContext(Dispatchers.Main.immediate) { player?.pause() }

    suspend fun toggle() = withContext(Dispatchers.Main.immediate) {
        val p = player ?: return@withContext
        if (p.isPlaying) p.pause() else p.play()
    }

    suspend fun seekTo(posMs: Long) = withContext(Dispatchers.Main.immediate) {
        val p = player ?: return@withContext
        val dur = p.duration.takeIf { it > 0 } ?: 0L
        val target = posMs.coerceIn(0L, dur)
        p.seekTo(target)
        PulseTorchRuntime.setFilePosMs(target)
    }

    fun latestAmplitude01(): Float = latestAmp01.coerceIn(0f, 1f)

    fun stopAndRelease() {
        scope.launch(Dispatchers.Main.immediate) {
            runCatching { visualizer?.release() }
            visualizer = null

            tickerJob?.cancel()
            tickerJob = null

            val p = player ?: return@launch
            player = null

            runCatching { p.stop() }
            runCatching { p.release() }
        }

        latestAmp01 = 0f
        PulseTorchRuntime.setFileIsPlaying(false)
        PulseTorchRuntime.setFilePosMs(0L)
        PulseTorchRuntime.setFileDurMs(0L)
    }

    private fun startTicker() {
        if (tickerJob != null) return

        tickerJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val p = player
                if (p != null) {
                    PulseTorchRuntime.setFilePosMs(p.currentPosition.coerceAtLeast(0L))
                    val dur = p.duration.takeIf { it > 0 } ?: 0L
                    PulseTorchRuntime.setFileDurMs(dur)
                }
                delay(200)
            }
        }
    }

    private fun attachVisualizer(sessionId: Int) {
        runCatching { visualizer?.release() }
        visualizer = null

        if (sessionId == 0) return

        runCatching {
            val v = Visualizer(sessionId)

            val range = Visualizer.getCaptureSizeRange()
            v.captureSize = range.last().coerceAtLeast(range.first())

            val rate = (Visualizer.getMaxCaptureRate() / 2).coerceAtLeast(8000)

            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        val w = waveform ?: return
                        // waveform bytes are 0..255 with 128 as center (device dependent but usually)
                        var sum = 0.0
                        for (b in w) {
                            val x = (b.toInt() - 128) / 128.0
                            sum += x * x
                        }
                        val rms = sqrt(sum / w.size).toFloat()

                        // map to 0..1 (tuned a bit)
                        val amp = (rms * 2.6f).coerceIn(0f, 1f)
                        latestAmp01 = amp
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // not used
                    }
                },
                rate,
                true,
                false
            )

            v.enabled = true
            visualizer = v
        }.onFailure {
            // Visualizer can fail on some devices or without MODIFY_AUDIO_SETTINGS
            visualizer = null
            latestAmp01 = 0f
        }
    }
}
