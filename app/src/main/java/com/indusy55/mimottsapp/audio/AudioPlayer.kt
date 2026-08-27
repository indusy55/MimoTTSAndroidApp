package com.indusy55.mimottsapp.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import java.io.FileOutputStream

class AudioPlayer(private val context: Context) {
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun playBase64Audio(base64Data: String) {
        val audioBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
        val extension = when {
            audioBytes.size >= 4 && audioBytes.copyOfRange(0, 4).contentEquals(byteArrayOf(0x52, 0x49, 0x46, 0x46)) -> "wav"
            audioBytes.size >= 3 && audioBytes.copyOfRange(0, 3).contentEquals(byteArrayOf(0x49, 0x44, 0x33)) -> "mp3"
            audioBytes.size >= 8 && audioBytes.copyOfRange(4, 8).contentEquals(byteArrayOf(0x66, 0x74, 0x79, 0x70)) -> "m4a"
            else -> "audio"
        }
        val tempFile = File(context.cacheDir, "temp_audio.$extension")
        FileOutputStream(tempFile).use { it.write(audioBytes) }

        val mediaItem = MediaItem.fromUri(tempFile.toURI().toString())
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun release() {
        player.release()
    }
}
