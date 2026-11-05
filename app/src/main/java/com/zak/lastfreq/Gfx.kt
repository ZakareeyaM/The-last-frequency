package com.zak.lastfreq

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface

/** The game's restrained palette: midnight navy, charcoal, desaturated blue, muted teal, with warm amber accents. */
object Pal {
    val NAVY = 0xFF0B1020.toInt()
    val NIGHT = 0xFF121A2B.toInt()
    val CHARCOAL = 0xFF1A1D24.toInt()
    val DGREY = 0xFF2B2F36.toInt()
    val GREY = 0xFF4A505A.toInt()
    val LGREY = 0xFF8A909A.toInt()
    val BLUE = 0xFF3C5A78.toInt()
    val TEAL = 0xFF3F7F82.toInt()
    val DTEAL = 0xFF234A4E.toInt()
    val AMBER = 0xFFE0A040.toInt()
    val DAMBER = 0xFF8A5A1E.toInt()
    val ORANGE = 0xFFC8703A.toInt()
    val RED = 0xFFA8403C.toInt()
    val PALE = 0xFFE8D58A.toInt()
    val PAPER = 0xFFCFC3A1.toInt()
    val INK = 0xFF2A2218.toInt()
    val BONE = 0xFFE6E2D6.toInt()
    val WOOD = 0xFF4A3A2A.toInt()
    val DWOOD = 0xFF32271C.toInt()
    val BLACK = 0xFF000000.toInt()

    fun alpha(c: Int, a: Float): Int {
        val aa = (a.coerceIn(0f, 1f) * 255f).toInt()
        return (aa shl 24) or (c and 0x00FFFFFF)
    }

    fun mix(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        val r = ((a shr 16 and 255) * (1f - k) + (b shr 16 and 255) * k).toInt()
        val g = ((a shr 8 and 255) * (1f - k) + (b shr 8 and 255) * k).toInt()
        val bl = ((a and 255) * (1f - k) + (b and 255) * k).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}

class Btn(var id: String, var label: String, var x: Float, var y: Float, var w: Float, var h: Float) {
    var enabled = true
    var down = false
    fun hit(px: Float, py: Float): Boolean = px >= x && px <= x + w && py >= y && py <= y + h
}

object UI {
    private val fillP = Paint()
    private val strokeP = Paint()
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    val mono: Typeface = Typeface.MONOSPACE
    val monoBold: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    val serif: Typeface = Typeface.SERIF
    val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)

    /** Body text multiplier from the Text Size setting. */
    var textScale = 1f

    init {
        strokeP.style = Paint.Style.STROKE
        strokeP.isAntiAlias = true
        strokeP.strokeCap = Paint.Cap.ROUND
        fillP.isAntiAlias = false
    }

    fun fill(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int) {
        fillP.style = Paint.Style.FILL
        fillP.color = color
        c.drawRect(x, y, x + w, y + h, fillP)
    }

    fun stroke(c: Canvas, x: Float, y: Float, w: Float, h: Float, color: Int, sw: Float = 1f) {
        strokeP.color = color
        strokeP.strokeWidth = sw
        c.drawRect(x, y, x + w, y + h, strokeP)
    }

    fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, sw: Float = 1f) {
        strokeP.color = color
        strokeP.strokeWidth = sw
        c.drawLine(x1, y1, x2, y2, strokeP)
    }

    fun circle(c: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        fillP.style = Paint.Style.FILL
        fillP.isAntiAlias = true
        fillP.color = color
        c.drawCircle(cx, cy, r, fillP)
        fillP.isAntiAlias = false
    }

    fun ring(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, sw: Float = 1f) {
        strokeP.color = color
        strokeP.strokeWidth = sw
        c.drawCircle(cx, cy, r, strokeP)
    }

    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        align: Paint.Align = Paint.Align.LEFT, bold: Boolean = false, face: Typeface? = null
    ) {
        textP.textSize = size
        textP.color = color
        textP.textAlign = align
        textP.typeface = face ?: (if (bold) monoBold else mono)
        c.drawText(s, x, y, textP)
    }

    fun measure(s: String, size: Float, bold: Boolean = false, face: Typeface? = null): Float {
        textP.textSize = size
        textP.typeface = face ?: (if (bold) monoBold else mono)
        return textP.measureText(s)
    }

    fun wrap(s: String, maxW: Float, size: Float, bold: Boolean = false, face: Typeface? = null): List<String> {
        val out = ArrayList<String>()
        for (para in s.split('\n')) {
            val words = para.split(' ')
            var line = StringBuilder()
            for (w in words) {
                val trial = if (line.isEmpty()) w else line.toString() + " " + w
                if (measure(trial, size, bold, face) <= maxW || line.isEmpty()) {
                    line = StringBuilder(trial)
                } else {
                    out.add(line.toString())
                    line = StringBuilder(w)
                }
            }
            out.add(line.toString())
        }
        return out
    }

    fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float, fillC: Int = Pal.CHARCOAL, border: Int = Pal.GREY) {
        fill(c, x, y, w, h, fillC)
        stroke(c, x + 0.5f, y + 0.5f, w - 1f, h - 1f, border, 1f)
        fill(c, x + 2f, y + 2f, w - 4f, 1f, Pal.alpha(Pal.BONE, 0.07f))
    }

    fun button(c: Canvas, b: Btn, accent: Int = Pal.AMBER, size: Float = 13f) {
        val base = if (!b.enabled) Pal.alpha(Pal.DGREY, 0.7f) else if (b.down) Pal.GREY else Pal.DGREY
        fill(c, b.x, b.y, b.w, b.h, base)
        stroke(c, b.x + 0.5f, b.y + 0.5f, b.w - 1f, b.h - 1f, if (b.enabled) Pal.alpha(accent, 0.8f) else Pal.GREY, 1f)
        if (!b.down) fill(c, b.x + 2f, b.y + 2f, b.w - 4f, 1f, Pal.alpha(Pal.BONE, 0.10f))
        val col = if (b.enabled) (if (b.down) Pal.PALE else accent) else Pal.LGREY
        val lines = wrap(b.label, b.w - 8f, size, true)
        val total = lines.size * (size + 2f)
        var yy = b.y + (b.h - total) / 2f + size - 1f + (if (b.down) 1f else 0f)
        for (l in lines) {
            text(c, l, b.x + b.w / 2f, yy, size, col, Paint.Align.CENTER, true)
            yy += size + 2f
        }
    }

    fun bar(c: Canvas, x: Float, y: Float, w: Float, h: Float, frac: Float, color: Int, bg: Int = Pal.DGREY) {
        fill(c, x, y, w, h, bg)
        fill(c, x, y, w * frac.coerceIn(0f, 1f), h, color)
        stroke(c, x, y, w, h, Pal.GREY, 1f)
    }

    fun led(c: Canvas, cx: Float, cy: Float, r: Float, on: Boolean, color: Int) {
        circle(c, cx, cy, r + 1.5f, Pal.BLACK)
        circle(c, cx, cy, r, if (on) color else Pal.alpha(color, 0.18f))
        if (on) circle(c, cx - r * 0.3f, cy - r * 0.3f, r * 0.35f, Pal.alpha(Pal.BONE, 0.7f))
    }

    /** Rotary knob. angle in degrees, 0 = straight up, clockwise positive. */
    fun knob(c: Canvas, cx: Float, cy: Float, r: Float, angle: Float, body: Int = Pal.DGREY) {
        circle(c, cx, cy, r + 3f, Pal.BLACK)
        circle(c, cx, cy, r, body)
        ring(c, cx, cy, r - 3f, Pal.alpha(Pal.LGREY, 0.5f), 1f)
        val rad = Math.toRadians((angle - 90f).toDouble())
        val ex = cx + Math.cos(rad).toFloat() * (r - 3f)
        val ey = cy + Math.sin(rad).toFloat() * (r - 3f)
        val sx = cx + Math.cos(rad).toFloat() * (r * 0.35f)
        val sy = cy + Math.sin(rad).toFloat() * (r * 0.35f)
        line(c, sx, sy, ex, ey, Pal.AMBER, 2.5f)
        circle(c, cx, cy, r * 0.18f, Pal.GREY)
    }

    /** Colour-independent tone symbols: 0 triangle, 1 circle, 2 square, 3 diamond. */
    fun symbol(c: Canvas, i: Int, cx: Float, cy: Float, s: Float, color: Int) {
        fillP.style = Paint.Style.FILL
        fillP.isAntiAlias = true
        fillP.color = color
        when (i) {
            0 -> {
                path.reset()
                path.moveTo(cx, cy - s)
                path.lineTo(cx + s, cy + s * 0.8f)
                path.lineTo(cx - s, cy + s * 0.8f)
                path.close()
                c.drawPath(path, fillP)
            }
            1 -> c.drawCircle(cx, cy, s * 0.95f, fillP)
            2 -> c.drawRect(cx - s * 0.85f, cy - s * 0.85f, cx + s * 0.85f, cy + s * 0.85f, fillP)
            else -> {
                path.reset()
                path.moveTo(cx, cy - s)
                path.lineTo(cx + s, cy)
                path.lineTo(cx, cy + s)
                path.lineTo(cx - s, cy)
                path.close()
                c.drawPath(path, fillP)
            }
        }
        fillP.isAntiAlias = false
    }

    fun poly(c: Canvas, pts: FloatArray, color: Int, fillIt: Boolean, sw: Float = 1f) {
        path.reset()
        path.moveTo(pts[0], pts[1])
        var i = 2
        while (i + 1 < pts.size) {
            path.lineTo(pts[i], pts[i + 1])
            i += 2
        }
        path.close()
        if (fillIt) {
            fillP.style = Paint.Style.FILL
            fillP.isAntiAlias = true
            fillP.color = color
            c.drawPath(path, fillP)
            fillP.isAntiAlias = false
        } else {
            strokeP.color = color
            strokeP.strokeWidth = sw
            c.drawPath(path, strokeP)
        }
    }
}
