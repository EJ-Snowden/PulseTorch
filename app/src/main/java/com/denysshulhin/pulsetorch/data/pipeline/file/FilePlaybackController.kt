package com.denysshulhin.pulsetorch.data.pipeline.file

import android.content.Context
import android.media.audiofx.Visualizer
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

@UnstableApi
class FilePlaybackController(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var player: ExoPlayer? = null

    // Visualizer оставлен, но по умолчанию выключен (часто дает рваный звук)
    private val enableVisualizer = false
    private var visualizer: Visualizer? = null

    private var tickerJob: Job? = null

    // --- Thread-safe cached state for non-main callers (pipeline thread) ---
    @Volatile private var cachedIsPlaying: Boolean = false
    @Volatile private var cachedPosMs: Long = 0L
    @Volatile private var cachedDurMs: Long = 0L

    // --- features (thread-safe) ---
    private val energyRef = AtomicReference(0f)

    @Volatile private var latestEnergy01: Float = 0f
    @Volatile private var latestFlux01: Float = 0f
    @Volatile private var lastEnergy01: Float = 0f

    // ---------- Public safe getters (NO player access) ----------
    fun isActuallyPlaying(): Boolean = cachedIsPlaying
    fun latestPosMs(): Long = cachedPosMs.coerceAtLeast(0L)
    fun latestDurMs(): Long = cachedDurMs.coerceAtLeast(0L)
    fun latestEnergy01(): Float = latestEnergy01.coerceIn(0f, 1f)
    fun latestFlux01(): Float = latestFlux01.coerceIn(0f, 1f)

    // ---------- Player lifecycle (MAIN only) ----------
    private suspend fun getOrCreatePlayer(): ExoPlayer = withContext(Dispatchers.Main.immediate) {
        player?.let { return@withContext it }

        val tap = PcmTapAudioProcessor(energyRef)

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioOutputPlaybackParams)
                    .setAudioProcessors(arrayOf(tap))
                    .build()
            }
        }

        val p = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF

                val attrs = AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build()
                setAudioAttributes(attrs, true)

                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        cachedIsPlaying = isPlaying
                        PulseTorchRuntime.setFileIsPlaying(isPlaying)

                        if (!isPlaying) {
                            // мгновенно гасим фичи, чтобы пайплайн не держал фонарь
                            energyRef.set(0f)
                            latestEnergy01 = 0f
                            latestFlux01 = 0f
                            lastEnergy01 = 0f
                        }

                        if (enableVisualizer) {
                            visualizer?.enabled = isPlaying
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        val dur = duration.takeIf { it > 0 } ?: 0L
                        cachedDurMs = dur
                        PulseTorchRuntime.setFileDurMs(dur)

                        if (playbackState == Player.STATE_ENDED) {
                            cachedIsPlaying = false
                            PulseTorchRuntime.setFileIsPlaying(false)
                            energyRef.set(0f)
                            latestEnergy01 = 0f
                            latestFlux01 = 0f
                            lastEnergy01 = 0f
                        }
                    }

                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        if (enableVisualizer) attachVisualizer(audioSessionId)
                    }
                })
            }

        player = p
        startTickerIfNeeded()
        return@withContext p
    }

    suspend fun ensurePlayer() {
        getOrCreatePlayer()
    }

    suspend fun prepare(uri: Uri) = withContext(Dispatchers.Main.immediate) {
        val p = getOrCreatePlayer()
        p.setMediaItem(MediaItem.fromUri(uri))
        p.prepare()

        cachedPosMs = 0L
        PulseTorchRuntime.setFilePosMs(0L)

        val dur = p.duration.takeIf { it > 0 } ?: 0L
        cachedDurMs = dur
        PulseTorchRuntime.setFileDurMs(dur)

        // сброс фич при смене трека
        energyRef.set(0f)
        latestEnergy01 = 0f
        latestFlux01 = 0f
        lastEnergy01 = 0f
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

        cachedPosMs = target
        PulseTorchRuntime.setFilePosMs(target)
    }

    fun stopAndRelease() {
        scope.launch(Dispatchers.Main.immediate) {
            runCatching { visualizer?.release() }
            visualizer = null

            tickerJob?.cancel()
            tickerJob = null

            val p = player
            player = null
            runCatching { p?.stop() }
            runCatching { p?.release() }
        }

        cachedIsPlaying = false
        cachedPosMs = 0L
        cachedDurMs = 0L

        energyRef.set(0f)
        latestEnergy01 = 0f
        latestFlux01 = 0f
        lastEnergy01 = 0f

        PulseTorchRuntime.setFileIsPlaying(false)
        PulseTorchRuntime.setFilePosMs(0L)
        PulseTorchRuntime.setFileDurMs(0L)
    }

    // ---------- Ticker (MAIN only) ----------
    private fun startTickerIfNeeded() {
        if (tickerJob != null) return

        // 50 Hz обновление, чтобы и прогресс и фичи были плавные, но без перегруза
        tickerJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val p = player
                if (p != null) {
                    val pos = p.currentPosition.coerceAtLeast(0L)
                    val dur = p.duration.takeIf { it > 0 } ?: 0L

                    cachedPosMs = pos
                    cachedDurMs = dur
                    cachedIsPlaying = p.isPlaying

                    PulseTorchRuntime.setFilePosMs(pos)
                    PulseTorchRuntime.setFileDurMs(dur)
                    PulseTorchRuntime.setFileIsPlaying(p.isPlaying)

                    // PCM tap дает energy, flux делаем сами (и чуть сглаживаем)
                    val eRaw = energyRef.get().coerceIn(0f, 1f)

                    val e = (latestEnergy01 * 0.85f + eRaw * 0.15f).coerceIn(0f, 1f)
                    val f = (abs(e - lastEnergy01) * 3.2f).coerceIn(0f, 1f)

                    latestEnergy01 = e
                    latestFlux01 = (latestFlux01 * 0.70f + f * 0.30f).coerceIn(0f, 1f)
                    lastEnergy01 = e
                } else {
                    cachedIsPlaying = false
                }

                delay(20)
            }
        }
    }

    // ---------- Visualizer (optional) ----------
    private fun attachVisualizer(sessionId: Int) {
        runCatching { visualizer?.release() }
        visualizer = null

        if (sessionId == 0) return

        runCatching {
            val v = Visualizer(sessionId)

            val range = Visualizer.getCaptureSizeRange()
            val size = 512.coerceIn(range[0], range[1])
            v.captureSize = size

            // низкий rate, чтобы меньше ломало звук
            val rate = (Visualizer.getMaxCaptureRate() / 6).coerceIn(3000, 9000)

            v.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) = Unit

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) = Unit
                },
                rate,
                false,
                true
            )

            v.enabled = cachedIsPlaying
            visualizer = v
        }.onFailure {
            runCatching { visualizer?.release() }
            visualizer = null
        }
    }
}
