# THE LAST FREQUENCY

An atmospheric radio-decoding mystery for Android (landscape). You are Elias Ward, night operator at Station 47 above the fishing town of Greyhaven. Something is calling on the emergency bands.

Everything is generated in code: all pixel art is drawn at runtime and all sound (rain, wind, sea, generator hum, voices, Morse, tones, music) is synthesised. There are no asset files and no third-party libraries.

## How to play

- **Radio desk** (walk to it in the station, tap USE): drag the dial strip to tune, use the fine buttons, aim the antenna, set GAIN and SQUELCH, flip FILTER for buried signals. When the signal is clear enough you can decode it, record it (REC) and reply.
- **Investigation board**: clues become cards. Switch LINK on, tap two cards to join them with string. A correct pair unlocks a new place on the map.
- **Map**: travel to unlocked places (costs 25 minutes of the night).
- **Survival**: generator fuel, radio battery and radio condition run down over the night. Flashlight and sprint use energy.
- **Five nights.** Each is 420 game-minutes (23:00 to 06:00). Four endings: Truth, Silence, Transmission, Loop.
- **Saving**: autosaves on each night and when you leave the app. Three manual slots.

Controls: left thumb = joystick, USE / flashlight / RUN buttons on the right, BOOK (journal), MAP and II (pause) at the top. Left-handed layout, text size, text speed, brightness, subtitles, haptics and reduced screen shake are in Settings.

## Build the APK (GitHub Actions)

1. Create an empty GitHub repository.
2. In this folder:
   ```
   git init
   git add .
   git commit -m "The Last Frequency"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main
   ```
3. Open the repo's **Actions** tab. The "Build APK" workflow runs on every push (or press "Run workflow").
4. When it is green, open the run and download the artifact **last-frequency-debug-apk**. Unzip it to get `app-debug.apk`.
5. Copy the APK to your phone, open it, and allow "install from unknown sources" when asked.

Local build (needs Android SDK + Gradle 8.9 + JDK 17): `gradle assembleDebug`.

## Project layout

```
app/src/main/java/com/zak/lastfreq/
  core/        pure Kotlin, no Android imports (tested on the JVM)
    Content.kt     story: characters, clues, links, transmissions, scenes, endings
    GameState.kt   all game state, save format ("TLF1" text), ending logic
    RadioSim.kt    signal clarity, Morse, tone and garble maths
    AudioMixer.kt  sound synthesis
    World.kt       collision and interaction
    Models.kt      data classes and Settings
  App.kt           screen manager, dialogs, storage, night flow
  Art.kt / Gfx.kt  procedural art, weather, lighting, UI widgets
  *Screen.kt       menu, world, radio, journal, board, map, story screens
  MainActivity.kt / GameView.kt / AudioOut.kt   Android glue
tools/
  CoreTest.kt      story integrity, radio, full playthrough, save/load tests
  Smoke.kt         crash fuzz test of every screen (uses stub Android classes)
```

## Scope notes

This is a mobile adaptation of a larger PC design: 5 nights, 6 locations and 4 endings instead of the full set; no key remapping, resolution or vsync options (not meaningful on a phone). Voices are synthesised murmurs with per-character pitch, and every audio puzzle also has a visual form (transcripts, tone symbols, Morse option list) for accessibility.
