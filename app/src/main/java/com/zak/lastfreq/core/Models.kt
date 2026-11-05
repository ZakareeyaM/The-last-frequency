package com.zak.lastfreq.core

enum class Weather(
    val label: String,
    val rain: Float,
    val fog: Float,
    val wind: Float,
    val storm: Boolean,
    val interference: Float,
    val desc: String
) {
    CLEAR("Clear night", 0f, 0f, 0.1f, false, 0.00f, "Clear sky. Reception is good."),
    LIGHT_RAIN("Light rain", 0.35f, 0.1f, 0.2f, false, 0.05f, "Light rain. A little static on every band."),
    HEAVY_RAIN("Heavy rain", 0.9f, 0.2f, 0.4f, false, 0.12f, "Heavy rain. Slow going outside, steady static inside."),
    FOG("Dense fog", 0.05f, 0.9f, 0.1f, false, 0.04f, "Dense fog. Visibility outside is very poor."),
    STORM("Thunderstorm", 1f, 0.3f, 0.7f, true, 0.18f, "Thunderstorm. Expect interference spikes and drifting bearings."),
    WIND("Strong winds", 0.1f, 0f, 1f, false, 0.05f, "Strong coastal wind. The antenna bearing will drift.")
}

enum class Kind { VOICE, MORSE, TONES, HIDDEN }

enum class ClueType { RECORDING, DOCUMENT, PHOTO, CLIPPING, NOTE, COORD }

class Reply(
    val text: String,
    val response: String,
    val trust: Map<String, Int> = emptyMap(),
    val flags: List<String> = emptyList(),
    val decision: String = ""
)

class Character(
    val id: String,
    val name: String,
    val role: String,
    val bio: String,
    val pitch: Float,
    val rate: Float,
    val bright: Float,
    val rough: Float,
    val hidden: Boolean = false
)

class Clue(
    val id: String,
    val type: ClueType,
    val title: String,
    val text: String,
    val altFlag: String? = null,
    val altText: String? = null
)

class Link(val a: String, val b: String, val flag: String, val message: String)

class Transmission(
    val id: String,
    val night: Int,
    val freq: Double,
    val bearing: Int,
    gainReq: Double,
    val kind: Kind,
    val speaker: String,
    val title: String,
    val start: Int,
    val end: Int,
    val lines: List<String>,
    val clues: List<String> = emptyList(),
    val recClue: String? = null,
    val requiresFlag: String? = null,
    val options: List<String> = emptyList(),
    val answer: Int = 0,
    val code: List<Int> = emptyList(),
    val replies: List<Reply> = emptyList(),
    val setFlags: List<String> = emptyList(),
    val altFlag: String? = null,
    val altLines: List<String> = emptyList(),
    val flavor: Boolean = false
) {
    val minGain: Float = gainReq.toFloat()

    fun linesFor(flags: Set<String>): List<String> =
        if (altFlag != null && flags.contains(altFlag) && altLines.isNotEmpty()) altLines else lines
}

class Interact(
    val id: String,
    val prompt: String,
    val pages: List<String> = emptyList(),
    val clue: String? = null,
    val items: Map<String, Int> = emptyMap(),
    val flag: String? = null,
    val usedPages: List<String>? = null,
    val altFlag: String? = null,
    val altPages: List<String>? = null,
    val special: String? = null,
    val choices: List<Reply> = emptyList(),
    val needClue: String? = null,
    val lockedPages: List<String>? = null
)

class Obj(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val w: Float = 1f,
    val h: Float = 1f,
    val solid: Boolean = true,
    val interact: String? = null,
    val light: Float = 0f
)

class Amb(val ocean: Float, val wind: Float, val hum: Float, val music: Float)

class Scene(
    val id: String,
    val name: String,
    val theme: String,
    val dark: Float,
    val indoors: Boolean,
    val tiles: Array<CharArray>,
    val objects: List<Obj>,
    val spawnX: Float,
    val spawnY: Float,
    val amb: Amb
) {
    val w: Int get() = tiles[0].size
    val h: Int get() = tiles.size
}

class LocationInfo(
    val id: String,
    val name: String,
    val sceneId: String,
    val flag: String?,
    val mapX: Float,
    val mapY: Float,
    val blurb: String
)

class Recording(
    val id: String,
    val tid: String,
    val night: Int,
    val minute: Int,
    val freq: Double,
    val quality: Float,
    var tag: Int
)

class LinkResult(val kind: Int, val message: String)

class Settings {
    var master = 0.8f
    var music = 0.6f
    var sfx = 0.8f
    var brightness = 0.5f
    var textSpeed = 1
    var textScale = 1
    var subtitles = true
    var haptics = true
    var reduceShake = false
    var leftHanded = false

    fun serialize(): String {
        val sb = StringBuilder()
        sb.append("master=").append(master).append('\n')
        sb.append("music=").append(music).append('\n')
        sb.append("sfx=").append(sfx).append('\n')
        sb.append("brightness=").append(brightness).append('\n')
        sb.append("textSpeed=").append(textSpeed).append('\n')
        sb.append("textScale=").append(textScale).append('\n')
        sb.append("subtitles=").append(subtitles).append('\n')
        sb.append("haptics=").append(haptics).append('\n')
        sb.append("reduceShake=").append(reduceShake).append('\n')
        sb.append("leftHanded=").append(leftHanded).append('\n')
        return sb.toString()
    }

    companion object {
        fun parse(text: String?): Settings {
            val s = Settings()
            if (text == null) return s
            for (line in text.split('\n')) {
                val i = line.indexOf('=')
                if (i <= 0) continue
                val k = line.substring(0, i)
                val v = line.substring(i + 1).trim()
                try {
                    when (k) {
                        "master" -> s.master = v.toFloat().coerceIn(0f, 1f)
                        "music" -> s.music = v.toFloat().coerceIn(0f, 1f)
                        "sfx" -> s.sfx = v.toFloat().coerceIn(0f, 1f)
                        "brightness" -> s.brightness = v.toFloat().coerceIn(0f, 1f)
                        "textSpeed" -> s.textSpeed = v.toInt().coerceIn(0, 3)
                        "textScale" -> s.textScale = v.toInt().coerceIn(0, 2)
                        "subtitles" -> s.subtitles = v == "true"
                        "haptics" -> s.haptics = v == "true"
                        "reduceShake" -> s.reduceShake = v == "true"
                        "leftHanded" -> s.leftHanded = v == "true"
                    }
                } catch (e: Exception) {
                    // ignore a bad value and keep the default
                }
            }
            return s
        }
    }
}
