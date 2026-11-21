# NULL ARCHIVE — Prototype Playtest

This is an early mechanics prototype. It includes a command-built seven-room archive layout and a conditional ending interaction, but not automatic world generation, custom textures, or a journal GUI.

## Quick test (creative mode)

1. Enter a world and open the NULL ARCHIVE creative tab. Use `/archivejournal` at any time to view tape progress, the eight distinct clue slots, ending requirements, Sealed Door unlock state, Rift scan state, and a phase-specific next-step hint (or a completion message after the ending).
2. Take the Field Recorder. Right-click to play tapes in order. Each successful playback confirms the recording and announces the next available phase; from tape 3 onward, the archive pulse accompanies phase advancement.
3. At the Intake counter, right-click the concrete block at the counter's center (relative position +4, +1, +2 from the generated facility origin) after tape 1 to reveal a one-time visitor log. The entry changes after tape 3; if discovered earlier, return after tape 3 for the one-time reveal.
4. Right-click Evidence Tape, Damaged Photograph, Memory Fragment, and Sealed Document to catalogue each item type once. Duplicates do not increase progress; the first catalogue consumes one item.
5. Use the recorder again. Later tapes require evidence.
6. Right-click Observation Glass after tape 2 to record a clue. Right-click Memory Block after tape 2 to inspect the terminal; the query is locked without a cartridge. With at least two indexed clues, use a Terminal Cartridge on Memory Block to query `/SUBJECT/NULL/EXIT_LOG.txt` and record the terminal clue; the cartridge is consumed outside creative only for the first successful query. Repeated queries do not consume duplicates.
7. After tape 4 and three clues, use Archive Key on Sealed Door for the first unlock (consumed outside creative), then right-click to open/close it physically. The first unlock plays an archive pulse. Denied access before tape 4, without three clues, or without the key now has distinct sound feedback. The first access also catalogues the seal clue.
8. In the Records room (room 5), after tape 4 and at least two indexed clues, right-click the floor tile at the hidden shelf position to discover a one-time hidden record (clue 8).
9. Right-click Static Block, Null Block, or Corrupted Stone after the second tape to trigger escalating anomaly messages. Interactions have a 2-second cooldown to prevent spam; the escalation is player-wide. Right-click Archive Tile or Archive Concrete once per tape phase to discover phase-specific inscriptions; each phase's inscription is shown only once per player.
10. Use the EMF Reader while exploring: it scans phase-unlocked facility signals within roughly eight blocks, including nearby mutated anomaly blocks, reports strength from 1/5 to 5/5 and rough direction (ahead/behind, left/right, above/below), and gives room-specific readings. It does not catalogue clues. Also right-click the Null Shard, Archive Key, Rift Key, and Terminal Cartridge for their prototype responses.
11. After generating the facility, proximity events trigger in Observation, Basement, Incident Lab, Sealed Wing, and Rift Chamber as you advance phases. Each event is once per generated layout.
12. At phase 5 or later, right-click the Rift Frame once for a one-time resonance scan; it adds narrative feedback but does not count as evidence. For the ending: play all six tapes, catalogue at least five distinct clues out of eight, then right-click the Rift Frame while holding the Rift Key. In creative mode the key is not consumed.

## Admin test commands

- `/nullarchive generate` — builds a compact seven-room facility around the player (permission level 2), then places you inside Intake facing the corridor. Run in an open area; it replaces blocks in its footprint and drops curated progression items. Rooms now have landmarks: Intake counter, Basement ceiling strips, Observation glass divider, Incident Lab anomaly floor, Records shelves, Sealed Wing warning floor, and Rift Chamber corrupted cross. It is a command-built prototype, not automatic world generation. Start in Intake and follow the corridor. The two-block sealed threshold is in the connector before Rift Chamber; first access requires tape 4, three clues, and the Archive Key.
- `/archivejournal` — player-accessible field journal: recording progress, distinct clue checklist, and ending requirements.
- `/nullarchive status` — shows tape phase, evidence count, anomaly count, Sealed Door unlock state, Rift scan state, and ending state (permission level 2).
- `/nullarchive reset` — resets player progression, phase inscriptions, clue interactions, anomaly/stalking triggers, their cooldowns, and facility tracking for a fresh test. If run in the generated facility's dimension, it closes the two-block Sealed Door. When the ending was completed, it also restores the known final-tape Rift wall/floor changes and the sealed Rift Frame to their generated baseline, enabling a replay. Other world blocks and ambient anomaly mutations remain.
- `/nullarchive generate` also clears facility-specific interaction flags so a newly generated layout can be investigated again; catalogue progress remains with the player.

- The generator places Field Recorder, EMF Reader, evidence items, Terminal Cartridge, Archive Key, Rift Key, and Null Shard as item drops at room landmarks. Running the command again duplicates these drops. Ambient events are proximity-triggered, phase-gated, and each fires once per generation. From tape 3 onward, a phase-specific stalking cue can play a chime behind the player and report footsteps in the empty corridor; it fires once per phase while inside the facility. Observation permanently changes a wall section to black glass; Basement and Incident Lab mutate floor patches into anomaly blocks; the Sealed Wing event turns a side-wall section into black glass with a Null Block embedded in it (a false passage); the Rift event adds a Null Block. These effects persist in the world. Playing tape 6 also permanently rewrites the Rift Chamber: Null Blocks breach the wall and corrupted stone spreads across the floor.

## Known limitations

- Progress and evidence are stored in player persistent data; there is no reset/journal UI yet.
- Evidence is now tracked by distinct item/clue flags: four evidence item types plus Observation Glass, Memory Block, Sealed Door, and the hidden Records room clues. The Rift ending still requires five distinct clues; duplicates cannot be farmed.
- Sealed Door now has a persistent open/closed state and changes collision so the player can pass; the open state uses an iron-bars visual placeholder.
- The EMF Reader is a proximity scanner prototype: it detects phase-gated landmarks within an 8-block radius and reports signal strength plus a rough direction along the facility corridor. It is not precise navigation and does not add evidence.
- Most other interactions remain text-and-sound prototypes. The Rift Key ending now physically seals the clicked frame with Black Glass, leaves a Null Block above, and changes the floor below to Corrupted Stone. Interacting with that seal after the ending returns a short confirmation response. This is a static seal, not an animation.
- Playing tape 6 triggers the final room-shift mutation once per playback (repeated playback is phase-locked). The sound registry currently uses vanilla sound assets.
- CI build must pass for the latest commit before calling this prototype verified.
