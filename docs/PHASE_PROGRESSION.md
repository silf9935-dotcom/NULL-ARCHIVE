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

The centerpiece Null Block in Room 0 contains Record 000. It can be copied once per player when the investigation has reached phase 4 and instability is at least 35%. This is an optional hidden narrative discovery, not a ninth standard evidence clue. After all six tapes have been recorded and Record 000 copied, using the Field Recorder while physically inside Room 0 plays a separate unlisted afterword once. It does not advance the phase, add evidence, consume the recorder, or increase instability; repeat use only confirms that the track is already copied. The journal reports the playback as locked until tape 6, ready, or complete, and suggests it as an afterword after the standard ending.

The EMF Reader scans nearby authored signals and mutated anomaly blocks within roughly eight blocks. It can detect a nearby anomaly even when no normal room landmark is in range; after Room 0 opens, its centerpiece receives a dedicated Room 0 reading. The field journal reports whether Room 0 is unopened, open, or already copied.

## Room-specific ambient mutations

The proximity events are anchored to the room layout rather than changing arbitrary nearby blocks. Each fires once per investigation after its phase gate is reached:

- **Basement (phase 2):** the north-wall pane at origin +15, y +1..2, z 0 becomes Black Glass.
- **Observation (phase 3):** the center block of the divider at origin +26, y +2, z +4 becomes Black Glass.
- **Incident Lab (phase 4):** a 3×3 floor patch at origin +36..38, y 0, z +4 becomes Static Blocks.
- **Sealed Wing (phase 4):** a false passage appears in the north wall at origin +59, y +1..3, z 0, with a Null Block in its center.
- **Rift Chamber (phase 5):** a Null Block appears beside the frame at origin +73, y +1, z +4.

These mutations are persistent world changes until reset. The /nullarchive reset command restores their generated baseline, including the Incident Lab's original concrete center floor block. For compatibility, reset also clears the former Sealed Wing false-passage position used by older builds.

## Optional Null Shard resonance

The Null Shard can be used on four anomaly block types within the tracked facility: Corrupted Stone (phase 2), Static Block (phase 3), Black Glass (phase 4), and Null Block (phase 4). Each type is indexed once in a separate four-bit player flag; the first successful resonance adds 4 instability but does not catalogue standard evidence or consume the shard. Repeats provide feedback only. The Room 0 centerpiece and completed ending seal retain their dedicated interactions. The journal reports the optional resonance count, and both `/nullarchive reset` and `/nullarchive generate` clear the resonance flags.

## Optional Observer trace capture

The Observer's phase-gated silhouette can be recorded with the Field Recorder while its three-beat encounter is active and the saved position is within twelve blocks. A successful capture ends that encounter, stores one phase bit for phases 3–6, adds 5 Archive Instability, and increments a separate 0–4 optional trace index. The recorder is not consumed, the six-tape sequence does not advance, and these traces are not standard evidence clues. Each phase's encounter can be captured only once; if the Observer disappears or its sequence expires, that phase's capture opportunity is gone for the current investigation. /archivejournal and /nullarchive status report the count. Both reset and facility generation clear the capture flags.

## Overworld field trace capture

The separate Overworld anomaly layer now leaves a short-lived, investigable residue after each ambient event. It runs only in the Overworld; the per-player playtime threshold advances only while the player is there. The first 2,400 Overworld ticks are quiet, after which rare cosmetic events can occur. Each event stores its type, block position, dimension, and a 600-tick expiry in player persistent data.

The investigation uses two existing devices in sequence:

1. Use the EMF Reader within eight blocks of the active residue. It reports a strength reading and rough direction, then marks that event as localized.
2. While still within eight blocks, use the Field Recorder to preserve the trace. If the player has moved too far away, the recorder asks them to approach; if the 600-tick window expires, the residue is discarded.

A successful capture increments the separate `nullarchive_world_trace_count` total and clears the active signal. It does not consume either device, advance the six facility tapes, add to the eight standard clues, or increase facility instability. The trace total appears in `/archivejournal` and `/nullarchive status`. The legacy `/nullarchive reset` command intentionally leaves this Overworld field log and Overworld playtime untouched.

## Replay and authority

- `/archivejournal` shows recording progress, all eight standard clue slots, ending requirements, access states, Room 0 state and optional playback status, Observer Trace capture count, and the current next-step/afterword hint.
- `/nullarchive status` reports the player's progression, instability, breach count, access state, ending state, Room 0 playback state, and optional Observer Trace captures (permission level 2).
- `/nullarchive reset` resets player progression and investigation triggers. It retains the generated facility coordinates. If a facility is tracked, the command refuses to reset from another dimension so it cannot discard the information needed to restore world changes. Run it in the facility's dimension to restore the Sealed Door, Room 0 passage, final-tape Rift mutations, and tracked Basement/Observation/Incident Lab/Sealed Wing/Rift ambient mutations to their generated baseline. Arbitrary player edits and unrelated world blocks are not reverted.
- `/nullarchive generate` builds a new seven-room facility around the command executor and moves them into Intake. It replaces blocks in its footprint and creates new item drops, so rerunning it duplicates drops.

The server owns progression changes. Client-side interactions do not write player progress. The current implementation remains a mechanics prototype: the journal is chat-based, the facility is command-generated, and the textures/audio are not yet a finished art pass.
