package com.denysshulhin.pulsetorch.core.runtime

import android.content.Context
import android.content.Intent
import android.os.Build
import com.denysshulhin.pulsetorch.service.PulseTorchForegroundService

object PulseTorchServiceController {

    fun start(context: Context) {
        val i = Intent(context, PulseTorchForegroundService::class.java).apply {
            action = PulseTorchForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i)
        } else {
            context.startService(i)
        }
    }

    fun stop(context: Context) {
        val i = Intent(context, PulseTorchForegroundService::class.java).apply {
            action = PulseTorchForegroundService.ACTION_STOP
        }
        context.startService(i)
    }
}
