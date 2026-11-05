import com.zak.lastfreq.core.*

var fails = 0
fun check(ok: Boolean, msg: String) {
    if (!ok) { fails++; println("FAIL: $msg") }
}

fun main() {
    // ---- content integrity
    for (s in Content.scenes.values) {
        val w = s.tiles[0].size
        check(s.tiles.all { it.size == w }, "scene ${s.id} rows equal width")
        val world = World(s)
        val pos = floatArrayOf(s.spawnX * Content.TILE, s.spawnY * Content.TILE)
        check(!world.blocked(pos[0], pos[1]), "scene ${s.id} spawn is free")
        for (o in s.objects) {
            if (o.interact != null) check(Content.interacts.containsKey(o.interact), "interact ${o.interact} exists")
            // every interactable must be reachable: some free spot within reach
            if (o.interact != null) {
                var reachable = false
                var y = 0f
                while (y < world.heightPx && !reachable) {
                    var x = 0f
                    while (x < world.widthPx && !reachable) {
                        if (!world.blocked(x, y) && world.distanceTo(o, x, y) < 28f) reachable = true
                        x += 8f
                    }
                    y += 8f
                }
                check(reachable, "object ${o.id} in ${s.id} reachable")
            }
        }
        // flood fill from spawn to be sure every interactable region is connected
        val seen = HashSet<Long>()
        val q = ArrayDeque<Pair<Int, Int>>()
        q.add(Pair(pos[0].toInt() / 8, pos[1].toInt() / 8))
        while (q.isNotEmpty()) {
            val (cx, cy) = q.removeFirst()
            val key = cx.toLong() * 100000 + cy
            if (!seen.add(key)) continue
            for ((dx, dy) in listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))) {
                val nx = cx + dx; val ny = cy + dy
                if (nx < 0 || ny < 0) continue
                if (!world.blocked(nx * 8f, ny * 8f) && !seen.contains(nx.toLong() * 100000 + ny)) q.add(Pair(nx, ny))
            }
        }
        for (o in s.objects) {
            if (o.interact == null) continue
            var ok = false
            for (key in seen) {
                val cx = (key / 100000).toFloat() * 8f
                val cy = (key % 100000).toFloat() * 8f
                if (world.distanceTo(o, cx, cy) < 28f) { ok = true; break }
            }
            check(ok, "object ${o.id} in ${s.id} connected to spawn")
        }
    }
    for (l in Content.links) {
        check(Content.clues.containsKey(l.a), "link clue ${l.a}")
        check(Content.clues.containsKey(l.b), "link clue ${l.b}")
    }
    for (t in Content.transmissions) {
        check(Content.characters.any { it.id == t.speaker }, "speaker ${t.speaker}")
        for (c in t.clues) check(Content.clues.containsKey(c), "tx clue $c")
        if (t.recClue != null) check(Content.clues.containsKey(t.recClue), "rec clue ${t.recClue}")
        if (t.kind == Kind.MORSE) check(t.options.size > t.answer, "morse options")
        if (t.kind == Kind.TONES) check(t.code.isNotEmpty(), "tone code")
        check(t.freq in RadioSim.MIN_FREQ..RadioSim.MAX_FREQ, "freq range ${t.id}")
    }
    // no two same-night, overlapping-window transmissions share a frequency band
    for (a in Content.transmissions) for (b in Content.transmissions) {
        if (a.id < b.id && a.night == b.night && Math.abs(a.freq - b.freq) < 0.12 && a.start < b.end && b.start < a.end)
            check(false, "freq clash ${a.id} ${b.id}")
    }
    // every clue obtainable
    val obtainable = HashSet<String>()
    for (i in Content.interacts.values) if (i.clue != null) obtainable.add(i.clue)
    for (t in Content.transmissions) { obtainable.addAll(t.clues); if (t.recClue != null) obtainable.add(t.recClue) }
    for (c in Content.clues.keys) check(obtainable.contains(c), "clue $c obtainable")
    // every flag required by a transmission is set somewhere
    val settable = HashSet<String>()
    for (l in Content.links) settable.add(l.flag)
    for (i in Content.interacts.values) { if (i.flag != null) settable.add(i.flag); for (c in i.choices) settable.addAll(c.flags) }
    for (t in Content.transmissions) { settable.addAll(t.setFlags); for (r in t.replies) settable.addAll(r.flags) }
    for (t in Content.transmissions) if (t.requiresFlag != null) check(settable.contains(t.requiresFlag), "flag ${t.requiresFlag} settable")
    for (l in Content.locations) if (l.flag != null) check(settable.contains(l.flag), "location flag ${l.flag} settable")
    for (f in Content.evidenceFlags) check(settable.contains(f), "evidence flag $f settable")

    // ---- radio simulation
    val tx = Content.transmission("t1_marta")!!
    val perfect = RadioSim.clarity(tx, tx.freq, 0.6f, tx.bearing.toFloat(), false, Weather.CLEAR, 100f, 0f)
    check(perfect > 0.95f, "perfect tuning is clear ($perfect)")
    val off = RadioSim.clarity(tx, tx.freq + 0.2, 0.6f, tx.bearing.toFloat(), false, Weather.CLEAR, 100f, 0f)
    check(off == 0f, "far off frequency is silent")
    val wrongAnt = RadioSim.clarity(tx, tx.freq, 0.6f, (tx.bearing + 180) % 360f, false, Weather.CLEAR, 100f, 0f)
    check(wrongAnt < perfect * 0.5f, "wrong antenna bearing hurts ($wrongAnt)")
    val hid = Content.transmission("t2_tom")!!
    val h0 = RadioSim.clarity(hid, hid.freq, 0.6f, hid.bearing.toFloat(), false, Weather.FOG, 100f, 0f)
    val h1 = RadioSim.clarity(hid, hid.freq, 0.6f, hid.bearing.toFloat(), true, Weather.FOG, 100f, 0f)
    check(h0 < 0.4f && h1 > RadioSim.LOCK, "hidden needs filter ($h0 / $h1)")
    for (w in Weather.values()) {
        for (t in Content.transmissions) {
            val c = RadioSim.clarity(t, t.freq, 0.6f, t.bearing.toFloat(), true, w, 100f, 1.0f)
            // storms spike; others must be listenable
            if (!w.storm) check(c >= RadioSim.LISTEN_MIN, "${t.id} listenable in ${w.name} ($c)")
        }
    }
    var okLock = 0
    for (i in 0..200) {
        val c = RadioSim.clarity(Content.transmission("t5_halloran")!!, 3.70, 0.6f, 176f, true, Weather.STORM, 100f, i * 0.37f)
        if (c >= RadioSim.LISTEN_MIN) okLock++
    }
    check(okLock > 120, "storm still allows listening most of the time ($okLock/200)")
    check(RadioSim.garble("Hello there", 1f, 3, 1) == "Hello there", "clear text unchanged")
    check(RadioSim.garble("Hello there", 0f, 3, 1) != "Hello there", "static text garbled")
    check(Morse.revealed("PIER NINE", 100f).startsWith(".--. .. . .-."), "morse reveals P I E R: " + Morse.revealed("PIER NINE", 100f))

    // ---- full scripted playthrough
    val st = GameState()
    st.startNight(1)
    check(st.active().any { it.id == "t1_marta" }, "marta active night 1")
    st.decode(Content.transmission("t1_marta")!!)
    st.record(Content.transmission("t1_marta")!!, 0.9f)
    st.decode(Content.transmission("t1_morse")!!)
    check(st.owns("rec_marta1") && st.owns("note_pier9"), "night1 clues owned")
    val r1 = st.connect("rec_marta1", "note_pier9")
    check(r1.kind == 2 && st.has("harbour_open"), "link unlocks harbour")
    check(st.connect("rec_marta1", "note_pier9").kind == 1, "second connect removes string")
    check(st.connect("rec_marta1", "doc_logbook").kind == 0, "wrong link is accepted silently")
    check(st.objective().isNotEmpty(), "objective not empty")

    // night over test
    var over = false
    var guard = 0
    while (!over && guard < 10000) { over = st.advance(1f); guard++ }
    check(guard in 400..440, "night length ~420s ($guard)")
    check(st.fuel < 100f && st.fuel > 20f, "fuel drains reasonably (${st.fuel})")

    // evidence + endings
    val e = GameState()
    e.startNight(5)
    check(e.resolveEnding("shutdown") == "silence", "silence ending")
    check(e.resolveEnding("broadcast") == "loop", "broadcast without evidence loops")
    for (f in listOf("storm_doubt", "crew_missing", "callum_lying", "beam_inland", "official_lie")) e.setFlag(f)
    check(e.resolveEnding("broadcast") == "truth", "truth ending with evidence")
    e.setFlag("destroyed_records")
    check(e.resolveEnding("broadcast") == "loop", "destroyed records blocks truth")
    val t = GameState()
    t.startNight(5)
    t.addTrust("marta", 1); t.addTrust("wren", 2)
    check(t.resolveEnding("answer") == "transmission", "transmission ending with trust")
    t.setFlag("concealed_callum")
    check(t.resolveEnding("answer") == "loop", "concealed evidence leads to loop")

    // destroying records removes the clue and its strings
    val d = GameState()
    d.startNight(4)
    d.addClue("doc_ward_records"); d.addClue("clip_storm")
    d.connect("doc_ward_records", "clip_storm")
    check(d.has("official_lie"), "official_lie flag")
    d.setFlag("destroyed_records")
    check(!d.owns("doc_ward_records") && d.strings.isEmpty(), "destroyed ledger removed")
    check(!d.addClue("doc_ward_records"), "cannot re-add destroyed ledger")

    // ---- save / load
    val s2 = GameState()
    s2.startNight(3)
    s2.addClue("rec_marta1"); s2.addClue("note_pier9"); s2.connect("rec_marta1", "note_pier9")
    s2.addItem("battery", 2); s2.addTrust("wren", 2)
    s2.decisions.add("Promised, with a = sign\nand a newline")
    s2.record(Content.transmission("t1_marta")!!, 0.8f)
    s2.listen["t3_wren"] = 4.5f
    s2.clock = 133.7f
    val text = s2.serialize()
    val back = GameState.parse(text)
    check(back != null, "save parses")
    if (back != null) {
        check(back.night == 3 && back.minute == 133, "night/minute restored")
        check(back.owns("rec_marta1") && back.has("harbour_open"), "clues and flags restored")
        check(back.itemCount("battery") == 2 && back.trustOf("wren") == 2, "items and trust restored")
        check(back.decisions[0] == "Promised, with a = sign\nand a newline", "decision escaping")
        check(back.recordings.size == 1 && back.recordings[0].tid == "t1_marta", "recordings restored")
        check(back.listen["t3_wren"] == 4.5f, "listen progress restored")
        check(back.strings.size == 1, "strings restored")
        check(back.serialize() == text, "serialize round trip identical")
    }
    check(GameState.parse("garbage") == null, "garbage save rejected")
    check(GameState.parse("TLF1\nnight=zzz\n") == null, "corrupt number rejected")
    check(GameState.parse(null) == null, "null save rejected")
    check(GameState.parse(text.substring(0, text.length / 2)) != null || true, "truncated save does not crash")
    val sett = Settings()
    sett.master = 0.3f; sett.leftHanded = true; sett.textSpeed = 2
    val sb = Settings.parse(sett.serialize())
    check(sb.master == 0.3f && sb.leftHanded && sb.textSpeed == 2, "settings round trip")
    check(Settings.parse("master=banana").master == 0.8f, "bad setting ignored")

    // ---- audio mixer
    val mx = AudioMixer()
    mx.rain = 0.8f; mx.wind = 0.6f; mx.ocean = 0.8f; mx.hum = 0.5f; mx.staticLevel = 0.5f
    mx.voiceVol = 0.8f; mx.toneVol = 0.7f; mx.musicLevel = 0.5f
    mx.thunder(); mx.click(); mx.step()
    val buf = ShortArray(22050)
    var peak = 0
    var sumSq = 0.0
    for (blk in 0 until 6) {
        mx.render(buf, buf.size)
        for (v in buf) { peak = maxOf(peak, Math.abs(v.toInt())); sumSq += v.toDouble() * v }
    }
    val rms = Math.sqrt(sumSq / (buf.size * 6))
    check(peak in 1000..32767, "audio peak sane ($peak)")
    check(rms > 100 && rms < 20000, "audio rms sane ($rms)")
    val silent = AudioMixer()
    val b2 = ShortArray(4096)
    silent.render(b2, b2.size)
    check(b2.all { Math.abs(it.toInt()) < 400 }, "idle mixer nearly silent")

    // ---- bearing helper matches intended directions
    val hb = Content.bearingOf(Content.location("harbour"))
    check(Math.abs(hb - 81f) < 6f, "harbour bearing ($hb)")
    println("bearings: " + Content.locations.map { it.id + "=" + Content.bearingOf(it).toInt() })
    println("transmissions: ${Content.transmissions.size}, clues: ${Content.clues.size}, links: ${Content.links.size}, interacts: ${Content.interacts.size}")
    println(if (fails == 0) "ALL CORE TESTS PASSED" else "$fails FAILURES")
    if (fails > 0) System.exit(1)
}
