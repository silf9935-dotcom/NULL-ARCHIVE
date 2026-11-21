# NULL ARCHIVE

A psychological-horror / ARG Minecraft mod for **Minecraft 1.21.1**, **Minecraft Forge 52.x**, and **Java 21**.

> The archive is not corrupted. It is correcting itself.

## Development status

**Playable mechanics prototype — not a release build.** The mod currently has a six-stage field recorder progression, persistent player investigation data, eight distinct clue types, a command-built seven-room facility, a text field journal, a directional EMF scanner, anomaly interactions, phase-gated access, and a conditional ending. Core mechanics are implemented server-side, but gameplay/build verification has not been completed in this environment.

Not yet implemented: automatic world generation, custom textures/models beyond placeholders, a graphical journal, a cinematic ending, and a polished audio/visual experience.

## Requirements

- Java 21 JDK
- Gradle 8.8 installed locally (the repository currently has no Gradle wrapper)
- Minecraft Forge 52.x for Minecraft 1.21.1

## Build

Run from the project root:

```bash
gradle build
```

The distributable JAR should be written to `build/libs/`. Build success is not currently verified; please report compiler or runtime errors as issues.

## Quick start

1. Launch a Forge 1.21.1 world with the mod installed.
2. Use `/nullarchive generate` (operator permission required) in an open area to build the test facility around you. **This replaces blocks in its footprint and drops test items; use a disposable world.**
3. Start in Intake and use the Field Recorder. Follow the six recording phases and catalogue clues.
4. Use `/archivejournal` to review tape progress, all eight clue slots, and the ending requirements.
5. Explore with the EMF Reader. It reports nearby phase-unlocked signals and rough direction; scanning does not count as evidence.
6. Complete all six recordings and collect at least five unique clues. Use the Rift Key on the Rift Frame to trigger the prototype ending.

Admin commands:
- `/nullarchive generate` — create the seven-room test facility at your location.
- `/nullarchive status` — show your current progression and ending state.
- `/nullarchive reset` — reset player investigation progress and event triggers. World blocks are not restored.
- `/archivejournal` — player-accessible investigation journal.

## Prototype mechanics

- Six recordings with phase and evidence progression gates.
- Eight distinct clue flags with legacy evidence-counter migration.
- Command-built seven-room facility with curated item drops and room landmarks.
- Persistent environmental changes and one-time proximity anomalies, including stalking cues and false passages.\n- Archive Instability (0–100): tape playback, unique evidence, EMF scans, access grants, and Rift scans raise pressure; thresholds warn the player. At 100, a breach triggers portal/smoke particles and one of four narrative events, then drops pressure to 35 with a 60-second cooldown.
- Intake log with a delayed reveal, phase-specific archive inscriptions, and a hidden Records clue.
- Memory terminal requires a cartridge and two indexed clues; its query can register a clue.
- Sealed Door requires phase 4, three clues, and an Archive Key on first access.
- EMF scanner reads authored landmarks and nearby anomaly blocks, with signal strength and rough direction.
- Rift scan and ending interaction; ending requires all six tapes and at least five of eight clues.

See [playtest instructions](docs/PLAYTEST.md), [phase progression](docs/PHASE_PROGRESSION.md), and [facility blueprint](docs/FACILITY_LAYOUT.md).

## Project layout

- `src/main/java` — mod code
- `src/main/resources/META-INF/mods.toml` — Forge metadata
- `src/main/resources/assets/nullarchive` — language, models, sound definitions, and textures
- `docs/` — design, playtest, and implementation notes

## License

All rights reserved unless a license is added to this repository.
