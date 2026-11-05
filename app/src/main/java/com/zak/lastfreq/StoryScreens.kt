package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import com.zak.lastfreq.core.Content
import com.zak.lastfreq.core.LocationInfo
import com.zak.lastfreq.core.Reply
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// ====================================================================== map

class MapScreen(app: App) : Screen(app) {
    var from: Screen? = null
    private var sel: LocationInfo? = null
    private val bBack = Btn("back", "BACK", 8f, 8f, 60f, 24f)
    private val bGo = Btn("go", "TRAVEL (25 min)", 0f, 0f, 170f, 30f)

    private fun mapRect(): FloatArray = floatArrayOf(14f, 44f, app.W - 250f, app.H - 58f)

    override fun enter() {
        sel = null
    }

    private fun px(l: LocationInfo): Float { val r = mapRect(); return r[0] + l.mapX * r[2] }
    private fun py(l: LocationInfo): Float { val r = mapRect(); return r[1] + l.mapY * r[3] }

    override fun draw(c: Canvas) {
        val st = app.st
        val W = app.W
        val H = app.H
        val r = mapRect()
        UI.fill(c, 0f, 0f, W, H, Pal.DWOOD)
        UI.text(c, "NAUTICAL CHART  /  GREYHAVEN COAST", 80f, 25f, 12f, Pal.AMBER, Paint.Align.LEFT, true)
        UI.button(c, bBack, Pal.AMBER, 11f)

        // chart paper
        UI.fill(c, r[0], r[1], r[2], r[3], 0xFF18303C.toInt())
        // land mass: a ragged coastline on the left
        val land = floatArrayOf(
            r[0], r[1], r[0] + r[2] * 0.62f, r[1], r[0] + r[2] * 0.58f, r[1] + r[3] * 0.18f,
            r[0] + r[2] * 0.66f, r[1] + r[3] * 0.34f, r[0] + r[2] * 0.62f, r[1] + r[3] * 0.46f,
            r[0] + r[2] * 0.72f, r[1] + r[3] * 0.62f, r[0] + r[2] * 0.66f, r[1] + r[3] * 0.78f,
            r[0] + r[2] * 0.58f, r[1] + r[3], r[0], r[1] + r[3]
        )
        UI.poly(c, land, 0xFF2C3A30.toInt(), true)
        UI.poly(c, land, Pal.alpha(Pal.PALE, 0.5f), false, 1.5f)
        // grid
        for (i in 1 until 8) UI.fill(c, r[0] + r[2] * i / 8f, r[1], 1f, r[3], Pal.alpha(Pal.BONE, 0.06f))
        for (i in 1 until 5) UI.fill(c, r[0], r[1] + r[3] * i / 5f, r[2], 1f, Pal.alpha(Pal.BONE, 0.06f))
        UI.stroke(c, r[0], r[1], r[2], r[3], Pal.GREY, 2f)
        // compass rose
        val cx = r[0] + r[2] - 30f
        val cy = r[1] + 34f
        UI.ring(c, cx, cy, 18f, Pal.alpha(Pal.PAPER, 0.6f), 1f)
        UI.line(c, cx, cy - 18f, cx, cy + 18f, Pal.alpha(Pal.PAPER, 0.6f), 1f)
        UI.line(c, cx - 18f, cy, cx + 18f, cy, Pal.alpha(Pal.PAPER, 0.6f), 1f)
        UI.text(c, "N", cx, cy - 22f, 10f, Pal.PAPER, Paint.Align.CENTER, true)

        val stn = Content.location("station")
        val sx = px(stn)
        val sy = py(stn)
        // antenna bearing
        val ang = Math.toRadians((st.bearing - 90f).toDouble())
        val bx = sx + cos(ang).toFloat() * 160f
        val by = sy + sin(ang).toFloat() * 160f
        UI.line(c, sx, sy, bx, by, Pal.alpha(Pal.AMBER, 0.5f), 1.5f)
        UI.text(c, "antenna " + st.bearing.toInt() + " deg", bx, by, 9f, Pal.alpha(Pal.AMBER, 0.8f), Paint.Align.CENTER)

        for (l in Content.locations) {
            if (!st.locationUnlocked(l)) continue
            val x = px(l)
            val y = py(l)
            val here = st.scene == l.sceneId
            val isSel = sel === l
            if (isSel) UI.ring(c, x, y, 16f, Pal.PALE, 2f)
            UI.circle(c, x, y, 8f, Pal.BLACK)
            UI.circle(c, x, y, 6f, if (l.id == "station") Pal.AMBER else Pal.TEAL)
            if (here) UI.ring(c, x, y, 11f, Pal.alpha(Pal.AMBER, 0.6f + 0.4f * sin(app.time * 4f).toFloat()), 1.5f)
            UI.text(c, l.name, x, y + 24f, 10f, Pal.BONE, Paint.Align.CENTER, true)
        }

        // info panel
        val ix = W - 230f
        UI.panel(c, ix, 44f, 216f, H - 58f, Pal.CHARCOAL, Pal.GREY)
        val s = sel
        if (s == null) {
            var y = 66f
            for (l in UI.wrap("Tap a location to read about it. You are at " + st.sceneName() + ".", 190f, 11f * UI.textScale)) {
                UI.text(c, l, ix + 12f, y, 11f * UI.textScale, Pal.LGREY)
                y += 14f * UI.textScale
            }
            UI.text(c, "New places appear as you solve", ix + 12f, y + 14f, 10f, Pal.LGREY)
            UI.text(c, "the night's puzzles.", ix + 12f, y + 26f, 10f, Pal.LGREY)
        } else {
            UI.text(c, s.name, ix + 12f, 66f, 13f, Pal.PALE, Paint.Align.LEFT, true)
            var y = 86f
            for (l in UI.wrap(s.blurb, 192f, 11f * UI.textScale, false, UI.serif)) {
                UI.text(c, l, ix + 12f, y, 11f * UI.textScale, Pal.BONE, Paint.Align.LEFT, false, UI.serif)
                y += 14f * UI.textScale
            }
            val brg = Content.bearingOf(s)
            if (s.id != "station") UI.text(c, "Bearing from station: " + brg.toInt() + " deg", ix + 12f, y + 8f, 10f, Pal.AMBER)
            val here = st.scene == s.sceneId
            bGo.x = ix + 12f
            bGo.y = H - 58f
            bGo.w = 192f
            bGo.enabled = !here
            bGo.label = if (here) "YOU ARE HERE" else if (s.id == "station") "RETURN (25 min)" else "TRAVEL (25 min)"
            UI.button(c, bGo, Pal.AMBER, 11f)
        }
        UI.text(c, st.clockText() + "  " + st.weather.label, W - 14f, 25f, 10f, Pal.LGREY, Paint.Align.RIGHT)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a == 0) {
            bBack.down = bBack.hit(x, y)
            bGo.down = sel != null && bGo.enabled && bGo.hit(x, y)
        } else if (a == 2) {
            if (bBack.down && bBack.hit(x, y)) { bBack.down = false; app.mixer.click(); back(); return }
            bBack.down = false
            if (bGo.down && bGo.hit(x, y)) {
                bGo.down = false
                val s = sel ?: return
                app.mixer.click()
                app.world.travel(s)
                return
            }
            bGo.down = false
            var best: LocationInfo? = null
            var bd = 28f
            for (l in Content.locations) {
                if (!app.st.locationUnlocked(l)) continue
                val d = Math.hypot((px(l) - x).toDouble(), (py(l) - y).toDouble()).toFloat()
                if (d < bd) { bd = d; best = l }
            }
            if (best != null) { sel = best; app.mixer.click() }
        }
    }

    override fun back() {
        app.go(from ?: app.world)
    }
}

// ====================================================================== night intro

class IntroScreen(app: App) : Screen(app) {
    private var night = 1
    private var lines: List<String> = emptyList()
    private var shown = 0
    private var t = 0f

    fun setNight(n: Int) {
        night = n
        lines = Content.introLines(n, app.st.flags)
        shown = 0
        t = 0f
    }

    override fun enter() {
        if (lines.isEmpty()) setNight(app.st.night)
        t = 0f
    }

    override fun update(dt: Float) {
        t += dt
        app.mixer.staticLevel = 0.04f
    }

    override fun draw(c: Canvas) {
        val W = app.W
        val H = app.H
        UI.fill(c, 0f, 0f, W, H, Pal.NAVY)
        app.weatherFx.drawRain(c, app.st.weather, W, H, 0.5f)
        app.weatherFx.drawFog(c, app.st.weather, W, H, app.time)
        val a = (t / 1.2f).coerceIn(0f, 1f)
        spaced(c, "NIGHT " + night, W / 2f - spacedWidth("NIGHT " + night, 14f, 6f, UI.serif) / 2f, 70f, 14f, Pal.alpha(Pal.AMBER, a), 6f, UI.serif)
        val title = Content.nightTitles[night.coerceIn(1, 5)]
        spaced(c, title, W / 2f - spacedWidth(title, 26f, 5f, UI.serifBold) / 2f, 108f, 26f, Pal.alpha(Pal.BONE, a), 5f, UI.serifBold)
        UI.fill(c, W / 2f - 60f, 122f, 120f, 1f, Pal.alpha(Pal.AMBER, 0.6f * a))
        UI.text(c, app.st.weather.label + "  /  23:00", W / 2f, 142f, 11f, Pal.alpha(Pal.LGREY, a), Paint.Align.CENTER)

        val body = 13f * UI.textScale
        var y = 176f
        for (i in 0 until minOf(shown + 1, lines.size)) {
            val fadeIn = if (i == shown) ((t - 1.4f) / 0.8f).coerceIn(0f, 1f) else 1f
            if (i == shown && t < 1.4f) continue
            for (l in UI.wrap(lines[i], W - 120f, body, false, UI.serif)) {
                UI.text(c, l, W / 2f, y, body, Pal.alpha(Pal.BONE, fadeIn), Paint.Align.CENTER, false, UI.serif)
                y += body + 4f
            }
            y += 6f
        }
        val blink = if ((app.time * 2f).toInt() % 2 == 0) Pal.AMBER else Pal.DAMBER
        if (t > 1.6f) UI.text(c, if (shown >= lines.size - 1) "tap to begin your shift" else "tap to continue",
            W / 2f, H - 18f, 10f, blink, Paint.Align.CENTER)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a != 0 || t < 0.6f) return
        app.mixer.click()
        if (shown < lines.size - 1) {
            shown++
            t = if (t < 1.4f) 1.4f else 1.4f
        } else {
            app.beginShift()
        }
    }

    override fun back() {
        app.toMenu()
    }
}

// ====================================================================== night summary

class SummaryScreen(app: App) : Screen(app) {
    private var lines: List<String> = emptyList()
    private var night = 1
    private var scroll = 0f
    private var maxScroll = 0f
    private var dragId = -1
    private var lastY = 0f
    private var startY = 0f
    private var moved = false
    private val bNext = Btn("next", "CONTINUE", 0f, 0f, 180f, 30f)
    private var t = 0f

    fun prepare() {
        lines = app.st.summary()
        night = app.st.night
        scroll = 0f
        t = 0f
    }

    override fun update(dt: Float) {
        t += dt
    }

    override fun draw(c: Canvas) {
        val W = app.W
        val H = app.H
        UI.fill(c, 0f, 0f, W, H, Pal.NAVY)
        UI.panel(c, 30f, 24f, W - 60f, H - 48f, Pal.CHARCOAL, Pal.GREY)
        UI.text(c, "NIGHT " + night + " COMPLETE", W / 2f, 52f, 15f, Pal.AMBER, Paint.Align.CENTER, true)
        UI.text(c, "Dawn over Greyhaven.", W / 2f, 70f, 11f, Pal.LGREY, Paint.Align.CENTER, false, UI.serif)
        val top = 82f
        val bottom = H - 76f
        c.save()
        c.clipRect(44f, top, W - 44f, bottom)
        var y = top + 12f - scroll
        val body = 11f * UI.textScale
        for (s in lines) {
            for (l in UI.wrap(s, W - 110f, body)) {
                UI.text(c, l, 48f, y, body, if (s.startsWith("   ")) Pal.PALE else Pal.BONE)
                y += body + 3f
            }
        }
        c.restore()
        maxScroll = (y + scroll - bottom).coerceAtLeast(0f)
        bNext.x = W / 2f - 90f
        bNext.y = H - 62f
        UI.button(c, bNext, Pal.AMBER, 12f)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        when (a) {
            0 -> {
                bNext.down = bNext.hit(x, y)
                dragId = id; lastY = y; startY = y; moved = false
            }
            1 -> if (id == dragId) {
                if (abs(y - startY) > 8f) moved = true
                if (moved) { scroll = (scroll - (y - lastY)).coerceIn(0f, maxScroll); lastY = y }
            }
            2 -> {
                if (bNext.down && bNext.hit(x, y) && !moved && t > 0.5f) {
                    app.mixer.click()
                    app.nextNight()
                }
                bNext.down = false
                dragId = -1
            }
        }
    }

    override fun back() {
        app.toMenu()
    }
}

// ====================================================================== final decision

class FinalScreen(app: App) : Screen(app) {
    private val btns = ArrayList<Btn>()
    private val bBack = Btn("back", "NOT YET", 8f, 8f, 76f, 24f)
    private val keys = listOf("broadcast", "shutdown", "answer")
    private val names = listOf("BROADCAST EVERYTHING", "SHUT THE STATION DOWN", "ANSWER THE SIGNAL")
    private val descs = listOf(
        "Patch every recording and document into the transmitter and send it to the world.",
        "Pull the breaker and cut the cable. Whatever is out there loses its voice.",
        "Key the microphone and say your name. Listen to what answers."
    )
    private var pending = -1

    override fun enter() {
        pending = -1
        btns.clear()
        for (i in 0 until 3) btns.add(Btn("o" + i, names[i], 0f, 0f, 0f, 0f))
    }

    override fun update(dt: Float) {
        app.mixer.staticLevel = 0.08f
    }

    override fun draw(c: Canvas) {
        val W = app.W
        val H = app.H
        UI.fill(c, 0f, 0f, W, H, Pal.NAVY)
        UI.text(c, "THE MAIN TRANSMITTER", W / 2f, 44f, 16f, Pal.AMBER, Paint.Align.CENTER, true)
        val sub = "Dawn is coming. Whatever you do now, you do for everyone."
        UI.text(c, sub, W / 2f, 64f, 11f * UI.textScale, Pal.LGREY, Paint.Align.CENTER, false, UI.serif)
        UI.button(c, bBack, Pal.AMBER, 10f)
        val bw = minOf(W - 80f, 520f)
        val x = W / 2f - bw / 2f
        var y = 84f
        for (i in 0 until 3) {
            val b = btns[i]
            b.x = x; b.y = y; b.w = bw; b.h = 74f
            val base = if (b.down) Pal.GREY else Pal.DGREY
            UI.fill(c, b.x, b.y, b.w, b.h, base)
            UI.stroke(c, b.x + 0.5f, b.y + 0.5f, b.w - 1f, b.h - 1f, Pal.alpha(Pal.AMBER, 0.8f), 1f)
            UI.text(c, names[i], b.x + 16f, b.y + 24f, 14f, Pal.PALE, Paint.Align.LEFT, true)
            var ty = b.y + 44f
            for (l in UI.wrap(descs[i], bw - 32f, 11f * UI.textScale, false, UI.serif)) {
                UI.text(c, l, b.x + 16f, ty, 11f * UI.textScale, Pal.BONE, Paint.Align.LEFT, false, UI.serif)
                ty += 14f * UI.textScale
            }
            y += 84f
        }
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a == 0) {
            bBack.down = bBack.hit(x, y)
            for (b in btns) b.down = b.hit(x, y)
        } else if (a == 2) {
            if (bBack.down && bBack.hit(x, y)) { bBack.down = false; app.mixer.click(); back(); return }
            bBack.down = false
            for (i in btns.indices) {
                val b = btns[i]
                if (b.down && b.hit(x, y)) {
                    b.down = false
                    app.mixer.click()
                    confirm(i)
                    return
                }
                b.down = false
            }
        }
    }

    private fun confirm(i: Int) {
        app.ask(
            listOf(names[i] + ". This cannot be undone.", "Are you sure?"),
            listOf(Reply("Yes. Do it.", ""), Reply("Wait.", "")),
            "Final decision"
        ) { pick ->
            if (pick == 0) app.finishGame(app.st.resolveEnding(keys[i]))
        }
    }

    override fun back() {
        app.go(app.world)
    }
}

// ====================================================================== endings

class EndingScreen(app: App) : Screen(app) {
    private var id = "loop"
    private var page = 0
    private var typed = 0f
    private var done = false
    private var t = 0f
    private val bMenu = Btn("menu", "RETURN TO MENU", 0f, 0f, 190f, 30f)

    fun setEnding(e: String) {
        id = if (Content.endings.containsKey(e)) e else "loop"
        page = 0
        typed = 0f
        done = false
        t = 0f
    }

    override fun update(dt: Float) {
        t += dt
        app.mixer.staticLevel = if (id == "silence") 0f else if (id == "transmission") 0.09f else 0.04f
        val cps = when (app.settings.textSpeed) { 0 -> 22f; 1 -> 45f; 2 -> 90f; else -> 100000f }
        typed += dt * cps
    }

    override fun draw(c: Canvas) {
        val W = app.W
        val H = app.H
        drawArt(c, W, H)
        val e = Content.endings[id]!!
        UI.fill(c, 0f, H - 150f, W, 150f, Pal.alpha(Pal.BLACK, 0.55f))
        if (!done) {
            val text = e.pages[page]
            val shown = text.substring(0, minOf(text.length, typed.toInt()))
            val body = 13f * UI.textScale
            val lines = UI.wrap(text, W - 90f, body, false, UI.serif)
            var rem = shown.length
            var y = H - 126f
            for (l in lines) {
                val part = if (rem >= l.length) l else l.substring(0, maxOf(0, rem))
                UI.text(c, part, 44f, y, body, Pal.BONE, Paint.Align.LEFT, false, UI.serif)
                rem -= l.length + 1
                y += body + 4f
            }
            if (typed >= text.length) {
                val blink = if ((app.time * 2f).toInt() % 2 == 0) Pal.AMBER else Pal.DAMBER
                UI.text(c, "tap to continue", W - 20f, H - 12f, 10f, blink, Paint.Align.RIGHT)
            }
        } else {
            val st = app.st
            UI.text(c, e.title, W / 2f, H - 112f, 22f, Pal.AMBER, Paint.Align.CENTER, true, UI.serifBold)
            val mins = (st.playSeconds / 60f).toInt()
            UI.text(c, "Nights survived: 5    Evidence flags: " + st.evidenceCount() + "/" + Content.evidenceFlags.size +
                "    Time in shift: " + mins + " min", W / 2f, H - 88f, 10f, Pal.LGREY, Paint.Align.CENTER)
            UI.text(c, "Other endings remain. Start a new game to find them.", W / 2f, H - 72f, 10f, Pal.LGREY, Paint.Align.CENTER)
            bMenu.x = W / 2f - 95f
            bMenu.y = H - 56f
            UI.button(c, bMenu, Pal.AMBER, 12f)
        }
    }

    private fun drawArt(c: Canvas, W: Float, H: Float) {
        val tt = app.time
        when (id) {
            "truth" -> {
                var y = 0f
                while (y < H) { UI.fill(c, 0f, y, W, 8f, Pal.mix(0xFF1E2A44.toInt(), 0xFFE0A860.toInt(), (y / H).coerceIn(0f, 1f))); y += 8f }
                UI.fill(c, 0f, H * 0.62f, W, H, 0xFF0C1620.toInt())
                lighthouse(c, W * 0.5f, H * 0.62f, false, tt)
                UI.circle(c, W * 0.5f, H * 0.58f, 26f + sin(tt * 0.5f).toFloat() * 2f, Pal.alpha(Pal.PALE, 0.35f))
            }
            "silence" -> {
                UI.fill(c, 0f, 0f, W, H, 0xFF05070C.toInt())
                app.weatherFx.drawRain(c, com.zak.lastfreq.core.Weather.LIGHT_RAIN, W, H, 0.5f)
                UI.fill(c, W / 2f - 40f, H * 0.42f, 80f, 44f, 0xFF10151E.toInt())
                UI.stroke(c, W / 2f - 40f, H * 0.42f, 80f, 44f, Pal.GREY, 1f)
                UI.circle(c, W / 2f - 18f, H * 0.42f + 22f, 10f, 0xFF0A0D14.toInt())
                UI.circle(c, W / 2f + 18f, H * 0.42f + 22f, 10f, 0xFF0A0D14.toInt())
                UI.led(c, W / 2f + 30f, H * 0.42f + 8f, 3f, false, Pal.RED)
            }
            "transmission" -> {
                UI.fill(c, 0f, 0f, W, H, 0xFF0A1220.toInt())
                for (i in 0 until 7) {
                    val r = ((tt * 30f + i * 38f) % 260f)
                    UI.ring(c, W / 2f, H * 0.38f, r, Pal.alpha(Pal.TEAL, (1f - r / 260f) * 0.7f), 2f)
                }
                UI.fill(c, W / 2f - 44f, H * 0.38f - 24f, 88f, 48f, 0xFF1A2230.toInt())
                UI.stroke(c, W / 2f - 44f, H * 0.38f - 24f, 88f, 48f, Pal.AMBER, 1f)
                UI.circle(c, W / 2f - 20f, H * 0.38f, 11f, Pal.alpha(Pal.AMBER, 0.6f + 0.3f * sin(tt * 3f).toFloat()))
                UI.circle(c, W / 2f + 20f, H * 0.38f, 11f, Pal.DGREY)
            }
            else -> {
                UI.fill(c, 0f, 0f, W, H, 0xFF14100A.toInt())
                for (i in 0 until 6) {
                    val r = 24f + i * 26f
                    UI.ring(c, W / 2f, H * 0.38f, r, Pal.alpha(Pal.AMBER, 0.55f - i * 0.08f), 2f)
                    val a = tt * (0.8f - i * 0.1f) + i
                    UI.circle(c, W / 2f + cos(a).toFloat() * r, H * 0.38f + sin(a).toFloat() * r, 4f, Pal.PALE)
                }
                UI.circle(c, W / 2f, H * 0.38f, 10f, Pal.AMBER)
            }
        }
    }

    private fun lighthouse(c: Canvas, x: Float, base: Float, beam: Boolean, t: Float) {
        UI.poly(c, floatArrayOf(x - 14f, base, x - 8f, base - 90f, x + 8f, base - 90f, x + 14f, base), 0xFF0A1018.toInt(), true)
        UI.fill(c, x - 11f, base - 100f, 22f, 10f, 0xFF0A1018.toInt())
        UI.fill(c, x - 5f, base - 98f, 10f, 6f, Pal.PALE)
        if (beam) UI.poly(c, floatArrayOf(x, base - 95f, x + 300f, base - 130f, x + 300f, base - 60f), Pal.alpha(Pal.PALE, 0.2f), true)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a == 0) {
            if (done) {
                bMenu.down = bMenu.hit(x, y)
            } else {
                val e = Content.endings[this.id]!!
                val text = e.pages[page]
                if (typed < text.length) typed = text.length.toFloat()
                else if (page < e.pages.size - 1) { page++; typed = 0f }
                else done = true
            }
        } else if (a == 2) {
            if (bMenu.down && bMenu.hit(x, y)) {
                bMenu.down = false
                app.mixer.click()
                app.toMenu()
            }
            bMenu.down = false
        }
    }

    override fun back() {
        if (done) app.toMenu()
    }
}
