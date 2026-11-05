package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import com.zak.lastfreq.core.Content
import com.zak.lastfreq.core.Kind
import com.zak.lastfreq.core.RadioSim
import com.zak.lastfreq.core.Recording
import com.zak.lastfreq.core.Morse
import kotlin.math.abs

/** Station log: transmissions, recordings, people and collected clues. */
class JournalScreen(app: App) : Screen(app) {
    var from: Screen? = null
    private var tab = 0
    private var scroll = 0f
    private var maxScroll = 0f
    private var dragId = -1
    private var dragY = 0f
    private var dragStartY = 0f
    private var moved = false
    private val tabNames = listOf("LOG", "TAPES", "PEOPLE", "CLUES")
    private val tabBtns = ArrayList<Btn>()
    private val bBack = Btn("back", "BACK", 8f, 8f, 60f, 24f)
    private val rows = ArrayList<Row>()
    private val tagNames = listOf("-", "KEY", "DOUBT", "LIE")

    private var playing: Recording? = null
    private var playT = 0f

    private class Row(val id: String, val y: Float, val h: Float, val kind: Int)

    fun openTab(i: Int) {
        tab = i
        scroll = 0f
    }

    override fun enter() {
        scroll = 0f
        playing = null
        for (i in tabNames.indices) {
            if (tabBtns.size <= i) tabBtns.add(Btn("t$i", tabNames[i], 0f, 8f, 70f, 24f))
        }
    }

    override fun update(dt: Float) {
        val p = playing ?: return
        val st = app.st
        val m = app.mixer
        val tx = Content.transmission(p.tid)
        if (tx == null) { playing = null; return }
        playT += dt
        val payload = RadioSim.payloadDuration(tx, st.flags)
        if (playT >= payload) { playing = null; return }
        m.staticLevel = (1f - p.quality) * 0.55f + 0.03f
        when (tx.kind) {
            Kind.VOICE, Kind.HIDDEN -> {
                val (idx, prog) = RadioSim.lineAt(tx.linesFor(st.flags), playT)
                if (idx >= 0 && prog < 0.86f) {
                    val ch = Content.character(tx.speaker)
                    m.voicePitch = ch.pitch
                    m.voiceRate = ch.rate
                    m.voiceBright = ch.bright
                    m.voiceRough = ch.rough
                    m.voiceVol = 0.4f + p.quality * 0.55f
                }
            }
            Kind.MORSE -> if (Morse.keyDown(tx.options[tx.answer], playT)) {
                m.toneFreq = 640f
                m.toneVol = p.quality * 0.8f
            }
            Kind.TONES -> {
                val i = RadioSim.toneAt(tx.code, playT)
                if (i >= 0) {
                    m.toneFreq = RadioSim.TONE_HZ[i]
                    m.toneVol = p.quality * 0.9f
                }
            }
        }
    }

    private fun layoutTabs() {
        val x0 = app.W / 2f - (tabBtns.size * 74f) / 2f + 20f
        for (i in tabBtns.indices) {
            tabBtns[i].x = x0 + i * 74f
            tabBtns[i].w = 70f
        }
        bBack.x = 8f
    }

    override fun draw(c: Canvas) {
        layoutTabs()
        val W = app.W
        val H = app.H
        val st = app.st
        UI.fill(c, 0f, 0f, W, H, Pal.DWOOD)
        UI.panel(c, 4f, 4f, W - 8f, H - 8f, 0xFF1D2128.toInt(), Pal.GREY)
        UI.button(c, bBack, Pal.AMBER, 11f)
        for (i in tabBtns.indices) {
            val b = tabBtns[i]
            b.label = tabNames[i]
            UI.button(c, b, if (i == tab) Pal.PALE else Pal.LGREY, 11f)
            if (i == tab) UI.fill(c, b.x + 4f, b.y + b.h - 3f, b.w - 8f, 2f, Pal.AMBER)
        }
        UI.text(c, st.brief(), W - 14f, 24f, 10f, Pal.LGREY, Paint.Align.RIGHT)

        val top = 42f
        val bottom = H - 12f
        c.save()
        c.clipRect(12f, top, W - 12f, bottom)
        rows.clear()
        var y = top + 4f - scroll
        when (tab) {
            0 -> y = drawLog(c, y)
            1 -> y = drawTapes(c, y)
            2 -> y = drawPeople(c, y)
            else -> y = drawClues(c, y)
        }
        c.restore()
        maxScroll = (y + scroll - bottom + 10f).coerceAtLeast(0f)
        if (maxScroll > 0f) {
            val frac = (bottom - top) / (bottom - top + maxScroll)
            val th = (bottom - top) * frac
            val ty = top + (scroll / maxScroll) * ((bottom - top) - th)
            UI.fill(c, W - 10f, ty, 3f, th, Pal.alpha(Pal.LGREY, 0.6f))
        }
    }

    private fun drawLog(c: Canvas, y0: Float): Float {
        val st = app.st
        val W = app.W
        var y = y0
        val body = 11f * UI.textScale
        UI.text(c, "OBJECTIVE", 20f, y + 10f, 10f, Pal.AMBER, Paint.Align.LEFT, true)
        y += 16f
        for (l in UI.wrap(st.objective(), W - 56f, body)) {
            UI.text(c, l, 20f, y + 10f, body, Pal.BONE)
            y += body + 3f
        }
        y += 8f
        UI.text(c, "RADIO LOG  (tap an entry to read it)", 20f, y + 10f, 10f, Pal.AMBER, Paint.Align.LEFT, true)
        y += 18f
        val done = Content.transmissions.filter { st.decoded.contains(it.id) }
        if (done.isEmpty()) {
            UI.text(c, "Nothing decoded yet.", 20f, y + 10f, body, Pal.LGREY)
            y += 18f
        }
        for (tx in done) {
            val h = 26f
            UI.panel(c, 16f, y, W - 40f, h, Pal.CHARCOAL, Pal.DGREY)
            UI.text(c, "N" + tx.night, 24f, y + 17f, 11f, Pal.AMBER, Paint.Align.LEFT, true)
            UI.text(c, tx.title, 56f, y + 17f, 11f, Pal.BONE)
            UI.text(c, Content.character(tx.speaker).name, W - 30f, y + 17f, 10f, Pal.LGREY, Paint.Align.RIGHT)
            rows.add(Row(tx.id, y, h, 0))
            y += h + 4f
        }
        y += 6f
        UI.text(c, "NIGHT NOTES", 20f, y + 10f, 10f, Pal.AMBER, Paint.Align.LEFT, true)
        y += 16f
        for (s in st.summary()) {
            for (l in UI.wrap(s, W - 56f, 10f * UI.textScale)) {
                UI.text(c, l, 20f, y + 9f, 10f * UI.textScale, Pal.LGREY)
                y += 12f * UI.textScale
            }
        }
        return y + 8f
    }

    private fun drawTapes(c: Canvas, y0: Float): Float {
        val st = app.st
        val W = app.W
        var y = y0
        if (st.recordings.isEmpty()) {
            UI.text(c, "No recordings. Hold a clear signal and press REC on the radio.", 20f, y + 12f, 11f, Pal.LGREY)
            return y + 30f
        }
        for (r in st.recordings) {
            val tx = Content.transmission(r.tid) ?: continue
            val h = 44f
            UI.panel(c, 16f, y, W - 40f, h, Pal.CHARCOAL, Pal.DGREY)
            UI.text(c, tx.title, 26f, y + 17f, 12f, Pal.BONE, Paint.Align.LEFT, true)
            val mins = 23 * 60 + r.minute
            val ts = String.format("%02d:%02d", (mins / 60) % 24, mins % 60)
            UI.text(c, "Night " + r.night + "  " + ts + "  " + String.format("%.2f", r.freq) + " MHz  quality " + (r.quality * 100f).toInt() + "%",
                26f, y + 33f, 10f, Pal.LGREY)
            val isPlaying = playing === r
            val bp = Btn("play", if (isPlaying) "STOP" else "PLAY", W - 150f, y + 8f, 54f, 28f)
            val bt = Btn("tag", tagNames[r.tag.coerceIn(0, 3)], W - 90f, y + 8f, 56f, 28f)
            UI.button(c, bp, Pal.TEAL, 11f)
            UI.button(c, bt, if (r.tag == 0) Pal.LGREY else Pal.AMBER, 10f)
            rows.add(Row(r.id, y, h, 1))
            y += h + 5f
        }
        UI.text(c, "TAG is your own annotation: KEY, DOUBT or LIE.", 20f, y + 12f, 10f, Pal.LGREY)
        return y + 24f
    }

    private fun drawPeople(c: Canvas, y0: Float): Float {
        val st = app.st
        val W = app.W
        var y = y0
        val body = 11f * UI.textScale
        var any = false
        for (ch in Content.characters) {
            val met = st.decoded.any { Content.transmission(it)?.speaker == ch.id }
            if (!met) continue
            any = true
            val lines = UI.wrap(ch.bio, W - 70f, body, false, UI.serif)
            val h = 32f + lines.size * (body + 3f)
            UI.panel(c, 16f, y, W - 40f, h, Pal.CHARCOAL, Pal.DGREY)
            UI.text(c, ch.name, 26f, y + 17f, 12f, Pal.PALE, Paint.Align.LEFT, true)
            UI.text(c, ch.role, 26f, y + 30f, 10f, Pal.LGREY)
            if (ch.id == "marta" || ch.id == "tom" || ch.id == "wren" || ch.id == "callum" || st.trust.containsKey(ch.id)) {
                val tv = st.trustOf(ch.id)
                UI.text(c, "TRUST", W - 132f, y + 17f, 9f, Pal.LGREY, Paint.Align.RIGHT)
                for (k in 0 until 5) {
                    UI.fill(c, W - 124f + k * 14f, y + 9f, 11f, 9f, if (k < tv) Pal.TEAL else Pal.DGREY)
                }
                if (tv < 0) UI.text(c, "distrustful", W - 54f, y + 30f, 9f, Pal.ORANGE, Paint.Align.RIGHT)
            }
            var ty = y + 44f
            for (l in lines) {
                UI.text(c, l, 26f, ty, body, Pal.BONE, Paint.Align.LEFT, false, UI.serif)
                ty += body + 3f
            }
            y += h + 5f
        }
        if (!any) {
            UI.text(c, "Nobody yet. Voices appear here once you have decoded them.", 20f, y + 12f, 11f, Pal.LGREY)
            y += 30f
        }
        return y + 6f
    }

    private fun drawClues(c: Canvas, y0: Float): Float {
        val st = app.st
        val W = app.W
        var y = y0
        if (st.clues.isEmpty()) {
            UI.text(c, "No clues yet. Decode signals and search locations.", 20f, y + 12f, 11f, Pal.LGREY)
            return y + 30f
        }
        for (id in st.clues) {
            val cl = Content.clues[id] ?: continue
            val h = 28f
            UI.panel(c, 16f, y, W - 40f, h, Pal.CHARCOAL, Pal.DGREY)
            UI.text(c, cl.type.name, 26f, y + 18f, 9f, Pal.AMBER, Paint.Align.LEFT, true)
            UI.text(c, cl.title, 96f, y + 18f, 11f, Pal.BONE)
            rows.add(Row(id, y, h, 2))
            y += h + 4f
        }
        return y + 8f
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        when (a) {
            0 -> {
                bBack.down = bBack.hit(x, y)
                for (b in tabBtns) b.down = b.hit(x, y)
                dragId = id
                dragY = y
                dragStartY = y
                moved = false
            }
            1 -> if (id == dragId) {
                if (abs(y - dragStartY) > 8f) moved = true
                if (moved) {
                    scroll = (scroll - (y - dragY)).coerceIn(0f, maxScroll)
                    dragY = y
                }
            }
            2 -> {
                if (id != dragId) return
                dragId = -1
                if (moved) { bBack.down = false; for (b in tabBtns) b.down = false; return }
                if (bBack.down && bBack.hit(x, y)) { bBack.down = false; app.mixer.click(); back(); return }
                bBack.down = false
                for (i in tabBtns.indices) {
                    if (tabBtns[i].down && tabBtns[i].hit(x, y)) {
                        app.mixer.click()
                        playing = null
                        openTab(i)
                    }
                    tabBtns[i].down = false
                }
                if (y > 42f) tapRow(x, y)
            }
        }
    }

    private fun tapRow(x: Float, y: Float) {
        val st = app.st
        for (r in rows) {
            if (y < r.y || y > r.y + r.h) continue
            when (r.kind) {
                0 -> {
                    val tx = Content.transmission(r.id) ?: return
                    app.mixer.click()
                    val text = tx.linesFor(st.flags).joinToString("\n")
                    app.say(listOf(text), tx.title)
                }
                1 -> {
                    val rec = st.recordings.firstOrNull { it.id == r.id } ?: return
                    val w = app.W
                    if (x >= w - 150f && x <= w - 96f) {
                        app.mixer.click()
                        if (playing === rec) playing = null else { playing = rec; playT = 0f }
                    } else if (x >= w - 90f && x <= w - 34f) {
                        app.mixer.click()
                        rec.tag = (rec.tag + 1) % 4
                        app.autosave()
                    }
                }
                2 -> {
                    val cl = Content.clues[r.id] ?: return
                    app.mixer.click()
                    val alt = cl.altFlag != null && st.has(cl.altFlag) && cl.altText != null
                    app.say(listOf(if (alt) cl.altText!! else cl.text), cl.title)
                }
            }
            return
        }
    }

    override fun back() {
        playing = null
        app.go(from ?: app.world)
    }
}
