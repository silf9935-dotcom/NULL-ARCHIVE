# NULL ARCHIVE

A psychological-horror / ARG Minecraft mod for **Minecraft 1.21.1**, **Minecraft Forge 52.x**, and **Java 21**.

> The archive is not corrupted. It is correcting itself.

## Development status

This repository is being built in staged, testable milestones. The initial commit establishes the Forge project and technical conventions; gameplay systems will be added incrementally.

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
./gradlew build
```
On Windows:
```bat
gradlew.bat build
```

The distributable JAR is written to `build/libs/`.

## Project layout
- `src/main/java` — mod code
- `src/main/resources/META-INF/mods.toml` — Forge metadata
- `src/main/resources/assets/nullarchive` — language, models, sounds, and textures
- `docs/` — design and implementation notes

## License
All rights reserved unless a license is added to this repository.
