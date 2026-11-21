# NULL ARCHIVE — Implementation Roadmap

## Milestone 1 — Foundation
- [x] Forge 1.21.1 / Java 21 project layout
- [x] Mod entrypoint and deferred registries
- [x] Initial block, item, and creative-tab registrations
- [x] JSON blockstates and model files for initial content
- [x] GitHub Actions build workflow
- [ ] Add Gradle wrapper
- [ ] Confirm Forge client launch and in-game registration

## Milestone 2 — World and atmosphere
- [ ] Custom archive structure and placement
- [ ] Spatial rift behavior and safe dimension transition
- [ ] Custom texture and sound assets
- [ ] Environmental anomaly controller
- [x] Sound event registry prototype

## Milestone 3 — Evidence and ARG
- [x] Field recorder with six narrative messages
- [x] Distinct evidence catalogue (four item types + three environmental clues)
- [x] Phase-gated tape playback
- [x] Memory terminal interaction
- [x] EMF reader with phase-sensitive readings
- [x] Sealed access and rift monitor interactions
- [x] Stateful sealed door with phase/evidence gate, collision, and open/close sound
- [x] Conditional Rift Key ending flag (six tapes + five distinct clues)
- [ ] Journal UI with readable clue records
- [ ] Persistent progression graph and quest objectives
- [ ] Reset/test command for progression

## Milestone 4 — Horror systems
- [x] Archive Instability pressure model (0–100), event thresholds, breach cooldown, and non-repeating event selection
- [ ] Sanity / anomaly pressure model
- [ ] Hallucination events with accessibility controls
- [ ] Client-side visual distortion effects
- [ ] Audio cues and entity encounters
- [ ] Escalating anomaly schedule

## Milestone 5 — Content and polish
- [ ] Complete localization
- [ ] Original textures, models, blockstates, and sound assets
- [ ] In-game tutorial and journal
- [ ] Balance a full ~2-hour playthrough
- [x] Instability-dependent alternate ending\n- [ ] Endings and replayability

## Engineering rules
- Keep the project on Forge, not NeoForge.
- Keep gameplay progression deterministic where possible; randomness may affect presentation, not critical evidence.
- Never make a required clue permanently missable.
- All client-only rendering code must be isolated from dedicated-server loading.
- Server-side events own progression changes.
