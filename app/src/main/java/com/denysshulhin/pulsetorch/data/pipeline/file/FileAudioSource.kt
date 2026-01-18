package com.denysshulhin.pulsetorch.data.pipeline.file

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import com.denysshulhin.pulsetorch.data.pipeline.AudioFeatures
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

    override fun readFeatures(): AudioFeatures? {
        // Никогда не трогаем ExoPlayer.
        if (!controller.isActuallyPlaying()) {
            return AudioFeatures(energy01 = 0f, flux01 = 0f)
        }

        return AudioFeatures(
            energy01 = controller.latestEnergy01(),
            flux01 = controller.latestFlux01()
        )
    }
}
