package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import com.zak.lastfreq.core.ClueType
import com.zak.lastfreq.core.Content
import kotlin.math.abs
import kotlin.math.hypot

/** The investigation board: drag evidence cards around and join them with string. */
class BoardScreen(app: App) : Screen(app) {
    var from: Screen? = null
    private val CW = 88f
    private val CH = 64f
    private var panX = 0f
    private var panY = 0f
    private var linkMode = false
    private var selected: String? = null
    private var dragCard: String? = null
    private var dragPan = false
    private var dragId = -1
    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f
    private var moved = false
    private var grabDX = 0f
    private var grabDY = 0f
    private val bBack = Btn("back", "BACK", 8f, 8f, 60f, 24f)
    private val bLink = Btn("link", "LINK", 76f, 8f, 70f, 24f)
    private val bTidy = Btn("tidy", "TIDY", 154f, 8f, 60f, 24f)

    override fun enter() {
        selected = null
        linkMode = false
        dragCard = null
        panX = 0f
        panY = 0f
    }

    private fun cardAt(wx: Float, wy: Float): String? {
        val st = app.st
        var hit: String? = null
        for ((id, p) in st.cards) {
            if (wx >= p[0] && wx <= p[0] + CW && wy >= p[1] && wy <= p[1] + CH) hit = id
        }
        return hit
    }

    private fun typeColor(t: ClueType): Int = when (t) {
        ClueType.RECORDING -> 0xFF9DB8C8.toInt()
        ClueType.DOCUMENT -> Pal.PAPER
        ClueType.PHOTO -> 0xFFC8C8C0.toInt()
        ClueType.CLIPPING -> 0xFFD9D2B8.toInt()
        ClueType.NOTE -> 0xFFE3D58A.toInt()
        ClueType.COORD -> 0xFFA8C8A8.toInt()
    }

    override fun draw(c: Canvas) {
        val st = app.st
        val W = app.W
        val H = app.H
        // cork
        UI.fill(c, 0f, 0f, W, H, 0xFF5A4328.toInt())
        for (i in 0 until 220) {
            val h = Art.hash(i, 11)
            val x = ((h % 997) / 997f) * W
            val y = (((h / 997) % 389) / 389f) * H
            Art.r(c, x, y, 2f, 2f, if (i % 3 == 0) 0xFF6B5030.toInt() else 0xFF4A3720.toInt())
        }
        UI.stroke(c, 3f, 3f, W - 6f, H - 6f, Pal.DWOOD, 6f)

        c.save()
        c.translate(panX, panY)
        // strings
        for (s in st.strings) {
            val a = st.cards[s.first] ?: continue
            val b = st.cards[s.second] ?: continue
            val ax = a[0] + CW / 2f
            val ay = a[1] + 6f
            val bx = b[0] + CW / 2f
            val by = b[1] + 6f
            val solved = Content.links.any {
                ((it.a == s.first && it.b == s.second) || (it.a == s.second && it.b == s.first)) && st.has(it.flag)
            }
            val col = if (solved) Pal.RED else Pal.alpha(Pal.RED, 0.55f)
            val mx = (ax + bx) / 2f
            val my = (ay + by) / 2f + 10f
            UI.line(c, ax, ay, mx, my, col, if (solved) 2f else 1.5f)
            UI.line(c, mx, my, bx, by, col, if (solved) 2f else 1.5f)
        }
        // cards
        for ((id, p) in st.cards) {
            val cl = Content.clues[id] ?: continue
            val x = p[0]
            val y = p[1]
            val sel = selected == id
            UI.fill(c, x + 3f, y + 4f, CW, CH, Pal.alpha(Pal.BLACK, 0.35f))
            UI.fill(c, x, y, CW, CH, typeColor(cl.type))
            UI.stroke(c, x + 0.5f, y + 0.5f, CW - 1f, CH - 1f, if (sel) Pal.AMBER else Pal.alpha(Pal.INK, 0.4f), if (sel) 3f else 1f)
            UI.text(c, cl.type.name, x + 5f, y + 16f, 8f, Pal.alpha(Pal.INK, 0.7f), Paint.Align.LEFT, true)
            val lines = UI.wrap(cl.title, CW - 10f, 10f, true)
            var ty = y + 29f
            for ((i, l) in lines.withIndex()) {
                if (i >= 3) break
                UI.text(c, l, x + 5f, ty, 10f, Pal.INK, Paint.Align.LEFT, true)
                ty += 11f
            }
            UI.circle(c, x + CW / 2f, y + 6f, 4f, Pal.RED)
            UI.circle(c, x + CW / 2f - 1f, y + 5f, 1.4f, Pal.alpha(Pal.BONE, 0.8f))
        }
        c.restore()

        UI.fill(c, 0f, 0f, W, 38f, Pal.alpha(Pal.NAVY, 0.88f))
        UI.button(c, bBack, Pal.AMBER, 11f)
        bLink.label = if (linkMode) "LINK ON" else "LINK"
        UI.button(c, bLink, if (linkMode) Pal.PALE else Pal.TEAL, 11f)
        if (linkMode) UI.stroke(c, bLink.x - 1f, bLink.y - 1f, bLink.w + 2f, bLink.h + 2f, Pal.PALE, 2f)
        UI.button(c, bTidy, Pal.LGREY, 11f)
        val hint = if (st.cards.isEmpty()) "No evidence yet. Decode signals and search locations."
        else if (linkMode) (if (selected == null) "Tap a card, then tap another to join them. Join twice to cut."
        else "Now tap the card to connect it to.")
        else "Drag cards or the board. Tap a card to read it."
        UI.text(c, hint, W - 10f, 24f, 10f, Pal.LGREY, Paint.Align.RIGHT)
    }

    override fun touch(a: Int, id: Int, x: Float, y: Float) {
        when (a) {
            0 -> {
                if (y < 38f) {
                    bBack.down = bBack.hit(x, y)
                    bLink.down = bLink.hit(x, y)
                    bTidy.down = bTidy.hit(x, y)
                    return
                }
                dragId = id
                lastX = x; lastY = y; startX = x; startY = y
                moved = false
                val wx = x - panX
                val wy = y - panY
                val hit = cardAt(wx, wy)
                dragCard = null
                dragPan = false
                if (hit != null) {
                    if (!linkMode) {
                        dragCard = hit
                        val p = app.st.cards[hit]!!
                        grabDX = wx - p[0]
                        grabDY = wy - p[1]
                    }
                } else {
                    dragPan = true
                }
            }
            1 -> if (id == dragId) {
                if (hypot(x - startX, y - startY) > 8f) moved = true
                if (moved) {
                    val dc = dragCard
                    if (dc != null) {
                        val p = app.st.cards[dc]!!
                        p[0] = (x - panX - grabDX).coerceIn(0f, 1400f)
                        p[1] = (y - panY - grabDY).coerceIn(40f, 900f)
                    } else if (dragPan) {
                        panX += x - lastX
                        panY += y - lastY
                        panX = panX.coerceIn(-900f, 40f)
                        panY = panY.coerceIn(-700f, 40f)
                    }
                }
                lastX = x; lastY = y
            }
            2 -> {
                if (bBack.down || bLink.down || bTidy.down) {
                    if (bBack.down && bBack.hit(x, y)) { app.mixer.click(); back() }
                    if (bLink.down && bLink.hit(x, y)) {
                        app.mixer.click()
                        linkMode = !linkMode
                        selected = null
                    }
                    if (bTidy.down && bTidy.hit(x, y)) { app.mixer.click(); tidy() }
                    bBack.down = false; bLink.down = false; bTidy.down = false
                    return
                }
                if (id != dragId) return
                dragId = -1
                if (moved) {
                    if (dragCard != null) app.autosave()
                    dragCard = null
                    dragPan = false
                    return
                }
                dragCard = null
                dragPan = false
                val hit = cardAt(x - panX, y - panY) ?: run { selected = null; return }
                tapCard(hit)
            }
        }
    }

    private fun tapCard(id: String) {
        val st = app.st
        if (!linkMode) {
            val cl = Content.clues[id] ?: return
            app.mixer.click()
            val alt = cl.altFlag != null && st.has(cl.altFlag) && cl.altText != null
            app.say(listOf(if (alt) cl.altText!! else cl.text), cl.title)
            return
        }
        val first = selected
        if (first == null) {
            selected = id
            app.mixer.click()
            return
        }
        if (first == id) {
            selected = null
            return
        }
        selected = null
        val res = st.connect(first, id)
        when (res.kind) {
            1 -> { app.toast("String removed"); app.mixer.click() }
            2 -> {
                app.mixer.click()
                app.buzz(true)
                app.toast("Connection found")
                app.say(listOf(res.message), "Connection")
                app.autosave()
            }
            else -> { app.toast("Connected. Nothing obvious yet."); app.mixer.click() }
        }
    }

    private fun tidy() {
        val st = app.st
        var i = 0
        for ((_, p) in st.cards) {
            p[0] = 20f + (i % 6) * 100f
            p[1] = 56f + (i / 6) * 76f
            i++
        }
        panX = 0f
        panY = 0f
        app.toast("Cards arranged")
        app.autosave()
    }

    override fun back() {
        app.autosave()
        app.go(from ?: app.world)
    }
}
