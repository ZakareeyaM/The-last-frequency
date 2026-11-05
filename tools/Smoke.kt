package com.zak.lastfreq

import android.graphics.Canvas
import com.zak.lastfreq.core.AudioMixer
import com.zak.lastfreq.core.Content
import java.io.File
import kotlin.random.Random

/** JVM smoke test: drives every screen with stub graphics and random touches to catch crashes. */
fun main() {
    val dir = File(System.getProperty("java.io.tmpdir"), "lf-smoke-" + System.nanoTime())
    val host = object : Host { override fun haptic(long: Boolean) {} override fun quit() {} }
    val mixer = AudioMixer(22050)
    val app = App(mixer, Storage(dir), host)
    app.resize(800, 360)
    val canvas = Canvas()
    val buf = ShortArray(1024)
    val rnd = Random(42)
    var frames = 0
    var failures = 0

    fun frame() {
        app.update(0.016f)
        app.draw(canvas)
        mixer.render(buf, 1024)
        frames++
    }

    fun fuzz(label: String, n: Int) {
        try {
            for (i in 0 until n) {
                val x = rnd.nextFloat() * app.W
                val y = rnd.nextFloat() * 360f
                app.touch(0, 0, x, y)
                for (k in 0 until rnd.nextInt(1, 6)) {
                    frame()
                    app.touch(1, 0, x + rnd.nextFloat() * 60f - 30f, y + rnd.nextFloat() * 60f - 30f)
                }
                if (rnd.nextInt(3) == 0) {
                    app.touch(0, 1, rnd.nextFloat() * app.W, rnd.nextFloat() * 360f)
                    frame()
                    app.touch(2, 1, rnd.nextFloat() * app.W, rnd.nextFloat() * 360f)
                }
                app.touch(2, 0, x, y)
                frame()
                if (rnd.nextInt(40) == 0) app.onBack()
            }
        } catch (e: Throwable) {
            failures++
            println("FAIL in $label: $e")
            e.printStackTrace(System.out)
        }
    }

    // menu + new game flow
    fuzz("menu", 200)
    app.newGame(0)
    for (i in 0 until 80) frame()
    for (i in 0 until 6) { app.touch(0, 0, 300f, 200f); app.touch(2, 0, 300f, 200f); for (k in 0 until 40) frame() }
    println("after intro screen=" + app.screen.javaClass.simpleName)

    for (night in 1..5) {
        app.st.startNight(night)
        for (l in Content.locations) l.flag?.let { app.st.setFlag(it) }
        for (id in Content.clues.keys) app.st.addClue(id)
        for (t in Content.transmissions) if (t.night <= night) app.st.decoded.add(t.id)
        app.st.setFlag("harbour_open")
        for (tx in Content.transmissions.filter { it.night == night }) {
            val r = app.st.record(tx, 0.9f)
        }
        val screens = listOf(app.world, app.radio, app.journal, app.board, app.map, app.pause, app.finalScreen, app.summary, app.intro)
        for (s in screens) {
            for (scn in Content.scenes.keys) {
                app.st.scene = scn
                val sc = Content.scenes[scn]!!
                app.st.px = sc.spawnX * Content.TILE
                app.st.py = sc.spawnY * Content.TILE
                app.dialog = null
                if (s === app.summary) app.summary.prepare()
                if (s === app.intro) app.intro.setNight(night)
                app.world.reset()
                app.go(s, true)
                for (k in 0 until 10) frame()
                fuzz("night $night ${s.javaClass.simpleName} $scn", 60)
                // never leave the night via the clock or endings: reset game-over state
                app.st.ending = ""
            }
        }
        // endings
        for (e in Content.endings.keys) {
            app.ending.setEnding(e)
            app.go(app.ending, true)
            fuzz("ending $e", 40)
        }
        // save / load roundtrip
        app.st.ending = ""
        if (!app.saveTo(1)) { println("FAIL save"); failures++ }
        val back = app.storage.load(1)
        if (back == null) { println("FAIL load"); failures++ }
    }
    println("frames=$frames failures=$failures")
    println(if (failures == 0) "SMOKE OK" else "SMOKE FAILED")
}
