package com.denysshulhin.pulsetorch.core.runtime

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.denysshulhin.pulsetorch.service.PulseTorchForegroundService

object PulseTorchServiceController {

    @OptIn(UnstableApi::class)
    fun start(context: Context) {
        val i = Intent(context, PulseTorchForegroundService::class.java).apply {
            action = PulseTorchForegroundService.ACTION_START
        }
        context.startForegroundService(i)
    }

    @OptIn(UnstableApi::class)
    fun stop(context: Context) {
        val i = Intent(context, PulseTorchForegroundService::class.java).apply {
            action = PulseTorchForegroundService.ACTION_STOP
        }
        context.startService(i)
    }

    @OptIn(UnstableApi::class)
    fun fileTogglePlay(context: Context) {
        val i = Intent(context, PulseTorchForegroundService::class.java).apply {
            action = PulseTorchForegroundService.ACTION_FILE_TOGGLE
        }
        context.startService(i)
    }

    @OptIn(UnstableApi::class)
    fun fileSeek(context: Context, posMs: Long) {
        val i = Intent(context, PulseTorchForegroundService::class.java).apply {
            action = PulseTorchForegroundService.ACTION_FILE_SEEK
            putExtra(PulseTorchForegroundService.EXTRA_SEEK_MS, posMs)
        }
        context.startService(i)
    }
}
