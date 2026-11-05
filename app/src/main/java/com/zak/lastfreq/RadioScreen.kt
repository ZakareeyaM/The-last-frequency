package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import com.zak.lastfreq.core.Content
import com.zak.lastfreq.core.Kind
import com.zak.lastfreq.core.Morse
import com.zak.lastfreq.core.RadioSim
import com.zak.lastfreq.core.Recording
import com.zak.lastfreq.core.Reply
import com.zak.lastfreq.core.Transmission
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RadioScreen(app: App) : Screen(app) {
    private val playheads = HashMap<String, Float>()
    private var best: Transmission? = null
    private var bestClarity = 0f
    private var focus: Transmission? = null
    private var audible = false
    private var hiddenHint = false

    private var knobIdx = -1
    private var knobId = -1
    private var knobPrev = 0f
    private var stripId = -1
    private var stripLast = 0f
    private var lastTick = 0.0

    private var recOn = false
    private var recId = ""
    private var recT = 0f
    private var recSum = 0f
    private var playing: Recording? = null
    private var playT = 0f

    private val entry = ArrayList<Int>()
    private val dyn = ArrayList<Btn>()
    private val fixed = ArrayList<Btn>()
    private val toneBtns = ArrayList<Btn>()
    private var ox = 0f

    private val bRec = Btn("rec", "REC", 0f, 0f, 96f, 32f)
    private val bPlay = Btn("play", "PLAY LAST", 0f, 0f, 108f, 32f)
    private val bLog = Btn("log", "LOG", 0f, 0f, 80f, 32f)
    private val bRepair = Btn("repair", "REPAIR", 0f, 0f, 110f, 32f)
    private val bExit = Btn("exit", "EXIT", 0f, 0f, 90f, 32f)
    private val bFine = arrayOf(
        Btn("-10", "<< .10", 0f, 0f, 64f, 24f), Btn("-1", "< .01", 0f, 0f, 64f, 24f),
        Btn("+1", ".01 >", 0f, 0f, 64f, 24f), Btn("+10", ".10 >>", 0f, 0f, 64f, 24f)
    )

    override fun enter() {
        knobIdx = -1; stripId = -1
        recOn = false
        playing = null
        entry.clear()
    }

    private fun kx(i: Int): Float = ox + (when (i) { 0 -> 58f; 1 -> 138f; else -> 226f })
    private val ky = 232f
    private fun kr(i: Int): Float = if (i == 2) 32f else 26f

    private fun layout() {
        ox = (app.W - 640f) / 2f
        val y = 312f
        bRec.x = ox + 24f; bRec.y = y
        bPlay.x = ox + 24f + 110f; bPlay.y = y
        bLog.x = ox + 24f + 232f; bLog.y = y
        bRepair.x = ox + 24f + 326f; bRepair.y = y
        bExit.x = ox + 24f + 452f; bExit.y = y
        for (i in 0 until 4) {
            bFine[i].x = ox + 24f + i * 70f
            bFine[i].y = 136f
        }
        bRec.label = if (recOn) "STOP REC" else "REC"
        bRepair.enabled = app.st.condition < 95f && app.st.itemCount("tube") > 0
        bPlay.enabled = app.st.recordings.isNotEmpty()
    }

    // ------------------------------------------------------------------ simulation

    override fun update(dt: Float) {
        val st = app.st
        val t = app.time
        if (!st.powered()) {
            app.say(listOf("The generator coughs and dies. The radio goes dark."))
            app.go(app.world)
            return
        }
        st.condition = (st.condition - (0.015f + st.weather.interference * 0.15f) * dt).coerceAtLeast(0f)

        val act = st.active()
        for (tx in act) {
            val cyc = RadioSim.cycleDuration(tx, st.flags)
            var p = (playheads[tx.id] ?: 0f) + dt
            if (p >= cyc) p -= cyc
            playheads[tx.id] = p
        }

        var bTx: Transmission? = null
        var bC = 0f
        hiddenHint = false
        for (tx in act) {
            val cl = RadioSim.clarity(tx, st.tune, st.gain, st.bearing, st.filter, st.weather, st.condition, t)
            if (cl > bC) { bC = cl; bTx = tx }
            if (cl >= 0.3f) st.found.add(tx.id)
            if (RadioSim.hiddenNeedsFilter(tx, st.tune, st.gain, st.bearing, st.weather, st.condition, t) && !st.decoded.contains(tx.id)) hiddenHint = true
        }
        best = bTx
        bestClarity = bC
        audible = bTx != null && RadioSim.audible(bC, st.squelch)
        if (audible) {
            focus = bTx
            if (bC >= 0.35f && bTx != null) st.recvNight.add(bTx.id)
        } else {
            val f = focus
            if (f != null && !(f.kind == Kind.MORSE || f.kind == Kind.TONES) ) focus = null
            if (f != null && st.decoded.contains(f.id)) focus = null
            if (f != null && !st.heard.contains(f.id)) focus = null
        }

        val m = app.mixer
        val pb = playing
        if (pb != null) {
            updatePlayback(dt, pb)
        } else {
            m.staticLevel = RadioSim.staticLevel(bC, st.weather, st.condition, st.squelch)
            val tx = bTx
            if (audible && tx != null) driveAudio(tx, bC, dt)
        }
        updateRecording(dt)

        if (abs(st.tune - lastTick) >= 0.02) {
            lastTick = st.tune
            m.click()
        }

        if (st.advance(dt)) app.endNight()
    }

    private fun speak(tx: Transmission, vol: Float) {
        val ch = Content.character(tx.speaker)
        val m = app.mixer
        m.voicePitch = ch.pitch
        m.voiceRate = ch.rate
        m.voiceBright = ch.bright
        m.voiceRough = ch.rough
        m.voiceVol = vol
    }

    private fun driveAudio(tx: Transmission, clarity: Float, dt: Float) {
        val st = app.st
        val m = app.mixer
        val p = playheads[tx.id] ?: 0f
        val payload = RadioSim.payloadDuration(tx, st.flags)
        if (p >= payload) return
        when (tx.kind) {
            Kind.VOICE, Kind.HIDDEN -> {
                val lines = tx.linesFor(st.flags)
                val (idx, prog) = RadioSim.lineAt(lines, p)
                if (idx >= 0 && prog < 0.86f) speak(tx, clarity * 0.95f)
            }
            Kind.MORSE -> {
                if (Morse.keyDown(tx.options[tx.answer], p)) {
                    m.toneFreq = 640f
                    m.toneVol = clarity * 0.8f
                }
            }
            Kind.TONES -> {
                val i = RadioSim.toneAt(tx.code, p)
                if (i >= 0) {
                    m.toneFreq = RadioSim.TONE_HZ[i]
                    m.toneVol = clarity * 0.9f
                }
            }
        }
        if (clarity >= RadioSim.LISTEN_MIN) {
            val need = RadioSim.listenNeeded(tx, st.flags)
            val cur = st.listen[tx.id] ?: 0f
            if (cur < need) {
                val nv = cur + dt
                st.listen[tx.id] = nv
                if (nv >= need) heardFully(tx)
            }
        }
    }

    private fun heardFully(tx: Transmission) {
        val st = app.st
        if (tx.kind == Kind.VOICE || tx.kind == Kind.HIDDEN) {
            if (!st.decoded.contains(tx.id)) {
                for (msg in st.decode(tx)) app.toast(msg)
                app.mixer.click()
                app.buzz()
                if (tx.replies.isNotEmpty() && !st.replied.contains(tx.id)) askReply(tx)
                app.autosave()
            }
        } else if (!st.heard.contains(tx.id)) {
            st.heard.add(tx.id)
            app.toast(if (tx.kind == Kind.MORSE) "Pattern captured. Decode it." else "Tones captured. Repeat the sequence.")
            focus = tx
        }
    }

    private fun askReply(tx: Transmission) {
        val st = app.st
        val ch = Content.character(tx.speaker)
        val choices = ArrayList<Reply>(tx.replies)
        choices.add(Reply("Not now", ""))
        app.ask(listOf(ch.name + " is waiting for your reply."), choices, "Respond") { k ->
            if (k < tx.replies.size) {
                val r = tx.replies[k]
                st.applyReply(r)
                st.replied.add(tx.id)
                app.say(listOf(r.response), ch.name)
                app.autosave()
            }
        }
    }

    private fun updateRecording(dt: Float) {
        if (!recOn) return
        val st = app.st
        val tx = Content.transmission(recId)
        if (tx == null || playing != null) { recOn = false; return }
        val cl = if (best == tx && audible) bestClarity else 0f
        recT += dt
        recSum += cl * dt
        if (recT >= 6f) {
            recOn = false
            val q = recSum / recT
            val r = st.record(tx, q)
            if (r != null) {
                app.toast("Recorded: " + tx.title + " (" + (q * 100f).toInt() + "%)")
                if (tx.recClue != null) app.toast("New recording on the board")
                app.mixer.click()
                app.autosave()
            } else {
                app.toast("Recording too noisy (" + (q * 100f).toInt() + "%). Improve the signal.")
            }
        }
    }

    private fun updatePlayback(dt: Float, r: Recording) {
        val st = app.st
        val tx = Content.transmission(r.tid)
        if (tx == null) { playing = null; return }
        playT += dt
        val payload = RadioSim.payloadDuration(tx, st.flags)
        if (playT >= payload) { playing = null; return }
        app.mixer.staticLevel = (1f - r.quality) * 0.55f + 0.03f
        when (tx.kind) {
            Kind.VOICE, Kind.HIDDEN -> {
                val (idx, prog) = RadioSim.lineAt(tx.linesFor(st.flags), playT)
                if (idx >= 0 && prog < 0.86f) speak(tx, 0.4f + r.quality * 0.55f)
            }
            Kind.MORSE -> if (Morse.keyDown(tx.options[tx.answer], playT)) {
                app.mixer.toneFreq = 640f
                app.mixer.toneVol = r.quality * 0.8f
            }
            Kind.TONES -> {
                val i = RadioSim.toneAt(tx.code, playT)
                if (i >= 0) {
                    app.mixer.toneFreq = RadioSim.TONE_HZ[i]
                    app.mixer.toneVol = r.quality * 0.9f
                }
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    override fun draw(c: Canvas) {
        layout()
        val st = app.st
        val W = app.W
        val t = app.time
        UI.fill(c, 0f, 0f, W, app.H, Pal.DWOOD)
        for (i in 0 until 12) UI.fill(c, 0f, i * 30f + 14f, W, 1f, Pal.alpha(Pal.BLACK, 0.25f))
        UI.panel(c, ox + 6f, 6f, 628f, 348f, 0xFF20242B.toInt(), Pal.GREY)
        for (sx in floatArrayOf(ox + 14f, ox + 626f)) for (sy in floatArrayOf(14f, 346f)) UI.circle(c, sx, sy, 3f, Pal.LGREY)

        drawReadout(c, st.tune, t)
        drawScope(c, t)
        drawMeters(c)
        drawDial(c, t)
        for (b in bFine) UI.button(c, b, Pal.AMBER, 10f)
        drawKnobs(c)
        drawDecodePanel(c, t)
        UI.button(c, bRec, if (recOn) Pal.RED else Pal.AMBER)
        UI.button(c, bPlay)
        UI.button(c, bLog)
        UI.button(c, bRepair, Pal.TEAL)
        UI.button(c, bExit)
        // recording progress
        if (recOn) {
            UI.bar(c, bRec.x, bRec.y - 8f, bRec.w, 5f, recT / 6f, Pal.RED)
            UI.led(c, bRec.x + 10f, bRec.y + 16f, 4f, ((t * 3f).toInt() % 2) == 0, Pal.RED)
        }
        UI.text(c, st.clockText(), ox + 616f, 28f, 10f, Pal.alpha(Pal.AMBER, 0.0f), Paint.Align.RIGHT)
    }

    private fun drawReadout(c: Canvas, tune: Double, t: Float) {
        val x = ox + 24f
        UI.fill(c, x, 20f, 172f, 58f, 0xFF0B0F14.toInt())
        UI.stroke(c, x, 20f, 172f, 58f, Pal.GREY, 1f)
        val s = String.format("%05.2f", tune)
        UI.text(c, s, x + 12f, 58f, 34f, Pal.AMBER, Paint.Align.LEFT, true)
        UI.text(c, "MHz", x + 128f, 58f, 12f, Pal.DAMBER, Paint.Align.LEFT, true)
        UI.text(c, "FREQ", x + 6f, 31f, 8f, Pal.LGREY)
        UI.text(c, st().clockText(), x + 166f, 31f, 9f, Pal.LGREY, Paint.Align.RIGHT)
        UI.led(c, x + 150f, 70f, 4f, bestClarity >= RadioSim.LOCK, Pal.AMBER)
        UI.text(c, "LOCK", x + 138f, 73f, 8f, Pal.LGREY, Paint.Align.RIGHT)
    }

    private fun st() = app.st

    private fun drawScope(c: Canvas, t: Float) {
        val x = ox + 206f
        val y = 20f
        val w = 190f
        val h = 58f
        UI.fill(c, x, y, w, h, 0xFF08130F.toInt())
        UI.stroke(c, x, y, w, h, Pal.GREY, 1f)
        UI.fill(c, x + 2f, y + h / 2f, w - 4f, 1f, Pal.alpha(Pal.TEAL, 0.3f))
        val mid = y + h / 2f
        val noiseAmp = 3f + app.mixer.staticLevel * 14f
        val sigAmp = (if (audible || playing != null) (app.mixer.voiceVol + app.mixer.toneVol).coerceAtMost(1f) else 0f) * 16f
        var px = x + 2f
        var py = mid
        var i = 2
        while (i < w - 2f) {
            val n = (Art.hash(i, (t * 30f).toInt()) % 1000) / 500f - 1f
            val s = sin(i * 0.45f + t * 22f) * sigAmp * (0.5f + 0.5f * sin(i * 0.11f + t * 4f))
            val nx = x + i
            val ny = (mid + n * noiseAmp + s).coerceIn(y + 3f, y + h - 3f)
            UI.line(c, px, py, nx, ny, 0xFF6FE0B0.toInt(), 1.2f)
            px = nx
            py = ny
            i += 3
        }
        UI.text(c, "SCOPE", x + 5f, y + 10f, 8f, Pal.alpha(Pal.TEAL, 0.9f))
    }

    private fun drawMeters(c: Canvas) {
        val st = app.st
        val x = ox + 406f
        UI.fill(c, x, 20f, 210f, 58f, 0xFF0B0F14.toInt())
        UI.stroke(c, x, 20f, 210f, 58f, Pal.GREY, 1f)
        UI.text(c, "SIGNAL", x + 8f, 33f, 8f, Pal.LGREY)
        val lit = (bestClarity * 14f).toInt()
        for (i in 0 until 14) {
            val col = if (i < 9) Pal.TEAL else if (i < 12) Pal.AMBER else Pal.RED
            UI.fill(c, x + 48f + i * 11f, 24f, 9f, 11f, if (i < lit) col else Pal.alpha(col, 0.15f))
        }
        UI.text(c, "TUBES", x + 8f, 51f, 8f, Pal.LGREY)
        UI.bar(c, x + 48f, 43f, 152f, 8f, st.condition / 100f, if (st.condition < 40f) Pal.RED else Pal.TEAL)
        UI.text(c, "FUEL", x + 8f, 68f, 8f, Pal.LGREY)
        UI.bar(c, x + 48f, 60f, 152f, 8f, st.fuel / 100f, if (st.fuel < 20f) Pal.RED else Pal.ORANGE)
    }

    private fun drawDial(c: Canvas, t: Float) {
        val st = app.st
        val x = ox + 24f
        val y = 86f
        val w = 592f
        val h = 44f
        UI.fill(c, x, y, w, h, 0xFF0B0F14.toInt())
        UI.stroke(c, x, y, w, h, Pal.GREY, 1f)
        val ppm = 197f
        val cx = x + w / 2f
        val lo = st.tune - 1.5
        val hi = st.tune + 1.5
        var f = Math.ceil(lo * 20.0) / 20.0
        while (f <= hi) {
            val fx = cx + ((f - st.tune) * ppm).toFloat()
            val tenth = Math.round(f * 20.0).toInt()
            val major = tenth % 10 == 0
            val mid = tenth % 2 == 0
            UI.fill(c, fx, y + 4f, 1f, if (major) 14f else if (mid) 9f else 5f, Pal.alpha(Pal.LGREY, if (major) 0.9f else 0.5f))
            if (major) UI.text(c, String.format("%.1f", f), fx, y + 31f, 9f, Pal.LGREY, Paint.Align.CENTER)
            f += 0.05
        }
        for (tx in st.active()) {
            if (!st.found.contains(tx.id)) continue
            val fx = cx + ((tx.freq - st.tune) * ppm).toFloat()
            if (fx < x + 4f || fx > x + w - 4f) continue
            UI.poly(c, floatArrayOf(fx, y + 20f, fx - 4f, y + 28f, fx + 4f, y + 28f), if (st.decoded.contains(tx.id)) Pal.TEAL else Pal.AMBER, true)
        }
        UI.fill(c, cx - 1f, y + 1f, 2f, h - 2f, Pal.RED)
        UI.fill(c, cx - 6f, y + h - 8f, 12f, 3f, Pal.alpha(Pal.RED, 0.6f))
        UI.text(c, "drag to tune", x + w - 6f, y + h - 3f, 8f, Pal.alpha(Pal.LGREY, 0.7f), Paint.Align.RIGHT)
    }

    private fun drawKnobs(c: Canvas) {
        val st = app.st
        val names = arrayOf("GAIN", "SQUELCH", "ANTENNA")
        for (i in 0 until 3) {
            val cx = kx(i)
            val r = kr(i)
            val ang = when (i) { 0 -> -135f + st.gain * 270f; 1 -> -135f + st.squelch * 270f; else -> st.bearing }
            // scale ticks
            if (i < 2) {
                for (k in 0..10) {
                    val a = Math.toRadians((-135.0 + k * 27.0 - 90.0))
                    val x1 = cx + cos(a).toFloat() * (r + 5f)
                    val y1 = ky + sin(a).toFloat() * (r + 5f)
                    val x2 = cx + cos(a).toFloat() * (r + 9f)
                    val y2 = ky + sin(a).toFloat() * (r + 9f)
                    UI.line(c, x1, y1, x2, y2, Pal.alpha(Pal.LGREY, 0.6f), 1f)
                }
            } else {
                UI.text(c, "N", cx, ky - r - 7f, 8f, Pal.AMBER, Paint.Align.CENTER, true)
                UI.text(c, "E", cx + r + 9f, ky + 3f, 8f, Pal.LGREY, Paint.Align.CENTER)
                UI.text(c, "S", cx, ky + r + 14f, 8f, Pal.LGREY, Paint.Align.CENTER)
                UI.text(c, "W", cx - r - 9f, ky + 3f, 8f, Pal.LGREY, Paint.Align.CENTER)
                val jit = RadioSim.windJitter(st.weather, app.time)
                if (abs(jit) > 0.5f) {
                    val a = Math.toRadians((st.bearing + jit - 90f).toDouble())
                    UI.line(c, cx, ky, cx + cos(a).toFloat() * (r - 2f), ky + sin(a).toFloat() * (r - 2f), Pal.alpha(Pal.RED, 0.7f), 1.5f)
                }
            }
            UI.knob(c, cx, ky, r, ang)
            UI.text(c, names[i], cx, ky + r + (if (i == 2) 26f else 22f), 9f, Pal.BONE, Paint.Align.CENTER, true)
            val v = when (i) {
                0 -> (st.gain * 100f).toInt().toString() + "%"
                1 -> (st.squelch * 100f).toInt().toString() + "%"
                else -> st.bearing.toInt().toString() + " deg"
            }
            UI.text(c, v, cx, ky + r + (if (i == 2) 37f else 33f), 9f, Pal.AMBER, Paint.Align.CENTER)
        }
        // filter switch
        val fx = ox + 296f
        UI.panel(c, fx - 18f, 202f, 36f, 60f, Pal.DGREY, Pal.GREY)
        UI.fill(c, fx - 3f, 208f, 6f, 48f, Pal.BLACK)
        val ly = if (st.filter) 212f else 244f
        UI.fill(c, fx - 8f, ly, 16f, 12f, if (st.filter) Pal.AMBER else Pal.LGREY)
        UI.text(c, "FILTER", fx, 276f, 9f, Pal.BONE, Paint.Align.CENTER, true)
        UI.text(c, if (st.filter) "ON" else "OFF", fx, 288f, 9f, if (st.filter) Pal.AMBER else Pal.LGREY, Paint.Align.CENTER)
    }

    private fun garbleFor(text: String, clarity: Float, salt: Int): String =
        RadioSim.garble(text, clarity, (app.time * 7f).toInt(), salt)

    private fun drawDecodePanel(c: Canvas, t: Float) {
        val st = app.st
        val x = ox + 342f
        val y = 164f
        val w = 274f
        val h = 142f
        UI.panel(c, x, y, w, h, 0xFF0B0F14.toInt(), Pal.GREY)
        dyn.clear()
        toneBtns.clear()

        val pb = playing
        val tx: Transmission? = if (pb != null) Content.transmission(pb.tid) else focus ?: (if (audible) best else null)
        val clarity = if (pb != null) pb.quality else bestClarity
        val shown = pb != null || audible || (focus != null)

        if (tx == null || !shown) {
            drawIdle(c, x, y, w, h)
            return
        }
        val ch = Content.character(tx.speaker)
        val revealed = st.decoded.contains(tx.id) || clarity >= RadioSim.LOCK
        val title = if (pb != null) "PLAYBACK" else if (revealed) tx.title else "UNKNOWN SIGNAL"
        UI.text(c, title.uppercase(), x + 8f, y + 14f, 10f, Pal.AMBER, Paint.Align.LEFT, true)
        val who = if (revealed) ch.name else "???"
        UI.text(c, who, x + w - 8f, y + 14f, 9f, Pal.LGREY, Paint.Align.RIGHT)
        UI.bar(c, x + 8f, y + 20f, w - 16f, 5f, clarity, if (clarity >= RadioSim.LOCK) Pal.AMBER else Pal.TEAL)

        val body = 10.5f * UI.textScale
        val top = y + 38f
        val phase = if (pb != null) playT else (playheads[tx.id] ?: 0f)
        val payload = RadioSim.payloadDuration(tx, st.flags)
        val need = RadioSim.listenNeeded(tx, st.flags)
        val prog = ((st.listen[tx.id] ?: 0f) / need).coerceIn(0f, 1f)
        val decoded = st.decoded.contains(tx.id)

        when (tx.kind) {
            Kind.VOICE, Kind.HIDDEN -> {
                if (!app.settings.subtitles && pb == null && !decoded) {
                    UI.text(c, "[ subtitles are off ]", x + 8f, top + 10f, 10f, Pal.LGREY)
                } else {
                    val lines = tx.linesFor(st.flags)
                    val (idx, lp) = RadioSim.lineAt(lines, phase)
                    var yy = top + 4f
                    if (idx >= 0 && phase < payload) {
                        val from = maxOf(0, idx - 2)
                        for (i in from..idx) {
                            var txt = lines[i]
                            if (i == idx) txt = txt.substring(0, (txt.length * minOf(1f, lp * 1.25f)).toInt())
                            val clean = if (decoded || clarity >= RadioSim.LOCK) txt else garbleFor(txt, clarity, i + tx.id.length)
                            val col = if (i == idx) Pal.PALE else Pal.alpha(Pal.BONE, 0.55f)
                            for (l in UI.wrap(clean, w - 18f, body, false, UI.serif)) {
                                if (yy < y + h - 24f) UI.text(c, l, x + 8f, yy + body, body, col, Paint.Align.LEFT, false, UI.serif)
                                yy += body + 3f
                            }
                        }
                    } else {
                        UI.text(c, "...", x + 8f, top + 12f, 11f, Pal.LGREY)
                    }
                }
                if (pb == null) {
                    if (decoded) UI.text(c, "LOGGED", x + 8f, y + h - 8f, 9f, Pal.TEAL, Paint.Align.LEFT, true)
                    else UI.text(c, "DECODING " + (prog * 100f).toInt() + "%", x + 8f, y + h - 8f, 9f, Pal.LGREY)
                    if (decoded && tx.replies.isNotEmpty() && !st.replied.contains(tx.id)) {
                        val b = Btn("respond", "RESPOND", x + w - 94f, y + h - 28f, 86f, 22f)
                        dyn.add(b); UI.button(c, b, Pal.AMBER, 10f)
                    }
                }
            }
            Kind.MORSE -> drawMorse(c, tx, x, y, w, h, top, phase, prog, decoded, pb != null)
            Kind.TONES -> drawTones(c, tx, x, y, w, h, top, phase, prog, decoded, pb != null)
        }
        if (pb != null) {
            val b = Btn("stopplay", "STOP", x + w - 62f, y + h - 28f, 54f, 22f)
            dyn.add(b); UI.button(c, b, Pal.RED, 10f)
        }
    }

    private fun drawIdle(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val st = app.st
        UI.text(c, "NO CLEAR SIGNAL", x + 8f, y + 14f, 10f, Pal.LGREY, Paint.Align.LEFT, true)
        var yy = y + 30f
        if (hiddenHint) {
            for (l in UI.wrap("Something is buried under the static. Try the FILTER switch.", w - 16f, 10f)) {
                UI.text(c, l, x + 8f, yy, 10f, Pal.PALE)
                yy += 12f
            }
            yy += 4f
        }
        UI.text(c, "Known signals tonight:", x + 8f, yy, 9f, Pal.AMBER)
        yy += 12f
        var n = 0
        for (tx in st.active()) {
            if (!st.found.contains(tx.id)) continue
            if (n >= 5) break
            val mark = if (st.decoded.contains(tx.id)) "[x] " else "[ ] "
            UI.text(c, mark + String.format("%.2f", tx.freq) + " MHz  " + tx.bearing + " deg", x + 8f, yy, 9f,
                if (st.decoded.contains(tx.id)) Pal.TEAL else Pal.BONE)
            yy += 11f
            n++
        }
        if (n == 0) {
            for (l in UI.wrap("None yet. Sweep the dial slowly. Aim the antenna. Raise GAIN for faint signals.", w - 16f, 9f)) {
                UI.text(c, l, x + 8f, yy, 9f, Pal.LGREY)
                yy += 11f
            }
        }
    }

    private fun drawMorse(c: Canvas, tx: Transmission, x: Float, y: Float, w: Float, h: Float, top: Float,
                          phase: Float, prog: Float, decoded: Boolean, playback: Boolean) {
        val st = app.st
        val word = tx.options[tx.answer]
        val syms = if (playback || prog > 0f || decoded) Morse.revealed(word, if (phase < Morse.duration(word)) phase else Morse.duration(word)) else ""
        var cx = x + 10f
        var cy = top + 8f
        for (ch in syms) {
            when (ch) {
                '.' -> { UI.circle(c, cx + 3f, cy, 3f, Pal.PALE); cx += 10f }
                '-' -> { UI.fill(c, cx, cy - 2f, 14f, 4f, Pal.PALE); cx += 19f }
                ' ' -> cx += 8f
                '/' -> cx += 16f
            }
            if (cx > x + w - 20f) { cx = x + 10f; cy += 12f }
        }
        if (decoded) {
            UI.text(c, "DECODED: " + word, x + 8f, y + h - 8f, 10f, Pal.TEAL, Paint.Align.LEFT, true)
            return
        }
        if (!st.heard.contains(tx.id)) {
            if (!playback) UI.text(c, "CAPTURING " + (prog * 100f).toInt() + "%", x + 8f, y + h - 8f, 9f, Pal.LGREY)
            return
        }
        UI.text(c, "What does it say?", x + 8f, top + 42f, 9f, Pal.AMBER)
        for (i in tx.options.indices) {
            val bx = x + 8f + (i % 2) * 130f
            val by = top + 48f + (i / 2) * 26f
            val b = Btn("opt$i", tx.options[i], bx, by, 126f, 22f)
            dyn.add(b); UI.button(c, b, Pal.TEAL, 10f)
        }
    }

    private fun drawTones(c: Canvas, tx: Transmission, x: Float, y: Float, w: Float, h: Float, top: Float,
                          phase: Float, prog: Float, decoded: Boolean, playback: Boolean) {
        val st = app.st
        val n = tx.code.size
        val shownN = if (decoded || playback || prog > 0f) minOf(n, (phase / RadioSim.TONE_STEP).toInt() + 1) else 0
        for (i in 0 until n) {
            val sx = x + 34f + i * 44f
            val sy = top + 10f
            UI.ring(c, sx, sy, 15f, Pal.alpha(Pal.GREY, 0.7f), 1f)
            if (i < shownN || decoded) UI.symbol(c, tx.code[i], sx, sy, 8f, Pal.PALE)
        }
        if (decoded) {
            UI.text(c, "SEQUENCE ACCEPTED", x + 8f, y + h - 8f, 10f, Pal.TEAL, Paint.Align.LEFT, true)
            return
        }
        if (!st.heard.contains(tx.id)) {
            if (!playback) UI.text(c, "CAPTURING " + (prog * 100f).toInt() + "%", x + 8f, y + h - 8f, 9f, Pal.LGREY)
            return
        }
        UI.text(c, "Repeat the tones:", x + 8f, top + 36f, 9f, Pal.AMBER)
        for (i in 0 until n) {
            val sx = x + 34f + i * 44f
            val sy = top + 56f
            UI.ring(c, sx, sy, 14f, Pal.AMBER, 1f)
            if (i < entry.size) UI.symbol(c, entry[i], sx, sy, 7f, Pal.AMBER)
        }
        for (s in 0 until 4) {
            val b = Btn("tone$s", "", x + 8f + s * 36f, top + 76f, 32f, 26f)
            toneBtns.add(b)
            UI.button(c, b, Pal.TEAL, 10f)
            UI.symbol(c, s, b.x + 16f, b.y + 13f, 7f, Pal.PALE)
        }
        val clr = Btn("tclear", "CLEAR", x + 156f, top + 76f, 54f, 26f)
        val snd = Btn("tsend", "SEND", x + 214f, top + 76f, 52f, 26f)
        dyn.add(clr); dyn.add(snd)
        UI.button(c, clr, Pal.AMBER, 9f)
        UI.button(c, snd, Pal.AMBER, 9f)
    }

    // ------------------------------------------------------------------ touch

    private fun angleOf(x: Float, y: Float, cx: Float, cy: Float): Float =
        Math.toDegrees(atan2((x - cx).toDouble(), -(y - cy).toDouble())).toFloat()

    private fun normDelta(d: Float): Float {
        var v = d
        while (v > 180f) v -= 360f
        while (v < -180f) v += 360f
        return v
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        layout()
        val st = app.st
        if (a == 0) {
            val all = ArrayList<Btn>()
            all.addAll(dyn); all.addAll(toneBtns)
            all.add(bRec); all.add(bPlay); all.add(bLog); all.add(bRepair); all.add(bExit)
            for (b in bFine) all.add(b)
            for (b in all) if (b.enabled && b.hit(x, y)) { b.down = true; return }
            for (i in 0 until 3) {
                val dx = x - kx(i)
                val dy = y - ky
                if (dx * dx + dy * dy <= (kr(i) + 16f) * (kr(i) + 16f)) {
                    knobIdx = i
                    knobId = id
                    knobPrev = angleOf(x, y, kx(i), ky)
                    return
                }
            }
            val fx = ox + 296f
            if (x >= fx - 22f && x <= fx + 22f && y >= 198f && y <= 266f) {
                st.filter = !st.filter
                app.mixer.click()
                app.buzz()
                return
            }
            if (x >= ox + 24f && x <= ox + 616f && y >= 86f && y <= 130f) {
                stripId = id
                stripLast = x
            }
        } else if (a == 1) {
            if (id == knobId && knobIdx >= 0) {
                val ang = angleOf(x, y, kx(knobIdx), ky)
                val d = normDelta(ang - knobPrev)
                knobPrev = ang
                when (knobIdx) {
                    0 -> st.gain = (st.gain + d / 270f).coerceIn(0f, 1f)
                    1 -> st.squelch = (st.squelch + d / 270f).coerceIn(0f, 1f)
                    else -> {
                        var b = st.bearing + d
                        while (b < 0f) b += 360f
                        while (b >= 360f) b -= 360f
                        st.bearing = b
                    }
                }
            } else if (id == stripId) {
                val dx = x - stripLast
                stripLast = x
                st.tune = (Math.round((st.tune - dx / 197.0 * 0.6) * 100.0) / 100.0).coerceIn(RadioSim.MIN_FREQ, RadioSim.MAX_FREQ)
            }
        } else {
            if (id == knobId) { knobId = -1; knobIdx = -1 }
            if (id == stripId) stripId = -1
            val all = ArrayList<Btn>()
            all.addAll(dyn); all.addAll(toneBtns)
            all.add(bRec); all.add(bPlay); all.add(bLog); all.add(bRepair); all.add(bExit)
            for (b in bFine) all.add(b)
            for (b in all) {
                if (b.down && b.hit(x, y)) press(b)
                b.down = false
            }
        }
    }

    private fun press(b: Btn) {
        val st = app.st
        app.mixer.click()
        when {
            b.id == "exit" -> app.go(app.world)
            b.id == "log" -> { app.journal.from = this; app.journal.openTab(0); app.go(app.journal) }
            b.id == "rec" -> toggleRecord()
            b.id == "play" -> startPlayback()
            b.id == "stopplay" -> playing = null
            b.id == "repair" -> {
                if (st.itemCount("tube") > 0) {
                    st.takeItem("tube")
                    st.condition = minOf(100f, st.condition + 45f)
                    app.toast("Radio repaired: " + st.condition.toInt() + "%")
                }
            }
            b.id.startsWith("-") || b.id.startsWith("+") -> {
                val step = if (b.id.endsWith("10")) 0.10 else 0.01
                val sign = if (b.id.startsWith("-")) -1.0 else 1.0
                st.tune = (Math.round((st.tune + sign * step) * 100.0) / 100.0).coerceIn(RadioSim.MIN_FREQ, RadioSim.MAX_FREQ)
            }
            b.id == "respond" -> focusTx()?.let { askReply(it) }
            b.id.startsWith("opt") -> {
                val tx = focusTx() ?: return
                val i = b.id.substring(3).toInt()
                if (i == tx.answer) {
                    for (m in st.decode(tx)) app.toast(m)
                    app.buzz()
                    app.autosave()
                } else {
                    app.toast("That doesn't match the signal.")
                }
            }
            b.id.startsWith("tone") -> {
                val tx = focusTx() ?: return
                if (entry.size < tx.code.size) entry.add(b.id.substring(4).toInt())
            }
            b.id == "tclear" -> entry.clear()
            b.id == "tsend" -> {
                val tx = focusTx() ?: return
                if (entry == tx.code) {
                    for (m in st.decode(tx)) app.toast(m)
                    entry.clear()
                    app.buzz(true)
                    app.autosave()
                } else {
                    app.toast("Sequence does not match.")
                    entry.clear()
                }
            }
        }
    }

    private fun focusTx(): Transmission? = focus ?: best

    private fun toggleRecord() {
        if (recOn) {
            recOn = false
            app.toast("Recording cancelled")
            return
        }
        val tx = best
        if (tx == null || !audible) {
            app.toast("Nothing to record. Tune in a signal first.")
            return
        }
        recOn = true
        recId = tx.id
        recT = 0f
        recSum = 0f
        playing = null
        app.toast("Recording... hold the signal steady")
    }

    private fun startPlayback() {
        val r = app.st.recordings.lastOrNull() ?: return
        playing = r
        playT = 0f
        recOn = false
    }

    override fun back() {
        app.go(app.world)
    }
}
