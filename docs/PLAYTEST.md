# NULL ARCHIVE — Prototype Playtest

This is an early mechanics prototype. It includes a conditional ending interaction, but not the generated archive map, custom textures, or a journal GUI.

## Quick test (creative mode)

1. Enter a world and open the NULL ARCHIVE creative tab.
2. Take the Field Recorder. Right-click to play tapes in order.
3. Take Evidence Tape, Damaged Photograph, Memory Fragment, or Sealed Document. Right-click one to catalogue it; the item is consumed and the evidence counter increases (up to five).
4. Use the recorder again. Later tapes require evidence.
5. Right-click Memory Block for terminal output.
6. Right-click Sealed Door and Rift Frame to see phase-gated messages.
7. Right-click Static Block, Null Block, or Corrupted Stone after the second tape to trigger escalating anomaly messages.
8. Right-click the EMF Reader, Null Shard, Archive Key, Rift Key, and Terminal Cartridge to hear their current prototype responses.\n9. For the ending: play all six tapes, catalogue five evidence fragments, then right-click the Rift Frame while holding the Rift Key. In creative mode the key is not consumed.

## Known limitations

- Progress and evidence are stored in player persistent data; there is no reset/journal UI yet.
- Evidence items currently catalogue themselves when used, not when discovered in the world.
- Most interactions remain text-and-sound prototypes; the Rift Key can trigger a persistent ending flag and message, but does not animate or physically close the rift.
- The sound registry currently uses vanilla sound assets.
- CI build must pass for the latest commit before calling this prototype verified.
