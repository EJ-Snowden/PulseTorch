package com.denysshulhin.pulsetorch.data.pipeline.file

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.denysshulhin.pulsetorch.data.pipeline.AudioSource

@UnstableApi
class FileAudioSource(
    private val controller: FilePlaybackController,
    private val uriProvider: () -> Uri?,
    private val onMissingUri: (() -> Unit)? = null
) : AudioSource {

    override suspend fun start() {
        val uri = uriProvider() ?: run {
            onMissingUri?.invoke()
            return
        }

        controller.ensurePlayer()
        controller.prepare(uri)
        controller.play()
    }

    override suspend fun stop() {
        controller.pause()
    }

    override fun readAmplitude01(): Float? {
        return controller.latestAmplitude01()
    }
}
