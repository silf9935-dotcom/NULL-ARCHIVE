# NULL ARCHIVE — Phase Progression

## First playable loop

The Field Recorder is the first narrative progression item. Right-clicking it on the server advances the player's personal archive phase and displays the next tape transcript. Progress is stored in the player's persistent NBT under `nullarchive_phase`, so it survives relogging and world saves.

| Phase | Tape | Narrative beat |
|---|---|---|
| 1 | Intake | The facility is powered, but its visitor log is unsigned. |
| 2 | Basement | A second set of footsteps mirrors the player. |
| 3 | Observation | The observation glass shows an impossible room. |
| 4 | Incident | The recorder captures speech before it happens. |
| 5 | Breach | The sealed archive door appears open without breaking its seal. |
| 6 | Null | A recording from tomorrow warns the player not to leave. |

After phase 6, the recorder reports SIGNAL LOST and does not advance further.

## Design constraints

- Phase state is per-player, not global, so co-op investigations do not overwrite one another.
- The server owns phase changes; client-side right-clicks never mutate progression.
- The current implementation is intentionally a vertical-slice prototype. Future milestones will replace direct tape messages with evidence checks, terminal flags, anomaly triggers, and cinematic sequences.
- Phase state currently has no in-game reset item or admin command. Use a test profile/world when validating the progression loop.
