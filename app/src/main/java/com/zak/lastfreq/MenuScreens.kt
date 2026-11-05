package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import com.zak.lastfreq.core.Reply
import com.zak.lastfreq.core.Weather
import kotlin.math.cos
import kotlin.math.sin

fun spaced(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, spacing: Float, face: android.graphics.Typeface) {
    var cx = x
    for (ch in s) {
        val t = ch.toString()
        UI.text(c, t, cx, y, size, color, Paint.Align.LEFT, false, face)
        cx += UI.measure(t, size, false, face) + spacing
    }
}

fun spacedWidth(s: String, size: Float, spacing: Float, face: android.graphics.Typeface): Float {
    var w = 0f
    for (ch in s) w += UI.measure(ch.toString(), size, false, face) + spacing
    return w - spacing
}

// ====================================================================== main menu

class MenuScreen(app: App) : Screen(app) {
    private val btns = ArrayList<Btn>()
    private var hasSave = false

    override fun enter() {
        hasSave = app.storage.latest() >= 0
        layout()
    }

    private fun layout() {
        btns.clear()
        val bx = app.W - 214f
        var y = 92f
        fun add(id: String, label: String, enabled: Boolean = true) {
            val b = Btn(id, label, bx, y, 190f, 30f)
            b.enabled = enabled
            btns.add(b)
            y += 38f
        }
        add("continue", "CONTINUE", hasSave)
        add("new", "NEW GAME")
        add("load", "LOAD GAME", hasSave)
        add("settings", "SETTINGS")
        add("credits", "CREDITS")
        add("quit", "QUIT")
    }

    override fun update(dt: Float) {
        app.mixer.staticLevel = 0.07f
        if (btns.isNotEmpty() && btns[0].x != app.W - 214f) layout()
    }

    override fun draw(c: Canvas) {
        val W = app.W
        val H = app.H
        val t = app.time
        var y = 0f
        while (y < H) {
            UI.fill(c, 0f, y, W, 8f, Pal.mix(Pal.NAVY, 0xFF1A2640.toInt(), y / H))
            y += 8f
        }
        for (i in 0 until 60) {
            val h = Art.hash(i, 7)
            val sx = (h % 1000) / 1000f * W
            val sy = ((h / 1000) % 230).toFloat()
            val a = 0.2f + 0.4f * Math.abs(sin(t * 0.7f + i)).toFloat()
            Art.r(c, sx, sy, 2f, 2f, Pal.alpha(Pal.BONE, a))
        }
        // sea
        val seaY = H * 0.70f
        UI.fill(c, 0f, seaY, W, H - seaY, 0xFF0C1C2E.toInt())
        for (i in 0 until 26) {
            val h = Art.hash(i, 3)
            val wy = seaY + 6f + (h % 100) / 100f * (H - seaY - 10f)
            val wx = ((h / 100) % 1000) / 1000f * W + sin(t * 0.4f + i).toFloat() * 14f
            Art.r(c, wx, wy, 16f + (h % 18), 2f, Pal.alpha(0xFF3C5A78.toInt(), 0.35f))
        }
        // far lighthouse and its beam
        val lx = W * 0.40f
        val ly = seaY - 4f
        val ang = t * 0.5f
        val bx = lx + cos(ang) * 360f
        val by = ly - 70f + sin(ang) * 20f
        val beamVis = (cos(ang) * 0.5f + 0.5f)
        UI.poly(c, floatArrayOf(lx, ly - 66f, bx, by - 28f, bx, by + 28f), Pal.alpha(Pal.PALE, 0.10f * beamVis), true)
        Art.r(c, lx - 5f, ly - 62f, 10f, 62f, 0xFF1A1F2B.toInt())
        Art.r(c, lx - 7f, ly - 70f, 14f, 8f, 0xFF232A38.toInt())
        Art.r(c, lx - 3f, ly - 68f, 6f, 5f, Pal.alpha(Pal.PALE, 0.5f + 0.5f * beamVis))
        // cliff and Station 47
        UI.poly(c, floatArrayOf(0f, seaY + 20f, 0f, H * 0.44f, W * 0.22f, H * 0.46f, W * 0.30f, seaY - 6f, W * 0.34f, seaY + 20f), 0xFF070B14.toInt(), true)
        val sx = W * 0.07f
        val sy = H * 0.44f
        Art.r(c, sx, sy - 40f, 96f, 40f, 0xFF0E1420.toInt())
        Art.r(c, sx - 4f, sy - 46f, 104f, 8f, 0xFF161D2C.toInt())
        val flick = if (Art.hash((t * 6f).toInt(), 1) % 9 == 0) 0.55f else 1f
        Art.r(c, sx + 12f, sy - 28f, 16f, 14f, Pal.alpha(Pal.AMBER, 0.85f * flick))
        Art.r(c, sx + 20f, sy - 28f, 2f, 14f, Pal.alpha(Pal.BLACK, 0.5f))
        Art.r(c, sx + 60f, sy - 28f, 14f, 14f, Pal.alpha(Pal.AMBER, 0.35f))
        Art.r(c, sx + 84f, sy - 96f, 3f, 56f, 0xFF0E1420.toInt())
        Art.r(c, sx + 70f, sy - 90f, 30f, 2f, 0xFF0E1420.toInt())
        Art.r(c, sx + 74f, sy - 80f, 22f, 2f, 0xFF0E1420.toInt())
        if (((t * 1.2f).toInt() % 2) == 0) Art.r(c, sx + 83f, sy - 100f, 5f, 5f, Pal.RED)
        // rain
        app.weatherFx.drawRain(c, Weather.LIGHT_RAIN, W, H, 0.3f)
        app.weatherFx.drawFog(c, Weather.FOG, W, H, t)

        // title
        val face = UI.serifBold
        val flicker = if (Art.hash((t * 9f).toInt(), 5) % 23 == 0) 0.6f else 1f
        spaced(c, "THE LAST", 34f, 176f, 30f, Pal.alpha(Pal.BONE, flicker), 9f, face)
        spaced(c, "FREQUENCY", 34f, 214f, 38f, Pal.alpha(Pal.AMBER, flicker), 5f, face)
        UI.fill(c, 34f, 228f, 150f, 1f, Pal.alpha(Pal.AMBER, 0.6f))
        UI.text(c, "STATION 47  /  GREYHAVEN", 34f, 246f, 11f, Pal.LGREY)
        // decorative dial scale
        for (i in 0..24) {
            val h = if (i % 4 == 0) 8f else 4f
            Art.r(c, 34f + i * 7f, 262f, 1f, h, Pal.alpha(Pal.LGREY, 0.5f))
        }
        val needle = 34f + (sin(t * 0.3f) * 0.5f + 0.5f) * 168f
        Art.r(c, needle, 258f, 1f, 18f, Pal.alpha(Pal.AMBER, 0.9f))
        UI.text(c, "v1.0", 14f, H - 10f, 9f, Pal.alpha(Pal.LGREY, 0.6f))

        for (b in btns) UI.button(c, b)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        for (b in btns) {
            if (a == 0 && b.hit(x, y) && b.enabled) b.down = true
            if (a == 2) {
                if (b.down && b.hit(x, y)) press(b.id)
                b.down = false
            }
        }
    }

    private fun press(id: String) {
        app.mixer.click()
        when (id) {
            "continue" -> {
                val s = app.storage.latest()
                if (s >= 0) app.loadGame(s)
            }
            "new" -> { app.slots.open(SlotScreen.NEW, this); app.go(app.slots) }
            "load" -> { app.slots.open(SlotScreen.LOAD, this); app.go(app.slots) }
            "settings" -> { app.settingsScreen.from = this; app.go(app.settingsScreen) }
            "credits" -> app.go(app.credits)
            "quit" -> app.quit()
        }
    }
}

// ====================================================================== save slots

class SlotScreen(app: App) : Screen(app) {
    companion object { const val NEW = 0; const val LOAD = 1; const val SAVE = 2 }

    private var mode = NEW
    private var from: Screen = app.menu
    private val info = arrayOfNulls<String>(Storage.SLOTS)
    private val corrupt = BooleanArray(Storage.SLOTS)
    private val back = Btn("back", "BACK", 20f, 316f, 100f, 28f)

    fun open(m: Int, f: Screen) {
        mode = m
        from = f
    }

    override fun enter() {
        for (i in 0 until Storage.SLOTS) {
            corrupt[i] = false
            info[i] = null
            if (app.storage.exists(i)) {
                val s = app.storage.load(i)
                if (s == null) corrupt[i] = true else info[i] = s.brief()
            }
        }
    }

    private fun card(i: Int): Btn {
        val w = minOf(app.W - 60f, 460f)
        return Btn("s$i", "", (app.W - w) / 2f, 86f + i * 74f, w, 62f)
    }

    override fun draw(c: Canvas) {
        UI.fill(c, 0f, 0f, app.W, app.H, Pal.NAVY)
        val title = when (mode) { NEW -> "NEW GAME: CHOOSE A SLOT"; LOAD -> "LOAD GAME"; else -> "SAVE GAME" }
        UI.text(c, title, app.W / 2f, 52f, 18f, Pal.AMBER, Paint.Align.CENTER, true)
        UI.text(c, if (mode == NEW) "Pick an empty slot. Existing saves are never overwritten without asking." else "Tap a slot.",
            app.W / 2f, 70f, 10f, Pal.LGREY, Paint.Align.CENTER)
        for (i in 0 until Storage.SLOTS) {
            val b = card(i)
            val has = info[i] != null
            UI.panel(c, b.x, b.y, b.w, b.h, Pal.CHARCOAL, if (has) Pal.AMBER else Pal.GREY)
            UI.text(c, "SLOT " + (i + 1), b.x + 14f, b.y + 24f, 14f, Pal.AMBER, Paint.Align.LEFT, true)
            val line = when {
                corrupt[i] -> "Damaged save (can be overwritten)"
                has -> info[i]!!
                else -> "Empty"
            }
            UI.text(c, line, b.x + 14f, b.y + 46f, 12f, if (has) Pal.BONE else Pal.LGREY)
            if (mode == LOAD && !has) UI.fill(c, b.x, b.y, b.w, b.h, Pal.alpha(Pal.BLACK, 0.45f))
        }
        UI.button(c, back)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a == 0 && back.hit(x, y)) back.down = true
        if (a == 2) {
            if (back.down && back.hit(x, y)) back()
            back.down = false
            for (i in 0 until Storage.SLOTS) if (card(i).hit(x, y)) choose(i)
        }
    }

    private fun choose(i: Int) {
        app.mixer.click()
        val occupied = info[i] != null || corrupt[i]
        when (mode) {
            LOAD -> {
                if (info[i] != null) app.loadGame(i)
            }
            NEW -> {
                if (!occupied) app.newGame(i)
                else app.ask(
                    listOf("Slot " + (i + 1) + " already holds a game. Starting a new game here will erase it."),
                    listOf(Reply("Overwrite slot " + (i + 1), ""), Reply("Cancel", "")), "Overwrite?"
                ) { k -> if (k == 0) app.newGame(i) }
            }
            else -> {
                val doSave = {
                    if (app.saveTo(i)) app.toast("Saved to slot " + (i + 1)) else app.toast("Could not save")
                    enter()
                }
                if (!occupied || i == app.slot) doSave()
                else app.ask(
                    listOf("Replace the save in slot " + (i + 1) + "?"),
                    listOf(Reply("Replace", ""), Reply("Cancel", "")), "Overwrite?"
                ) { k -> if (k == 0) doSave() }
            }
        }
    }

    override fun back() {
        app.go(from)
    }
}

// ====================================================================== settings

class SettingsScreen(app: App) : Screen(app) {
    var from: Screen = app.menu
    private val back = Btn("back", "BACK", 20f, 316f, 100f, 28f)
    private var drag = -1
    private var dragId = -1
    private val toggles = ArrayList<Btn>()
    private val steppers = ArrayList<Btn>()

    private val sliderNames = arrayOf("Master volume", "Music volume", "Effects volume", "Brightness")

    private fun sliderRect(i: Int): FloatArray {
        val x = 28f
        return floatArrayOf(x + 120f, 78f + i * 38f, 150f, 14f)
    }

    private fun get(i: Int): Float = when (i) {
        0 -> app.settings.master; 1 -> app.settings.music; 2 -> app.settings.sfx; else -> app.settings.brightness
    }

    private fun set(i: Int, v: Float) {
        when (i) {
            0 -> app.settings.master = v
            1 -> app.settings.music = v
            2 -> app.settings.sfx = v
            else -> app.settings.brightness = v
        }
        app.applySettings()
    }

    override fun enter() {
        drag = -1
    }

    private fun colX(): Float = minOf(330f, app.W - 300f)

    private fun layoutRight() {
        toggles.clear()
        steppers.clear()
        val x = colX()
        var y = 66f
        fun tog(id: String, label: String, on: Boolean) {
            toggles.add(Btn(id, label + ": " + (if (on) "ON" else "OFF"), x, y, 280f, 28f))
            y += 34f
        }
        tog("subs", "Subtitles", app.settings.subtitles)
        tog("hap", "Vibration", app.settings.haptics)
        tog("shake", "Reduce screen shake", app.settings.reduceShake)
        tog("left", "Left-handed controls", app.settings.leftHanded)
        val speeds = arrayOf("Slow", "Normal", "Fast", "Instant")
        val sizes = arrayOf("100%", "115%", "130%")
        steppers.add(Btn("speed", "Text speed: " + speeds[app.settings.textSpeed.coerceIn(0, 3)], x, y, 280f, 28f))
        y += 34f
        steppers.add(Btn("size", "Text size: " + sizes[app.settings.textScale.coerceIn(0, 2)], x, y, 280f, 28f))
    }

    override fun draw(c: Canvas) {
        UI.fill(c, 0f, 0f, app.W, app.H, Pal.NAVY)
        UI.text(c, "SETTINGS", 28f, 42f, 18f, Pal.AMBER, Paint.Align.LEFT, true)
        for (i in 0 until 4) {
            val r = sliderRect(i)
            UI.text(c, sliderNames[i], 28f, r[1] + 11f, 11f, Pal.BONE)
            UI.fill(c, r[0], r[1] + 4f, r[2], 6f, Pal.DGREY)
            UI.fill(c, r[0], r[1] + 4f, r[2] * get(i), 6f, Pal.AMBER)
            UI.stroke(c, r[0], r[1] + 4f, r[2], 6f, Pal.GREY, 1f)
            UI.fill(c, r[0] + r[2] * get(i) - 4f, r[1], 8f, 14f, Pal.PALE)
            UI.text(c, (get(i) * 100f).toInt().toString() + "%", r[0] + r[2] + 10f, r[1] + 11f, 10f, Pal.LGREY)
        }
        UI.text(c, "Display uses your device's native", 28f, 250f, 10f, Pal.LGREY)
        UI.text(c, "resolution in full screen with vsync.", 28f, 264f, 10f, Pal.LGREY)
        layoutRight()
        for (b in toggles) UI.button(c, b, Pal.AMBER, 11f)
        for (b in steppers) UI.button(c, b, Pal.TEAL, 11f)
        UI.button(c, back)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a == 0) {
            for (i in 0 until 4) {
                val r = sliderRect(i)
                if (x >= r[0] - 12f && x <= r[0] + r[2] + 12f && y >= r[1] - 8f && y <= r[1] + r[3] + 8f) {
                    drag = i
                    dragId = id
                    set(i, ((x - r[0]) / r[2]).coerceIn(0f, 1f))
                }
            }
            if (back.hit(x, y)) back.down = true
            for (b in toggles) if (b.hit(x, y)) b.down = true
            for (b in steppers) if (b.hit(x, y)) b.down = true
        } else if (a == 1) {
            if (drag >= 0 && id == dragId) {
                val r = sliderRect(drag)
                set(drag, ((x - r[0]) / r[2]).coerceIn(0f, 1f))
            }
        } else {
            if (drag >= 0 && id == dragId) {
                drag = -1
                app.saveSettings()
                app.mixer.click()
            }
            if (back.down && back.hit(x, y)) back()
            back.down = false
            for (b in toggles) {
                if (b.down && b.hit(x, y)) {
                    app.mixer.click()
                    when (b.id) {
                        "subs" -> app.settings.subtitles = !app.settings.subtitles
                        "hap" -> app.settings.haptics = !app.settings.haptics
                        "shake" -> app.settings.reduceShake = !app.settings.reduceShake
                        "left" -> app.settings.leftHanded = !app.settings.leftHanded
                    }
                    app.saveSettings()
                    if (b.id == "hap" && app.settings.haptics) app.buzz()
                }
                b.down = false
            }
            for (b in steppers) {
                if (b.down && b.hit(x, y)) {
                    app.mixer.click()
                    if (b.id == "speed") app.settings.textSpeed = (app.settings.textSpeed + 1) % 4
                    else app.settings.textScale = (app.settings.textScale + 1) % 3
                    app.saveSettings()
                }
                b.down = false
            }
        }
    }

    override fun back() {
        app.saveSettings()
        app.go(from)
    }
}

// ====================================================================== credits

class CreditsScreen(app: App) : Screen(app) {
    private var scroll = 0f
    private val lines = listOf(
        "THE LAST FREQUENCY", "", "A radio mystery for Android", "",
        "Design, code, art and sound", "written in Kotlin", "",
        "All art is drawn in code.", "All sound is synthesised in code.", "",
        "Nights: 5", "Voices on the radio: 9", "Endings: 4", "",
        "Thank you for listening.", "", "", "STATION 47 IS STILL ON THE AIR."
    )

    override fun enter() { scroll = 0f }

    override fun update(dt: Float) {
        scroll += dt * 24f
        app.mixer.staticLevel = 0.04f
    }

    override fun draw(c: Canvas) {
        UI.fill(c, 0f, 0f, app.W, app.H, Pal.NAVY)
        var y = app.H - scroll
        for (l in lines) {
            val big = l == "THE LAST FREQUENCY" || l.startsWith("STATION")
            UI.text(c, l, app.W / 2f, y, if (big) 20f else 13f, if (big) Pal.AMBER else Pal.BONE, Paint.Align.CENTER, big, UI.serif)
            y += 30f
        }
        if (y < 0f) scroll = 0f
        UI.text(c, "tap to return", app.W / 2f, app.H - 10f, 9f, Pal.LGREY, Paint.Align.CENTER)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        if (a == 2) back()
    }

    override fun back() {
        app.go(app.menu)
    }
}

// ====================================================================== pause

class PauseScreen(app: App) : Screen(app) {
    private val btns = ArrayList<Btn>()

    override fun enter() {
        btns.clear()
        val x = app.W / 2f - 100f
        var y = 96f
        for ((id, label) in listOf("resume" to "RESUME", "journal" to "JOURNAL", "save" to "SAVE GAME", "settings" to "SETTINGS", "menu" to "SAVE AND EXIT TO MENU")) {
            btns.add(Btn(id, label, x, y, 200f, 30f))
            y += 38f
        }
    }

    override fun draw(c: Canvas) {
        app.world.draw(c)
        UI.fill(c, 0f, 0f, app.W, app.H, Pal.alpha(Pal.NAVY, 0.82f))
        UI.text(c, "PAUSED", app.W / 2f, 72f, 20f, Pal.AMBER, Paint.Align.CENTER, true)
        for (b in btns) UI.button(c, b)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        for (b in btns) {
            if (a == 0 && b.hit(x, y)) b.down = true
            if (a == 2) {
                if (b.down && b.hit(x, y)) press(b.id)
                b.down = false
            }
        }
    }

    private fun press(id: String) {
        app.mixer.click()
        when (id) {
            "resume" -> app.go(app.world, true)
            "journal" -> { app.journal.from = this; app.go(app.journal) }
            "save" -> { app.slots.open(SlotScreen.SAVE, this); app.go(app.slots) }
            "settings" -> { app.settingsScreen.from = this; app.go(app.settingsScreen) }
            "menu" -> app.toMenu()
        }
    }

    override fun back() {
        app.go(app.world, true)
    }
}
