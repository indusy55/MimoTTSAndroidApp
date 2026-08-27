package com.indusy55.mimottsapp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class StreamingAudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 24000

    @Synchronized
    fun init() {

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) return

        audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: RuntimeException) {
            null
        }
        audioTrack?.play()
    }

    @Synchronized
    fun write(data: ByteArray) {
        try {
            audioTrack?.write(data, 0, data.size)
        } catch (_: IllegalStateException) {
            audioTrack = null
        }
    }

    @Synchronized
    fun stop() {

        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            audioTrack = null
        }
    }

    @Synchronized
    fun release() {
        stop()
    }
}
