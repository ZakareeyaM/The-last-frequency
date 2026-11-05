package com.zak.lastfreq

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import com.zak.lastfreq.core.AudioMixer
import java.io.File

class MainActivity : Activity(), Host {
    private lateinit var app: App
    private lateinit var view: GameView
    private lateinit var audio: AudioOut
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        val mixer = AudioMixer(22050)
        val storage = Storage(File(filesDir, "lastfreq"))
        app = App(mixer, storage, this)
        audio = AudioOut(mixer)
        view = GameView(this, app)
        setContentView(view)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onResume() {
        super.onResume()
        audio.start()
        audio.resume()
        hideSystemBars()
    }

    override fun onPause() {
        app.autosave()
        audio.pause()
        super.onPause()
    }

    override fun onDestroy() {
        audio.stop()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        app.onBack()
    }

    override fun haptic(long: Boolean) {
        val v = vibrator ?: return
        val ms = if (long) 40L else 15L
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (e: Exception) {
        }
    }

    override fun quit() {
        runOnUiThread { finish() }
    }
}
