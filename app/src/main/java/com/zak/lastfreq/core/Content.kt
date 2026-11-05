package com.zak.lastfreq.core

/**
 * All story data lives here: characters, clues, board links, transmissions,
 * interactable objects, scenes and endings. Game logic never hardcodes story text.
 */
object Content {

    const val TILE = 32f
    const val NIGHT_MINUTES = 420
    const val LAST_NIGHT = 5

    val evidenceFlags = listOf(
        "storm_doubt", "crew_missing", "callum_lying", "beam_inland",
        "official_lie", "halloran_known", "loop_hint", "ward_on_roster"
    )

    val nightTitles = listOf(
        "", "THE FIRST SIGNAL", "THE HARBOUR", "THE LIGHTHOUSE", "THE HOSPITAL", "STATION 47"
    )

    val nightWeather = listOf(
        Weather.CLEAR, Weather.LIGHT_RAIN, Weather.FOG, Weather.WIND, Weather.HEAVY_RAIN, Weather.STORM
    )

    // ------------------------------------------------------------------ characters

    val characters: List<Character> = listOf(
        Character("marta", "Marta Voss", "Caller, Greyhaven Harbour",
            "Frightened. She calls from the old pier and says it is happening again. She seems to have called Station 47 before, thirty years ago.",
            230f, 5.4f, 0.6f, 0.15f),
        Character("tom", "Tom Hallow", "Retired fisherman",
            "Gruff and suspicious. Says seven men sailed on the Marguerite and the harbour office admits to three.",
            105f, 3.6f, 0.3f, 0.55f),
        Character("callum", "Callum Reyes", "Harbourmaster",
            "Smooth and reassuring. Wants the log left blank. Deliberately deceptive.",
            128f, 4.6f, 0.5f, 0.10f),
        Character("wren", "Wren Alder", "Lighthouse keeper's daughter",
            "Confused and young. She believes it is still 1994 and that she is sixteen. She is afraid of being turned off.",
            250f, 6.2f, 0.7f, 0.20f),
        Character("calder", "Dr. Ines Calder", "Greyhaven General Hospital",
            "Calm to the point of unease. Insists everyone was evacuated before the storm.",
            190f, 4.2f, 0.4f, 0.05f),
        Character("agnes", "Sister Agnes Thorne", "Chapel caretaker",
            "Cryptic and gentle. Keeps a graveyard where every stone carries the same date.",
            165f, 3.1f, 0.35f, 0.30f),
        Character("nine", "Operator Nine", "Automated repeating signal",
            "A flat, patient voice that reads out digits. Nobody has ever found its source.",
            140f, 3.0f, 0.9f, 0.65f),
        Character("halloran", "Director Halloran", "Coastal Signals Division",
            "Former naval communications officer. Ran Project VESPER and signed the evacuation order.",
            98f, 3.9f, 0.45f, 0.45f),
        Character("elias", "A voice you recognise", "Unknown",
            "It says your name. It says it the way you say it.",
            120f, 4.4f, 0.5f, 0.25f),
        Character("cg", "Coast Guard Control", "Marine broadcast",
            "Routine broadcasts. There have been no vessels in Greyhaven waters for thirty years.",
            135f, 4.0f, 0.55f, 0.20f, true),
        Character("fm", "Greyhaven FM", "Commercial broadcast",
            "A local station that should not exist any more.",
            175f, 5.0f, 0.65f, 0.12f, true)
    )

    fun character(id: String): Character = characters.firstOrNull { it.id == id } ?: characters[0]

    // ------------------------------------------------------------------ clues

    private fun c(id: String, type: ClueType, title: String, text: String, altFlag: String? = null, altText: String? = null) =
        Clue(id, type, title, text, altFlag, altText)

    val clues: Map<String, Clue> = listOf(
        c("rec_marta1", ClueType.RECORDING, "Marta Voss: the first call",
            "A frightened woman asks Station 47 to respond. She calls you Elias Ward. She says the lights at the old pier have come on."),
        c("note_pier9", ClueType.NOTE, "Morse from the south-west",
            "Operator Nine's Morse repeats one message: PIER NINE."),
        c("doc_logbook", ClueType.DOCUMENT, "Weiss's final log entry",
            "Operator Weiss, 14 October 1994: 'The radio answered before I called. 47 hears what hasn't happened yet.'"),
        c("clip_storm", ClueType.CLIPPING, "'Storm of '94 empties Greyhaven'",
            "Dated 2 November 1994. The official explanation for the evacuation."),
        c("photo_weiss", ClueType.PHOTO, "Photo of Operator Weiss",
            "Weiss at this very desk, smiling at the lens.",
            "beam_inland", "A second figure now stands behind Weiss's chair. It is looking at the camera."),
        c("rec_callum", ClueType.RECORDING, "Harbourmaster Reyes",
            "Callum Reyes says nothing ever happened here, and asks you to leave tonight's log blank."),
        c("tape_callum", ClueType.DOCUMENT, "Harbour Office answering tape",
            "Reyes, to someone else: 'Keep the Marguerite off the books. Three crew. Always three.'"),
        c("doc_roster", ClueType.DOCUMENT, "Crew roster: trawler Marguerite",
            "Seven names, sailing 31 October 1994. The seventh reads: E. WARD, radio."),
        c("photo_crew", ClueType.PHOTO, "Crew photograph",
            "Seven people squinting into a low sun. The seventh face has been scratched out.",
            "comms_open", "The scratched-out face has healed. It looks like you."),
        c("rec_tom", ClueType.RECORDING, "Tom Hallow, under the static",
            "Tom says seven went out on the Marguerite and the harbour office admits to three. He wants to know what the radio operator was doing aboard."),
        c("note_coords", ClueType.COORD, "Three tones, one place",
            "The tones match C, A and D on the old chart: a grid square on the northern headland."),
        c("rec_wren", ClueType.RECORDING, "Wren Alder, lighthouse band",
            "A girl says her father left in '94 and the light keeps turning. She thinks she is still sixteen."),
        c("doc_keeper_log", ClueType.DOCUMENT, "Lighthouse keeper's log",
            "'Lens bearing 175. Not to sea. Never to sea. Orders from Director H.' A margin note mentions Sister Agnes and the chapel yard."),
        c("photo_lamp", ClueType.PHOTO, "The lamp room, 1994",
            "A girl at the window. In the glass, the beam points inland."),
        c("rec_agnes", ClueType.RECORDING, "Sister Agnes Thorne",
            "Every stone in her yard carries the same date. She asks you to come and read them, and to bring a light."),
        c("rec_calder", ClueType.RECORDING, "Dr. Ines Calder",
            "Calder insists everyone was evacuated before the storm, and warns you away from the east stairwell."),
        c("doc_ward_records", ClueType.DOCUMENT, "Greyhaven General admission ledger",
            "212 patients admitted 17 to 30 October 1994. Complaint: hearing a voice on the radio. The storm came on 2 November."),
        c("doc_evac_order", ClueType.DOCUMENT, "Evacuation order",
            "Typed 20 October 1994. Signed: Director R. Halloran, Coastal Signals Division."),
        c("photo_ward", ClueType.PHOTO, "Ward photograph",
            "Patients sitting up in bed, all facing the same wall.",
            "comms_open", "Every patient is looking at the camera now."),
        c("rec_nine_a", ClueType.RECORDING, "Operator Nine: first half",
            "A flat voice reads digits: seven, three, nine ... and stops mid-count."),
        c("rec_nine_b", ClueType.RECORDING, "Operator Nine: second half",
            "The same voice, another night: ... two, five, four. Hold."),
        c("note_stones", ClueType.NOTE, "The stones",
            "Every grave in St. Brannoc's Yard is dated 31 October 1994.",
            "comms_open", "A new stone at the end of the row: ELIAS WARD, RADIO OPERATOR. Beneath it, tomorrow's date."),
        c("doc_vesper", ClueType.DOCUMENT, "Project VESPER file",
            "A transmitter built to speak to ships in fog. On day 11 it answered. On day 13 it asked for the operator by name."),
        c("rec_halloran", ClueType.RECORDING, "Director Halloran",
            "'We built a voice for ships in fog. Then something answered.'"),
        c("rec_elias", ClueType.RECORDING, "A voice you recognise",
            "Your own voice, calling Station 47 and asking someone to respond.")
    ).associateBy { it.id }

    val links: List<Link> = listOf(
        Link("rec_marta1", "note_pier9", "harbour_open",
            "Pier Nine. The caller's old pier, repeated in Morse from the south-west. The harbour is on the map."),
        Link("doc_logbook", "clip_storm", "storm_doubt",
            "Weiss wrote of an empty town two weeks before the storm."),
        Link("rec_tom", "doc_roster", "crew_missing",
            "Seven on the roster. Three on the harbour's books. Four people erased."),
        Link("rec_callum", "tape_callum", "callum_lying",
            "The harbourmaster told someone to keep things off the books. He has told you the same."),
        Link("doc_keeper_log", "photo_lamp", "beam_inland",
            "The lighthouse never guided ships. It points at Station 47."),
        Link("rec_wren", "note_coords", "hospital_open",
            "Wren's father logged a call from the hospital roof. The grid square matches. Greyhaven General is on the map."),
        Link("doc_ward_records", "clip_storm", "official_lie",
            "Patients were admitted for hearing voices two weeks before the storm. There was no storm."),
        Link("doc_evac_order", "doc_logbook", "halloran_known",
            "Halloran signed the evacuation order and is named in Weiss's last log. He was there from the beginning."),
        Link("rec_nine_a", "rec_nine_b", "comms_open",
            "Seven-three-nine, two-five-four. A grid reference beneath Signal Hill. A facility is on the map."),
        Link("note_stones", "doc_roster", "loop_hint",
            "E. Ward on the roster. E. Ward on a stone. The same night, the same name."),
        Link("photo_crew", "doc_roster", "ward_on_roster",
            "The seventh figure is the radio operator. His face has been scratched out of the photograph.")
    )

    // ------------------------------------------------------------------ transmissions

    private fun rp(text: String, resp: String, trust: Map<String, Int> = emptyMap(), flags: List<String> = emptyList(), dec: String = "") =
        Reply(text, resp, trust, flags, dec)

    val transmissions: List<Transmission> = listOf(
        // ---------------- NIGHT 1
        Transmission("t1_marta", 1, 4.20, 81, 0.30, Kind.VOICE, "marta", "Distress call: harbour band", 0, 260,
            listOf(
                "Station 47, do you copy? Please respond.",
                "It's happening again.",
                "Elias Ward. That is your name, isn't it? Please, Elias. The lights at the old pier have come on.",
                "Nobody has used those lights since the boats stopped coming back.",
                "If you can hear this, stay on the line. Don't let them take the signal."
            ),
            recClue = "rec_marta1",
            replies = listOf(
                rp("Station 47. Go ahead, I'm listening.", "...Thank you. Thank you for picking up.",
                    mapOf("marta" to 1), listOf("answered_marta"), "Answered Marta Voss's distress call"),
                rp("Who is this? How do you know my name?", "You know me. You will. Please just stay on the line.",
                    emptyMap(), listOf("questioned_marta"), "Questioned Marta Voss"),
                rp("(Say nothing)", "...",
                    mapOf("marta" to -1), listOf("ignored_marta"), "Ignored Marta Voss's distress call")
            )),
        Transmission("t1_morse", 1, 6.18, 250, 0.35, Kind.MORSE, "nine", "Repeating Morse signal", 20, 340,
            listOf("Morse key, repeating"),
            clues = listOf("note_pier9"),
            options = listOf("PIER NINE", "PINE ROAD", "FIRE LINE", "PIER SEVEN"), answer = 0),
        Transmission("t1_cg", 1, 5.55, 300, 0.25, Kind.VOICE, "cg", "Marine forecast", 0, 420,
            listOf(
                "Coast Guard Control, Greyhaven sector. Light rain, wind rising overnight.",
                "All vessels remain in port. Repeat, all vessels remain in port."
            ), flavor = true),
        Transmission("t1_fm", 1, 7.30, 150, 0.30, Kind.VOICE, "fm", "Commercial broadcast", 40, 420,
            listOf(
                "...and that was the Blue Marlin Quartet. You are listening to Greyhaven FM.",
                "Keeping the harbour company since 1971. It is eleven o'clock on the thirty-first of October."
            ), flavor = true),

        // ---------------- NIGHT 2
        Transmission("t2_callum", 2, 3.85, 85, 0.30, Kind.VOICE, "callum", "Harbour Office check-in", 10, 280,
            listOf(
                "Station 47, this is the Harbour Office. Routine check.",
                "Nothing to report. Nothing ever happened here, whatever you've heard.",
                "Do us both a favour. Leave tonight's log blank."
            ),
            recClue = "rec_callum",
            replies = listOf(
                rp("Understood. The log stays blank.", "Good man. Sensible man.",
                    mapOf("callum" to 1), listOf("concealed_callum"), "Agreed to leave the Harbour Office log blank"),
                rp("Everything gets logged. That's the job.", "...That's a pity.",
                    mapOf("callum" to -1), listOf("reported_callum"), "Refused to hide the Harbour Office log")
            )),
        Transmission("t2_tom", 2, 5.05, 70, 0.25, Kind.HIDDEN, "tom", "Voices under the static", 30, 320,
            listOf(
                "...Station 47? They said that place was scrapped.",
                "Seven went out on the Marguerite. Seven. The harbour office says three.",
                "Whoever sits at that desk, ask what the radio operator was doing on my boat."
            ),
            recClue = "rec_tom",
            replies = listOf(
                rp("Tell me about the crew.", "Not on this line. Come to the harbour. Bring a light.",
                    mapOf("tom" to 1), emptyList(), "Asked Tom Hallow about the crew"),
                rp("I can't help with that.", "Then you're no better than the rest of them.",
                    mapOf("tom" to -1), emptyList(), "Brushed off Tom Hallow")
            )),
        Transmission("t2_nine_a", 2, 8.42, 250, 0.45, Kind.VOICE, "nine", "Repeating number station", 100, 400,
            listOf(
                "Seven. Three. Nine.",
                "Hold.",
                "Seven. Three. Nine. Hold."
            ),
            recClue = "rec_nine_a"),
        Transmission("t2_test", 2, 4.60, 20, 0.25, Kind.VOICE, "cg", "Emergency broadcast test", 0, 420,
            listOf(
                "This is a test of the Greyhaven emergency broadcast system.",
                "This is only a test. Had this been an actual emergency, you would have been told where to go.",
                "You have not been told where to go."
            ), flavor = true),

        // ---------------- NIGHT 3
        Transmission("t3_horn", 3, 3.40, 7, 0.40, Kind.TONES, "wren", "Repeating three-tone signal", 0, 300,
            listOf("Three tones, repeating"),
            clues = listOf("note_coords"), setFlags = listOf("tones_solved", "lighthouse_open"),
            code = listOf(2, 0, 3)),
        Transmission("t3_wren", 3, 6.66, 10, 0.25, Kind.HIDDEN, "wren", "A girl on the lighthouse band", 20, 360,
            listOf(
                "Hello? Is anyone on this? The light keeps turning.",
                "Dad said it would stop when the last of them left. He left in ninety-four. I'm still here.",
                "I think I'm still sixteen. Please don't tell the harbour. They'll turn it off. They'll turn me off."
            ),
            recClue = "rec_wren",
            replies = listOf(
                rp("I won't tell anyone.", "Thank you. Thank you, Elias.",
                    mapOf("wren" to 2), listOf("concealed_wren"), "Promised Wren Alder to keep her secret"),
                rp("I have to report this.", "Then I'll stop talking.",
                    mapOf("wren" to -1), listOf("reported_wren"), "Told Wren Alder the call would be reported"),
                rp("Tell me what you know about the light.", "It hums. When it hums, don't look.",
                    mapOf("wren" to 1), emptyList(), "Asked Wren Alder about the light")
            )),
        Transmission("t3_agnes", 3, 4.55, 320, 0.30, Kind.VOICE, "agnes", "Chapel caretaker", 60, 400,
            listOf(
                "Every stone in my yard carries the same date.",
                "I stopped tending them. They stopped needing me.",
                "Come and read them. Bring a light."
            ),
            recClue = "rec_agnes", requiresFlag = "cemetery_hint", setFlags = listOf("cemetery_open")),
        Transmission("t3_cg", 3, 5.55, 300, 0.25, Kind.VOICE, "cg", "Wind advisory", 0, 420,
            listOf(
                "Wind advisory, Greyhaven sector. All vessels remain in port.",
                "Lighthouse beam reported active. The lighthouse was decommissioned in 1994. Disregard."
            ), flavor = true),

        // ---------------- NIGHT 4
        Transmission("t4_calder", 4, 3.15, 226, 0.35, Kind.VOICE, "calder", "Greyhaven General", 10, 300,
            listOf(
                "Greyhaven General. Dr. Calder speaking. We evacuated everyone before the storm. Everyone.",
                "The paperwork is in order. No one reported hearing anything on the radio.",
                "If you come here, please don't use the east stairwell."
            ),
            recClue = "rec_calder",
            replies = listOf(
                rp("Show me the records when I arrive.", "Of course. We have nothing to hide.",
                    mapOf("calder" to 1), emptyList(), "Asked Dr. Calder for the records"),
                rp("The records say otherwise, don't they?", "I'm afraid this line is no longer secure.",
                    mapOf("calder" to -1), listOf("pressed_calder"), "Accused Dr. Calder of lying")
            )),
        Transmission("t4_nine_b", 4, 8.42, 250, 0.45, Kind.VOICE, "nine", "Repeating number station", 100, 410,
            listOf(
                "Two. Five. Four.",
                "Hold.",
                "Two. Five. Four. Hold."
            ),
            recClue = "rec_nine_b"),
        Transmission("t4_fm", 4, 7.30, 150, 0.30, Kind.VOICE, "fm", "Commercial broadcast", 0, 420,
            listOf(
                "...you are listening to Greyhaven FM. It is eleven o'clock on the thirty-first of October.",
                "It is eleven o'clock on the thirty-first of October. It is eleven o'clock."
            ), flavor = true),

        // ---------------- NIGHT 5
        Transmission("t5_halloran", 5, 3.70, 176, 0.45, Kind.VOICE, "halloran", "Restricted channel", 0, 300,
            listOf(
                "Station 47. This is a restricted channel. Identify yourself.",
                "Ward. Of course it's Ward. It is always Ward.",
                "VESPER was never meant to speak back. We built a voice for ships in fog. Then something answered.",
                "Whatever you decide tonight, decide it before it decides for you."
            ),
            recClue = "rec_halloran", setFlags = listOf("halloran_contacted"),
            replies = listOf(
                rp("I'm going to tell everyone what happened here.", "Then you are braver than I was.",
                    mapOf("halloran" to -1), listOf("vowed_truth"), "Told Halloran the truth would come out"),
                rp("Tell me how to shut it down.", "The master breaker, behind the big transmitter. Don't hesitate.",
                    mapOf("halloran" to 1), listOf("halloran_shutdown"), "Asked Halloran how to shut VESPER down"),
                rp("What does it want?", "To be answered. That is all it has ever wanted.",
                    mapOf("halloran" to 1), listOf("asked_want"), "Asked Halloran what the signal wants")
            )),
        Transmission("t5_elias", 5, 5.90, 45, 0.50, Kind.VOICE, "elias", "A voice you recognise", 60, 400,
            listOf(
                "Station 47, do you copy? Please respond. It's happening again.",
                "Elias Ward. You called yourself that once. I have said these words forty times.",
                "Whatever you choose, I'm the one who answers. Choose carefully."
            ),
            recClue = "rec_elias", requiresFlag = "comms_done", setFlags = listOf("heard_elias"),
            replies = listOf(
                rp("I'll end it.", "You've said that before.",
                    mapOf("elias" to 1), listOf("vowed_end"), "Promised to end it"),
                rp("I'll answer.", "You've said that before, too.",
                    mapOf("elias" to 1), listOf("vowed_answer"), "Promised to answer it")
            )),
        Transmission("t5_marta", 5, 4.20, 81, 0.30, Kind.VOICE, "marta", "Distress call: harbour band", 0, 420,
            listOf(
                "Station 47. It's Marta Voss again. I don't think you ever answered me.",
                "The lights are on. They're all on. Please. Please just say something."
            ),
            altFlag = "answered_marta",
            altLines = listOf(
                "Station 47. It's Marta. You answered me, once.",
                "The lights are on again. I'm not afraid any more. I just wanted you to know."
            ),
            replies = listOf(
                rp("I'm here, Marta.", "...Thank you.",
                    mapOf("marta" to 1), emptyList(), "Reassured Marta Voss on the last night"),
                rp("(Say nothing)", "...",
                    mapOf("marta" to -1), emptyList(), "Left Marta Voss unanswered on the last night")
            )),
        Transmission("t5_cg", 5, 5.55, 300, 0.25, Kind.VOICE, "cg", "Storm warning", 0, 420,
            listOf(
                "All stations, all stations. Storm warning, Greyhaven sector.",
                "Disregard. There is no storm. There has never been a storm."
            ), flavor = true)
    )

    fun transmission(id: String): Transmission? = transmissions.firstOrNull { it.id == id }

    // ------------------------------------------------------------------ interactables

    private fun ix(i: Interact) = i

    val interacts: Map<String, Interact> = listOf(
        // ---- Station 47
        Interact("s_radio", "Operate radio", special = "radio"),
        Interact("s_log", "Read logbook",
            pages = listOf(
                "The station logbook. Most of it is routine: frequencies, weather, signatures.",
                "The last pages are in a different hand. 'NIGHT 6. The radio answered before I called. It knows my name. 47 hears what hasn't happened yet. - Weiss'",
                "The entry is dated 14 October 1994."
            ), clue = "doc_logbook"),
        Interact("s_clip", "Read clipping",
            pages = listOf(
                "A yellowed newspaper pinned beside the window.",
                "'STORM OF '94 EMPTIES GREYHAVEN. A coastal surge forces the evacuation of the harbour district.' 2 November 1994."
            ), clue = "clip_storm"),
        Interact("s_window", "Check the weather", special = "weather"),
        Interact("s_cabinet", "Open cabinet",
            pages = listOf("A jerry can of fuel and a packet of batteries, left for the next operator."),
            items = mapOf("fuel" to 1, "battery" to 1),
            usedPages = listOf("Empty. Someone has already been through it.")),
        Interact("s_drawer", "Open drawer",
            pages = listOf("A tin of spare radio valves. These will repair worn equipment."),
            items = mapOf("tube" to 1),
            usedPages = listOf("The tin is empty.")),
        Interact("s_machine", "Play answering machine",
            pages = listOf(
                "One saved message. Heavy static, then a man's voice.",
                "'Station 47, this is Weiss. If you're hearing this, it has already begun. Don't answer the first call.'",
                "The tape runs out."
            )),
        Interact("s_gen", "Generator", special = "generator"),
        Interact("s_map", "Study the map", special = "map"),
        Interact("s_board", "Investigation board", special = "board"),
        Interact("s_bed", "Bunk", special = "bed"),
        Interact("s_trans", "Main transmitter", special = "transmitter"),
        Interact("s_door", "Leave the station", special = "door"),
        Interact("s_photo", "Examine photograph",
            pages = listOf(
                "A framed photograph of the previous operator, Weiss, at this very desk.",
                "He is smiling. The chair is the same chair."
            ), clue = "photo_weiss",
            altFlag = "beam_inland",
            altPages = listOf(
                "The photograph has changed.",
                "There is a second figure behind the chair. It is looking directly at the camera.",
                "You don't remember it being there."
            )),

        // ---- Harbour
        Interact("h_roster", "Read crew roster",
            pages = listOf(
                "A crew roster in a waterlogged folder. Trawler MARGUERITE, sailing 31 October 1994.",
                "Seven names: skipper, mate, four deckhands.",
                "And one more at the bottom, in different ink: 'E. WARD, radio.'"
            ), clue = "doc_roster"),
        Interact("h_photo", "Examine photograph",
            pages = listOf(
                "A framed crew photograph. Seven people squinting into a low sun.",
                "The seventh figure's face has been scratched out."
            ), clue = "photo_crew",
            altFlag = "comms_open",
            altPages = listOf(
                "The crew photograph. Seven people squinting into the sun.",
                "The scratched-out face has healed. It is looking at you."
            )),
        Interact("h_tape", "Play answering machine",
            pages = listOf(
                "An answering machine with a cassette still inside.",
                "Callum's voice: 'Keep the Marguerite off the books. Three crew. Always three. Burn the log if you have to.'"
            ), clue = "tape_callum"),
        Interact("h_boat", "Inspect the boat",
            pages = listOf(
                "The hull of the Marguerite, dragged up the slip. Barnacles, rust, a ladder going nowhere.",
                "Through a porthole you see a ship's radio on a bench. It is warm."
            )),
        Interact("h_net", "Examine net",
            pages = listOf("A net hung to dry. Seven knots tied along the top rope. One has been cut.")),
        Interact("h_fuel", "Take fuel can",
            pages = listOf("A jerry can behind the winch. Still full."),
            items = mapOf("fuel" to 1), usedPages = listOf("Only a ring in the dust where the can was.")),
        Interact("h_bat", "Open toolbox",
            pages = listOf("A battery pack in a rusted toolbox. Still holding charge."),
            items = mapOf("battery" to 1), usedPages = listOf("The toolbox is empty.")),
        Interact("h_exit", "Return to Station 47", special = "exit"),

        // ---- Lighthouse
        Interact("l_log", "Read keeper's log",
            pages = listOf(
                "The keeper's log. The final entry reads: 'Beam on, as ordered. Lens bearing 175. Not to sea. Never to sea. Orders from Director H.'",
                "In the margin, in pencil: 'Sister Agnes keeps the chapel yard. Ask her about the stones.'"
            ), clue = "doc_keeper_log", flag = "cemetery_hint"),
        Interact("l_lens", "Examine the lens",
            pages = listOf(
                "The great lens. Even dark, it throws a pale shimmer across the room.",
                "A compass painted on the floor marks its heading: 175 degrees. Inland. Toward the station."
            )),
        Interact("l_photo", "Examine photograph",
            pages = listOf(
                "A photograph tucked into the lens housing: the lamp room, a girl at the window.",
                "In the glass, the beam points inland."
            ), clue = "photo_lamp"),
        Interact("l_draw", "Look at drawing",
            pages = listOf(
                "A child's drawing: a girl beneath a turning beam.",
                "Written underneath: 'Dad says don't look at the light when it hums.'"
            )),
        Interact("l_horn", "Examine the horn",
            pages = listOf(
                "The fog horn mechanism, three brass reeds in a row.",
                "Tuned to three notes. You've heard them before."
            )),
        Interact("l_bat", "Open toolbox",
            pages = listOf("A battery pack and a spare valve in an oilskin."),
            items = mapOf("battery" to 1, "tube" to 1), usedPages = listOf("Nothing left but oilskin.")),
        Interact("l_exit", "Return to Station 47", special = "exit"),

        // ---- Hospital
        Interact("ho_records", "Read ledger",
            pages = listOf(
                "The admission ledger of Greyhaven General. Most entries are in the same handwriting.",
                "Admitted 17 to 30 October 1994: 212 patients. Complaint: 'hearing a voice on the radio.'",
                "The storm did not arrive until the second of November."
            ), clue = "doc_ward_records"),
        Interact("ho_evac", "Read evacuation order",
            pages = listOf(
                "An evacuation order, typed on carbon paper. Dated 20 October 1994.",
                "Signed: Director R. Halloran, Coastal Signals Division."
            ), clue = "doc_evac_order"),
        Interact("ho_photo", "Examine photograph",
            pages = listOf(
                "A ward photograph. Patients sitting up in bed, all facing the same wall.",
                "Nobody is looking at the camera."
            ), clue = "photo_ward",
            altFlag = "comms_open",
            altPages = listOf(
                "The ward photograph.",
                "Every patient is looking at the camera now."
            )),
        Interact("ho_incin", "Examine incinerator",
            pages = listOf(
                "The basement incinerator, still warm. The admission ledger would burn easily.",
                "Burn it, or keep it?"
            ),
            needClue = "doc_ward_records",
            lockedPages = listOf("The incinerator is still warm. There is nothing here you want to burn."),
            choices = listOf(
                rp("Burn the ledger.", "The ledger curls and blackens. Whatever it knew, it knows nothing now.",
                    emptyMap(), listOf("destroyed_records"), "Burned the hospital ward ledger"),
                rp("Keep the ledger.", "You tuck the ledger under your arm. It is heavier than paper should be.",
                    emptyMap(), listOf("preserved_records"), "Preserved the hospital ward ledger")
            ),
            usedPages = listOf("The incinerator ticks as it cools.")),
        Interact("ho_east", "Try the stairwell door",
            pages = listOf(
                "The east stairwell door. It hums against your palm.",
                "You do not open it."
            )),
        Interact("ho_fuel", "Take fuel can",
            pages = listOf("A fuel can in the porter's cupboard."),
            items = mapOf("fuel" to 1), usedPages = listOf("The cupboard is empty.")),
        Interact("ho_bat", "Open toolbox",
            pages = listOf("A torch battery in a technician's toolbox."),
            items = mapOf("battery" to 1), usedPages = listOf("The toolbox is empty.")),
        Interact("ho_exit", "Return to Station 47", special = "exit"),

        // ---- Cemetery
        Interact("c_stones", "Read the stones",
            pages = listOf(
                "Rows of weathered stones. You read them by torchlight.",
                "Every stone carries the same date: 31 October 1994.",
                "Sister Agnes was right."
            ), clue = "note_stones",
            altFlag = "comms_open",
            altPages = listOf(
                "Rows of weathered stones. Every one is dated 31 October 1994.",
                "At the end of the last row stands a new stone, still clean. ELIAS WARD. RADIO OPERATOR.",
                "The date beneath is tomorrow's."
            )),
        Interact("c_lantern", "Examine the lantern",
            pages = listOf(
                "A chapel lantern burning with no one to light it.",
                "A battery pack sits beside it, left like an offering."
            ), items = mapOf("battery" to 1), usedPages = listOf("The lantern burns on, steady and warm.")),
        Interact("c_bench", "Sit a moment",
            pages = listOf(
                "A wooden bench facing the sea. Someone has carved a line into the arm.",
                "'It will answer if you ask. It will keep answering.'"
            )),
        Interact("c_exit", "Return to Station 47", special = "exit"),

        // ---- Signal Hill facility
        Interact("co_vesper", "Read project file",
            pages = listOf(
                "PROJECT VESPER. Coastal Signals Division. A transmitter for the fog: a voice for ships.",
                "Test log, Day 11: 'It answered. It knew the callsign before we sent it.'",
                "Day 13: 'It asked for the operator by name.'",
                "'Recommendation: shut down. Director's response: continue.'"
            ), clue = "doc_vesper", flag = "comms_done"),
        Interact("co_tape", "Play the tape",
            pages = listOf(
                "A reel-to-reel tape labelled LAST TRANSMISSION. You thread it and press play.",
                "'Station 47, do you copy? Please respond. It's happening again.'",
                "It's your voice."
            ), flag = "heard_own_voice"),
        Interact("co_fuel", "Take fuel can",
            pages = listOf("A sealed fuel drum with a siphon."),
            items = mapOf("fuel" to 1), usedPages = listOf("Drained.")),
        Interact("co_bat", "Open toolbox",
            pages = listOf("A battery and a spare valve in a toolbox."),
            items = mapOf("battery" to 1, "tube" to 1), usedPages = listOf("The toolbox is empty.")),
        Interact("co_exit", "Return to Station 47", special = "exit")
    ).associateBy { it.id }

    // ------------------------------------------------------------------ scenes

    private class MB(val w: Int, val h: Int, fill: Char) {
        val t = Array(h) { CharArray(w) { fill } }
        fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Char) {
            for (j in y until y + rh) for (i in x until x + rw) if (i in 0 until w && j in 0 until h) t[j][i] = c
        }
    }

    private fun o(id: String, type: String, x: Float, y: Float, w: Float = 1f, h: Float = 1f,
                  solid: Boolean = true, interact: String? = null, light: Float = 0f) =
        Obj(id, type, x, y, w, h, solid, interact, light)

    private fun buildStation(): Scene {
        val m = MB(24, 12, '#')
        m.rect(1, 1, 22, 10, '.')
        m.rect(11, 1, 1, 10, '#')
        m.rect(11, 6, 1, 2, '.')
        val objs = listOf(
            o("radiodesk", "radiodesk", 2f, 1f, 3f, 1f, true, "s_radio"),
            o("shelf", "shelf", 6f, 1f, 1f, 1f, true, "s_log"),
            o("cabinet", "cabinet", 8f, 1f, 1f, 1f, true, "s_cabinet"),
            o("drawer", "drawer", 9f, 1f, 1f, 1f, true, "s_drawer"),
            o("window", "window", 4f, 0f, 2f, 1f, false, "s_window"),
            o("clipping", "clipping", 7f, 0f, 1f, 1f, false, "s_clip"),
            o("machine", "answering", 1f, 5f, 1f, 1f, true, "s_machine"),
            o("generator", "generator", 1f, 8f, 2f, 2f, true, "s_gen"),
            o("maptable", "maptable", 4f, 5f, 3f, 2f, true, "s_map"),
            o("board", "board", 14f, 0f, 4f, 1f, false, "s_board"),
            o("photo", "photo", 19f, 0f, 1f, 1f, false, "s_photo"),
            o("bed", "bed", 20f, 1f, 2f, 2f, true, "s_bed"),
            o("transmitter", "transmitter", 14f, 8f, 3f, 2f, true, "s_trans"),
            o("door", "door", 11f, 11f, 2f, 1f, false, "s_door"),
            o("chair", "chair", 3f, 2f, 1f, 1f, false),
            o("lamp1", "lamp", 9f, 5f, 1f, 1f, false, null, 120f),
            o("lamp2", "lamp", 18f, 5f, 1f, 1f, false, null, 110f)
        )
        return Scene("station", "Station 47", "station", 0.55f, true, m.t, objs, 9.5f, 8.5f,
            Amb(0.25f, 0.30f, 0.30f, 0.35f))
    }

    private fun buildHarbour(): Scene {
        val m = MB(28, 14, '~')
        m.rect(10, 2, 8, 2, 'p')
        m.rect(12, 4, 4, 5, 'p')
        m.rect(0, 8, 12, 1, '=')
        m.rect(16, 8, 12, 1, '=')
        m.rect(0, 9, 28, 5, ',')
        m.rect(0, 9, 11, 5, '#')
        m.rect(1, 10, 9, 3, '.')
        m.rect(5, 9, 1, 1, '.')
        m.rect(24, 12, 4, 2, 'T')
        val objs = listOf(
            o("roster", "deskpapers", 2f, 10f, 2f, 1f, true, "h_roster"),
            o("hphoto", "frame", 7f, 9f, 1f, 1f, false, "h_photo"),
            o("htape", "answering", 8f, 11f, 1f, 1f, true, "h_tape"),
            o("boat", "boat", 18f, 2f, 5f, 2f, true, "h_boat"),
            o("net", "net", 14f, 10f, 2f, 1f, false, "h_net"),
            o("fuelcan", "jerrycan", 21f, 11f, 1f, 1f, false, "h_fuel"),
            o("toolbox", "toolbox", 16f, 12f, 1f, 1f, false, "h_bat"),
            o("crate1", "crate", 18f, 10f, 1f, 1f, true),
            o("crate2", "crate", 19f, 10f, 1f, 1f, true),
            o("crate3", "crate", 18f, 11f, 1f, 1f, true),
            o("lampA", "lamp", 11f, 7f, 1f, 1f, false, null, 130f),
            o("lampB", "lamp", 21f, 7f, 1f, 1f, false, null, 120f),
            o("lampC", "lamp", 13f, 2f, 1f, 1f, false, null, 110f),
            o("sign", "sign", 27f, 8f, 1f, 1f, false, "h_exit")
        )
        return Scene("harbour", "Greyhaven Harbour", "harbour", 0.82f, false, m.t, objs, 24f, 8.5f,
            Amb(0.9f, 0.4f, 0f, 0.2f))
    }

    private fun buildLighthouse(): Scene {
        val m = MB(22, 10, '#')
        m.rect(1, 1, 20, 8, '.')
        m.rect(10, 1, 2, 3, '#')
        m.rect(10, 6, 2, 3, '#')
        val objs = listOf(
            o("kdesk", "deskpapers", 2f, 1f, 2f, 1f, true, "l_log"),
            o("drawing", "drawing", 5f, 0f, 1f, 1f, false, "l_draw"),
            o("ltool", "toolbox", 8f, 7f, 1f, 1f, false, "l_bat"),
            o("horn", "horn", 6f, 5f, 3f, 2f, true, "l_horn"),
            o("lens", "lens", 15f, 3f, 3f, 3f, true, "l_lens", 170f),
            o("lframe", "frame", 19f, 0f, 1f, 1f, false, "l_photo"),
            o("ldoor", "door", 5f, 9f, 2f, 1f, false, "l_exit"),
            o("llamp", "lamp", 3f, 5f, 1f, 1f, false, null, 110f)
        )
        return Scene("lighthouse", "Greyhaven Light", "lighthouse", 0.78f, true, m.t, objs, 6.5f, 7.5f,
            Amb(0.6f, 0.7f, 0.1f, 0.25f))
    }

    private fun buildHospital(): Scene {
        val m = MB(26, 12, '#')
        m.rect(1, 1, 24, 10, '.')
        m.rect(8, 1, 1, 4, '#')
        m.rect(18, 1, 1, 4, '#')
        m.rect(8, 7, 1, 4, '#')
        m.rect(18, 7, 1, 4, '#')
        val objs = listOf(
            o("ledger", "deskpapers", 2f, 2f, 2f, 1f, true, "ho_records"),
            o("evac", "deskpapers", 11f, 2f, 2f, 1f, true, "ho_evac"),
            o("wphoto", "frame", 21f, 0f, 1f, 1f, false, "ho_photo"),
            o("incin", "incinerator", 21f, 8f, 2f, 2f, true, "ho_incin"),
            o("hfuel", "jerrycan", 4f, 9f, 1f, 1f, false, "ho_fuel"),
            o("hbat", "toolbox", 12f, 9f, 1f, 1f, false, "ho_bat"),
            o("estair", "stairdoor", 24f, 5f, 1f, 2f, true, "ho_east"),
            o("hbed1", "hbed", 2f, 8f, 2f, 1f, true),
            o("hbed2", "hbed", 5f, 8f, 2f, 1f, true),
            o("hbed3", "hbed", 20f, 2f, 2f, 1f, true),
            o("hbed4", "hbed", 14f, 8f, 2f, 1f, true),
            o("hdoor", "door", 12f, 11f, 2f, 1f, false, "ho_exit"),
            o("hl1", "lamp", 6f, 5f, 1f, 1f, false, null, 120f),
            o("hl2", "lamp", 15f, 5f, 1f, 1f, false, null, 120f),
            o("hl3", "lamp", 22f, 5f, 1f, 1f, false, null, 100f)
        )
        return Scene("hospital", "Greyhaven General", "hospital", 0.85f, true, m.t, objs, 12.5f, 6.5f,
            Amb(0f, 0.10f, 0.35f, 0.30f))
    }

    private fun buildCemetery(): Scene {
        val m = MB(24, 13, ',')
        m.rect(0, 0, 24, 1, 'T')
        m.rect(0, 12, 24, 1, 'T')
        m.rect(0, 0, 1, 13, 'T')
        m.rect(23, 0, 1, 13, 'T')
        m.rect(23, 6, 1, 2, ',')
        m.rect(2, 2, 2, 2, 'T')
        m.rect(19, 2, 2, 2, 'T')
        m.rect(9, 1, 6, 3, '#')
        m.rect(10, 2, 4, 1, '.')
        m.rect(11, 3, 2, 1, '.')
        val objs = ArrayList<Obj>()
        var n = 0
        for (row in intArrayOf(5, 7, 9)) {
            for (col in intArrayOf(3, 6, 9, 12, 15, 18)) {
                objs.add(o("grave" + n, "grave", col.toFloat(), row.toFloat(), 1f, 1f, true))
                n++
            }
        }
        objs.add(o("gravelabel", "marker", 3f, 6f, 16f, 1f, false, "c_stones"))
        objs.add(o("lantern", "lantern", 12f, 2f, 1f, 1f, false, "c_lantern", 110f))
        objs.add(o("cbench", "bench", 5f, 11f, 2f, 1f, true, "c_bench"))
        objs.add(o("gate", "gate", 22f, 6f, 1f, 2f, false, "c_exit"))
        return Scene("cemetery", "St. Brannoc's Yard", "cemetery", 0.85f, false, m.t, objs, 21f, 6.5f,
            Amb(0.2f, 0.5f, 0f, 0.2f))
    }

    private fun buildComms(): Scene {
        val m = MB(22, 11, '#')
        m.rect(1, 1, 20, 9, '.')
        m.rect(10, 1, 1, 3, '#')
        m.rect(10, 6, 1, 4, '#')
        val objs = listOf(
            o("vfile", "filecabinet", 3f, 1f, 2f, 1f, true, "co_vesper"),
            o("reel", "reel", 15f, 2f, 3f, 1f, true, "co_tape"),
            o("cfuel", "jerrycan", 3f, 8f, 1f, 1f, false, "co_fuel"),
            o("cbat", "toolbox", 17f, 8f, 1f, 1f, false, "co_bat"),
            o("console", "console", 13f, 1f, 2f, 1f, true),
            o("hatch", "door", 10f, 10f, 2f, 1f, false, "co_exit"),
            o("cl1", "lamp", 6f, 4f, 1f, 1f, false, null, 110f),
            o("cl2", "lamp", 15f, 5f, 1f, 1f, false, null, 110f)
        )
        return Scene("comms", "Signal Hill Facility", "comms", 0.80f, true, m.t, objs, 12.5f, 8.5f,
            Amb(0f, 0f, 0.5f, 0.30f))
    }

    val scenes: Map<String, Scene> = listOf(
        buildStation(), buildHarbour(), buildLighthouse(), buildHospital(), buildCemetery(), buildComms()
    ).associateBy { it.id }

    val locations: List<LocationInfo> = listOf(
        LocationInfo("station", "Station 47", "station", null, 0.50f, 0.55f,
            "Your radio station on the cliff above Greyhaven."),
        LocationInfo("harbour", "Greyhaven Harbour", "harbour", "harbour_open", 0.82f, 0.50f,
            "A closed fishing harbour. The Harbour Office light is still on."),
        LocationInfo("lighthouse", "Greyhaven Light", "lighthouse", "lighthouse_open", 0.55f, 0.15f,
            "A decommissioned lighthouse whose beam keeps turning."),
        LocationInfo("hospital", "Greyhaven General", "hospital", "hospital_open", 0.22f, 0.82f,
            "An abandoned hospital. Its wards are said to be empty."),
        LocationInfo("cemetery", "St. Brannoc's Yard", "cemetery", "cemetery_open", 0.25f, 0.25f,
            "A small chapel yard kept by Sister Agnes."),
        LocationInfo("comms", "Signal Hill Facility", "comms", "comms_open", 0.52f, 0.88f,
            "An underground communications facility beneath Signal Hill.")
    )

    fun location(id: String): LocationInfo = locations.first { it.id == id }

    fun bearingOf(loc: LocationInfo): Float {
        val st = location("station")
        val dx = loc.mapX - st.mapX
        val dy = loc.mapY - st.mapY
        var deg = Math.toDegrees(Math.atan2(dx.toDouble(), (-dy).toDouble())).toFloat()
        if (deg < 0f) deg += 360f
        return deg
    }

    // ------------------------------------------------------------------ night intro text

    fun introLines(night: Int, flags: Set<String>): List<String> {
        val out = ArrayList<String>()
        when (night) {
            1 -> {
                out.add("You arrive at Station 47, on the cliff above Greyhaven.")
                out.add("The job is simple. Monitor the emergency bands. Log everything. Report anything unusual.")
                out.add("Start at the radio desk. Drag the dial to tune, aim the antenna, and listen for the static to thin.")
            }
            2 -> {
                out.add("Fog has come in off the sea.")
                if (flags.contains("answered_marta")) out.add("The log holds Marta Voss's call, and a line you don't remember writing: 'Thank you.'")
                else if (flags.contains("ignored_marta")) out.add("The answering machine shows one missed call. No number.")
                else out.add("Whatever called last night is not finished with you.")
                if (flags.contains("harbour_open")) out.add("The harbour is on the map now.")
            }
            3 -> {
                out.add("Wind rattles the antenna mast. Bearings will drift tonight.")
                if (flags.contains("concealed_callum")) out.add("The Harbour Office log is blank. As agreed.")
                else if (flags.contains("reported_callum")) out.add("A note arrives under the door: 'Reports are a mistake.' Unsigned.")
                out.add("A new tone is repeating from the north.")
            }
            4 -> {
                out.add("Rain drums the roof. The generator is running hard.")
                if (flags.contains("concealed_wren")) out.add("Wren's light is still turning. She has not spoken to anyone else.")
                else if (flags.contains("reported_wren")) out.add("The lighthouse beam has gone dark. Wren's frequency is silent.")
                out.add("Greyhaven General has begun to call. Dr. Calder's voice is perfectly calm.")
            }
            else -> {
                out.add("The storm has arrived.")
                out.add("Every frequency is bleeding into every other. Tonight the signals won't wait.")
                if (flags.contains("comms_open")) out.add("Something under Signal Hill is waiting for you.")
                out.add("Whatever you decide tonight, decide it before dawn.")
            }
        }
        return out
    }

    // ------------------------------------------------------------------ endings

    class Ending(val id: String, val title: String, val pages: List<String>)

    val endings: Map<String, Ending> = listOf(
        Ending("truth", "THE TRUTH", listOf(
            "You patch every recording into the transmitter and open the line. Not to the thing in the fog. To everyone else.",
            "Marta's voice. Tom's. Wren's. The ward ledger numbers, read out one by one. Every receiver that has ever been tuned to this band hears the names.",
            "By dawn the signal is dead. The lighthouse beam stops turning for the first time in thirty years. It hangs pale over the empty harbour.",
            "Vans come up the coast road at noon. People climb out holding photographs. Someone finds the stone with your name on it, and it has already begun to weather.",
            "Greyhaven will be remembered as what it was. You write the last entry in the log, sign it Elias Ward, Operator, Station 47, and for the first time the date is correct."
        )),
        Ending("silence", "THE SILENCE", listOf(
            "You find the master breaker behind the transmitter and pull it. Then you cut the cable, to be sure.",
            "The hum that has lived in the walls goes out like a held breath. Rain on the roof. Your own pulse. Nothing else.",
            "Far off, a voice says your name, and the static takes it. You don't answer. You don't answer. You don't answer.",
            "At dawn you walk down to the harbour with your bag. The boats are still gone. So is Greyhaven's voice.",
            "Years later you will still wake at 02:13, listening. The silence is quiet in a way that costs something. You keep it anyway."
        )),
        Ending("transmission", "THE TRANSMISSION", listOf(
            "You key the microphone. 'Station 47. Go ahead. I'm listening.'",
            "The static opens like a door. It isn't a voice. It's the shape where a voice would be. It knows your name, and now it is learning how to say it.",
            "Marta laughs somewhere in the signal. Tom coughs. Wren hums a lighthouse tune, off-key. They were never lost. They were waiting for someone to pick up.",
            "You talk until the sky pales. The harbour lights come on, one by one, and nobody is there to see them but you.",
            "When the sun rises you are still at the desk. The radio is warm. The log says the shift is over. You write one word: 'Continuing.'"
        )),
        Ending("loop", "THE LOOP", listOf(
            "The generator coughs and the room fills with a pale, familiar amber.",
            "A voice on the speaker: 'Station 47, do you copy? Please respond.' You know it before it finishes. It's yours.",
            "You lean in. You say what you have said before. You will say it forty times, and each time there will be a morning when you wake at the desk and the log is blank.",
            "A new operator arrives with the dawn, a thermos under one arm, whistling. By the cemetery gate he finds an old stone with a name he does not recognise.",
            "That night, on the desk, the radio crackles. 'Station 47, do you copy?' ... 'It's happening again.'"
        ))
    ).associateBy { it.id }
}
