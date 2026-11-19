# NULL ARCHIVE — Facility Layout Blueprint

Target playtime: approximately 90–120 minutes. The first playable map should be a compact, hand-authored facility rather than random maze generation.

## Spatial plan

Build around a central spine corridor (3 blocks wide), with six zones connected in order. Keep a visible return route so the player understands the layout before it becomes unreliable.

| Zone | Approx. footprint | Purpose | Gate |
|---|---:|---|---|
| Intake | 15 × 13 | Spawn, orientation, visitor log, recorder tutorial | Open |
| Records | 19 × 15 | First evidence, filing shelves, tape playback | Tape 1 |
| Basement | 17 × 17 | Utility loop, EMF tutorial, footsteps anomaly | Tape 2 |
| Observation | 19 × 15 | Glass room, impossible reflection, photograph | Tape 3 |
| Incident Lab | 21 × 17 | Terminal, memory blocks, incident tape | Tape 4 + 3 evidence |
| Sealed Wing | 17 × 17 | Sealed door, archive key, Null Shard | Tape 5 |
| Rift Chamber | 21 × 21 | Rift frame, final evidence, ending | Tape 6 + 5 evidence + Rift Key |

## Building language

- Palette: archive concrete, dark glass, iron, pale tile, restrained warning red.
- Ceiling height: 4 blocks in public/records areas; 5 in labs and the Rift Chamber.
- Use a 3-block-wide corridor; place occasional alcoves to prevent long, empty sightlines.
- Avoid jumpscares as the primary mechanic. Use silence, repeated geometry, impossible room dimensions, delayed audio and changed signage.
- Add service routes behind walls for staged events; never trap the player without a clearly discoverable escape.
- Each zone needs one landmark visible from its entrance and one optional clue.

## Evidence placement

1. Visitor log fragment — Intake; establishes the player is already registered.
2. Damaged photograph — Observation; depicts a room from a different angle.
3. Evidence tape — Records; playback includes a second voice.
4. Memory fragment — Basement; reveals a missing staff member.
5. Sealed document — Incident Lab; contains tomorrow's date.
6. Null shard — Sealed Wing; points toward the Rift Chamber.

The prototype currently allows catalogue items to be used directly. In the map implementation, replace this with discovery interactions and retain the catalogue count as the progression gate.

## Encounter schedule

- Intake: tutorial only, no hostile encounter.
- Records: one environmental anomaly after tape 1.
- Basement: footsteps stop when the player stops; no entity chase.
- Observation: reflection mismatch, then room appears empty.
- Incident Lab: recorder plays a future line; terminal gives partial truth.
- Sealed Wing: door appears open in sound but remains visually closed.
- Rift Chamber: audio distortion builds; final key interaction ends the sequence.

## Implementation order

1. Greybox floor, walls, rooms, doors and landmarks; make the route navigable.
2. Place evidence and interactable blocks; verify gates in sequence.
3. Add lighting and sightline passes.
4. Add anomaly triggers and audio timing.
5. Add custom textures/audio and final polish.
6. Test a clean survival playthrough and a creative debug route.

This blueprint is design documentation only. It does not claim that world generation or a playable facility has been implemented.
