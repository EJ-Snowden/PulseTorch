package com.denysshulhin.pulsetorch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import com.denysshulhin.pulsetorch.R
import com.denysshulhin.pulsetorch.app.MainActivity
import com.denysshulhin.pulsetorch.core.permissions.AudioPermission
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import com.denysshulhin.pulsetorch.data.pipeline.AudioPipelineManager
import com.denysshulhin.pulsetorch.data.pipeline.BasicAudioAnalyzer
import com.denysshulhin.pulsetorch.data.pipeline.BasicEffectEngine
import com.denysshulhin.pulsetorch.data.pipeline.file.FileAudioSource
import com.denysshulhin.pulsetorch.data.pipeline.file.FilePlaybackController
import com.denysshulhin.pulsetorch.data.pipeline.mic.MicAudioSource
import com.denysshulhin.pulsetorch.data.settings.SettingsRepository
import com.denysshulhin.pulsetorch.data.torch.AndroidTorchController
import com.denysshulhin.pulsetorch.domain.model.AppSettings
import com.denysshulhin.pulsetorch.domain.model.AppUiState
import com.denysshulhin.pulsetorch.domain.model.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@UnstableApi
class PulseTorchForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.denysshulhin.pulsetorch.action.START"
        const val ACTION_STOP = "com.denysshulhin.pulsetorch.action.STOP"

        const val ACTION_FILE_TOGGLE = "com.denysshulhin.pulsetorch.action.FILE_TOGGLE"
        const val ACTION_FILE_SEEK = "com.denysshulhin.pulsetorch.action.FILE_SEEK"

        const val ACTION_FILE_SEEK_REL = "com.denysshulhin.pulsetorch.action.FILE_SEEK_REL"
        const val EXTRA_SEEK_DELTA_MS = "seek_delta_ms"
        const val EXTRA_SEEK_MS = "seek_ms"

        private const val CHANNEL_ID = "pulsetorch_run"
        private const val CHANNEL_NAME = "PulseTorch running"
        private const val NOTIF_ID = 2001
    }

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val workScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var repo: SettingsRepository
    private lateinit var torch: AndroidTorchController
    private lateinit var pipeline: AudioPipelineManager
    private lateinit var fileController: FilePlaybackController

    private val isRunning = MutableStateFlow(false)
    private val statusText = MutableStateFlow("IDLE")
    private val signalLevel01 = MutableStateFlow(0f)

    private lateinit var uiState: StateFlow<AppUiState>

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        repo = SettingsRepository(applicationContext)
        torch = AndroidTorchController(applicationContext, workScope)
        fileController = FilePlaybackController(applicationContext, mainScope)

        uiState =
            combine(repo.settingsFlow, isRunning, statusText, signalLevel01) { settings, running, status, sig ->
                AppUiState(
                    settings = settings,
                    isRunning = running,
                    statusText = status,
                    signalLevel01 = sig
                )
            }.stateIn(
                mainScope,
                SharingStarted.Eagerly,
                AppUiState(settings = AppSettings())
            )

        pipeline = AudioPipelineManager(
            torch = torch,
            sourceFactory = { mode ->
                when (mode) {
                    Mode.MIC -> MicAudioSource(sampleRate = 44100, chunkMs = 20)

                    Mode.FILE -> FileAudioSource(
                        controller = fileController,
                        uriProvider = { uiState.value.settings.fileUri?.let(Uri::parse) },
                        onMissingUri = { setStatus("SELECT A FILE") }
                    )

                    Mode.SYSTEM -> MicAudioSource(sampleRate = 44100, chunkMs = 20)
                }
            },
            analyzer = BasicAudioAnalyzer(fps = 60),
            engine = BasicEffectEngine(),
            onSignal = { v ->
                signalLevel01.value = v
                PulseTorchRuntime.setSignal(v)
                // keep notification fresh, but not too often
                if ((System.currentTimeMillis() % 5L) == 0L) updateNotification()
            }
        )

        createNotificationChannel()
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            ACTION_FILE_TOGGLE -> handleFileToggle()
            ACTION_FILE_SEEK -> handleFileSeek(intent.getLongExtra(EXTRA_SEEK_MS, 0L))
            ACTION_FILE_SEEK_REL -> handleFileSeekRelative(intent.getLongExtra(EXTRA_SEEK_DELTA_MS, 0L))
        }
        return START_STICKY
    }

    private fun handleStart() {
        if (isRunning.value) {
            updateNotification()
            return
        }

        if (!torch.isTorchAvailable()) {
            setStatus("NO TORCH")
            stopSelfSafely()
            return
        }

        val mode = uiState.value.settings.mode

        if (mode == Mode.MIC && !AudioPermission.hasRecordAudio(applicationContext)) {
            setStatus("MIC PERMISSION REQUIRED")
            stopSelfSafely()
            return
        }

        if (mode == Mode.FILE && uiState.value.settings.fileUri.isNullOrBlank()) {
            setStatus("SELECT A FILE")
            stopSelfSafely()
            return
        }

        if (mode == Mode.FILE) {
            mainScope.launch {
                runCatching { fileController.ensurePlayer() }
                    .onFailure {
                        setStatus("FILE INIT ERROR")
                        handleStop()
                    }
            }
        }

        val notif = buildNotification()

        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, notif, fgTypesFor(mode))
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, notif)
            }
        }.onFailure {
            setStatus("FOREGROUND FAILED")
            stopSelfSafely()
            return
        }

        acquireWakeLock()

        isRunning.value = true
        statusText.value = "RUNNING"
        PulseTorchRuntime.setRunning(true)
        PulseTorchRuntime.setStatus("RUNNING")

        runCatching {
            pipeline.start(workScope, uiState)
        }.onFailure {
            setStatus("PIPELINE START ERROR")
            handleStop()
            return
        }

        updateNotification()
    }

    private fun handleFileToggle() {
        if (!isRunning.value) {
            handleStart()
            return
        }

        mainScope.launch {
            runCatching {
                fileController.ensurePlayer()
                fileController.toggle()
            }.onFailure {
                setStatus("FILE PLAY ERROR")
                handleStop()
                return@launch
            }

            updateNotification()
        }
    }

    private fun handleFileSeek(posMs: Long) {
        mainScope.launch {
            runCatching {
                fileController.ensurePlayer()
                fileController.seekTo(posMs)
            }.onFailure {
                setStatus("FILE SEEK ERROR")
                handleStop()
            }
        }
    }

    private fun handleStop() {
        val wasRunning = isRunning.value
        isRunning.value = false

        signalLevel01.value = 0f
        PulseTorchRuntime.setSignal(0f)
        PulseTorchRuntime.setRunning(false)
        PulseTorchRuntime.setStatus("IDLE")
        statusText.value = "IDLE"

        runCatching { pipeline.stop() }
        runCatching { torch.shutdown() }
        releaseWakeLock()

        mainScope.launch {
            runCatching { fileController.stopAndRelease() }
            stopForegroundCompat()
            stopSelfSafely()
            if (!wasRunning) PulseTorchRuntime.resetUi()
            updateNotification()
        }
    }

    private fun setStatus(text: String) {
        statusText.value = text
        PulseTorchRuntime.setStatus(text)
        updateNotification()
    }

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val mode = uiState.value.settings.mode
        val fileName = uiState.value.settings.fileName ?: "Selected audio"
        val isPlaying = PulseTorchRuntime.fileIsPlaying.value

        val title = "PulseTorch"
        val content = if (isRunning.value) {
            if (mode == Mode.FILE) {
                (if (isPlaying) "Playing" else "Paused") + " - " + fileName
            } else {
                "Running - ${mode.name}"
            }
        } else {
            statusText.value
        }

        val openAppIntent = Intent(this, com.denysshulhin.pulsetorch.app.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val piFlags = if (Build.VERSION.SDK_INT >= 23)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

        val contentPi = PendingIntent.getActivity(this, 10, openAppIntent, piFlags)

        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (mode == Mode.FILE && isRunning.value) {
            val pos = PulseTorchRuntime.filePosMs.value.coerceAtLeast(0L)
            val dur = PulseTorchRuntime.fileDurMs.value.coerceAtLeast(0L)

            if (dur > 0L) {
                b.setProgress(dur.toInt(), pos.coerceAtMost(dur).toInt(), false)
            }

            fun svcPi(req: Int, action: String, extras: (Intent.() -> Unit)? = null): PendingIntent {
                val i = Intent(this, PulseTorchForegroundService::class.java).apply {
                    this.action = action
                    extras?.invoke(this)
                }
                return PendingIntent.getService(this, req, i, piFlags)
            }

            b.addAction(
                R.drawable.ic_rewind_24,
                "-10s",
                svcPi(21, ACTION_FILE_SEEK_REL) { putExtra(EXTRA_SEEK_DELTA_MS, -10_000L) }
            )

            val toggleIcon = if (isPlaying) R.drawable.ic_pause_24 else R.drawable.ic_play_24
            val toggleText = if (isPlaying) "Pause" else "Play"
            b.addAction(toggleIcon, toggleText, svcPi(22, ACTION_FILE_TOGGLE))

            b.addAction(
                R.drawable.ic_forward_24,
                "+10s",
                svcPi(23, ACTION_FILE_SEEK_REL) { putExtra(EXTRA_SEEK_DELTA_MS, 10_000L) }
            )

            b.addAction(R.drawable.ic_stop_24, "Stop", svcPi(24, ACTION_STOP))
        }

        return b.build()
    }

    private fun handleFileSeekRelative(deltaMs: Long) {
        mainScope.launch {
            runCatching {
                fileController.ensurePlayer()

                val cur = PulseTorchRuntime.filePosMs.value
                val dur = PulseTorchRuntime.fileDurMs.value.takeIf { it > 0 } ?: Long.MAX_VALUE
                val target = (cur + deltaMs).coerceIn(0L, dur)

                fileController.seekTo(target)
                updateNotification()
            }.onFailure {
                setStatus("FILE SEEK ERROR")
            }
        }
    }

    private fun fgTypesFor(mode: Mode): Int {
        val camera = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        return when (mode) {
            Mode.MIC -> camera or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            Mode.FILE -> camera or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            Mode.SYSTEM -> camera or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    private fun stopSelfSafely() {
        runCatching { stopSelf() }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PulseTorch:Audio").apply {
            setReferenceCounted(false)
        }
        runCatching { wl.acquire(); wakeLock = wl }.onFailure { wakeLock = null }
    }

    private fun releaseWakeLock() {
        val wl = wakeLock ?: return
        wakeLock = null
        runCatching { if (wl.isHeld) wl.release() }
    }

    override fun onDestroy() {
        runCatching { pipeline.stop() }
        runCatching { torch.shutdown() }
        releaseWakeLock()

        mainScope.launch { runCatching { fileController.stopAndRelease() } }

        PulseTorchRuntime.setRunning(false)
        super.onDestroy()

        mainScope.cancel()
        workScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
