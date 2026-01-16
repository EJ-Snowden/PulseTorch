package com.denysshulhin.pulsetorch.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.denysshulhin.pulsetorch.R
import com.denysshulhin.pulsetorch.core.permissions.AudioPermission
import com.denysshulhin.pulsetorch.core.runtime.PulseTorchRuntime
import com.denysshulhin.pulsetorch.data.pipeline.AudioPipelineManager
import com.denysshulhin.pulsetorch.data.pipeline.BasicAudioAnalyzer
import com.denysshulhin.pulsetorch.data.pipeline.BasicEffectEngine
import com.denysshulhin.pulsetorch.data.pipeline.DemoAudioSource
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

class PulseTorchForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.denysshulhin.pulsetorch.action.START"
        const val ACTION_STOP = "com.denysshulhin.pulsetorch.action.STOP"

        private const val CHANNEL_ID = "pulsetorch_run"
        private const val CHANNEL_NAME = "PulseTorch running"
        private const val NOTIF_ID = 2001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var repo: SettingsRepository
    private lateinit var torch: AndroidTorchController
    private lateinit var pipeline: AudioPipelineManager

    private val isRunning = MutableStateFlow(false)
    private val statusText = MutableStateFlow("IDLE")
    private val signalLevel01 = MutableStateFlow(0f)

    private lateinit var uiState: StateFlow<AppUiState>

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()

        repo = SettingsRepository(applicationContext)
        torch = AndroidTorchController(applicationContext, serviceScope)

        pipeline = AudioPipelineManager(
            torch = torch,
            sourceFactory = { mode: Mode ->
                when (mode) {
                    Mode.MIC -> MicAudioSource(sampleRate = 44100, chunkMs = 30)
                    Mode.FILE -> DemoAudioSource()
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

        uiState =
            combine(repo.settingsFlow, isRunning, statusText, signalLevel01) { settings, running, status, sig ->
                AppUiState(
                    settings = settings,
                    isRunning = running,
                    statusText = status,
                    signalLevel01 = sig
                )
            }.stateIn(
                serviceScope,
                SharingStarted.Eagerly,
                AppUiState(settings = AppSettings())
            )

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    private fun handleStart() {
        if (isRunning.value) {
            updateNotification()
            return
        }

        // Permissions that can crash startForeground/notify on newer Android
        if (Build.VERSION.SDK_INT >= 33) {
            val notifGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!notifGranted) {
                setStatus("NOTIFICATION PERMISSION REQUIRED")
                stopSelf()
                return
            }
        }

        val camGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        if (!camGranted) {
            setStatus("CAMERA PERMISSION REQUIRED")
            stopSelf()
            return
        }

        // Torch check
        if (!torch.isTorchAvailable()) {
            setStatus("NO TORCH")
            stopSelf()
            return
        }

        val mode = uiState.value.settings.mode

        // Mic permission only when MIC mode
        if (mode == Mode.MIC && !AudioPermission.hasRecordAudio(applicationContext)) {
            setStatus("MIC PERMISSION REQUIRED")
            stopSelf()
            return
        }

        // Foreground notification must be shown immediately
        val notification = buildNotification("Running")

        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                if (mode == Mode.MIC) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                startForeground(NOTIF_ID, notification, type)
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIF_ID, notification)
            }
        }.onFailure {
            // If we failed here, app would otherwise crash. Show status and stop.
            setStatus("FOREGROUND START FAILED")
            stopSelf()
            return
        }

        acquireWakeLock()

        isRunning.value = true
        statusText.value = "RUNNING"
        PulseTorchRuntime.setRunning(true)
        PulseTorchRuntime.setStatus("RUNNING")

        pipeline.start(serviceScope, uiState)
        updateNotification()
    }

    private fun handleStop() {
        if (!isRunning.value) {
            runCatching { torch.shutdown() }
            stopForegroundCompat()
            stopSelf()
            PulseTorchRuntime.resetUi()
            return
        }

        isRunning.value = false
        statusText.value = "IDLE"
        signalLevel01.value = 0f

        PulseTorchRuntime.setRunning(false)
        PulseTorchRuntime.setSignal(0f)
        PulseTorchRuntime.setStatus("IDLE")

        runCatching { pipeline.stop() }
        runCatching { torch.shutdown() }
        releaseWakeLock()

        stopForegroundCompat()
        stopSelf()
    }

    private fun setStatus(text: String) {
        statusText.value = text
        PulseTorchRuntime.setStatus(text)
        updateNotification()
    }

    private fun updateNotification() {
        val title = "PulseTorch"
        val content = if (isRunning.value) {
            "Running - ${uiState.value.settings.mode.name}"
        } else {
            statusText.value
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching {
            nm.notify(NOTIF_ID, buildNotification(content, title))
        }
    }

    private fun buildNotification(contentText: String, title: String = "PulseTorch"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        nm.createNotificationChannel(ch)
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
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
        PulseTorchRuntime.setRunning(false)
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
