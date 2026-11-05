package com.zak.lastfreq

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.zak.lastfreq.core.AudioMixer

/** Streams the synthesised mixer output to the device on a background thread. */
class AudioOut(private val mixer: AudioMixer) {
    @Volatile private var running = false
    @Volatile private var paused = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    fun start() {
        if (running) { paused = false; return }
        running = true
        paused = false
        val t = Thread {
            val rate = mixer.rate
            val minBuf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val chunk = 1024
            val bufBytes = maxOf(minBuf, chunk * 2 * 4)
            val tr = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (e: Exception) {
                null
            }
            if (tr == null) { running = false; return@Thread }
            track = tr
            try {
                tr.play()
                val buf = ShortArray(chunk)
                while (running) {
                    if (paused) {
                        Thread.sleep(40)
                        continue
                    }
                    mixer.render(buf, chunk)
                    tr.write(buf, 0, chunk)
                }
            } catch (e: Exception) {
                // audio is optional; the game keeps running without it
            } finally {
                try { tr.stop() } catch (e: Exception) {}
                tr.release()
                track = null
            }
        }
        t.isDaemon = true
        t.name = "lastfreq-audio"
        thread = t
        t.start()
    }

    fun pause() {
        paused = true
        try { track?.pause() } catch (e: Exception) {}
    }

    fun resume() {
        paused = false
        try { track?.play() } catch (e: Exception) {}
        if (!running) start()
    }

    fun stop() {
        running = false
        thread = null
    }
}
