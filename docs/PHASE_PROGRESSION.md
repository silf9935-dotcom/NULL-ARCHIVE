# NULL ARCHIVE — Phase Progression

## Recording sequence

The Field Recorder advances the player's personal investigation through six server-authoritative phases. Progress is stored in player persistent data under `nullarchive_phase` and survives relogging and world saves.

| Phase | Recording | Narrative beat |
|---|---|---|
| 1 | Intake | The facility is powered, but its visitor log is unsigned. |
| 2 | Basement | A second set of footsteps mirrors the player. |
| 3 | Observation | The observation glass shows an impossible room. |
| 4 | Incident | The recorder captures speech before it happens. |
| 5 | Breach | The sealed archive door appears open without breaking its seal. |
| 6 | Null | A recording from tomorrow warns the player not to leave. |

Later recordings are gated by the investigation's evidence count. The recorder does not advance beyond phase 6; the Rift Key is used at the Rift Frame to complete the standard ending.

## Evidence catalogue

Eight distinct discoveries can be indexed once per player:

1. Evidence Tape
2. Damaged Photograph
3. Memory Fragment
4. Sealed Document
5. Observation Glass
6. Memory Block terminal query
7. Sealed Door
8. Hidden Records floor tile

Duplicates and repeat visits do not increase the catalogue. The standard ending requires all six recordings and at least five distinct clues. The Room 0 record described below is deliberately separate from these eight clues and does not increase the ending counter.

Legacy player data with the earlier evidence counter is migrated when the catalogue is next updated.

## Instability and Room 0

Investigation actions increase Archive Instability. At 100%, one of four breach events occurs, pressure returns to 35%, and a 60-second breach cooldown begins. The first breach after phase 4 opens an unlisted chamber beyond the Rift Chamber and creates a passage through its east wall.

The centerpiece Null Block in Room 0 contains Record 000. It can be copied once per player when the investigation has reached phase 4 and instability is at least 35%. This is an optional hidden narrative discovery, not a ninth standard evidence clue.

The EMF Reader scans nearby authored signals and mutated anomaly blocks within roughly eight blocks. It can detect a nearby anomaly even when no normal room landmark is in range; after Room 0 opens, its centerpiece receives a dedicated Room 0 reading. The field journal reports whether Room 0 is unopened, open, or already copied.

## Optional Null Shard resonance

The Null Shard can be used on four anomaly block types within the tracked facility: Corrupted Stone (phase 2), Static Block (phase 3), Black Glass (phase 4), and Null Block (phase 4). Each type is indexed once in a separate four-bit player flag; the first successful resonance adds 4 instability but does not catalogue standard evidence or consume the shard. Repeats provide feedback only. The Room 0 centerpiece and completed ending seal retain their dedicated interactions. The journal reports the optional resonance count, and both `/nullarchive reset` and `/nullarchive generate` clear the resonance flags.

## Replay and authority

- `/archivejournal` shows recording progress, all eight standard clue slots, ending requirements, access states, Room 0 state, and the current next-step hint.
- `/nullarchive status` reports the player's progression, instability, breach count, access state, and ending state (permission level 2).
- `/nullarchive reset` resets player progression and investigation triggers. It retains the generated facility coordinates and restores the Sealed Door, Room 0 passage, and known final-tape Rift mutations where applicable; unrelated ambient world mutations remain.
- `/nullarchive generate` builds a new seven-room facility around the command executor and moves them into Intake. It replaces blocks in its footprint and creates new item drops, so rerunning it duplicates drops.

The server owns progression changes. Client-side interactions do not write player progress. The current implementation remains a mechanics prototype: the journal is chat-based, the facility is command-generated, and the textures/audio are not yet a finished art pass.
