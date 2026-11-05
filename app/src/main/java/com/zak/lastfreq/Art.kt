package com.zak.lastfreq

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.zak.lastfreq.core.Content
import com.zak.lastfreq.core.GameState
import com.zak.lastfreq.core.Obj
import com.zak.lastfreq.core.Scene
import com.zak.lastfreq.core.Weather
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Everything in the game world is drawn in code: tiles, props, the player, weather and light. */
object Art {
    private val p = Paint()
    private val bitmaps = HashMap<String, Bitmap>()

    fun hash(x: Int, y: Int, s: Int = 0): Int {
        var h = x * 374761393 + y * 668265263 + s * 1274126177
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 0x7fffffff
    }

    fun r(c: Canvas, x: Float, y: Float, w: Float, h: Float, col: Int) {
        p.color = col
        c.drawRect(x, y, x + w, y + h, p)
    }

    // ------------------------------------------------------------------ tiles

    fun sceneBitmap(sc: Scene): Bitmap {
        val cached = bitmaps[sc.id]
        if (cached != null) return cached
        val bmp = Bitmap.createBitmap((sc.w * 32), (sc.h * 32), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        for (ty in 0 until sc.h) for (tx in 0 until sc.w) tile(c, sc, tx, ty)
        bitmaps[sc.id] = bmp
        return bmp
    }

    private fun isWall(sc: Scene, tx: Int, ty: Int): Boolean {
        if (tx < 0 || ty < 0 || tx >= sc.w || ty >= sc.h) return true
        val ch = sc.tiles[ty][tx]
        return ch == '#' || ch == 'T'
    }

    private fun tile(c: Canvas, sc: Scene, tx: Int, ty: Int) {
        val x = tx * 32f
        val y = ty * 32f
        when (sc.tiles[ty][tx]) {
            '.' -> floor(c, sc.theme, x, y, tx, ty)
            '#' -> wall(c, sc, x, y, tx, ty)
            ',' -> grass(c, x, y, tx, ty)
            '~' -> water(c, x, y, tx, ty)
            '=' -> road(c, x, y, tx, ty)
            'p' -> pier(c, x, y, tx, ty)
            'T' -> tree(c, x, y, tx, ty)
            else -> r(c, x, y, 32f, 32f, Pal.BLACK)
        }
    }

    private fun floor(c: Canvas, theme: String, x: Float, y: Float, tx: Int, ty: Int) {
        when (theme) {
            "station", "harbour" -> {
                val base = if (theme == "station") Pal.WOOD else 0xFF3A3731.toInt()
                val dark = if (theme == "station") Pal.DWOOD else 0xFF2A2824.toInt()
                r(c, x, y, 32f, 32f, base)
                for (i in 0 until 4) {
                    val sh = hash(tx, ty * 4 + i)
                    if (sh % 3 == 0) r(c, x, y + i * 8f, 32f, 8f, Pal.alpha(dark, 0.45f))
                    r(c, x, y + i * 8f + 7f, 32f, 1f, dark)
                    val off = (sh % 24).toFloat()
                    r(c, x + off, y + i * 8f, 1f, 8f, dark)
                    if (sh % 11 == 0) r(c, x + 6f + (sh % 18), y + i * 8f + 3f, 2f, 2f, dark)
                }
            }
            "lighthouse", "cemetery" -> {
                r(c, x, y, 32f, 32f, 0xFF4B4F57.toInt())
                r(c, x, y + 15f, 32f, 2f, 0xFF30343A.toInt())
                r(c, x + 15f, y, 2f, 16f, 0xFF30343A.toInt())
                r(c, x + (if (hash(tx, ty) % 2 == 0) 6f else 24f), y + 20f, 2f, 12f, 0xFF30343A.toInt())
                if (hash(tx, ty, 5) % 4 == 0) r(c, x + 8f, y + 6f, 4f, 2f, 0xFF5A5F68.toInt())
            }
            "hospital" -> {
                val a = if ((tx + ty) % 2 == 0) 0xFF4E5A5C.toInt() else 0xFF445052.toInt()
                r(c, x, y, 32f, 32f, a)
                r(c, x, y, 32f, 1f, 0xFF354042.toInt())
                r(c, x, y, 1f, 32f, 0xFF354042.toInt())
                if (hash(tx, ty, 2) % 7 == 0) r(c, x + 10f, y + 12f, 8f, 6f, Pal.alpha(Pal.DTEAL, 0.5f))
            }
            else -> {
                r(c, x, y, 32f, 32f, 0xFF3A3D42.toInt())
                for (i in 0 until 6) {
                    val h = hash(tx * 7 + i, ty)
                    r(c, x + (h % 30), y + ((h / 31) % 30), 2f, 2f, 0xFF44484E.toInt())
                }
                r(c, x, y + 31f, 32f, 1f, 0xFF2C2F34.toInt())
            }
        }
    }

    private fun wall(c: Canvas, sc: Scene, x: Float, y: Float, tx: Int, ty: Int) {
        val base: Int
        val dark: Int
        val light: Int
        when (sc.theme) {
            "station" -> { base = 0xFF2A2018.toInt(); dark = 0xFF1A130E.toInt(); light = 0xFF3A2D22.toInt() }
            "harbour" -> { base = 0xFF4A3028.toInt(); dark = 0xFF2E1C18.toInt(); light = 0xFF5E3E34.toInt() }
            "lighthouse" -> { base = 0xFF5A5E66.toInt(); dark = 0xFF3A3D44.toInt(); light = 0xFF70747C.toInt() }
            "hospital" -> { base = 0xFF3E5658.toInt(); dark = 0xFF283A3C.toInt(); light = 0xFF557174.toInt() }
            "cemetery" -> { base = 0xFF54575C.toInt(); dark = 0xFF36393E.toInt(); light = 0xFF6C7076.toInt() }
            else -> { base = 0xFF2C2F34.toInt(); dark = 0xFF1C1E22.toInt(); light = 0xFF3C4046.toInt() }
        }
        r(c, x, y, 32f, 32f, base)
        when (sc.theme) {
            "harbour" -> {
                for (i in 0 until 4) {
                    r(c, x, y + i * 8f + 7f, 32f, 1f, dark)
                    val off = if (i % 2 == 0) 0f else 8f
                    r(c, x + off + 15f, y + i * 8f, 1f, 8f, dark)
                    r(c, x + off + 31f, y + i * 8f, 1f, 8f, dark)
                }
            }
            "lighthouse", "cemetery" -> {
                r(c, x, y + 15f, 32f, 1f, dark)
                r(c, x + 15f, y, 1f, 16f, dark)
                r(c, x + 7f, y + 16f, 1f, 16f, dark)
                r(c, x + 23f, y + 16f, 1f, 16f, dark)
            }
            "hospital" -> {
                r(c, x, y + 15f, 32f, 1f, dark)
                r(c, x + 15f, y, 1f, 32f, dark)
            }
            "comms" -> {
                r(c, x + 2f, y + 2f, 2f, 2f, light)
                r(c, x + 28f, y + 2f, 2f, 2f, light)
                r(c, x + 2f, y + 28f, 2f, 2f, light)
                r(c, x + 28f, y + 28f, 2f, 2f, light)
            }
            else -> {
                for (i in 1 until 4) r(c, x + i * 8f - 1f, y, 1f, 32f, dark)
            }
        }
        if (!isWall(sc, tx, ty - 1)) r(c, x, y, 32f, 3f, light)
        if (!isWall(sc, tx, ty + 1)) {
            r(c, x, y + 24f, 32f, 8f, Pal.alpha(dark, 0.8f))
            r(c, x, y + 31f, 32f, 1f, Pal.BLACK)
        }
        if (!isWall(sc, tx - 1, ty)) r(c, x, y, 1f, 32f, Pal.alpha(Pal.BLACK, 0.4f))
        if (!isWall(sc, tx + 1, ty)) r(c, x + 31f, y, 1f, 32f, Pal.alpha(Pal.BLACK, 0.4f))
    }

    private fun grass(c: Canvas, x: Float, y: Float, tx: Int, ty: Int) {
        r(c, x, y, 32f, 32f, 0xFF1C2B26.toInt())
        for (i in 0 until 9) {
            val h = hash(tx * 13 + i, ty * 7 + i)
            val bx = (h % 15) * 2f
            val by = ((h / 16) % 15) * 2f
            r(c, x + bx, y + by, 2f, 4f, if (i % 3 == 0) 0xFF2B4236.toInt() else 0xFF24372E.toInt())
        }
        if (hash(tx, ty, 9) % 9 == 0) r(c, x + 12f, y + 18f, 4f, 4f, 0xFF3A3F36.toInt())
    }

    private fun water(c: Canvas, x: Float, y: Float, tx: Int, ty: Int) {
        r(c, x, y, 32f, 32f, 0xFF132A40.toInt())
        for (i in 0 until 5) {
            val h = hash(tx * 5 + i, ty * 3 + i, 4)
            r(c, x + (h % 28), y + ((h / 29) % 30), 4f, 2f, 0xFF1A3A56.toInt())
        }
    }

    private fun road(c: Canvas, x: Float, y: Float, tx: Int, ty: Int) {
        r(c, x, y, 32f, 32f, 0xFF2A2D33.toInt())
        r(c, x, y + 31f, 32f, 1f, 0xFF1E2025.toInt())
        r(c, x + 15f, y, 1f, 32f, 0xFF22252A.toInt())
        if (hash(tx, ty, 3) % 3 == 0) {
            r(c, x + 6f, y + 10f, 18f, 8f, Pal.alpha(0xFF4A5E74.toInt(), 0.45f))
            r(c, x + 10f, y + 12f, 8f, 2f, Pal.alpha(0xFF6A8098.toInt(), 0.5f))
        }
    }

    private fun pier(c: Canvas, x: Float, y: Float, tx: Int, ty: Int) {
        r(c, x, y, 32f, 32f, 0xFF4A3826.toInt())
        for (i in 0 until 4) {
            r(c, x + i * 8f + 7f, y, 1f, 32f, 0xFF2E2216.toInt())
            if (hash(tx, ty, i) % 5 == 0) r(c, x + i * 8f + 2f, y + 6f, 3f, 12f, 0xFF3A2C1E.toInt())
        }
        r(c, x + 3f, y + 4f, 2f, 2f, 0xFF7A6A52.toInt())
        r(c, x + 27f, y + 26f, 2f, 2f, 0xFF7A6A52.toInt())
    }

    private fun tree(c: Canvas, x: Float, y: Float, tx: Int, ty: Int) {
        r(c, x, y, 32f, 32f, 0xFF1C2B26.toInt())
        r(c, x + 12f, y + 20f, 8f, 12f, 0xFF2E2216.toInt())
        r(c, x + 2f, y + 2f, 28f, 22f, 0xFF16251F.toInt())
        r(c, x, y + 8f, 32f, 12f, 0xFF16251F.toInt())
        r(c, x + 6f, y + 4f, 8f, 4f, 0xFF1F3A2C.toInt())
        r(c, x + 18f, y + 12f, 8f, 4f, 0xFF1F3A2C.toInt())
        if (hash(tx, ty) % 2 == 0) r(c, x + 8f, y + 14f, 6f, 4f, 0xFF102018.toInt())
    }

    // ------------------------------------------------------------------ props

    private fun glint(c: Canvas, x: Float, y: Float, t: Float, used: Boolean) {
        if (used) return
        val a = 0.35f + 0.35f * sin(t * 3f + x)
        r(c, x, y, 2f, 2f, Pal.alpha(Pal.PALE, a))
    }

    fun prop(c: Canvas, o: Obj, t: Float, st: GameState, used: Boolean) {
        val x = o.x * 32f
        val y = o.y * 32f
        val w = o.w * 32f
        val h = o.h * 32f
        when (o.type) {
            "radiodesk" -> {
                r(c, x, y + 12f, w, 20f, Pal.DWOOD)
                r(c, x, y + 8f, w, 6f, Pal.WOOD)
                r(c, x + 8f, y, 56f, 16f, Pal.CHARCOAL)
                r(c, x + 8f, y, 56f, 2f, Pal.GREY)
                val on = st.powered()
                val fl = if (on) 0.75f + 0.25f * sin(t * 9f) else 0.1f
                r(c, x + 12f, y + 3f, 26f, 9f, Pal.alpha(Pal.AMBER, fl))
                r(c, x + 12f, y + 7f, 26f, 1f, Pal.alpha(Pal.DWOOD, 0.8f))
                r(c, x + 43f, y + 5f, 6f, 6f, Pal.LGREY)
                r(c, x + 52f, y + 5f, 6f, 6f, Pal.LGREY)
                r(c, x + 68f, y + 2f, 22f, 12f, Pal.DGREY)
                for (i in 0 until 4) r(c, x + 70f, y + 4f + i * 3f, 18f, 1f, Pal.BLACK)
                r(c, x + 6f, y + 24f, 84f, 2f, Pal.alpha(Pal.BLACK, 0.4f))
            }
            "shelf" -> {
                r(c, x + 2f, y + 3f, 28f, 27f, Pal.DWOOD)
                r(c, x + 2f, y + 15f, 28f, 2f, Pal.WOOD)
                for (i in 0 until 5) {
                    r(c, x + 4f + i * 5f, y + 5f, 4f, 10f, if (i % 2 == 0) Pal.DAMBER else Pal.BLUE)
                    r(c, x + 4f + i * 5f, y + 18f, 4f, 10f, if (i % 2 == 0) Pal.DTEAL else Pal.RED)
                }
                glint(c, x + 26f, y + 6f, t, used)
            }
            "cabinet" -> {
                r(c, x + 4f, y + 2f, 24f, 28f, Pal.DGREY)
                r(c, x + 4f, y + 2f, 24f, 2f, Pal.GREY)
                r(c, x + 15f, y + 6f, 2f, 22f, Pal.BLACK)
                r(c, x + 12f, y + 16f, 2f, 5f, Pal.LGREY)
                r(c, x + 18f, y + 16f, 2f, 5f, Pal.LGREY)
                glint(c, x + 25f, y + 5f, t, used)
            }
            "drawer" -> {
                r(c, x + 3f, y + 6f, 26f, 24f, Pal.WOOD)
                r(c, x + 3f, y + 6f, 26f, 2f, Pal.alpha(Pal.BONE, 0.2f))
                r(c, x + 5f, y + 10f, 22f, 8f, Pal.DWOOD)
                r(c, x + 5f, y + 20f, 22f, 8f, Pal.DWOOD)
                r(c, x + 14f, y + 13f, 4f, 2f, Pal.AMBER)
                r(c, x + 14f, y + 23f, 4f, 2f, Pal.AMBER)
                glint(c, x + 25f, y + 8f, t, used)
            }
            "window" -> {
                r(c, x + 4f, y + 6f, w - 8f, 22f, Pal.DWOOD)
                r(c, x + 7f, y + 9f, w - 14f, 16f, 0xFF0F1C2C.toInt())
                r(c, x + w / 2f - 1f, y + 9f, 2f, 16f, Pal.DWOOD)
                r(c, x + 7f, y + 16f, w - 14f, 1f, Pal.DWOOD)
                if (st.weather.rain > 0.2f) {
                    for (i in 0 until 5) {
                        val yy = ((t * 30f + i * 7f) % 16f)
                        r(c, x + 9f + i * (w - 18f) / 5f, y + 9f + yy, 1f, 3f, Pal.alpha(Pal.BLUE, 0.7f))
                    }
                }
            }
            "clipping" -> {
                r(c, x + 8f, y + 6f, 16f, 20f, Pal.PAPER)
                for (i in 0 until 4) r(c, x + 10f, y + 9f + i * 4f, 12f, 1f, Pal.INK)
                r(c, x + 15f, y + 4f, 2f, 3f, Pal.RED)
            }
            "answering" -> {
                r(c, x + 5f, y + 12f, 22f, 16f, Pal.CHARCOAL)
                r(c, x + 5f, y + 12f, 22f, 2f, Pal.GREY)
                r(c, x + 8f, y + 17f, 8f, 6f, Pal.DGREY)
                val blink = if (sin(t * 4f) > 0f) Pal.RED else Pal.alpha(Pal.RED, 0.25f)
                r(c, x + 20f, y + 18f, 3f, 3f, blink)
            }
            "generator" -> {
                val vib = if (st.powered()) sin(t * 38f) * 0.6f else 0f
                r(c, x + vib, y + 6f, w, h - 6f, Pal.GREY)
                r(c, x + vib, y + 6f, w, 3f, Pal.LGREY)
                r(c, x + 4f + vib, y + 14f, w - 8f, 22f, Pal.DGREY)
                for (i in 0 until 5) r(c, x + 6f + vib, y + 17f + i * 4f, w - 12f, 1f, Pal.BLACK)
                r(c, x + w - 14f + vib, y - 6f, 5f, 12f, Pal.DGREY)
                r(c, x + 6f + vib, y + h - 14f, 6f, 6f, if (st.powered()) Pal.AMBER else Pal.alpha(Pal.AMBER, 0.15f))
                r(c, x + 16f + vib, y + h - 14f, 14f, 6f, Pal.BLACK)
                r(c, x + 16f + vib, y + h - 14f, 14f * (st.fuel / 100f), 6f, Pal.ORANGE)
            }
            "maptable" -> {
                r(c, x + 2f, y + 6f, w - 4f, h - 10f, Pal.WOOD)
                r(c, x + 6f, y + 10f, w - 12f, h - 18f, Pal.PAPER)
                r(c, x + 10f, y + 14f, 20f, 3f, Pal.BLUE)
                r(c, x + 30f, y + 20f, 24f, 3f, Pal.TEAL)
                r(c, x + 14f, y + 26f, 3f, 3f, Pal.RED)
                r(c, x + w - 8f, y + h - 10f, 4f, 4f, Pal.ORANGE)
            }
            "board" -> {
                r(c, x + 2f, y + 4f, w - 4f, 24f, 0xFF6B4E32.toInt())
                r(c, x + 2f, y + 4f, w - 4f, 2f, Pal.DWOOD)
                r(c, x + 2f, y + 26f, w - 4f, 2f, Pal.DWOOD)
                var i = 0
                var cx = x + 8f
                while (cx < x + w - 14f) {
                    r(c, cx, y + 9f + (i % 2) * 3f, 10f, 12f, if (i % 3 == 0) Pal.PAPER else if (i % 3 == 1) Pal.PALE else Pal.BONE)
                    r(c, cx + 4f, y + 8f + (i % 2) * 3f, 2f, 2f, Pal.RED)
                    cx += 15f
                    i++
                }
                r(c, x + 12f, y + 14f, w - 24f, 1f, Pal.alpha(Pal.RED, 0.8f))
            }
            "photo", "frame" -> {
                r(c, x + 8f, y + 6f, 16f, 20f, Pal.DWOOD)
                r(c, x + 10f, y + 8f, 12f, 16f, 0xFF8A8F94.toInt())
                r(c, x + 12f, y + 12f, 3f, 8f, Pal.DGREY)
                r(c, x + 17f, y + 12f, 3f, 8f, Pal.DGREY)
                glint(c, x + 22f, y + 7f, t, used)
            }
            "bed" -> {
                r(c, x + 2f, y + 2f, w - 4f, h - 4f, Pal.DWOOD)
                r(c, x + 5f, y + 5f, w - 10f, 14f, Pal.BONE)
                r(c, x + 5f, y + 20f, w - 10f, h - 26f, Pal.DTEAL)
                r(c, x + 5f, y + 20f, w - 10f, 3f, Pal.TEAL)
            }
            "transmitter" -> {
                r(c, x + 2f, y + 4f, w - 4f, h - 6f, Pal.DGREY)
                r(c, x + 2f, y + 4f, w - 4f, 3f, Pal.GREY)
                val glow = if (st.powered()) 0.55f + 0.35f * sin(t * 2.2f) else 0.1f
                for (i in 0 until 3) {
                    r(c, x + 10f + i * 28f, y + 12f, 12f, 24f, Pal.alpha(Pal.BLACK, 0.8f))
                    r(c, x + 12f + i * 28f, y + 20f, 8f, 14f, Pal.alpha(Pal.AMBER, glow))
                }
                r(c, x + 6f, y + h - 14f, w - 12f, 6f, Pal.BLACK)
                r(c, x + 10f, y + h - 12f, 4f, 2f, Pal.RED)
            }
            "door" -> {
                r(c, x + 2f, y + 2f, w - 4f, 28f, Pal.DWOOD)
                r(c, x + 5f, y + 4f, w - 10f, 24f, Pal.WOOD)
                r(c, x + w / 2f - 1f, y + 4f, 2f, 24f, Pal.DWOOD)
                r(c, x + w / 2f - 6f, y + 16f, 3f, 3f, Pal.AMBER)
                r(c, x + w / 2f + 3f, y + 16f, 3f, 3f, Pal.AMBER)
            }
            "chair" -> {
                r(c, x + 8f, y + 10f, 16f, 14f, Pal.DWOOD)
                r(c, x + 8f, y + 8f, 16f, 3f, Pal.WOOD)
            }
            "lamp" -> {
                r(c, x + 14f, y + 10f, 3f, 20f, Pal.DGREY)
                r(c, x + 10f, y + 4f, 11f, 8f, Pal.DGREY)
                val fl = 0.7f + 0.3f * sin(t * 13f + o.x * 5f) * (if (hash(o.x.toInt(), o.y.toInt()) % 3 == 0) 1f else 0.2f)
                r(c, x + 12f, y + 6f, 7f, 5f, Pal.alpha(Pal.PALE, fl))
            }
            "deskpapers" -> {
                r(c, x + 2f, y + 8f, w - 4f, h - 10f, Pal.WOOD)
                r(c, x + 2f, y + 8f, w - 4f, 2f, Pal.alpha(Pal.BONE, 0.2f))
                r(c, x + 8f, y + 12f, 14f, 10f, Pal.PAPER)
                r(c, x + 12f, y + 14f, 14f, 10f, Pal.BONE)
                r(c, x + 14f, y + 17f, 8f, 1f, Pal.INK)
                glint(c, x + w - 8f, y + 10f, t, used)
            }
            "boat" -> {
                r(c, x + 2f, y + 8f, w - 4f, h - 14f, 0xFF3A4A58.toInt())
                r(c, x + 2f, y + 8f, w - 4f, 3f, 0xFF566878.toInt())
                r(c, x + 2f, y + h - 12f, w - 4f, 6f, 0xFF4A3028.toInt())
                r(c, x + 20f, y + 2f, 30f, 14f, Pal.DGREY)
                r(c, x + 26f, y + 5f, 6f, 6f, Pal.alpha(Pal.AMBER, 0.55f + 0.2f * sin(t * 3f)))
                r(c, x + 38f, y + 5f, 6f, 6f, Pal.alpha(Pal.AMBER, 0.55f))
            }
            "net" -> {
                for (i in 0 until (w / 8f).toInt()) {
                    r(c, x + i * 8f, y + 8f, 1f, 18f, 0xFF5A5A48.toInt())
                }
                for (j in 0 until 4) r(c, x, y + 8f + j * 5f, w, 1f, 0xFF5A5A48.toInt())
                r(c, x, y + 6f, w, 2f, Pal.WOOD)
            }
            "jerrycan" -> {
                r(c, x + 8f, y + 8f, 16f, 18f, Pal.RED)
                r(c, x + 8f, y + 8f, 16f, 2f, Pal.alpha(Pal.BONE, 0.3f))
                r(c, x + 11f, y + 4f, 6f, 4f, Pal.RED)
                r(c, x + 12f, y + 14f, 8f, 6f, Pal.alpha(Pal.BLACK, 0.3f))
                glint(c, x + 20f, y + 10f, t, used)
            }
            "toolbox" -> {
                r(c, x + 6f, y + 12f, 20f, 14f, Pal.DGREY)
                r(c, x + 6f, y + 12f, 20f, 2f, Pal.LGREY)
                r(c, x + 12f, y + 8f, 8f, 4f, Pal.GREY)
                glint(c, x + 22f, y + 14f, t, used)
            }
            "crate" -> {
                r(c, x + 2f, y + 4f, 28f, 26f, Pal.WOOD)
                r(c, x + 2f, y + 4f, 28f, 2f, Pal.alpha(Pal.BONE, 0.2f))
                r(c, x + 2f, y + 15f, 28f, 2f, Pal.DWOOD)
                r(c, x + 15f, y + 4f, 2f, 26f, Pal.DWOOD)
            }
            "sign" -> {
                r(c, x + 14f, y + 8f, 3f, 22f, Pal.DWOOD)
                r(c, x + 5f, y + 6f, 22f, 10f, Pal.DAMBER)
                r(c, x + 18f, y + 9f, 6f, 4f, Pal.PALE)
            }
            "drawing" -> {
                r(c, x + 7f, y + 6f, 18f, 20f, Pal.PAPER)
                r(c, x + 11f, y + 10f, 4f, 4f, Pal.INK)
                r(c, x + 12f, y + 14f, 2f, 7f, Pal.INK)
                r(c, x + 18f, y + 8f, 5f, 2f, Pal.AMBER)
                r(c, x + 18f, y + 12f, 5f, 2f, Pal.AMBER)
            }
            "horn" -> {
                r(c, x + 2f, y + 8f, w - 4f, h - 10f, Pal.DGREY)
                for (i in 0 until 3) {
                    r(c, x + 10f + i * 24f, y + 12f, 14f, h - 20f, Pal.DAMBER)
                    r(c, x + 12f + i * 24f, y + 12f, 3f, h - 20f, Pal.AMBER)
                }
            }
            "lens" -> {
                r(c, x + 2f, y + 2f, w - 4f, h - 4f, Pal.DGREY)
                val g = 0.35f + 0.15f * sin(t * 1.5f)
                for (i in 0 until 4) {
                    val m = 6f + i * 9f
                    r(c, x + m, y + m, w - 2f * m, h - 2f * m, Pal.alpha(Pal.PALE, g * (0.3f + i * 0.2f)))
                }
                r(c, x + w / 2f - 3f, y + w / 2f - 3f, 6f, 6f, Pal.PALE)
            }
            "hbed" -> {
                r(c, x + 2f, y + 4f, w - 4f, 24f, Pal.GREY)
                r(c, x + 4f, y + 6f, w - 8f, 20f, Pal.BONE)
                r(c, x + 4f, y + 6f, 10f, 20f, Pal.LGREY)
                r(c, x + 16f, y + 6f, w - 24f, 20f, Pal.alpha(Pal.TEAL, 0.5f))
            }
            "incinerator" -> {
                r(c, x + 2f, y + 4f, w - 4f, h - 6f, Pal.CHARCOAL)
                r(c, x + 2f, y + 4f, w - 4f, 3f, Pal.GREY)
                val g = 0.5f + 0.4f * sin(t * 7f)
                r(c, x + 10f, y + 24f, w - 20f, 10f, Pal.BLACK)
                r(c, x + 12f, y + 28f, w - 24f, 4f, Pal.alpha(Pal.ORANGE, g))
                r(c, x + w - 14f, y - 6f, 6f, 14f, Pal.DGREY)
            }
            "stairdoor" -> {
                r(c, x + 2f, y + 2f, w - 4f, h - 4f, Pal.DGREY)
                r(c, x + 6f, y + 6f, w - 12f, h - 12f, Pal.CHARCOAL)
                r(c, x + w - 10f, y + h / 2f, 3f, 5f, Pal.LGREY)
                val hum = 0.2f + 0.2f * sin(t * 12f)
                r(c, x + 8f, y + 10f, w - 16f, 1f, Pal.alpha(Pal.TEAL, hum + 0.3f))
            }
            "grave" -> {
                r(c, x + 7f, y + 8f, 18f, 22f, Pal.GREY)
                r(c, x + 9f, y + 4f, 14f, 6f, Pal.GREY)
                r(c, x + 7f, y + 8f, 18f, 2f, Pal.LGREY)
                r(c, x + 11f, y + 14f, 10f, 1f, Pal.DGREY)
                r(c, x + 11f, y + 18f, 10f, 1f, Pal.DGREY)
                r(c, x + 9f, y + 26f, 6f, 4f, Pal.DTEAL)
            }
            "lantern" -> {
                r(c, x + 11f, y + 10f, 10f, 14f, Pal.DGREY)
                r(c, x + 13f, y + 12f, 6f, 9f, Pal.alpha(Pal.AMBER, 0.7f + 0.2f * sin(t * 6f)))
                r(c, x + 12f, y + 8f, 8f, 3f, Pal.GREY)
                glint(c, x + 22f, y + 10f, t, used)
            }
            "bench" -> {
                r(c, x + 2f, y + 10f, w - 4f, 8f, Pal.WOOD)
                r(c, x + 2f, y + 18f, 3f, 10f, Pal.DWOOD)
                r(c, x + w - 5f, y + 18f, 3f, 10f, Pal.DWOOD)
                r(c, x + 2f, y + 6f, w - 4f, 3f, Pal.DWOOD)
            }
            "gate" -> {
                r(c, x + 4f, y + 2f, 4f, h - 4f, Pal.GREY)
                r(c, x + 24f, y + 2f, 4f, h - 4f, Pal.GREY)
                for (i in 0 until 4) r(c, x + 9f + i * 4f, y + 6f, 1f, h - 12f, Pal.DGREY)
            }
            "filecabinet" -> {
                r(c, x + 2f, y + 2f, w - 4f, 28f, Pal.DGREY)
                for (i in 0 until (w / 16f).toInt()) {
                    r(c, x + 4f + i * 16f, y + 5f, 12f, 11f, Pal.CHARCOAL)
                    r(c, x + 4f + i * 16f, y + 18f, 12f, 11f, Pal.CHARCOAL)
                    r(c, x + 8f + i * 16f, y + 9f, 4f, 2f, Pal.LGREY)
                }
                glint(c, x + w - 6f, y + 4f, t, used)
            }
            "reel" -> {
                r(c, x + 2f, y + 4f, w - 4f, 26f, Pal.DGREY)
                for (k in 0 until 2) {
                    val cx = x + 22f + k * 40f
                    val cy = y + 16f
                    val a = t * 1.5f + k
                    UI.ring(c, cx, cy, 9f, Pal.LGREY, 2f)
                    UI.line(c, cx, cy, cx + cos(a) * 8f, cy + sin(a) * 8f, Pal.AMBER, 2f)
                    UI.line(c, cx, cy, cx - cos(a) * 8f, cy - sin(a) * 8f, Pal.AMBER, 2f)
                }
                glint(c, x + w - 8f, y + 5f, t, used)
            }
            "console" -> {
                r(c, x + 2f, y + 4f, w - 4f, 26f, Pal.DGREY)
                for (i in 0 until 6) {
                    val on = sin(t * (2f + i) + i) > 0f
                    r(c, x + 6f + i * 9f, y + 8f, 4f, 4f, if (on) Pal.AMBER else Pal.alpha(Pal.AMBER, 0.2f))
                    r(c, x + 6f + i * 9f, y + 18f, 5f, 8f, Pal.CHARCOAL)
                }
            }
            "marker" -> { }
            else -> r(c, x + 4f, y + 4f, w - 8f, h - 8f, Pal.GREY)
        }
    }

    /** Elias, drawn in code. dir: 0 down, 1 up, 2 left, 3 right. frame: 0 idle, 1 and 2 walking. */
    fun player(c: Canvas, px: Float, py: Float, dir: Int, frame: Int, flashlight: Boolean) {
        val ox = px - 10f
        val oy = py + 8f - 30f
        val coat = 0xFF22344A.toInt()
        val coatD = 0xFF16222F.toInt()
        val skin = 0xFFC9A27E.toInt()
        val hair = 0xFF2A2118.toInt()
        val boot = 0xFF14161A.toInt()
        r(c, ox + 2f, oy + 28f, 16f, 3f, Pal.alpha(Pal.BLACK, 0.35f))
        val lOff = if (frame == 1) 2f else 0f
        val rOff = if (frame == 2) 2f else 0f
        r(c, ox + 4f, oy + 22f - lOff, 5f, 7f + lOff, boot)
        r(c, ox + 11f, oy + 22f - rOff, 5f, 7f + rOff, boot)
        r(c, ox + 3f, oy + 11f, 14f, 12f, coat)
        r(c, ox + 3f, oy + 20f, 14f, 3f, coatD)
        if (dir == 2 || dir == 3) {
            r(c, ox + 6f, oy + 12f, 8f, 9f, coatD)
        } else {
            r(c, ox + 1f, oy + 12f, 3f, 9f, coatD)
            r(c, ox + 16f, oy + 12f, 3f, 9f, coatD)
        }
        r(c, ox + 4f, oy + 10f, 12f, 3f, Pal.AMBER)
        r(c, ox + 5f, oy + 1f, 10f, 10f, skin)
        when (dir) {
            0 -> {
                r(c, ox + 4f, oy, 12f, 5f, hair)
                r(c, ox + 7f, oy + 6f, 2f, 2f, boot)
                r(c, ox + 12f, oy + 6f, 2f, 2f, boot)
            }
            1 -> r(c, ox + 4f, oy, 12f, 10f, hair)
            2 -> {
                r(c, ox + 4f, oy, 12f, 5f, hair)
                r(c, ox + 10f, oy + 4f, 6f, 6f, hair)
                r(c, ox + 6f, oy + 6f, 2f, 2f, boot)
            }
            else -> {
                r(c, ox + 4f, oy, 12f, 5f, hair)
                r(c, ox + 4f, oy + 4f, 6f, 6f, hair)
                r(c, ox + 12f, oy + 6f, 2f, 2f, boot)
            }
        }
        if (flashlight) {
            when (dir) {
                2 -> r(c, ox - 2f, oy + 17f, 5f, 3f, Pal.PALE)
                3 -> r(c, ox + 17f, oy + 17f, 5f, 3f, Pal.PALE)
                1 -> r(c, ox + 15f, oy + 6f, 3f, 5f, Pal.PALE)
                else -> r(c, ox + 15f, oy + 18f, 3f, 5f, Pal.PALE)
            }
        }
    }

    fun waterShimmer(c: Canvas, sc: Scene, t: Float, camX: Float, camY: Float, W: Float, H: Float) {
        val tx0 = (camX / 32f).toInt().coerceAtLeast(0)
        val ty0 = (camY / 32f).toInt().coerceAtLeast(0)
        val tx1 = ((camX + W) / 32f).toInt().coerceAtMost(sc.w - 1)
        val ty1 = ((camY + H) / 32f).toInt().coerceAtMost(sc.h - 1)
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            if (sc.tiles[ty][tx] != '~') continue
            for (i in 0 until 3) {
                val h = hash(tx * 3 + i, ty, 8)
                val ph = (t * (0.5f + (h % 5) * 0.12f) + (h % 100) * 0.1f)
                val a = 0.5f + 0.5f * sin(ph)
                if (a > 0.55f) {
                    val xx = tx * 32f + (h % 22) + sin(ph * 0.7f) * 3f
                    val yy = ty * 32f + ((h / 23) % 28)
                    r(c, xx, yy, 6f, 2f, Pal.alpha(0xFF5C86A8.toInt(), (a - 0.5f) * 1.2f))
                }
            }
        }
    }

    fun vegetation(c: Canvas, sc: Scene, t: Float, camX: Float, camY: Float, W: Float, H: Float) {
        val tx0 = (camX / 32f).toInt().coerceAtLeast(0)
        val ty0 = (camY / 32f).toInt().coerceAtLeast(0)
        val tx1 = ((camX + W) / 32f).toInt().coerceAtMost(sc.w - 1)
        val ty1 = ((camY + H) / 32f).toInt().coerceAtMost(sc.h - 1)
        for (ty in ty0..ty1) for (tx in tx0..tx1) {
            val ch = sc.tiles[ty][tx]
            if (ch == ',') {
                for (i in 0 until 2) {
                    val h = hash(tx * 3 + i, ty, 21)
                    val sway = sin(t * 1.8f + (h % 50) * 0.3f) * 2f
                    r(c, tx * 32f + 4f + (h % 22) + sway, ty * 32f + 6f + ((h / 23) % 20), 2f, 6f, 0xFF3A5A46.toInt())
                }
            } else if (ch == 'T') {
                val h = hash(tx, ty, 33)
                val sway = sin(t * 1.2f + (h % 50) * 0.2f) * 2f
                r(c, tx * 32f + 6f + sway, ty * 32f + 5f, 8f, 3f, 0xFF2A4A38.toInt())
                r(c, tx * 32f + 16f - sway, ty * 32f + 14f, 8f, 3f, 0xFF2A4A38.toInt())
            }
        }
    }
}

/** Rain, fog, dust and lightning. */
class WeatherFx {
    private val n = 150
    private val rx = FloatArray(n)
    private val ry = FloatArray(n)
    private val rv = FloatArray(n)
    private val fx = FloatArray(9)
    private val fy = FloatArray(9)
    private val fs = FloatArray(9)
    private val dx = FloatArray(28)
    private val dy = FloatArray(28)
    private var seeded = false
    private val fogShader = RadialGradient(0f, 0f, 100f, intArrayOf(0x66B8C4CC, 0x33B8C4CC, 0x00B8C4CC), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
    private val fogPaint = Paint()
    private var seed = 4242

    init {
        fogPaint.shader = fogShader
        fogPaint.isAntiAlias = true
    }

    private fun rnd(): Float {
        seed = seed * 1103515245 + 12345
        return ((seed ushr 8) and 0xFFFF) / 65535f
    }

    private fun ensure(W: Float, H: Float) {
        if (seeded) return
        for (i in 0 until n) {
            rx[i] = rnd() * (W + 200f)
            ry[i] = rnd() * H
            rv[i] = 380f + rnd() * 260f
        }
        for (i in 0 until 9) {
            fx[i] = rnd() * W
            fy[i] = rnd() * H
            fs[i] = 1.2f + rnd() * 1.6f
        }
        for (i in 0 until 28) {
            dx[i] = rnd() * W
            dy[i] = rnd() * H
        }
        seeded = true
    }

    fun update(dt: Float, w: Weather, W: Float, H: Float) {
        ensure(W, H)
        val drift = 40f + 150f * w.wind
        for (i in 0 until n) {
            ry[i] += rv[i] * dt
            rx[i] -= drift * dt
            if (ry[i] > H) {
                ry[i] = -10f
                rx[i] = rnd() * (W + 200f)
            }
            if (rx[i] < -20f) rx[i] += W + 200f
        }
        for (i in 0 until 9) {
            fx[i] += (10f + 20f * w.wind + i * 2f) * dt
            if (fx[i] > W + 160f) fx[i] = -160f
        }
        for (i in 0 until 28) {
            dx[i] += 4f * dt
            dy[i] -= 3f * dt
            if (dx[i] > W) dx[i] = 0f
            if (dy[i] < 0f) dy[i] = H
        }
    }

    fun drawRain(c: Canvas, w: Weather, W: Float, H: Float, wind: Float) {
        if (w.rain <= 0.02f) return
        val count = (n * w.rain).toInt()
        for (i in 0 until count) {
            val far = i % 2 == 0
            val len = if (far) 5f else 10f
            val a = if (far) 0.18f else 0.34f
            UI.line(c, rx[i], ry[i], rx[i] + len * (0.2f + wind * 0.5f), ry[i] + len, Pal.alpha(0xFFA8B8C8.toInt(), a), if (far) 1f else 1.5f)
        }
    }

    fun drawFog(c: Canvas, w: Weather, W: Float, H: Float, t: Float) {
        val a = (w.fog * 0.9f + 0.05f).coerceIn(0f, 1f)
        if (w.fog < 0.05f) return
        fogPaint.alpha = (a * 255f).toInt()
        for (i in 0 until 9) {
            c.save()
            c.translate(fx[i], fy[i] + sin(t * 0.2f + i) * 12f)
            c.scale(fs[i] * 1.6f, fs[i])
            c.drawCircle(0f, 0f, 100f, fogPaint)
            c.restore()
        }
    }

    fun drawDust(c: Canvas, t: Float) {
        for (i in 0 until 28) {
            val a = 0.15f + 0.2f * abs(sin(t * 0.6f + i))
            Art.r(c, dx[i] + sin(t * 0.4f + i) * 6f, dy[i], 2f, 2f, Pal.alpha(Pal.PALE, a))
        }
    }
}

/** Darkness with punched-out light holes. */
object Light {
    private val holeShader = RadialGradient(0f, 0f, 100f, intArrayOf(0xFFFFFFFF.toInt(), 0xBBFFFFFF.toInt(), 0x00FFFFFF), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    private val glowShader = RadialGradient(0f, 0f, 100f, intArrayOf(0x55E0A040, 0x22E0A040, 0x00E0A040), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
    private val holeP = Paint()
    private val glowP = Paint()
    private val darkP = Paint()

    init {
        holeP.shader = holeShader
        holeP.isAntiAlias = true
        holeP.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        glowP.shader = glowShader
        glowP.isAntiAlias = true
    }

    fun begin(c: Canvas, left: Float, top: Float, right: Float, bottom: Float, darkness: Float, tint: Int): Int {
        val sc = c.saveLayer(left, top, right, bottom, null)
        darkP.color = Pal.alpha(tint, darkness)
        c.drawRect(left, top, right, bottom, darkP)
        return sc
    }

    fun hole(c: Canvas, x: Float, y: Float, r: Float, a: Float) {
        holeP.alpha = (a.coerceIn(0f, 1f) * 255f).toInt()
        c.save()
        c.translate(x, y)
        c.scale(r / 100f, r / 100f)
        c.drawCircle(0f, 0f, 100f, holeP)
        c.restore()
    }

    fun end(c: Canvas, saved: Int) {
        c.restoreToCount(saved)
    }

    fun glow(c: Canvas, x: Float, y: Float, r: Float, a: Float) {
        glowP.alpha = (a.coerceIn(0f, 1f) * 255f).toInt()
        c.save()
        c.translate(x, y)
        c.scale(r / 100f, r / 100f)
        c.drawCircle(0f, 0f, 100f, glowP)
        c.restore()
    }
}
