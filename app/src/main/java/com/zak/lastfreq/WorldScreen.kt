package com.zak.lastfreq

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.zak.lastfreq.core.Content
import com.zak.lastfreq.core.Interact
import com.zak.lastfreq.core.LocationInfo
import com.zak.lastfreq.core.Obj
import com.zak.lastfreq.core.Reply
import com.zak.lastfreq.core.World
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class WorldScreen(app: App) : Screen(app) {
    private var world = World(Content.scenes["station"]!!)
    private var sorted: List<Obj> = emptyList()
    private var bmp: Bitmap? = null
    private val pos = FloatArray(2)
    private var camX = 0f
    private var camY = 0f
    private var animT = 0f
    private var stepT = 0f
    private var moving = false
    private var shakeT = 0f
    private var lowFuelWarned = false
    private var lowBatteryWarned = false
    private var target: Obj? = null

    // controls
    private var joyId = -1
    private var joyBX = 0f
    private var joyBY = 0f
    private var joyX = 0f
    private var joyY = 0f
    private var runId = -1
    private var running = false
    private var useId = -1
    private var lightId = -1
    private val bJournal = Btn("journal", "BOOK", 0f, 8f, 46f, 24f)
    private val bMap = Btn("map", "MAP", 0f, 8f, 46f, 24f)
    private val bPause = Btn("pause", "II", 0f, 8f, 34f, 24f)
    private val nearest = Paint().apply { isFilterBitmap = false }
    private val dst = RectF()

    fun shake(a: Float) {
        shakeT = a
    }

    fun reset() {
        loadScene(true)
        lowFuelWarned = false
        lowBatteryWarned = false
        joyId = -1; runId = -1; useId = -1; lightId = -1
        joyX = 0f; joyY = 0f
        running = false
    }

    private fun loadScene(keepPos: Boolean) {
        val sc = Content.scenes[app.st.scene] ?: Content.scenes["station"]!!
        world = World(sc)
        sorted = sc.objects.sortedBy { (it.y + it.h) }
        bmp = Art.sceneBitmap(sc)
        pos[0] = app.st.px
        pos[1] = app.st.py
        if (!keepPos || pos[0] <= 0f) {
            pos[0] = sc.spawnX * Content.TILE
            pos[1] = sc.spawnY * Content.TILE
        }
        world.unstick(pos)
        snapCamera()
    }

    private fun snapCamera() {
        val c = camTarget()
        camX = c[0]
        camY = c[1]
    }

    private fun camTarget(): FloatArray {
        val w = app.W
        val h = app.H
        var tx = pos[0] - w / 2f
        var ty = pos[1] - h / 2f
        tx = if (world.widthPx <= w) -(w - world.widthPx) / 2f else tx.coerceIn(0f, world.widthPx - w)
        ty = if (world.heightPx <= h) -(h - world.heightPx) / 2f else ty.coerceIn(0f, world.heightPx - h)
        return floatArrayOf(tx, ty)
    }

    override fun enter() {
        loadScene(true)
        joyId = -1; runId = -1; useId = -1; lightId = -1
        joyX = 0f; joyY = 0f
        running = false
    }

    // ------------------------------------------------------------------ update

    override fun update(dt: Float) {
        val st = app.st
        animT += dt
        if (shakeT > 0f) shakeT = (shakeT - dt).coerceAtLeast(0f)

        var dx = joyX
        var dy = joyY
        val mag = sqrt(dx * dx + dy * dy)
        moving = mag > 0.15f
        val w = st.weather
        var speed = 72f * (1f - 0.18f * w.rain)
        val sprint = running && moving && st.stamina > 0f
        if (sprint) {
            speed *= 1.65f
            st.stamina = (st.stamina - 24f * dt).coerceAtLeast(0f)
            if (st.stamina <= 0f) running = false
        } else {
            st.stamina = (st.stamina + 14f * dt).coerceAtMost(100f)
        }
        if (moving) {
            val nx = dx / maxOf(mag, 1f)
            val ny = dy / maxOf(mag, 1f)
            val m = minOf(mag, 1f)
            world.move(pos, nx * m * speed * dt, ny * m * speed * dt)
            st.facing = if (abs(dx) > abs(dy)) (if (dx < 0f) 2 else 3) else (if (dy < 0f) 1 else 0)
            stepT -= dt
            if (stepT <= 0f) {
                stepT = if (sprint) 0.2f else 0.32f
                app.mixer.step()
            }
        }
        st.px = pos[0]
        st.py = pos[1]
        target = world.nearest(pos[0], pos[1])

        val c = camTarget()
        val k = minOf(1f, dt * 6f)
        camX += (c[0] - camX) * k
        camY += (c[1] - camY) * k

        if (!lowFuelWarned && st.fuel < 20f && st.fuel > 0f) {
            lowFuelWarned = true
            app.toast("Generator fuel is low")
        }
        if (!lowBatteryWarned && st.flashlight && st.battery < 20f && st.itemCount("battery") == 0) {
            lowBatteryWarned = true
            app.toast("Flashlight battery is low")
        }
        if (st.advance(dt)) {
            app.endNight()
        }
    }

    // ------------------------------------------------------------------ drawing

    override fun draw(c: Canvas) {
        val st = app.st
        val sc = world.scene
        val W = app.W
        val H = app.H
        val t = animT
        UI.fill(c, 0f, 0f, W, H, Pal.BLACK)

        var sx = 0f
        var sy = 0f
        if (shakeT > 0f && !app.settings.reduceShake) {
            sx = sin(t * 91f) * shakeT * 8f
            sy = sin(t * 77f + 1f) * shakeT * 8f
        }
        c.save()
        c.translate(-camX + sx, -camY + sy)
        val b = bmp
        if (b != null) {
            dst.set(0f, 0f, sc.w * 32f, sc.h * 32f)
            c.drawBitmap(b, null, dst, nearest)
        }
        Art.waterShimmer(c, sc, t, camX, camY, W, H)
        Art.vegetation(c, sc, t, camX, camY, W, H)

        val playerBase = pos[1] + 8f
        var drawnPlayer = false
        val dir = st.facing
        val frame = if (moving) (if (((animT * 8f).toInt() % 2) == 0) 1 else 2) else 0
        for (o in sorted) {
            if (!drawnPlayer && (o.y + o.h) * 32f > playerBase) {
                Art.player(c, pos[0], pos[1], dir, frame, st.flashlight)
                drawnPlayer = true
            }
            val used = o.interact != null && st.used.contains(o.interact)
            Art.prop(c, o, t, st, used)
        }
        if (!drawnPlayer) Art.player(c, pos[0], pos[1], dir, frame, st.flashlight)

        val tg = target
        if (tg != null) {
            val a = 0.6f + 0.4f * sin(t * 6f)
            val x = tg.x * 32f - 3f
            val y = tg.y * 32f - 3f
            val w = tg.w * 32f + 6f
            val h = tg.h * 32f + 6f
            val col = Pal.alpha(Pal.AMBER, a)
            Art.r(c, x, y, 6f, 2f, col); Art.r(c, x, y, 2f, 6f, col)
            Art.r(c, x + w - 6f, y, 6f, 2f, col); Art.r(c, x + w - 2f, y, 2f, 6f, col)
            Art.r(c, x, y + h - 2f, 6f, 2f, col); Art.r(c, x, y + h - 6f, 2f, 6f, col)
            Art.r(c, x + w - 6f, y + h - 2f, 6f, 2f, col); Art.r(c, x + w - 2f, y + h - 6f, 2f, 6f, col)
        }

        drawLighting(c, t)
        c.restore()

        if (sc.indoors) app.weatherFx.drawDust(c, t) else app.weatherFx.drawRain(c, st.weather, W, H, st.weather.wind)
        app.weatherFx.drawFog(c, st.weather, W, H, t)
        if (app.flash > 0f) UI.fill(c, 0f, 0f, W, H, Pal.alpha(Pal.BONE, app.flash * (if (sc.indoors) 0.12f else 0.35f)))

        drawHud(c)
    }

    private fun drawLighting(c: Canvas, t: Float) {
        val st = app.st
        val sc = world.scene
        var dark = sc.dark + st.weather.fog * 0.05f
        if (sc.id == "station" && !st.powered()) dark = 0.96f
        dark *= (1f - app.flash * 0.6f)
        val saved = Light.begin(c, camX - 40f, camY - 40f, camX + app.W + 40f, camY + app.H + 40f, dark, 0xFF05070F.toInt())
        val fogK = 1f - 0.4f * st.weather.fog
        for (o in world.lightSources()) {
            if (sc.id == "station" && !st.powered()) continue
            val fl = 0.88f + 0.12f * sin(t * 11f + o.x * 3f)
            val dip = if (Art.hash((t * 7f).toInt(), (o.x * 10f).toInt()) % 17 == 0) 0.6f else 1f
            Light.hole(c, (o.x + 0.5f) * 32f, (o.y + 0.5f) * 32f, o.light * fl * dip * fogK, 0.95f)
        }
        Light.hole(c, pos[0], pos[1] - 6f, 62f * fogK, 0.8f)
        if (st.flashlight) {
            val d = floatArrayOf(0f, 1f)
            when (st.facing) { 1 -> { d[0] = 0f; d[1] = -1f }; 2 -> { d[0] = -1f; d[1] = 0f }; 3 -> { d[0] = 1f; d[1] = 0f } }
            Light.hole(c, pos[0] + d[0] * 50f, pos[1] - 6f + d[1] * 50f, 112f * fogK, 1f)
            Light.hole(c, pos[0] + d[0] * 92f, pos[1] - 6f + d[1] * 92f, 96f * fogK, 0.95f)
            Light.hole(c, pos[0] + d[0] * 130f, pos[1] - 6f + d[1] * 130f, 64f * fogK, 0.8f)
        }
        Light.end(c, saved)
        for (o in world.lightSources()) {
            if (sc.id == "station" && !st.powered()) continue
            Light.glow(c, (o.x + 0.5f) * 32f, (o.y + 0.5f) * 32f, o.light * 0.85f, 0.7f)
        }
    }

    private fun joyZoneLeft(): Boolean = !app.settings.leftHanded

    private fun layoutButtons() {
        val W = app.W
        bPause.x = W - 42f
        bMap.x = W - 42f - 52f
        bJournal.x = W - 42f - 104f
    }

    private fun useCx(): Float = if (joyZoneLeft()) app.W - 64f else 64f
    private fun useCy(): Float = app.H - 70f
    private fun lightCx(): Float = if (joyZoneLeft()) app.W - 136f else 136f
    private fun lightCy(): Float = app.H - 40f
    private fun runCx(): Float = if (joyZoneLeft()) app.W - 52f else 52f
    private fun runCy(): Float = app.H - 140f

    private fun inCircle(x: Float, y: Float, cx: Float, cy: Float, r: Float): Boolean =
        (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r

    private fun drawHud(c: Canvas) {
        val st = app.st
        val W = app.W
        val H = app.H
        layoutButtons()
        // clock + objective
        UI.fill(c, 8f, 8f, 262f, 22f, Pal.alpha(Pal.NAVY, 0.7f))
        UI.text(c, st.clockText(), 14f, 25f, 17f, Pal.AMBER, Paint.Align.LEFT, true)
        UI.text(c, "NIGHT " + st.night + "  " + st.weather.label.uppercase(), 70f, 24f, 9f, Pal.LGREY)
        val obj = st.objective()
        val lines = UI.wrap(obj, 246f, 10f * UI.textScale)
        val oh = 10f + lines.size * (11f * UI.textScale) + 4f
        UI.fill(c, 8f, 32f, 262f, oh, Pal.alpha(Pal.NAVY, 0.6f))
        UI.fill(c, 8f, 32f, 2f, oh, Pal.AMBER)
        var ly = 44f
        for (l in lines) {
            UI.text(c, l, 15f, ly, 10f * UI.textScale, Pal.BONE)
            ly += 11f * UI.textScale
        }
        // flashlight battery
        val by = 32f + oh + 6f
        UI.fill(c, 8f, by, 96f, 14f, Pal.alpha(Pal.NAVY, 0.7f))
        UI.text(c, "LIGHT", 12f, by + 10f, 8f, Pal.LGREY)
        UI.bar(c, 46f, by + 3f, 54f, 8f, st.battery / 100f, if (st.battery < 20f) Pal.RED else Pal.PALE)
        if (st.itemCount("battery") > 0) UI.text(c, "x" + st.itemCount("battery"), 108f, by + 11f, 9f, Pal.PALE)
        if (st.stamina < 99f) UI.bar(c, 8f, by + 18f, 96f, 4f, st.stamina / 100f, Pal.TEAL)

        UI.button(c, bJournal, Pal.AMBER, 9f)
        UI.button(c, bMap, Pal.AMBER, 9f)
        UI.button(c, bPause, Pal.AMBER, 10f)

        // joystick
        if (joyId >= 0) {
            UI.ring(c, joyBX, joyBY, 46f, Pal.alpha(Pal.BONE, 0.25f), 2f)
            UI.circle(c, joyBX + joyX * 40f, joyBY + joyY * 40f, 20f, Pal.alpha(Pal.BONE, 0.35f))
        } else {
            val hx = if (joyZoneLeft()) 80f else W - 80f
            UI.ring(c, hx, H - 76f, 40f, Pal.alpha(Pal.BONE, 0.12f), 2f)
            UI.circle(c, hx, H - 76f, 16f, Pal.alpha(Pal.BONE, 0.12f))
        }
        // action buttons
        val tg = target
        val ready = tg != null
        UI.circle(c, useCx(), useCy(), 31f, Pal.alpha(Pal.BLACK, 0.5f))
        UI.ring(c, useCx(), useCy(), 30f, if (ready) Pal.AMBER else Pal.alpha(Pal.LGREY, 0.5f), 2f)
        UI.text(c, "USE", useCx(), useCy() + 5f, 13f, if (ready) Pal.AMBER else Pal.LGREY, Paint.Align.CENTER, true)
        UI.circle(c, lightCx(), lightCy(), 23f, Pal.alpha(if (st.flashlight) Pal.DAMBER else Pal.BLACK, 0.6f))
        UI.ring(c, lightCx(), lightCy(), 22f, if (st.flashlight) Pal.PALE else Pal.alpha(Pal.LGREY, 0.6f), 2f)
        UI.text(c, "F", lightCx(), lightCy() + 5f, 14f, if (st.flashlight) Pal.PALE else Pal.LGREY, Paint.Align.CENTER, true)
        UI.circle(c, runCx(), runCy(), 23f, Pal.alpha(if (running) Pal.DTEAL else Pal.BLACK, 0.6f))
        UI.ring(c, runCx(), runCy(), 22f, if (running) Pal.TEAL else Pal.alpha(Pal.LGREY, 0.6f), 2f)
        UI.text(c, "RUN", runCx(), runCy() + 4f, 10f, if (running) Pal.BONE else Pal.LGREY, Paint.Align.CENTER, true)
        if (tg != null) {
            val label = Content.interacts[tg.interact ?: ""]?.prompt ?: ""
            val tw = UI.measure(label, 11f, true) + 18f
            val px = if (joyZoneLeft()) W - 64f - tw / 2f + 10f else 64f + tw / 2f - 10f
            UI.fill(c, px - tw / 2f, useCy() - 62f, tw, 20f, Pal.alpha(Pal.NAVY, 0.85f))
            UI.stroke(c, px - tw / 2f, useCy() - 62f, tw, 20f, Pal.AMBER, 1f)
            UI.text(c, label, px, useCy() - 48f, 11f, Pal.PALE, Paint.Align.CENTER, true)
        }
    }

    // ------------------------------------------------------------------ touch

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        val st = app.st
        val W = app.W
        if (a == 0) {
            if (bJournal.hit(x, y)) { bJournal.down = true; return }
            if (bMap.hit(x, y)) { bMap.down = true; return }
            if (bPause.hit(x, y)) { bPause.down = true; return }
            if (inCircle(x, y, useCx(), useCy(), 36f)) { useId = id; return }
            if (inCircle(x, y, lightCx(), lightCy(), 28f)) { lightId = id; return }
            if (inCircle(x, y, runCx(), runCy(), 28f)) { runId = id; running = true; return }
            val inZone = if (joyZoneLeft()) x < W * 0.45f else x > W * 0.55f
            if (inZone && joyId < 0 && y > 70f) {
                joyId = id
                joyBX = x
                joyBY = y
                joyX = 0f
                joyY = 0f
            }
        } else if (a == 1) {
            if (id == joyId) {
                var dx = (x - joyBX) / 40f
                var dy = (y - joyBY) / 40f
                val m = sqrt(dx * dx + dy * dy)
                if (m > 1f) { dx /= m; dy /= m }
                joyX = dx
                joyY = dy
            }
        } else {
            if (id == joyId) { joyId = -1; joyX = 0f; joyY = 0f }
            if (id == runId) { runId = -1; running = false }
            if (id == useId) {
                useId = -1
                if (inCircle(x, y, useCx(), useCy(), 40f)) useNow()
            }
            if (id == lightId) {
                lightId = -1
                if (inCircle(x, y, lightCx(), lightCy(), 32f)) {
                    if (st.battery > 0f || st.itemCount("battery") > 0) {
                        if (!st.flashlight && st.battery <= 0f) {
                            st.takeItem("battery")
                            st.battery = 100f
                        }
                        st.flashlight = !st.flashlight
                        app.mixer.click()
                    } else {
                        app.toast("No battery left")
                    }
                }
            }
            if (bJournal.down && bJournal.hit(x, y)) { app.mixer.click(); app.journal.from = this; app.go(app.journal) }
            if (bMap.down && bMap.hit(x, y)) { app.mixer.click(); app.map.from = this; app.go(app.map) }
            if (bPause.down && bPause.hit(x, y)) { app.mixer.click(); app.go(app.pause) }
            bJournal.down = false; bMap.down = false; bPause.down = false
        }
    }

    override fun back() {
        app.go(app.pause)
    }

    // ------------------------------------------------------------------ interaction

    private fun useNow() {
        val o = target ?: return
        val ix = Content.interacts[o.interact ?: return] ?: return
        app.mixer.click()
        app.buzz()
        when (ix.special) {
            "radio" -> openRadio()
            "weather" -> weatherReport()
            "generator" -> generator()
            "map", "door" -> { app.map.from = this; app.go(app.map) }
            "board" -> app.go(app.board)
            "bed" -> bed()
            "transmitter" -> transmitter()
            "exit" -> travel(Content.location("station"))
            else -> examine(ix)
        }
    }

    private fun itemName(id: String): String = when (id) {
        "battery" -> "Battery"; "fuel" -> "Fuel can"; "tube" -> "Spare valve"; else -> id
    }

    private fun examine(ix: Interact) {
        val st = app.st
        if (ix.needClue != null && !st.owns(ix.needClue)) {
            app.say(ix.lockedPages ?: listOf("Nothing here for you."))
            return
        }
        val first = !st.used.contains(ix.id)
        val pages: List<String> = when {
            ix.altFlag != null && st.has(ix.altFlag) && ix.altPages != null -> ix.altPages
            !first && ix.usedPages != null -> ix.usedPages
            else -> ix.pages
        }
        if (ix.choices.isNotEmpty() && first) {
            app.ask(pages, ix.choices, ix.prompt) { k ->
                val r: Reply = ix.choices[k]
                st.applyReply(r)
                st.used.add(ix.id)
                app.say(listOf(r.response))
                app.autosave()
            }
            return
        }
        if (first) {
            st.used.add(ix.id)
            if (ix.clue != null && st.addClue(ix.clue)) app.toast("New clue: " + (Content.clues[ix.clue]?.title ?: ix.clue))
            for ((k, v) in ix.items) {
                st.addItem(k, v)
                app.toast("+" + v + " " + itemName(k))
            }
            if (ix.flag != null) st.setFlag(ix.flag)
        } else if (ix.clue != null) {
            st.addClue(ix.clue)
        }
        app.say(pages, ix.prompt)
    }

    private fun openRadio() {
        if (!app.st.powered()) {
            app.say(listOf("The radio is dead. The generator has run out of fuel.", "Refuel it from the generator in the west room."))
            return
        }
        app.go(app.radio)
    }

    private fun weatherReport() {
        val st = app.st
        val next = if (st.night < Content.LAST_NIGHT) Content.nightWeather[st.night + 1].label else "Unknown"
        app.say(listOf(
            "Night " + st.night + ": " + st.weather.label + ".",
            st.weather.desc,
            "The barometer is falling. Tomorrow night looks like: " + next.lowercase() + "."
        ), "Window")
    }

    private fun generator() {
        val st = app.st
        val pct = st.fuel.toInt()
        if (st.itemCount("fuel") > 0 && st.fuel <= 70f) {
            st.takeItem("fuel")
            st.fuel = minOf(100f, st.fuel + 40f)
            lowFuelWarned = false
            app.toast("Generator refuelled")
            app.say(listOf("You pour a can of fuel into the generator. It coughs, then settles into a steady hum.",
                "Fuel: " + st.fuel.toInt() + "%."), "Generator")
        } else if (st.itemCount("fuel") > 0) {
            app.say(listOf("Fuel: " + pct + "%. The tank is nearly full already."), "Generator")
        } else {
            app.say(listOf("Fuel: " + pct + "%.", "You have no spare fuel cans. Look for some while exploring."), "Generator")
        }
    }

    private fun bed() {
        app.ask(
            listOf("End your shift now? The rest of the night will pass."),
            listOf(Reply("End the shift", ""), Reply("Keep working", "")), "Bunk"
        ) { k -> if (k == 0) app.endNight() }
    }

    private fun transmitter() {
        val st = app.st
        if (st.night < Content.LAST_NIGHT) {
            app.say(listOf("The main transmitter. Cold, silent, enormous.", "Nobody has run it in thirty years."), "Transmitter")
        } else if (st.finalReady()) {
            app.go(app.finalScreen)
        } else {
            app.say(listOf("The transmitter hums to itself. It is not time yet.", "Listen to what the night has to say first."), "Transmitter")
        }
    }

    fun travel(loc: LocationInfo) {
        val st = app.st
        st.clock += 25f
        if (st.clock >= Content.NIGHT_MINUTES) {
            app.endNight()
            return
        }
        st.scene = loc.sceneId
        val sc = Content.scenes[loc.sceneId]!!
        if (loc.id == "station") {
            st.px = 12f * Content.TILE
            st.py = 9.6f * Content.TILE
        } else {
            st.px = sc.spawnX * Content.TILE
            st.py = sc.spawnY * Content.TILE
            st.visitedNight.add(loc.sceneId)
        }
        st.facing = 0
        reset()
        app.toast("25 minutes pass")
        app.autosave()
        app.go(this)
    }
}
