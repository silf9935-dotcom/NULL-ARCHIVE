# Technical Architecture

## Runtime target
- Minecraft 1.21.1
- Minecraft Forge 52.1.0 (Forge 52.x range)
- Java 21
- Mod ID: `nullarchive`
- Root package: `com.nullarchive`

## Registry ownership
- `ModBlocks`: block registry entries
- `ModItems`: block items and investigation/evidence items
- `ModCreativeTabs`: developer-facing content tab
- `NullArchive`: mod entrypoint and registry event-bus wiring

## Planned subsystem boundaries
- `world`: structures, placement rules, dimension access
- `progression`: six-phase state machine and evidence graph
- `evidence`: recordings, photographs, documents, fragments
- `client`: overlays, hallucination rendering, audio presentation
- `network`: server-authoritative progression synchronization
- `data`: saved investigation state and codecs

Client presentation must never be referenced from common/server initialization. Critical progression evidence must be recoverable and must not depend on a one-time random event.
