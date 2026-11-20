# NULL ARCHIVE

A psychological-horror / ARG Minecraft mod for **Minecraft 1.21.1**, **Minecraft Forge 52.x**, and **Java 21**.

> The archive is not corrupted. It is correcting itself.

## Development status

**Current status: early playable mechanics prototype.** The project has registered archive blocks/items, six server-side recorder phases, persistent player progression, EMF/anomaly responses, distinct evidence cataloguing, phase-gated terminal/door/rift interactions, and a conditional ending flag. It does not yet include a generated facility, custom art/audio, journal GUI, cinematic ending, or a verified release build.

### Target experience
- Approximately 2 hours for a first playthrough
- Six progression phases, with discoveries that change how earlier evidence is interpreted
- Environmental storytelling, anomalous recordings, terminals, spatial anomalies, and controlled hallucination effects
- Cinematic pacing without making the core progression depend on random chance

## Requirements
- Java 21 (JDK, not just a JRE)
- Gradle 8.8 or the included Gradle wrapper
- Minecraft Forge 52.x for Minecraft 1.21.1

## Build
```bash
gradle build
```
On Windows:
```bat
gradle build
```

The distributable JAR is written to `build/libs/`. A Gradle wrapper will be added after the first CI-validated build; until then, run `gradle build` with Gradle 8.8 installed.

## Prototype mechanics
- Recorder progression: six tapes, with evidence gates on later playback.
- Evidence index: four item types and three environmental clues; each clue counts once per player.
- Persistent player data and legacy evidence-counter migration.
- Phase-gated interactions for the memory terminal, sealed door, rift frame, anomaly blocks, and investigation tools.
- Rift Key ending interaction requires all six tapes and at least five distinct evidence clues.

See [playtest instructions](docs/PLAYTEST.md), [phase progression](docs/PHASE_PROGRESSION.md), and [facility blueprint](docs/FACILITY_LAYOUT.md).

## Project layout
- `src/main/java` — mod code
- `src/main/resources/META-INF/mods.toml` — Forge metadata
- `src/main/resources/assets/nullarchive` — language, models, sounds, and textures
- `docs/` — design and implementation notes

## License
All rights reserved unless a license is added to this repository.
