package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import com.zak.lastfreq.core.AudioMixer
import com.zak.lastfreq.core.Content
import com.zak.lastfreq.core.GameState
import com.zak.lastfreq.core.Reply
import com.zak.lastfreq.core.Settings
import java.io.File

abstract class Screen(val app: App) {
    open fun enter() {}
    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas)
    /** a: 0 down, 1 move, 2 up */
    open fun touch(a: Int, id: Int, x: Float, y: Float) {}
    open fun back() {}
}

class Dialog(
    val title: String?,
    val pages: List<String>,
    val choices: List<Reply> = emptyList(),
    val onChoice: ((Int) -> Unit)? = null,
    val onClose: (() -> Unit)? = null
) {
    var page = 0
    var typed = 0f
}

class Storage(private val dir: File) {
    init { dir.mkdirs() }

    private fun slotFile(i: Int) = File(dir, "slot$i.sav")

    private fun read(f: File): String? = try {
        if (f.exists()) f.readText() else null
    } catch (e: Exception) {
        null
    }

    private fun write(f: File, text: String): Boolean {
        return try {
            val tmp = File(dir, f.name + ".tmp")
            tmp.writeText(text)
            if (f.exists()) f.delete()
            tmp.renameTo(f)
        } catch (e: Exception) {
            false
        }
    }

    fun exists(i: Int) = slotFile(i).exists()
    fun load(i: Int): GameState? = GameState.parse(read(slotFile(i)))
    fun save(i: Int, st: GameState): Boolean = write(slotFile(i), st.serialize())

    fun latest(): Int {
        var best = -1
        var t = -1L
        for (i in 0 until SLOTS) {
            val f = slotFile(i)
            if (f.exists() && f.lastModified() >= t && load(i) != null) {
                best = i
                t = f.lastModified()
            }
        }
        return best
    }

    fun loadSettings(): Settings = Settings.parse(read(File(dir, "settings.cfg")))
    fun saveSettings(s: Settings) { write(File(dir, "settings.cfg"), s.serialize()) }

    companion object { const val SLOTS = 3 }
}

interface Host {
    fun haptic(long: Boolean)
    fun quit()
}

class App(val mixer: AudioMixer, val storage: Storage, val host: Host) {
    var W = 640f
    var H = 360f
    var st = GameState()
    var settings = storage.loadSettings()
    var slot = 0
    var time = 0f
    var flash = 0f
    val weatherFx = WeatherFx()

    lateinit var screen: Screen
    val menu = MenuScreen(this)
    val slots = SlotScreen(this)
    val settingsScreen = SettingsScreen(this)
    val credits = CreditsScreen(this)
    val pause = PauseScreen(this)
    val world = WorldScreen(this)
    val radio = RadioScreen(this)
    val journal = JournalScreen(this)
    val board = BoardScreen(this)
    val map = MapScreen(this)
    val intro = IntroScreen(this)
    val summary = SummaryScreen(this)
    val finalScreen = FinalScreen(this)
    val ending = EndingScreen(this)

    var dialog: Dialog? = null
    private val toasts = ArrayList<Pair<String, FloatArray>>()
    private var pendingScreen: Screen? = null
    private var fade = 0f
    private var fadeDir = 0
    private var thunderTimer = 15f
    private var thunderDelay = -1f
    private val choiceBtns = ArrayList<Btn>()
    var inGame = false

    init {
        applySettings()
        screen = menu
        menu.enter()
    }

    fun applySettings() {
        mixer.master = settings.master
        mixer.musicVol = settings.music
        mixer.sfxVol = settings.sfx
        UI.textScale = when (settings.textScale) { 1 -> 1.15f; 2 -> 1.3f; else -> 1f }
    }

    fun saveSettings() {
        storage.saveSettings(settings)
        applySettings()
    }

    fun resize(w: Int, h: Int) {
        val s = h / 360f
        W = w / s
        H = 360f
    }

    // ------------------------------------------------------------------ navigation

    fun go(next: Screen, instant: Boolean = false) {
        if (instant) {
            screen = next
            next.enter()
            return
        }
        pendingScreen = next
        fadeDir = 1
    }

    fun buzz(long: Boolean = false) {
        if (settings.haptics) host.haptic(long)
    }

    fun toast(msg: String) {
        toasts.add(Pair(msg, floatArrayOf(3.2f)))
        if (toasts.size > 3) toasts.removeAt(0)
    }

    fun say(pages: List<String>, title: String? = null, onClose: (() -> Unit)? = null) {
        dialog = Dialog(title, pages, emptyList(), null, onClose)
    }

    fun ask(pages: List<String>, choices: List<Reply>, title: String? = null, onChoice: (Int) -> Unit) {
        dialog = Dialog(title, pages, choices, onChoice, null)
    }

    // ------------------------------------------------------------------ game flow

    fun newGame(slotIdx: Int) {
        slot = slotIdx
        st = GameState()
        st.startNight(1)
        inGame = true
        intro.setNight(1)
        go(intro)
    }

    fun loadGame(slotIdx: Int): Boolean {
        val s = storage.load(slotIdx) ?: return false
        st = s
        slot = slotIdx
        inGame = true
        world.reset()
        if (st.ending.isNotEmpty()) {
            ending.setEnding(st.ending)
            go(ending)
        } else {
            go(world)
        }
        return true
    }

    fun autosave() {
        if (inGame && st.ending.isEmpty()) storage.save(slot, st)
    }

    fun saveTo(slotIdx: Int): Boolean {
        slot = slotIdx
        return storage.save(slotIdx, st)
    }

    fun beginShift() {
        world.reset()
        autosave()
        go(world)
    }

    fun endNight() {
        dialog = null
        if (st.night >= Content.LAST_NIGHT && st.ending.isEmpty()) {
            finishGame("loop")
            return
        }
        summary.prepare()
        autosaveForSummary()
        go(summary)
    }

    private fun autosaveForSummary() {
        // save the state as "start of next night" so Continue resumes after the summary
        val copy = GameState.parse(st.serialize())
        if (copy != null && copy.night < Content.LAST_NIGHT) {
            copy.startNight(copy.night + 1)
            storage.save(slot, copy)
        }
    }

    fun nextNight() {
        st.startNight(st.night + 1)
        intro.setNight(st.night)
        go(intro)
    }

    fun finishGame(endingId: String) {
        st.ending = endingId
        storage.save(slot, st)
        ending.setEnding(endingId)
        go(ending)
    }

    fun toMenu() {
        autosave()
        inGame = false
        dialog = null
        go(menu)
    }

    fun quit() {
        autosave()
        host.quit()
    }

    // ------------------------------------------------------------------ frame

    fun update(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0f, 0.05f)
        time += dt
        if (flash > 0f) flash = (flash - dt * 2.2f).coerceAtLeast(0f)

        // audio defaults; screens override while they own the radio
        mixer.staticLevel = 0f
        mixer.voiceVol = 0f
        mixer.toneVol = 0f

        if (fadeDir == 1) {
            fade += dt * 4.5f
            if (fade >= 1f) {
                fade = 1f
                val p = pendingScreen
                if (p != null) {
                    screen = p
                    p.enter()
                }
                pendingScreen = null
                fadeDir = -1
            }
        } else if (fadeDir == -1) {
            fade -= dt * 3.5f
            if (fade <= 0f) {
                fade = 0f
                fadeDir = 0
            }
        }

        val d = dialog
        if (d != null) {
            val cps = when (settings.textSpeed) { 0 -> 25f; 1 -> 55f; 2 -> 110f; else -> 100000f }
            d.typed += dt * cps
        } else {
            screen.update(dt)
        }
        weatherFx.update(dt, st.weather, W, H)
        updateAmbience(dt)

        val it = toasts.iterator()
        while (it.hasNext()) {
            val t = it.next()
            t.second[0] -= dt
            if (t.second[0] <= 0f) it.remove()
        }
    }

    private fun isGameScreen(): Boolean =
        screen === world || screen === radio || screen === journal || screen === board || screen === map ||
            screen === pause || screen === finalScreen || screen === summary || screen === intro

    private fun updateAmbience(dt: Float) {
        val m = mixer
        if (!isGameScreen()) {
            val menuScreen = screen === menu || screen === slots || screen === settingsScreen || screen === credits
            m.ocean = if (menuScreen) 0.5f else 0.2f
            m.rain = if (menuScreen) 0.35f else 0.1f
            m.wind = 0.2f
            m.hum = 0.15f
            m.musicLevel = 0.9f
            return
        }
        val sc = Content.scenes[st.scene] ?: Content.scenes["station"]!!
        val w = st.weather
        val inside = sc.indoors
        m.ocean = sc.amb.ocean * (if (inside) 0.5f else 1f)
        m.rain = w.rain * (if (inside) 0.35f else 0.8f)
        m.wind = sc.amb.wind * (0.3f + w.wind * 0.8f)
        m.hum = if (sc.id == "station") (if (st.powered()) sc.amb.hum else 0f) else sc.amb.hum
        m.musicLevel = sc.amb.music * (if (screen === summary || screen === intro) 1.2f else 1f)

        if (w.storm || w.rain > 0.8f) {
            thunderTimer -= dt
            if (thunderTimer <= 0f) {
                thunderTimer = if (w.storm) 9f + (time * 7f) % 12f else 22f + (time * 5f) % 20f
                flash = 1f
                thunderDelay = 0.4f + (time * 3f) % 1.2f
            }
        }
        if (thunderDelay >= 0f) {
            thunderDelay -= dt
            if (thunderDelay < 0f) {
                mixer.thunder()
                if (!settings.reduceShake) world.shake(0.35f)
            }
        }
    }

    fun draw(c: Canvas) {
        screen.draw(c)
        drawToasts(c)
        drawDialog(c)
        if (fade > 0f) UI.fill(c, 0f, 0f, W, H, Pal.alpha(Pal.BLACK, fade))
        // brightness: below 0.5 darkens, above 0.5 lifts slightly
        val b = settings.brightness
        if (b < 0.5f) UI.fill(c, 0f, 0f, W, H, Pal.alpha(Pal.BLACK, (0.5f - b) * 1.2f))
        else if (b > 0.5f) UI.fill(c, 0f, 0f, W, H, Pal.alpha(Pal.BONE, (b - 0.5f) * 0.18f))
    }

    private fun drawToasts(c: Canvas) {
        var y = 48f
        for (t in toasts) {
            val a = (t.second[0] / 0.6f).coerceIn(0f, 1f)
            val tw = UI.measure(t.first, 12f, true) + 20f
            val x = W / 2f - tw / 2f
            UI.fill(c, x, y, tw, 22f, Pal.alpha(Pal.CHARCOAL, 0.92f * a))
            UI.stroke(c, x, y, tw, 22f, Pal.alpha(Pal.AMBER, 0.8f * a), 1f)
            UI.text(c, t.first, W / 2f, y + 15f, 12f, Pal.alpha(Pal.PALE, a), Paint.Align.CENTER, true)
            y += 26f
        }
    }

    private fun drawDialog(c: Canvas) {
        val d = dialog ?: return
        val pw = minOf(W - 24f, 660f)
        val px = (W - pw) / 2f
        val body = 13f * UI.textScale
        val text = d.pages[d.page]
        val shown = text.substring(0, minOf(text.length, d.typed.toInt()))
        val lines = UI.wrap(text, pw - 28f, body, false, UI.serif)
        val lastPage = d.page == d.pages.size - 1
        val showChoices = lastPage && d.choices.isNotEmpty() && d.typed >= text.length
        val ph = 26f + lines.size * (body + 4f) + (if (showChoices) d.choices.size * 30f + 8f else 18f)
        val py = H - ph - 10f
        UI.fill(c, 0f, 0f, W, H, Pal.alpha(Pal.BLACK, 0.35f))
        UI.panel(c, px, py, pw, ph, Pal.alpha(Pal.NAVY, 0.97f), Pal.AMBER)
        var ty = py + 10f + body
        if (d.title != null) {
            UI.text(c, d.title.uppercase(), px + 14f, py + 12f, 10f, Pal.AMBER, Paint.Align.LEFT, true)
            ty += 4f
        }
        var remaining = shown.length
        for (l in lines) {
            val part = if (remaining >= l.length) l else l.substring(0, maxOf(0, remaining))
            UI.text(c, part, px + 14f, ty + (if (d.title != null) 6f else 0f), body, Pal.BONE, Paint.Align.LEFT, false, UI.serif)
            remaining -= l.length + 1
            ty += body + 4f
        }
        choiceBtns.clear()
        if (showChoices) {
            var by = ty + (if (d.title != null) 6f else 0f) - body + 8f
            for (i in d.choices.indices) {
                val b = Btn("c$i", d.choices[i].text, px + 14f, by, pw - 28f, 26f)
                choiceBtns.add(b)
                UI.button(c, b, Pal.AMBER, 12f)
                by += 30f
            }
        } else if (d.typed >= text.length) {
            val blink = if ((time * 2f).toInt() % 2 == 0) Pal.AMBER else Pal.DAMBER
            UI.text(c, if (lastPage) "tap to close" else "tap to continue", px + pw - 14f, py + ph - 8f, 10f, blink, Paint.Align.RIGHT)
        }
    }

    // ------------------------------------------------------------------ input

    fun touch(a: Int, id: Int, x: Float, y: Float) {
        val d = dialog
        if (d != null) {
            if (a != 0) return
            val text = d.pages[d.page]
            val lastPage = d.page == d.pages.size - 1
            if (d.typed < text.length) {
                d.typed = text.length.toFloat()
                return
            }
            if (lastPage && d.choices.isNotEmpty()) {
                for (i in choiceBtns.indices) {
                    if (choiceBtns[i].hit(x, y)) {
                        dialog = null
                        mixer.click()
                        d.onChoice?.invoke(i)
                        return
                    }
                }
                return
            }
            if (lastPage) {
                dialog = null
                d.onClose?.invoke()
            } else {
                d.page++
                d.typed = 0f
            }
            return
        }
        if (fadeDir == 1) return
        screen.touch(a, id, x, y)
    }

    fun onBack() {
        val d = dialog
        if (d != null) {
            if (d.choices.isEmpty()) touch(0, 0, 0f, 0f)
            return
        }
        screen.back()
    }
}
