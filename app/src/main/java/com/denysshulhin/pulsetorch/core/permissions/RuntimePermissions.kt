package com.denysshulhin.pulsetorch.core.permissions

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

object RuntimePermissions {

    fun hasCamera(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun hasPostNotifications(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT < 33) true
        else ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun requiredForStart(ctx: Context, needsMic: Boolean): Array<String> {
        val list = mutableListOf<String>()

        // torch uses camera service type, request it explicitly
        list += Manifest.permission.CAMERA

        if (Build.VERSION.SDK_INT >= 33) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }

        if (needsMic) {
            list += Manifest.permission.RECORD_AUDIO
        }

        // return only missing
        return list.filter { perm ->
            ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }
}
