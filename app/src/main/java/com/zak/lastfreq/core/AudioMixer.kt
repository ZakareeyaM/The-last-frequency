package com.zak.lastfreq.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure-Kotlin procedural audio. Every sound in the game (ocean, rain, wind, generator hum,
 * radio static, murmured voices, Morse and tones, the ambient pad, thunder, clicks, footsteps)
 * is synthesised here, so no audio files are needed.
 */
class AudioMixer(val rate: Int = 22050) {

    @Volatile var master = 0.8f
    @Volatile var musicVol = 0.6f
    @Volatile var sfxVol = 0.8f

    @Volatile var rain = 0f
    @Volatile var wind = 0f
    @Volatile var ocean = 0f
    @Volatile var hum = 0f
    @Volatile var staticLevel = 0f
    @Volatile var musicLevel = 0f

    @Volatile var voiceVol = 0f
    @Volatile var voicePitch = 150f
    @Volatile var voiceRate = 4f
    @Volatile var voiceBright = 0.5f
    @Volatile var voiceRough = 0.2f

    @Volatile var toneVol = 0f
    @Volatile var toneFreq = 600f

    @Volatile private var thunderT = -1f
    @Volatile private var clickAmp = 0f
    @Volatile private var stepAmp = 0f

    private val twoPi = (2.0 * PI).toFloat()
    private var seed = 0x2F6E2B1

    private var sRain = 0f
    private var sWind = 0f
    private var sOcean = 0f
    private var sHum = 0f
    private var sStatic = 0f
    private var sVoice = 0f
    private var sTone = 0f
    private var sMusic = 0f

    private var lpOcean = 0f
    private var lpRain = 0f
    private var lpWind = 0f
    private var lpWind2 = 0f
    private var lpThunder = 0f
    private var lpVoice = 0f
    private var lpHum = 0f

    private var swellPh = 0f
    private var windPh = 0f
    private var whistlePh = 0f
    private var humPh = 0f
    private var sylPh = 0f
    private var voicePh = 0f
    private var vibPh = 0f
    private var tonePh = 0f
    private var drift = 1f
    private var cutoff = 900f
    private var voiced = true
    private var crackle = 0f
    private var chordClock = 0f
    private var stepPh = 0f

    private val chords = arrayOf(
        floatArrayOf(110f, 130.8f, 164.8f),
        floatArrayOf(87.3f, 110f, 130.8f),
        floatArrayOf(73.4f, 87.3f, 110f),
        floatArrayOf(82.4f, 98f, 123.5f)
    )
    private val padF = floatArrayOf(110f, 130.8f, 164.8f)
    private val padPh = FloatArray(6)

    fun thunder() { thunderT = 0f }
    fun click() { clickAmp = 0.9f }
    fun step() { stepAmp = 0.6f }

    private fun noise(): Float {
        seed = seed xor (seed shl 13)
        seed = seed xor (seed ushr 17)
        seed = seed xor (seed shl 5)
        return (seed and 0xFFFF) / 32768f - 1f
    }

    private fun softClip(x: Float): Float = x / (1f + abs(x))

    fun render(out: ShortArray, n: Int) {
        val k = 0.0015f
        val rainT = rain
        val windT = wind
        val oceanT = ocean
        val humT = hum
        val statT = staticLevel
        val voiceT = voiceVol
        val toneT = toneVol
        val musT = musicLevel * musicVol
        val sfx = sfxVol
        val mast = master
        val vPitch = voicePitch
        val vRate = voiceRate
        val vBright = voiceBright
        val vRough = voiceRough
        val tFreq = toneFreq
        val dt = 1f / rate

        for (i in 0 until n) {
            sRain += (rainT - sRain) * k
            sWind += (windT - sWind) * k
            sOcean += (oceanT - sOcean) * k
            sHum += (humT - sHum) * k
            sStatic += (statT - sStatic) * k
            sVoice += (voiceT - sVoice) * 0.004f
            sTone += (toneT - sTone) * 0.02f
            sMusic += (musT - sMusic) * 0.0005f

            // ---- ocean
            swellPh += dt / 7f
            if (swellPh > 1f) swellPh -= 1f
            val swell = 0.55f + 0.45f * sin(twoPi * swellPh)
            lpOcean += 0.03f * (noise() - lpOcean)
            val oceanS = lpOcean * 3.2f * swell * sOcean

            // ---- rain (high-passed noise)
            val nz = noise()
            lpRain += 0.35f * (nz - lpRain)
            val rainS = (nz - lpRain) * sRain

            // ---- wind
            windPh += dt / 3.3f
            if (windPh > 1f) windPh -= 1f
            val gust = 0.5f + 0.5f * sin(twoPi * windPh)
            lpWind += 0.02f * (noise() - lpWind)
            lpWind2 += 0.2f * (lpWind - lpWind2)
            whistlePh += (520f + 160f * gust) * dt
            if (whistlePh > 1f) whistlePh -= 1f
            val windS = (lpWind2 * 4f * gust + sin(twoPi * whistlePh) * 0.03f * gust * gust) * sWind

            // ---- generator hum
            humPh += 50f * dt
            if (humPh > 1f) humPh -= 1f
            lpHum += 0.01f * (noise() - lpHum)
            val humS = (sin(twoPi * humPh) * 0.6f + sin(twoPi * humPh * 2f) * 0.3f +
                sin(twoPi * humPh * 3f) * 0.15f + lpHum * 2f) * sHum

            // ---- radio static + crackle
            var stat = noise() * sStatic * 0.55f
            if (sStatic > 0.05f && noise() > 0.9992f) crackle = 0.9f * noise()
            stat += crackle * sStatic
            crackle *= 0.85f

            // ---- murmured voice
            var voice = 0f
            if (sVoice > 0.002f) {
                sylPh += vRate * dt
                if (sylPh >= 1f) {
                    sylPh -= 1f
                    drift = 1f + noise() * 0.12f
                    cutoff = 450f + (noise() + 1f) * 550f * (0.5f + vBright)
                    voiced = (noise() + 1f) > 0.25f
                }
                vibPh += 5.2f * dt
                if (vibPh > 1f) vibPh -= 1f
                val f0 = vPitch * drift * (1f + 0.02f * sin(twoPi * vibPh))
                voicePh += f0 * dt
                if (voicePh > 1f) voicePh -= 1f
                var src = 0f
                for (h in 1..5) src += sin(twoPi * voicePh * h) / h
                src = src * 0.5f + noise() * vRough * 0.7f
                val a = 1f - exp(-twoPi * cutoff * dt)
                lpVoice += a * (src - lpVoice)
                var env = sin(PI.toFloat() * sylPh)
                env *= env
                if (!voiced) env *= 0.12f
                voice = lpVoice * env * sVoice
            }

            // ---- tones / Morse key
            var tone = 0f
            if (sTone > 0.002f) {
                tonePh += tFreq * dt
                if (tonePh > 1f) tonePh -= 1f
                tone = sin(twoPi * tonePh) * sTone
            }

            // ---- thunder
            var thunder = 0f
            if (thunderT >= 0f) {
                lpThunder += 0.015f * (noise() - lpThunder)
                val env = exp(-thunderT / 1.4f) * (1f - exp(-thunderT * 8f))
                thunder = lpThunder * env * 5.5f
                thunderT += dt
                if (thunderT > 6f) thunderT = -1f
            }

            // ---- clicks and footsteps
            var fx = 0f
            if (clickAmp > 0.001f) {
                fx += noise() * clickAmp * 0.6f
                clickAmp *= 0.975f
            }
            if (stepAmp > 0.001f) {
                stepPh += 85f * dt
                if (stepPh > 1f) stepPh -= 1f
                fx += sin(twoPi * stepPh) * stepAmp * 0.5f
                stepAmp *= 0.9985f
            }

            // ---- ambient pad
            chordClock += dt
            val chord = chords[((chordClock / 14f).toInt()) % chords.size]
            var pad = 0f
            if (sMusic > 0.0005f) {
                for (v in 0 until 3) {
                    padF[v] += (chord[v] - padF[v]) * 0.00006f
                    padPh[v] += padF[v] * dt
                    if (padPh[v] > 1f) padPh[v] -= 1f
                    padPh[v + 3] += padF[v] * 2.003f * dt
                    if (padPh[v + 3] > 1f) padPh[v + 3] -= 1f
                    pad += sin(twoPi * padPh[v]) + 0.35f * sin(twoPi * padPh[v + 3])
                }
            }

            val amb = oceanS * 0.45f + rainS * 0.33f + windS * 0.45f + humS * 0.20f
            val radio = stat + voice * 0.75f + tone * 0.30f
            val mix = (amb + radio + thunder + fx) * sfx + pad * 0.07f * sMusic
            out[i] = (softClip(mix * mast * 1.4f) * 32000f).toInt().toShort()
        }
    }
}
