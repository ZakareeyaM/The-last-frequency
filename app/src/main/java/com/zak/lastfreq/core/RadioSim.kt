package com.zak.lastfreq.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

object Morse {
    private val table = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.",
        'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---", 'K' to "-.-", 'L' to ".-..",
        'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-",
        'Y' to "-.--", 'Z' to "--.."
    )

    const val UNIT = 0.15f

    /** on: key down. sym: '.', '-' for key-down, ' ' letter gap, '/' word gap, 0 for silent gap inside a letter. */
    class Seg(val on: Boolean, val dur: Float, val sym: Char)

    fun timeline(word: String): List<Seg> {
        val out = ArrayList<Seg>()
        for (ch in word.uppercase()) {
            if (ch == ' ') {
                out.add(Seg(false, UNIT * 4f, '/'))
                continue
            }
            val code = table[ch] ?: continue
            for (s in code) {
                out.add(Seg(true, if (s == '.') UNIT else UNIT * 3f, s))
                out.add(Seg(false, UNIT, '\u0000'))
            }
            out.add(Seg(false, UNIT * 2f, ' '))
        }
        return out
    }

    fun duration(word: String): Float {
        var d = 0f
        for (s in timeline(word)) d += s.dur
        return d
    }

    /** Symbols revealed by time t (seconds from start of this repeat). */
    fun revealed(word: String, t: Float): String {
        val sb = StringBuilder()
        var acc = 0f
        for (s in timeline(word)) {
            if (acc > t) break
            if (s.sym != '\u0000') sb.append(s.sym)
            acc += s.dur
        }
        return sb.toString()
    }

    fun keyDown(word: String, t: Float): Boolean {
        var acc = 0f
        for (s in timeline(word)) {
            if (t < acc + s.dur) return s.on
            acc += s.dur
        }
        return false
    }
}

object RadioSim {
    const val MIN_FREQ = 3.0
    const val MAX_FREQ = 9.0
    const val LOCK = 0.80f
    const val LISTEN_MIN = 0.60f
    const val GAP = 3f

    val TONE_HZ = floatArrayOf(262f, 330f, 392f, 494f)
    const val TONE_LEN = 0.7f
    const val TONE_STEP = 1.0f

    fun lineDur(s: String): Float = max(2.6f, s.length * 0.062f)

    fun voiceDuration(lines: List<String>): Float {
        var d = 0f
        for (l in lines) d += lineDur(l)
        return d
    }

    fun payloadDuration(t: Transmission, flags: Set<String>): Float = when (t.kind) {
        Kind.VOICE, Kind.HIDDEN -> voiceDuration(t.linesFor(flags))
        Kind.MORSE -> Morse.duration(t.options[t.answer])
        Kind.TONES -> t.code.size * TONE_STEP
    }

    fun cycleDuration(t: Transmission, flags: Set<String>): Float = payloadDuration(t, flags) + GAP

    /** Seconds of clear listening needed before a transmission counts as heard. */
    fun listenNeeded(t: Transmission, flags: Set<String>): Float = payloadDuration(t, flags) * 0.9f

    fun angDiff(a: Float, b: Float): Float {
        var d = abs(a - b) % 360f
        if (d > 180f) d = 360f - d
        return d
    }

    fun windJitter(weather: Weather, time: Float): Float =
        sin(time * 1.7f) * weather.wind * 10f + sin(time * 0.6f + 1.3f) * weather.wind * 8f

    private fun hash(x: Int): Int {
        var h = x * -1640531535
        h = h xor (h ushr 15)
        h *= 0x2c1b3c6d
        h = h xor (h ushr 12)
        return h and 0x7fffffff
    }

    fun spike(weather: Weather, time: Float): Float {
        if (!weather.storm) return 0f
        val k = (time / 3f).toInt()
        val frac = time / 3f - k
        return if (hash(k * 7 + 3) % 4 == 0 && frac < 0.35f) 0.30f else 0f
    }

    fun clarity(
        t: Transmission, freq: Double, gain: Float, bearing: Float, filter: Boolean,
        weather: Weather, condition: Float, time: Float
    ): Float {
        val d = abs(freq - t.freq).toFloat()
        val inTune = (1f - d / 0.05f).coerceIn(0f, 1f)
        val f = inTune.pow(1.4f)
        if (f <= 0f) return 0f
        val eff = bearing + windJitter(weather, time)
        val a = (1f - angDiff(eff, t.bearing.toFloat()) / 75f).coerceIn(0f, 1f)
        val g = if (gain < t.minGain) (gain / t.minGain).coerceIn(0f, 1f)
        else 1f - ((gain - 0.85f) / 0.15f).coerceIn(0f, 1f) * 0.4f
        val base = f * (0.30f + 0.70f * a) * g
        var interf = weather.interference + spike(weather, time) + (1f - condition / 100f) * 0.35f
        if (filter) interf *= 0.5f
        var q = base * (1f - interf * 1.6f)
        if (filter && t.kind != Kind.HIDDEN) q *= 0.92f
        if (t.kind == Kind.HIDDEN && !filter) q *= 0.30f
        if (condition < 10f) q = minOf(q, 0.5f)
        return q.coerceIn(0f, 1f)
    }

    /** True when a hidden transmission would be clear if the filter were on. */
    fun hiddenNeedsFilter(
        t: Transmission, freq: Double, gain: Float, bearing: Float,
        weather: Weather, condition: Float, time: Float
    ): Boolean {
        if (t.kind != Kind.HIDDEN) return false
        val on = clarity(t, freq, gain, bearing, true, weather, condition, time)
        val off = clarity(t, freq, gain, bearing, false, weather, condition, time)
        return on > 0.5f && off < 0.4f
    }

    fun staticLevel(bestClarity: Float, weather: Weather, condition: Float, squelch: Float): Float {
        var s = 0.28f + weather.interference * 1.5f + (1f - condition / 100f) * 0.4f - bestClarity * 0.85f
        s = s.coerceIn(0.04f, 1f)
        return s * (1f - squelch * 0.8f)
    }

    fun audible(clarity: Float, squelch: Float): Boolean = clarity > 0.06f && clarity > squelch * 0.5f

    private const val GLYPHS = "#%&*+=?/<>"

    fun garble(text: String, clarity: Float, seed: Int, salt: Int): String {
        val reveal = ((clarity - 0.25f) / 0.6f).coerceIn(0f, 1f)
        val sb = StringBuilder()
        for (i in text.indices) {
            val ch = text[i]
            if (!ch.isLetterOrDigit()) {
                sb.append(ch)
            } else {
                val r = (hash(i * 131 + salt * 7919) and 1023) / 1023f
                if (r < reveal) sb.append(ch)
                else sb.append(GLYPHS[(hash(i * 17 + seed * 31 + salt) shr 4) % GLYPHS.length])
            }
        }
        return sb.toString()
    }

    /** Which line is playing at playhead p. Returns (index, progressInLine) or (-1, 0) in the gap. */
    fun lineAt(lines: List<String>, p: Float): Pair<Int, Float> {
        var acc = 0f
        for (i in lines.indices) {
            val d = lineDur(lines[i])
            if (p < acc + d) return Pair(i, (p - acc) / d)
            acc += d
        }
        return Pair(-1, 0f)
    }

    fun toneAt(code: List<Int>, p: Float): Int {
        val i = (p / TONE_STEP).toInt()
        if (i < 0 || i >= code.size) return -1
        val within = p - i * TONE_STEP
        return if (within < TONE_LEN) code[i] else -1
    }
}
