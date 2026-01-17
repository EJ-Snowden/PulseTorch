package com.denysshulhin.pulsetorch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import com.denysshulhin.pulsetorch.core.permissions.AudioPermission
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import com.denysshulhin.pulsetorch.data.pipeline.AudioPipelineManager
import com.denysshulhin.pulsetorch.data.pipeline.BasicAudioAnalyzer
import com.denysshulhin.pulsetorch.data.pipeline.BasicEffectEngine
import com.denysshulhin.pulsetorch.data.pipeline.DemoAudioSource
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
        const val EXTRA_SEEK_MS = "seek_ms"

        private const val CHANNEL_ID = "pulsetorch_run"
        private const val CHANNEL_NAME = "PulseTorch running"
        private const val NOTIF_ID = 2001
    }

    // Main thread: Media3 player operations + notification updates
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Background thread: audio pipeline loop + torch control fallback PWM
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

        // IMPORTANT: Media3 player must be created/used on the main thread
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
                    Mode.MIC -> MicAudioSource(sampleRate = 44100, chunkMs = 30)

                    Mode.FILE -> FileAudioSource(
                        controller = fileController,
                        uriProvider = { uiState.value.settings.fileUri?.let(Uri::parse) },
                        onMissingUri = { setStatus("SELECT A FILE") }
                    )

                    Mode.SYSTEM -> DemoAudioSource()
                }
            },
            analyzer = BasicAudioAnalyzer(sampleRateFps = 60, rmsWindowMs = 180),
            engine = BasicEffectEngine(),
            onSignal = { v ->
                signalLevel01.value = v
                PulseTorchRuntime.setSignal(v)
            }
        )

        createNotificationChannel()
        updateNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Note: exceptions inside coroutines won't be caught here - each launch handles its own errors.
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()

            ACTION_FILE_TOGGLE -> handleFileToggle()
            ACTION_FILE_SEEK -> handleFileSeek(intent.getLongExtra(EXTRA_SEEK_MS, 0L))
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

        // Pre-warm player on MAIN thread for file mode to avoid first-touch races
        if (mode == Mode.FILE) {
            mainScope.launch {
                runCatching { fileController.ensurePlayer() }
                    .onFailure {
                        setStatus("FILE INIT ERROR")
                        handleStop()
                    }
            }
        }

        val notif = buildNotification(contentText = "Running", title = "PulseTorch")

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
        setStatus("RUNNING")
        PulseTorchRuntime.setRunning(true)

        // Run the pipeline loop on background dispatcher
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
        // If not running - start service first; FileAudioSource will start playback
        if (!isRunning.value) {
            handleStart()
            return
        }

        // All player operations must be on main
        mainScope.launch {
            runCatching {
                fileController.ensurePlayer()
                fileController.toggle()
            }.onFailure {
                setStatus("FILE PLAY ERROR")
                handleStop()
                return@launch
            }

            // If paused, drop amplitude + disable torch quickly
            if (!PulseTorchRuntime.fileIsPlaying.value) {
                signalLevel01.value = 0f
                PulseTorchRuntime.setSignal(0f)
                torch.setEnabled(false)
                torch.setLevel(0f)
            }
        }
    }

    private fun handleFileSeek(posMs: Long) {
        // Player operations must be on main
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
        // Make stop idempotent
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

        // Release player on main to prevent "wrong thread" crashes
        mainScope.launch {
            runCatching { fileController.stopAndRelease() }
            stopForegroundCompat()
            stopSelfSafely()

            if (!wasRunning) {
                PulseTorchRuntime.resetUi()
            }
        }

        updateNotification()
    }

    private fun setStatus(text: String) {
        statusText.value = text
        PulseTorchRuntime.setStatus(text)
        updateNotification()
    }

    private fun updateNotification() {
        val content = if (isRunning.value) {
            "Running - ${uiState.value.settings.mode.name}"
        } else {
            statusText.value
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(contentText = content, title = "PulseTorch"))
    }

    private fun buildNotification(contentText: String, title: String): Notification {
        // Note: small icon should be a proper monochrome 24dp drawable ideally
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
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
        runCatching {
            wl.acquire()
            wakeLock = wl
        }.onFailure {
            wakeLock = null
        }
    }

    private fun releaseWakeLock() {
        val wl = wakeLock ?: return
        wakeLock = null
        runCatching {
            if (wl.isHeld) wl.release()
        }
    }

    override fun onDestroy() {
        runCatching { pipeline.stop() }
        runCatching { torch.shutdown() }
        releaseWakeLock()

        // Release player on main
        mainScope.launch {
            runCatching { fileController.stopAndRelease() }
        }

        PulseTorchRuntime.setRunning(false)
        super.onDestroy()

        mainScope.cancel()
        workScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
