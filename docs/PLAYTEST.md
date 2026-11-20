# NULL ARCHIVE — Prototype Playtest

This is an early mechanics prototype. It includes a command-built seven-room archive layout and a conditional ending interaction, but not automatic world generation, custom textures, or a journal GUI.

## Quick test (creative mode)

1. Enter a world and open the NULL ARCHIVE creative tab.
2. Take the Field Recorder. Right-click to play tapes in order.
3. Right-click Evidence Tape, Damaged Photograph, Memory Fragment, and Sealed Document to catalogue each item type once. Duplicates do not increase progress; the first catalogue consumes one item.
4. Use the recorder again. Later tapes require evidence.
5. Right-click Observation Glass after tape 2 to record a clue. Right-click Memory Block after tape 2 to record a separate terminal clue and read its output.
6. After tape 4 and three clues, use Archive Key on Sealed Door for the first unlock (consumed outside creative), then right-click to open/close it physically. The first access also catalogues the seal clue. Before the gate, it reports denied access.
7. Right-click Static Block, Null Block, or Corrupted Stone after the second tape to trigger escalating anomaly messages.
8. Right-click the EMF Reader, Null Shard, Archive Key, Rift Key, and Terminal Cartridge to hear their current prototype responses.
9. After generating the facility, proximity events trigger in Observation, Basement, Incident Lab, Sealed Wing, and Rift Chamber as you advance phases. Each event is once per generated layout.
10. For the ending: play all six tapes, catalogue at least five distinct clues out of seven, then right-click the Rift Frame while holding the Rift Key. In creative mode the key is not consumed.

## Admin test commands

- `/nullarchive generate` — builds a compact seven-room facility around the player (permission level 2). Run in an open area; it replaces blocks in its footprint and drops curated progression items. It is a command-built prototype, not automatic world generation. Start in Intake and follow the corridor. The two-block sealed threshold is in the connector before Rift Chamber; first access requires tape 4, three clues, and the Archive Key.
- `/nullarchive status` — shows tape phase, evidence count, anomaly count, and ending state (permission level 2).
- `/nullarchive reset` — resets player progression for a fresh test. It does not reset world blocks or the door's physical state.

- The generator places Field Recorder, EMF Reader, evidence items, Terminal Cartridge, Archive Key, Rift Key, and Null Shard as item drops at room landmarks. Running the command again duplicates these drops. Ambient events are proximity-triggered, phase-gated, and each fires once per generation. From tape 3 onward, a phase-specific stalking cue can play a chime behind the player and report footsteps in the empty corridor; it fires once per phase while inside the facility. Observation permanently changes a wall section to black glass; Basement and Incident Lab mutate floor patches into anomaly blocks; the Sealed Wing event turns a side-wall section into black glass with a Null Block embedded in it (a false passage); the Rift event adds a Null Block. These effects persist in the world. Playing tape 6 also permanently rewrites the Rift Chamber: Null Blocks breach the wall and corrupted stone spreads across the floor.

## Known limitations

- Progress and evidence are stored in player persistent data; there is no reset/journal UI yet.
- Evidence is now tracked by distinct item/clue flags: four evidence item types plus Observation Glass, Memory Block, and Sealed Door clues. The Rift ending still requires five distinct clues; duplicates cannot be farmed.
- Sealed Door now has a persistent open/closed state and changes collision so the player can pass; the open state uses an iron-bars visual placeholder.
- Most other interactions remain text-and-sound prototypes; the Rift Key can trigger a persistent ending flag and message, but does not animate or physically close the rift.
- Playing tape 6 triggers the final room-shift mutation once per playback (repeated playback is phase-locked). The sound registry currently uses vanilla sound assets.
- CI build must pass for the latest commit before calling this prototype verified.
