# NULL ARCHIVE — Implementation Roadmap

## Milestone 1 — Foundation
- [x] Forge 1.21.1 / Java 21 project layout
- [x] Mod entrypoint and deferred registries
- [x] Initial block, item, and creative-tab registrations
- [ ] Validate with a real Gradle build and Forge client launch
- [ ] Add Gradle wrapper

## Milestone 2 — World and atmosphere
- [ ] Custom structures and world placement
- [ ] Spatial rift behavior and safe dimension transition
- [ ] Ambient soundscape and resource assets
- [ ] Environmental anomaly controller

## Milestone 3 — Evidence and ARG
- [ ] Recorder / evidence items
- [ ] Terminal interface and data fragments
- [ ] Persistent player investigation state
- [ ] Six-phase progression graph

## Milestone 4 — Horror systems
- [ ] Sanity / anomaly pressure model
- [ ] Hallucination events with accessibility controls
- [ ] Client-side visual distortion effects
- [ ] Audio cues and entity encounters

## Milestone 5 — Content and polish
- [ ] Complete localization
- [ ] Models, blockstates, textures, and sounds
- [ ] In-game tutorial and journal
- [ ] Balance a full ~2-hour playthrough

## Engineering rules
- Keep the project on Forge, not NeoForge.
- Keep gameplay progression deterministic where possible; randomness may affect presentation, not critical evidence.
- Never make a required clue permanently missable.
- All client-only rendering code must be isolated from dedicated-server loading.
