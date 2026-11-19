# NULL ARCHIVE — Prototype Playtest

This is an early mechanics prototype. It does not yet include the generated archive map, custom textures, a journal GUI, or a complete ending.

## Quick test (creative mode)

1. Enter a world and open the NULL ARCHIVE creative tab.
2. Take the Field Recorder. Right-click to play tapes in order.
3. Take Evidence Tape, Damaged Photograph, Memory Fragment, or Sealed Document. Right-click one to catalogue it; the item is consumed and the evidence counter increases (up to five).
4. Use the recorder again. Later tapes require evidence.
5. Right-click Memory Block for terminal output.
6. Right-click Sealed Door and Rift Frame to see phase-gated messages.
7. Right-click Static Block, Null Block, or Corrupted Stone after the second tape to trigger escalating anomaly messages.
8. Right-click the EMF Reader, Null Shard, Archive Key, Rift Key, and Terminal Cartridge to hear their current prototype responses.

## Known limitations

- Progress and evidence are stored in player persistent data; there is no reset/journal UI yet.
- Evidence items currently catalogue themselves when used, not when discovered in the world.
- Interactions are text-and-sound prototypes; no actual door state changes, rift teleportation, or anomaly visuals.
- The sound registry currently uses vanilla sound assets.
- CI build must pass for the latest commit before calling this prototype verified.
