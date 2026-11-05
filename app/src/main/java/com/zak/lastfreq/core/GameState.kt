package com.zak.lastfreq.core

class GameState {
    var night = 1
    var clock = 0f
    var weather = Weather.CLEAR
    var scene = "station"
    var px = 0f
    var py = 0f
    var facing = 0

    var battery = 100f
    var fuel = 100f
    var condition = 100f
    var stamina = 100f
    var flashlight = false

    val items = LinkedHashMap<String, Int>()
    val flags = LinkedHashSet<String>()
    val trust = LinkedHashMap<String, Int>()
    val clues = LinkedHashSet<String>()
    val used = LinkedHashSet<String>()
    val decoded = LinkedHashSet<String>()
    val replied = LinkedHashSet<String>()
    val heard = LinkedHashSet<String>()
    val found = LinkedHashSet<String>()
    val listen = LinkedHashMap<String, Float>()
    val recordings = ArrayList<Recording>()
    val cards = LinkedHashMap<String, FloatArray>()
    val strings = ArrayList<Pair<String, String>>()
    val decisions = ArrayList<String>()

    val recvNight = LinkedHashSet<String>()
    val decodedNight = LinkedHashSet<String>()
    val visitedNight = LinkedHashSet<String>()
    val linksNight = ArrayList<String>()
    val decisionsNight = ArrayList<String>()

    var tune = 5.0
    var gain = 0.5f
    var squelch = 0.15f
    var bearing = 0f
    var filter = false

    var ending = ""
    var playSeconds = 0f
    var recSeq = 0

    val minute: Int get() = clock.toInt().coerceIn(0, Content.NIGHT_MINUTES)

    // ------------------------------------------------------------------ basics

    fun has(flag: String) = flags.contains(flag)
    fun owns(clue: String) = clues.contains(clue)
    fun trustOf(id: String) = trust[id] ?: 0
    fun itemCount(id: String) = items[id] ?: 0

    fun addItem(id: String, n: Int) {
        items[id] = itemCount(id) + n
    }

    fun takeItem(id: String): Boolean {
        val c = itemCount(id)
        if (c <= 0) return false
        items[id] = c - 1
        return true
    }

    fun setFlag(f: String) {
        flags.add(f)
        if (f == "destroyed_records") removeClue("doc_ward_records")
    }

    fun addClue(id: String): Boolean {
        if (!Content.clues.containsKey(id) || clues.contains(id)) return false
        if (id == "doc_ward_records" && has("destroyed_records")) return false
        clues.add(id)
        val idx = cards.size
        cards[id] = floatArrayOf(20f + (idx % 8) * 96f, 62f + (idx / 8) * 78f)
        return true
    }

    private fun removeClue(id: String) {
        clues.remove(id)
        cards.remove(id)
        val it = strings.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (p.first == id || p.second == id) it.remove()
        }
    }

    fun addTrust(id: String, d: Int) {
        trust[id] = (trustOf(id) + d).coerceIn(-3, 5)
    }

    fun applyReply(r: Reply) {
        for ((k, v) in r.trust) addTrust(k, v)
        for (f in r.flags) setFlag(f)
        if (r.decision.isNotEmpty()) {
            decisions.add(r.decision)
            decisionsNight.add(r.decision)
        }
    }

    // ------------------------------------------------------------------ radio

    fun active(): List<Transmission> = Content.transmissions.filter {
        it.night == night && minute >= it.start && minute < it.end &&
            (it.requiresFlag == null || has(it.requiresFlag))
    }

    /** Returns messages describing what was just logged. */
    fun decode(t: Transmission): List<String> {
        val msgs = ArrayList<String>()
        if (decoded.contains(t.id)) return msgs
        decoded.add(t.id)
        decodedNight.add(t.id)
        msgs.add("Logged: " + t.title)
        for (c in t.clues) if (addClue(c)) msgs.add("New clue: " + (Content.clues[c]?.title ?: c))
        for (f in t.setFlags) setFlag(f)
        if (t.id == "t3_horn") msgs.add("Lighthouse horn tones solved. The lighthouse is on the map.")
        return msgs
    }

    fun record(t: Transmission, quality: Float): Recording? {
        if (quality < 0.55f) return null
        val existing = recordings.firstOrNull { it.tid == t.id }
        if (existing != null && existing.quality >= quality) return existing
        if (existing != null) recordings.remove(existing)
        recSeq++
        val r = Recording("r" + recSeq, t.id, night, minute, t.freq, quality, 0)
        recordings.add(r)
        if (t.recClue != null) addClue(t.recClue)
        return r
    }

    fun connect(a: String, b: String): LinkResult {
        val existing = strings.firstOrNull { (it.first == a && it.second == b) || (it.first == b && it.second == a) }
        if (existing != null) {
            strings.remove(existing)
            return LinkResult(1, "")
        }
        strings.add(Pair(a, b))
        val rule = Content.links.firstOrNull { (it.a == a && it.b == b) || (it.a == b && it.b == a) }
        if (rule != null && !has(rule.flag)) {
            setFlag(rule.flag)
            linksNight.add(rule.message)
            return LinkResult(2, rule.message)
        }
        return LinkResult(0, "")
    }

    // ------------------------------------------------------------------ time and resources

    /** Advances game time. Returns true if the night has ended. */
    fun advance(dt: Float): Boolean {
        playSeconds += dt
        clock += dt * MIN_PER_SEC
        if (fuel > 0f) fuel = (fuel - 0.14f * dt * MIN_PER_SEC).coerceAtLeast(0f)
        if (flashlight) {
            battery -= 0.45f * dt
            if (battery <= 0f) {
                if (takeItem("battery")) battery = 100f
                else {
                    battery = 0f
                    flashlight = false
                }
            }
        }
        return clock >= Content.NIGHT_MINUTES
    }

    fun clockText(): String {
        val total = 23 * 60 + minute
        val h = (total / 60) % 24
        val m = total % 60
        return (if (h < 10) "0" else "") + h + ":" + (if (m < 10) "0" else "") + m
    }

    fun powered(): Boolean = fuel > 0f

    fun sceneName(): String = Content.scenes[scene]?.name ?: scene

    fun locationUnlocked(l: LocationInfo): Boolean = l.flag == null || has(l.flag)

    // ------------------------------------------------------------------ nights

    fun startNight(n: Int) {
        night = n
        clock = 0f
        weather = Content.nightWeather[n.coerceIn(1, Content.LAST_NIGHT)]
        scene = "station"
        val s = Content.scenes["station"]!!
        px = s.spawnX * Content.TILE
        py = s.spawnY * Content.TILE
        facing = 0
        flashlight = false
        stamina = 100f
        recvNight.clear()
        decodedNight.clear()
        visitedNight.clear()
        linksNight.clear()
        decisionsNight.clear()
        if (n == 1) {
            items.clear()
            addItem("battery", 1)
            addItem("tube", 1)
            battery = 100f
            fuel = 100f
            condition = 100f
            bearing = 0f
            tune = 5.0
            // the pinned station notes: these signals are known from the start
            found.add("t1_marta")
            found.add("t1_cg")
            found.add("t1_fm")
        }
    }

    fun evidenceCount(): Int = Content.evidenceFlags.count { has(it) }

    fun trustSum(): Int = trustOf("marta") + trustOf("wren") + trustOf("tom")

    fun finalReady(): Boolean = night >= Content.LAST_NIGHT && (decoded.contains("t5_halloran") || minute >= 300)

    /** choice: "broadcast", "shutdown" or "answer". */
    fun resolveEnding(choice: String): String = when (choice) {
        "shutdown" -> "silence"
        "broadcast" -> if (evidenceCount() >= 5 && !has("destroyed_records")) "truth" else "loop"
        "answer" -> if (trustSum() >= 2 && !has("destroyed_records") && !has("concealed_callum")) "transmission" else "loop"
        else -> "loop"
    }

    fun objective(): String {
        fun d(id: String) = decoded.contains(id)
        when (night) {
            1 -> {
                if (!d("t1_marta")) return "Find the caller on the harbour band. Tune the dial, then aim the antenna east."
                if (!d("t1_morse")) return "A Morse signal repeats from the south-west. Decode it."
                if (!has("harbour_open")) return "On the board, connect the recording and the Morse note."
                return "The harbour is open. Visit it on the map, or keep listening."
            }
            2 -> {
                if (!d("t2_tom")) return "Voices are buried in static near 5 MHz. Try the FILTER."
                if (!owns("doc_roster")) return "Visit the harbour and find the crew roster."
                if (!has("crew_missing")) return "On the board, connect Tom's recording to the crew roster."
                if (!d("t2_callum")) return "The Harbour Office checks in. Listen on the harbour band."
                return "Keep listening for Operator Nine's numbers near 8.4 MHz."
            }
            3 -> {
                if (!d("t3_horn")) return "A three-tone signal repeats from the north. Hear it, then repeat the notes."
                if (!d("t3_wren")) return "A girl's voice hides under the static on the lighthouse band."
                if (!owns("doc_keeper_log")) return "Visit the lighthouse."
                if (!has("beam_inland")) return "On the board, connect the keeper's log to the lamp room photograph."
                return "Optional: someone is calling from the north-west."
            }
            4 -> {
                if (!d("t4_calder")) return "Greyhaven General is calling near 3 MHz."
                if (!has("hospital_open")) return "On the board, connect Wren's recording to the coordinates note."
                if (!owns("doc_ward_records") && !has("destroyed_records")) return "Visit the hospital and read the admission ledger."
                if (!d("t4_nine_b")) return "Operator Nine's second half airs near 8.4 MHz. Record it."
                if (!has("comms_open")) return "Connect both halves of Operator Nine's numbers."
                return "Signal Hill Facility is on the map. Time is short."
            }
            else -> {
                if (!d("t5_halloran")) return "A restricted channel is open near 3.7 MHz."
                if (!has("comms_done")) return if (has("comms_open")) "Visit Signal Hill Facility and read the project file." else "Find the source of Operator Nine's numbers."
                if (!d("t5_elias")) return "A familiar voice is calling near 5.9 MHz."
                return "Decide at the main transmitter before dawn."
            }
        }
    }

    fun summary(): List<String> {
        val out = ArrayList<String>()
        out.add("Transmissions heard: " + recvNight.size)
        out.add("Transmissions decoded: " + decodedNight.size)
        out.add("Locations investigated: " +
            if (visitedNight.isEmpty()) "none" else visitedNight.joinToString(", ") { Content.scenes[it]?.name ?: it })
        out.add("Evidence connected: " + linksNight.size)
        for (m in linksNight) out.add("   - " + m)
        out.add("Decisions: " + if (decisionsNight.isEmpty()) "none" else decisionsNight.size.toString())
        for (m in decisionsNight) out.add("   - " + m)
        out.add("Radio condition: " + condition.toInt() + "%   Generator fuel: " + fuel.toInt() + "%")
        val unresolved = Content.transmissions.filter {
            it.night == night && !it.flavor && !decoded.contains(it.id) &&
                (it.requiresFlag == null || has(it.requiresFlag))
        }
        out.add("Unresolved: " + if (unresolved.isEmpty()) "none" else unresolved.joinToString(", ") { it.title })
        return out
    }

    fun brief(): String = "Night " + night + "  " + clockText() + "  " + sceneName() +
        (if (ending.isNotEmpty()) "  (finished)" else "")

    // ------------------------------------------------------------------ save format

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n")
    private fun unesc(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                sb.append(if (n == 'n') '\n' else n)
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    fun serialize(): String {
        val sb = StringBuilder()
        fun kv(k: String, v: Any) { sb.append(k).append('=').append(v).append('\n') }
        sb.append("TLF1\n")
        kv("night", night); kv("clock", clock); kv("weather", weather.name)
        kv("scene", scene); kv("px", px); kv("py", py); kv("facing", facing)
        kv("battery", battery); kv("fuel", fuel); kv("condition", condition); kv("stamina", stamina)
        kv("flashlight", flashlight)
        kv("tune", tune); kv("gain", gain); kv("squelch", squelch); kv("bearing", bearing); kv("filter", filter)
        kv("ending", ending); kv("play", playSeconds); kv("recSeq", recSeq)
        for ((k, v) in items) kv("item", "$k:$v")
        for (f in flags) kv("flag", f)
        for ((k, v) in trust) kv("trust", "$k:$v")
        for (c in clues) kv("clue", c)
        for (u in used) kv("used", u)
        for (d in decoded) kv("decoded", d)
        for (r in replied) kv("replied", r)
        for (h in heard) kv("heard", h)
        for (f in found) kv("found", f)
        for ((k, v) in listen) kv("listen", "$k:$v")
        for (r in recordings) kv("rec", r.id + "|" + r.tid + "|" + r.night + "|" + r.minute + "|" + r.freq + "|" + r.quality + "|" + r.tag)
        for ((k, v) in cards) kv("card", k + "|" + v[0] + "|" + v[1])
        for (s in strings) kv("str", s.first + "|" + s.second)
        for (d in decisions) kv("dec", esc(d))
        for (r in recvNight) kv("nrecv", r)
        for (r in decodedNight) kv("ndec", r)
        for (r in visitedNight) kv("nvis", r)
        for (r in linksNight) kv("nlink", esc(r))
        for (r in decisionsNight) kv("ndecis", esc(r))
        return sb.toString()
    }

    companion object {
        const val MIN_PER_SEC = 1.0f

        fun parse(text: String?): GameState? {
            if (text == null) return null
            try {
                val lines = text.split('\n')
                if (lines.isEmpty() || lines[0].trim() != "TLF1") return null
                val s = GameState()
                for (idx in 1 until lines.size) {
                    val line = lines[idx]
                    val i = line.indexOf('=')
                    if (i <= 0) continue
                    val k = line.substring(0, i)
                    val v = line.substring(i + 1)
                    when (k) {
                        "night" -> s.night = v.toInt().coerceIn(1, Content.LAST_NIGHT)
                        "clock" -> s.clock = v.toFloat()
                        "weather" -> s.weather = Weather.valueOf(v)
                        "scene" -> s.scene = if (Content.scenes.containsKey(v)) v else "station"
                        "px" -> s.px = v.toFloat()
                        "py" -> s.py = v.toFloat()
                        "facing" -> s.facing = v.toInt()
                        "battery" -> s.battery = v.toFloat()
                        "fuel" -> s.fuel = v.toFloat()
                        "condition" -> s.condition = v.toFloat()
                        "stamina" -> s.stamina = v.toFloat()
                        "flashlight" -> s.flashlight = v == "true"
                        "tune" -> s.tune = v.toDouble()
                        "gain" -> s.gain = v.toFloat()
                        "squelch" -> s.squelch = v.toFloat()
                        "bearing" -> s.bearing = v.toFloat()
                        "filter" -> s.filter = v == "true"
                        "ending" -> s.ending = v
                        "play" -> s.playSeconds = v.toFloat()
                        "recSeq" -> s.recSeq = v.toInt()
                        "item" -> { val p = v.split(':'); s.items[p[0]] = p[1].toInt() }
                        "flag" -> s.flags.add(v)
                        "trust" -> { val p = v.split(':'); s.trust[p[0]] = p[1].toInt() }
                        "clue" -> s.clues.add(v)
                        "used" -> s.used.add(v)
                        "decoded" -> s.decoded.add(v)
                        "replied" -> s.replied.add(v)
                        "heard" -> s.heard.add(v)
                        "found" -> s.found.add(v)
                        "listen" -> { val p = v.split(':'); s.listen[p[0]] = p[1].toFloat() }
                        "rec" -> {
                            val p = v.split('|')
                            s.recordings.add(Recording(p[0], p[1], p[2].toInt(), p[3].toInt(), p[4].toDouble(), p[5].toFloat(), p[6].toInt()))
                        }
                        "card" -> { val p = v.split('|'); s.cards[p[0]] = floatArrayOf(p[1].toFloat(), p[2].toFloat()) }
                        "str" -> { val p = v.split('|'); s.strings.add(Pair(p[0], p[1])) }
                        "dec" -> s.decisions.add(s.unesc(v))
                        "nrecv" -> s.recvNight.add(v)
                        "ndec" -> s.decodedNight.add(v)
                        "nvis" -> s.visitedNight.add(v)
                        "nlink" -> s.linksNight.add(s.unesc(v))
                        "ndecis" -> s.decisionsNight.add(s.unesc(v))
                    }
                }
                // basic sanity: keep only known clues, make sure every clue has a board card
                s.clues.retainAll(Content.clues.keys)
                for (c in s.clues) if (!s.cards.containsKey(c)) s.cards[c] = floatArrayOf(20f, 62f)
                if (s.px <= 0f || s.py <= 0f) {
                    val sc = Content.scenes[s.scene]!!
                    s.px = sc.spawnX * Content.TILE
                    s.py = sc.spawnY * Content.TILE
                }
                return s
            } catch (e: Exception) {
                return null
            }
        }
    }
}
