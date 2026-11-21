package com.nullarchive;

import com.nullarchive.registry.ModBlocks;
import com.nullarchive.registry.ModItems;
import com.nullarchive.registry.ModSounds;
import com.nullarchive.block.ArchiveDoorBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative gameplay prototype: tapes, evidence, terminals, EMF, and anomaly gates. */
@Mod.EventBusSubscriber(modid = NullArchive.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArchiveEvents {
    private static final String PHASE_KEY = "nullarchive_phase";
    private static final String EVIDENCE_KEY = "nullarchive_evidence";
    private static final String ANOMALY_KEY = "nullarchive_anomaly";
    private static final String EVIDENCE_FLAGS_KEY = "nullarchive_evidence_flags";
    private static final int EVIDENCE_TAPE_FLAG = 1, PHOTO_FLAG = 2, MEMORY_FLAG = 4, DOCUMENT_FLAG = 8;
    private static final int GLASS_FLAG = 16, TERMINAL_FLAG = 32, DOOR_FLAG = 64, HIDDEN_FLAG = 128;

    private static int evidenceCount(CompoundTag data) {
        int flags = data.getInt(EVIDENCE_FLAGS_KEY);
        return flags == 0 ? Math.min(data.getInt(EVIDENCE_KEY), 5) : Integer.bitCount(flags);
    }

    private static boolean catalogueEvidence(CompoundTag data, int flag) {
        int flags = data.getInt(EVIDENCE_FLAGS_KEY);
        if (flags == 0) {
            int legacy = Math.min(data.getInt(EVIDENCE_KEY), 5);
            if (legacy > 0) flags = (1 << legacy) - 1;
        }
        if ((flags & flag) != 0) {
            data.putInt(EVIDENCE_FLAGS_KEY, flags);
            data.putInt(EVIDENCE_KEY, Integer.bitCount(flags));
            return false;
        }
        data.putInt(EVIDENCE_FLAGS_KEY, flags | flag);
        data.putInt(EVIDENCE_KEY, evidenceCount(data));
        return true;
    }

    private static final String[] PHASE_MESSAGES = {
            "TAPE 01 // INTAKE: The lights are on. Nobody signed the visitor log.",
            "TAPE 02 // BASEMENT: A second set of footsteps follows yours. It stops when you stop.",
            "TAPE 03 // OBSERVATION: The glass reflects a room that is not behind you.",
            "TAPE 04 // INCIDENT: The recorder captures your voice ten seconds before you speak.",
            "TAPE 05 // BREACH: The archive door is open. The seal is still intact.",
            "TAPE 06 // NULL: The final recording is dated tomorrow. It says: DO NOT LEAVE."
    };

    private static final String INSTABILITY_KEY = "nullarchive_instability";
    private static final String BREACH_COUNT_KEY = "nullarchive_breach_count";
    private static final String BREACH_COOLDOWN_KEY = "nullarchive_breach_cooldown";
    private static final String LAST_BREACH_KEY = "nullarchive_last_breach";

    private static void openRoomZero(ServerLevel level, BlockPos origin) {
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var concrete = ModBlocks.ARCHIVE_CONCRETE.get().defaultBlockState();
        var tile = ModBlocks.ARCHIVE_TILE.get().defaultBlockState();
        var glass = ModBlocks.BLACK_GLASS.get().defaultBlockState();
        var nullState = ModBlocks.NULL_BLOCK.get().defaultBlockState();
        // Room 0 is built beyond the generated Rift Chamber, only after a breach.
        for (int x = 77; x <= 85; x++) for (int z = 0; z <= 8; z++) {
            boolean wall = x == 77 || x == 85 || z == 0 || z == 8;
            level.setBlock(origin.offset(x, 0, z), tile, 3);
            level.setBlock(origin.offset(x, 4, z), concrete, 3);
            for (int y = 1; y <= 3; y++) level.setBlock(origin.offset(x, y, z), wall ? glass : air, 3);
        }
        // A narrow entrance remains open in the west wall; the rest is sealed.
        for (int y = 1; y <= 2; y++) level.setBlock(origin.offset(77, y, 4), air, 3);
        level.setBlock(origin.offset(81, 1, 4), nullState, 3);
        level.setBlock(origin.offset(81, 0, 4), ModBlocks.CORRUPTED_STONE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(81, 2, 4), glass, 3);
        level.setBlock(origin.offset(81, 3, 4), air, 3);
        level.setBlock(origin.offset(76, 1, 4), air, 3);
        level.setBlock(origin.offset(76, 2, 4), air, 3);
    }

    private static void increaseInstability(Player player, int amount, String source) {
        CompoundTag data = player.getPersistentData();
        int oldValue = data.getInt(INSTABILITY_KEY);
        int value = Math.min(100, oldValue + amount);
        data.putInt(INSTABILITY_KEY, value);
        if (amount > 0 && value > oldValue && ((value >= 25 && oldValue < 25)
                || (value >= 50 && oldValue < 50) || (value >= 75 && oldValue < 75))) {
            player.displayClientMessage(Component.literal("ARCHIVE INSTABILITY // " + value + "% — " + source), false);
        }
        if (value < 100) return;
        long now = player.level().getGameTime();
        if (now < data.getLong(BREACH_COOLDOWN_KEY)) {
            data.putInt(INSTABILITY_KEY, 99);
            return;
        }

        String[] events = {
                "FALSE ROOM // The corridor ahead is longer than the facility blueprint. The door at its end is not there when you approach.",
                "RECORDING ERROR // Your recorder clicks on. A voice says: “Stop collecting evidence. You are the evidence.”",
                "SHADOW TRACE // Footsteps cross behind you. A dark pulse moves through the wall, then vanishes.",
                "MEMORY COLLAPSE // Every inscription nearby seems to have changed. Your journal still remembers the old text."
        };
        int previous = data.getInt(LAST_BREACH_KEY);
        int eventIndex = player.getRandom().nextInt(events.length);
        if (data.getInt(BREACH_COUNT_KEY) > 0 && eventIndex == previous) eventIndex = (eventIndex + 1) % events.length;
        data.putInt(LAST_BREACH_KEY, eventIndex);
        data.putInt(BREACH_COUNT_KEY, data.getInt(BREACH_COUNT_KEY) + 1);
        data.putInt(INSTABILITY_KEY, 35);
        if (data.getBoolean("nullarchive_facility_generated") && !data.getBoolean("nullarchive_room_zero")
                && data.getInt(PHASE_KEY) >= 4 && player.level() instanceof ServerLevel serverLevel
                && data.contains("nullarchive_facility_x")
                && data.getString("nullarchive_facility_dimension").equals(serverLevel.dimension().location().toString())) {
            BlockPos facilityOrigin = new BlockPos(data.getInt("nullarchive_facility_x"),
                    data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
            openRoomZero(serverLevel, facilityOrigin);
            data.putBoolean("nullarchive_room_zero", true);
            player.displayClientMessage(Component.literal("ROOM 0 // Facility map updated. A room has been indexed beyond the Rift Chamber. Its number was not on the original plan."), false);
        }
        data.putLong(BREACH_COOLDOWN_KEY, now + 1200);
        player.displayClientMessage(Component.literal("!!! ARCHIVE BREACH // " + events[eventIndex]), false);
        player.level().playSound(null, player.blockPosition(), ModSounds.SIGNAL_DISTORTION.get(),
                SoundSource.PLAYERS, 1.0F, 0.25F + eventIndex * 0.12F);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
                    36, 0.8, 1.0, 0.8, 0.15);
            serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.8, player.getZ(),
                    12, 0.4, 0.5, 0.4, 0.02);
        }
    }

    private ArchiveEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("archivejournal").executes(context -> {
            Player player = context.getSource().getPlayerOrException();
            CompoundTag data = player.getPersistentData();
            int phase = data.getInt(PHASE_KEY);
            int flags = data.getInt(EVIDENCE_FLAGS_KEY);
            if (flags == 0) flags = (1 << Math.min(data.getInt(EVIDENCE_KEY), 5)) - 1;
            player.displayClientMessage(Component.literal("FIELD JOURNAL // NULL ARCHIVE"), false);
            player.displayClientMessage(Component.literal("Recordings: " + phase + "/6 // Unique clues: " + evidenceCount(data) + "/8 // Instability: " + data.getInt(INSTABILITY_KEY) + "% // Breaches: " + data.getInt(BREACH_COUNT_KEY) + " // Observer encounters " + data.getInt("nullarchive_observer_count")), false);
            player.displayClientMessage(Component.literal((phase >= 1 ? "[RECORDED]" : "[LOCKED]") + " 01 — Evidence Tape"), false);
            player.displayClientMessage(Component.literal((phase >= 2 ? "[RECORDED]" : "[LOCKED]") + " 02 — Basement"), false);
            player.displayClientMessage(Component.literal((phase >= 3 ? "[RECORDED]" : "[LOCKED]") + " 03 — Observation"), false);
            player.displayClientMessage(Component.literal((phase >= 4 ? "[RECORDED]" : "[LOCKED]") + " 04 — Incident"), false);
            player.displayClientMessage(Component.literal((phase >= 5 ? "[RECORDED]" : "[LOCKED]") + " 05 — Breach"), false);
            player.displayClientMessage(Component.literal((phase >= 6 ? "[RECORDED]" : "[LOCKED]") + " 06 — Null"), false);
            String[] clueNames = {"Evidence Tape", "Damaged Photograph", "Memory Fragment", "Sealed Document",
                    "Observation Glass", "Terminal Query", "Sealed Door", "Hidden Records"};
            int[] clueFlags = {EVIDENCE_TAPE_FLAG, PHOTO_FLAG, MEMORY_FLAG, DOCUMENT_FLAG,
                    GLASS_FLAG, TERMINAL_FLAG, DOOR_FLAG, HIDDEN_FLAG};
            for (int i = 0; i < clueNames.length; i++)
                player.displayClientMessage(Component.literal(((flags & clueFlags[i]) != 0 ? "[FOUND]" : "[UNKNOWN]")
                        + " CLUE " + (i + 1) + " — " + clueNames[i]), false);
            player.displayClientMessage(Component.literal("ENDING // " + (phase >= 6 && evidenceCount(data) >= 5
                    ? "Requirements met. Locate the Rift Frame and use the Rift Key."
                    : "Requires all 6 recordings and 5 distinct clues.")), false);
            player.displayClientMessage(Component.literal("ACCESS // Sealed Door: "
                    + (data.getBoolean("nullarchive_door_unlocked") ? "UNLOCKED" : "LOCKED")
                    + " // Rift scan: " + (data.getBoolean("nullarchive_rift_scan") ? "RECORDED" : "NOT RECORDED")), false);
            String nextStep;
            if (data.getBoolean("nullarchive_ending_complete")) nextStep = "COMPLETE // The Rift is sealed. The archive has no further instructions.";
            else if (phase == 0) nextStep = "NEXT // Use the Field Recorder to begin the Intake recording.";
            else if (phase == 1) nextStep = "NEXT // Inspect the Intake log and Archive Tile. Catalogue two distinct items, then play tape 2.";
            else if (phase == 2) nextStep = "NEXT // Inspect Observation Glass and query the Memory Block with a Terminal Cartridge. Find two clues to unlock tape 3.";
            else if (phase == 3) nextStep = "NEXT // Search anomaly blocks and room inscriptions. Gather three clues and locate the Archive Key before tape 4.";
            else if (phase == 4) nextStep = "NEXT // Open the Sealed Door with the Archive Key. Search the Records floor for its hidden index.";
            else if (phase == 5) nextStep = "NEXT // Scan the Rift Frame and finish the final recording. Five unique clues are needed to seal the breach.";
            else if (evidenceCount(data) < 5) nextStep = "NEXT // Find more unique clues. Inspect Observation Glass, Memory Block, Sealed Door, and the hidden Records tile.";
            else nextStep = "NEXT // All conditions met. Take the Rift Key to the Rift Frame.";
            player.displayClientMessage(Component.literal(nextStep), false);
            player.displayClientMessage(Component.literal("JOURNAL // This checklist tracks discoveries, not duplicate inventory."), false);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("nullarchive")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    CompoundTag data = player.getPersistentData();
                    player.displayClientMessage(Component.literal("NULL ARCHIVE // Phase " + data.getInt(PHASE_KEY)
                            + "/6 // Evidence " + evidenceCount(data) + "/8 // Anomaly " + data.getInt(ANOMALY_KEY)
                            + " // Gate " + (data.getBoolean("nullarchive_door_unlocked") ? "UNLOCKED" : "LOCKED")
                            + " // Rift Scan " + (data.getBoolean("nullarchive_rift_scan") ? "RECORDED" : "NOT RECORDED")
                            + " // Ending " + (data.getBoolean("nullarchive_ending_complete") ? "COMPLETE" : "INCOMPLETE")
                            + " // Instability " + data.getInt(INSTABILITY_KEY) + "% // Breaches " + data.getInt(BREACH_COUNT_KEY)), false);
                    return 1;
                }))
                .then(Commands.literal("generate").executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos origin = player.blockPosition().offset(0, -1, 0);
                    generateFacility(level, origin);
                    // Place the operator safely inside Intake instead of at the room corner/wall.
                    player.setPos(origin.getX() + 4.5, origin.getY() + 1.0, origin.getZ() + 4.5);
                    player.setYRot(-90.0F);
                    CompoundTag playerData = player.getPersistentData();
                    playerData.putInt("nullarchive_facility_x", origin.getX());
                    playerData.putInt("nullarchive_facility_y", origin.getY());
                    playerData.putInt("nullarchive_facility_z", origin.getZ());
                    playerData.putString("nullarchive_facility_dimension", level.dimension().location().toString());
                    playerData.putBoolean("nullarchive_facility_generated", true);
                    playerData.remove("nullarchive_room_zero");
                    playerData.remove("nullarchive_room_zero_clue");
                    playerData.putBoolean("nullarchive_facility_generated", true);
                    playerData.remove("nullarchive_room_zero");
                    playerData.remove("nullarchive_ambient_obs");
                    playerData.remove("nullarchive_ambient_corrupt");
                    playerData.remove("nullarchive_ambient_static");
                    playerData.remove("nullarchive_ambient_gate");
                    playerData.remove("nullarchive_ambient_rift");
                    playerData.remove("nullarchive_ambient_cooldown");
                    playerData.remove("nullarchive_random_anomaly_cooldown");\n                    playerData.remove("nullarchive_observer_cooldown");\n                    playerData.remove("nullarchive_observer_count");\n                    for (int observerPhase = 3; observerPhase <= 6; observerPhase++) playerData.remove("nullarchive_observer_" + observerPhase);
                    for (int phaseIndex = 0; phaseIndex <= 6; phaseIndex++) {
                        playerData.remove("nullarchive_presence_" + phaseIndex);
                        playerData.remove("nullarchive_inscription_" + phaseIndex);
                    }
                    playerData.remove("nullarchive_intake_log");
                    playerData.remove("nullarchive_intake_log_reveal");
                    playerData.remove("nullarchive_hidden_record");
                    playerData.remove("nullarchive_door_unlocked");
                    playerData.remove("nullarchive_rift_scan");
                    player.displayClientMessage(Component.literal("NULL ARCHIVE // Facility generated. Start in Intake and follow the corridor."), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    CompoundTag data = player.getPersistentData();
                    // Restore the physical gate and ending-specific Rift mutations for a replay.
                    boolean endingWasComplete = data.getBoolean("nullarchive_ending_complete");
                    boolean roomZeroWasOpen = data.getBoolean("nullarchive_room_zero");
                    if (roomZeroWasOpen && data.contains("nullarchive_facility_x")
                            && data.getString("nullarchive_facility_dimension").equals(context.getSource().getLevel().dimension().location().toString())) {
                        ServerLevel level = context.getSource().getLevel();
                        BlockPos origin = new BlockPos(data.getInt("nullarchive_facility_x"),
                                data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
                        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                        for (int x = 77; x <= 85; x++) for (int z = 0; z <= 8; z++)
                            for (int y = 0; y <= 4; y++) level.setBlock(origin.offset(x, y, z), air, 3);
                        var concrete = ModBlocks.ARCHIVE_CONCRETE.get().defaultBlockState();
                        level.setBlock(origin.offset(76, 1, 4), concrete, 3);
                        level.setBlock(origin.offset(76, 2, 4), concrete, 3);
                    }
                    if (data.contains("nullarchive_facility_x")
                            && data.getString("nullarchive_facility_dimension").equals(context.getSource().getLevel().dimension().location().toString())) {
                        ServerLevel level = context.getSource().getLevel();
                        BlockPos origin = new BlockPos(data.getInt("nullarchive_facility_x"),
                                data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
                        BlockPos doorBase = origin.offset(65, 1, 4);
                        var closedDoor = ModBlocks.SEALED_DOOR.get().defaultBlockState();
                        level.setBlock(doorBase, closedDoor, 3);
                        level.setBlock(doorBase.above(), closedDoor, 3);
                        if (endingWasComplete) {
                            // Revert only the known final-tape and ending seal positions.
                            var concrete = ModBlocks.ARCHIVE_CONCRETE.get().defaultBlockState();
                            var tile = ModBlocks.ARCHIVE_TILE.get().defaultBlockState();
                            var corrupted = ModBlocks.CORRUPTED_STONE.get().defaultBlockState();
                            var blackGlass = ModBlocks.BLACK_GLASS.get().defaultBlockState();
                            var nullBlock = ModBlocks.NULL_BLOCK.get().defaultBlockState();
                            level.setBlock(origin.offset(74, 1, 4), concrete, 3);
                            level.setBlock(origin.offset(74, 2, 4), concrete, 3);
                            level.setBlock(origin.offset(74, 3, 4), concrete, 3);
                            level.setBlock(origin.offset(74, 1, 5), concrete, 3);
                            level.setBlock(origin.offset(74, 2, 5), concrete, 3);
                            level.setBlock(origin.offset(74, 3, 5), concrete, 3);
                            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                                boolean baselineCorruption = dx == 0 && Math.abs(dz) <= 1;
                                level.setBlock(origin.offset(70 + dx, 0, 4 + dz),
                                        baselineCorruption ? corrupted : tile, 3);
                            }
                            level.setBlock(origin.offset(73, 1, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            level.setBlock(origin.offset(70, 1, 4), ModBlocks.RIFT_FRAME.get().defaultBlockState(), 3);
                            level.setBlock(origin.offset(70, 2, 4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                            level.setBlock(origin.offset(72, 1, 4), nullBlock, 3);
                            level.setBlock(origin.offset(70, 0, 4), corrupted, 3);
                            // The final tape also turns two adjacent floor tiles into corruption.
                            level.setBlock(origin.offset(69, 0, 4), tile, 3);
                            level.setBlock(origin.offset(71, 0, 4), tile, 3);
                            level.setBlock(origin.offset(70, 0, 3), tile, 3);
                            level.setBlock(origin.offset(70, 0, 5), tile, 3);
                        }
                    }
                    data.remove(PHASE_KEY);
                    data.remove(EVIDENCE_KEY);
                    data.remove(EVIDENCE_FLAGS_KEY);
                    data.remove(ANOMALY_KEY);
                    data.remove("nullarchive_anomaly_cooldown");
                    data.remove("nullarchive_emf_cooldown");
                    data.remove("nullarchive_shard_cooldown");
                    data.remove("nullarchive_ending_complete");
                    data.remove("nullarchive_door_unlocked");
                    data.remove("nullarchive_rift_scan");
                    data.remove("nullarchive_hidden_record");
                    data.remove("nullarchive_memory_echo");
                    data.remove("nullarchive_intake_log");
                    data.remove("nullarchive_intake_log_reveal");
                    data.remove("nullarchive_facility_x");
                    data.remove("nullarchive_facility_y");
                    data.remove("nullarchive_facility_z");
                    data.remove("nullarchive_facility_dimension");
                    data.remove("nullarchive_facility_generated");
                    data.remove("nullarchive_room_zero");
                    data.remove("nullarchive_room_zero_clue");
                    data.remove("nullarchive_ambient_obs");
                    data.remove("nullarchive_ambient_corrupt");
                    data.remove("nullarchive_ambient_static");
                    data.remove("nullarchive_ambient_gate");
                    data.remove("nullarchive_ambient_rift");
                    data.remove("nullarchive_ambient_cooldown");
                    data.remove("nullarchive_random_anomaly_cooldown");
                    data.remove("nullarchive_last_stalking_cue");
                    data.remove("nullarchive_stalking_cue_cooldown");
                    data.remove("nullarchive_ending_seal_feedback");
                    data.remove(INSTABILITY_KEY);
                    data.remove(BREACH_COUNT_KEY);
                    data.remove(BREACH_COOLDOWN_KEY);
                    data.remove(LAST_BREACH_KEY);
                    for (int phaseIndex = 0; phaseIndex <= 6; phaseIndex++) {
                        data.remove("nullarchive_presence_" + phaseIndex);
                        data.remove("nullarchive_inscription_" + phaseIndex);
                    }
                    player.displayClientMessage(Component.literal("NULL ARCHIVE // Your investigation progress has been reset. World blocks are unchanged."), false);
                    return 1;
                })));
    }



    private static void generateFacility(ServerLevel level, BlockPos origin) {
        // Admin-only, compact seven-room linear prototype. Each room is 9x9x5.
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var concrete = ModBlocks.ARCHIVE_CONCRETE.get().defaultBlockState();
        var tile = ModBlocks.ARCHIVE_TILE.get().defaultBlockState();
        var glass = ModBlocks.BLACK_GLASS.get().defaultBlockState();
        var corrupted = ModBlocks.CORRUPTED_STONE.get().defaultBlockState();
        var memory = ModBlocks.MEMORY_BLOCK.get().defaultBlockState();
        var staticState = ModBlocks.STATIC_BLOCK.get().defaultBlockState();
        var nullState = ModBlocks.NULL_BLOCK.get().defaultBlockState();
        var observation = ModBlocks.OBSERVATION_GLASS.get().defaultBlockState();
        var rift = ModBlocks.RIFT_FRAME.get().defaultBlockState();
        for (int room = 0; room < 7; room++) {
            int sx = room * 11;
            for (int x = 0; x < 9; x++) for (int z = 0; z < 9; z++) {
                boolean wall = x == 0 || z == 0 || x == 8 || z == 8;
                BlockPos p = origin.offset(sx + x, 0, z);
                level.setBlock(p, room == 2 ? corrupted : tile, 3);
                level.setBlock(p.above(4), concrete, 3);
                for (int y = 1; y <= 3; y++) level.setBlock(p.above(y), wall ? concrete : air, 3);
                if (wall && (room == 3 || room == 4) && z != 0 && z != 8)
                    level.setBlock(p.above(), glass, 3);
            }
            // Give each room a distinct visual landmark instead of a uniform empty shell.
            switch (room) {
                case 0 -> {
                    // Intake: desk-like archive counter and a narrow waiting alcove.
                    for (int x = 2; x <= 6; x++) level.setBlock(origin.offset(sx + x, 1, 2), concrete, 3);
                    for (int x = 2; x <= 6; x++) level.setBlock(origin.offset(sx + x, 2, 2), air, 3);
                    level.setBlock(origin.offset(sx + 2, 1, 6), glass, 3);
                    level.setBlock(origin.offset(sx + 6, 1, 6), glass, 3);
                }
                case 1 -> {
                    // Basement: low dark ceiling strips.
                    for (int x = 1; x <= 7; x += 2) {
                        level.setBlock(origin.offset(sx + x, 3, 2), glass, 3);
                        level.setBlock(origin.offset(sx + x, 3, 6), glass, 3);
                    }
                }
                case 2 -> {
                    // Observation: glass-lined side walls and a central divider.
                    for (int y = 1; y <= 3; y++) {
                        for (int z = 2; z <= 6; z++) {
                            level.setBlock(origin.offset(sx + 1, y, z), glass, 3);
                            level.setBlock(origin.offset(sx + 7, y, z), glass, 3);
                        }
                    }
                    for (int z = 2; z <= 6; z++) level.setBlock(origin.offset(sx + 4, 2, z), concrete, 3);
                }
                case 3 -> {
                    // Incident Lab: raised tile islands and an interrupted floor path.
                    for (int x = 2; x <= 6; x++) {
                        if (x != 4) level.setBlock(origin.offset(sx + x, 0, 2), corrupted, 3);
                        level.setBlock(origin.offset(sx + x, 0, 6), staticState, 3);
                    }
                    level.setBlock(origin.offset(sx + 4, 0, 4), concrete, 3);
                }
                case 4 -> {
                    // Records room: shelves represented by concrete columns.
                    for (int x : new int[] {2, 4, 6}) for (int z : new int[] {2, 6}) {
                        for (int y = 1; y <= 2; y++) level.setBlock(origin.offset(sx + x, y, z), concrete, 3);
                    }
                }
                case 5 -> {
                    // Sealed Wing: warning stripe pattern at the threshold.
                    for (int x = 2; x <= 6; x++) level.setBlock(origin.offset(sx + x, 0, 2), x % 2 == 0 ? corrupted : tile, 3);
                    level.setBlock(origin.offset(sx + 4, 1, 2), nullState, 3);
                }
                case 6 -> {
                    // Rift Chamber: a dark cross-shaped floor and frame accents.
                    for (int x = 2; x <= 6; x++) {
                        level.setBlock(origin.offset(sx + x, 0, 4), corrupted, 3);
                        level.setBlock(origin.offset(sx + 4, 0, x), corrupted, 3);
                    }
                    for (int y = 1; y <= 3; y++) {
                        level.setBlock(origin.offset(sx + 2, y, 2), glass, 3);
                        level.setBlock(origin.offset(sx + 6, y, 6), nullState, 3);
                    }
                }
            }
            if (room < 6) {
                for (int y = 1; y <= 3; y++) {
                    level.setBlock(origin.offset(sx + 8, y, 4), air, 3);
                    level.setBlock(origin.offset(sx + 9, y, 4), air, 3);
                    level.setBlock(origin.offset(sx + 10, y, 4), air, 3);
                }
                for (int x = 8; x <= 10; x++) level.setBlock(origin.offset(sx + x, 0, 4), tile, 3);
                // Clear the next room's west wall so the spine remains connected.
                int nextStart = sx + 11;
                for (int y = 1; y <= 3; y++)
                    level.setBlock(origin.offset(nextStart, y, 4), air, 3);
            }
        }
        level.setBlock(origin.offset(2, 1, 2), memory, 3);
        level.setBlock(origin.offset(15, 1, 4), observation, 3);
        level.setBlock(origin.offset(26, 1, 4), corrupted, 3);
        level.setBlock(origin.offset(37, 1, 4), staticState, 3);
        level.setBlock(origin.offset(48, 1, 4), memory, 3);
        // Two-block-high threshold in the connector between Sealed Wing and Rift Chamber.
        level.setBlock(origin.offset(65, 1, 4), ModBlocks.SEALED_DOOR.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(65, 2, 4), ModBlocks.SEALED_DOOR.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(70, 1, 4), rift, 3);
        level.setBlock(origin.offset(72, 1, 4), nullState, 3);

        // Curated evidence/key drops at room landmarks (one-time per generation).
        drop(level, origin.offset(3, 1, 3), ModItems.EVIDENCE_TAPE.get());
        drop(level, origin.offset(14, 1, 3), ModItems.DAMAGED_PHOTOGRAPH.get());
        drop(level, origin.offset(25, 1, 3), ModItems.MEMORY_FRAGMENT.get());
        drop(level, origin.offset(36, 1, 3), ModItems.SEALED_DOCUMENT.get());
        drop(level, origin.offset(47, 1, 3), ModItems.TERMINAL_CARTRIDGE.get());
        drop(level, origin.offset(57, 1, 3), ModItems.ARCHIVE_KEY.get());
        drop(level, origin.offset(69, 1, 3), ModItems.RIFT_KEY.get());
        drop(level, origin.offset(71, 1, 3), ModItems.NULL_SHARD.get());
        drop(level, origin.offset(68, 1, 5), ModItems.EMF_READER.get());
        drop(level, origin.offset(24, 1, 5), ModItems.FIELD_RECORDER.get());
    }

    private static void drop(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5,
                new ItemStack(item));
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    @SubscribeEvent
    public static void onAmbientFacilityTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        Player player = event.player;
        CompoundTag data = player.getPersistentData();
        if (!data.contains("nullarchive_facility_x") || !data.getString("nullarchive_facility_dimension").equals(player.level().dimension().location().toString())) return;
        int phase = data.getInt(PHASE_KEY);
        if (phase < 2) return;
        long now = player.level().getGameTime();
        if (now < data.getLong("nullarchive_ambient_cooldown")) return;
        BlockPos origin = new BlockPos(data.getInt("nullarchive_facility_x"),
                data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
        BlockPos pos = player.blockPosition();
        String key = null, message = null;
        float pitch = 0.7F;
        if (phase >= 2 && !data.getBoolean("nullarchive_ambient_obs") && pos.distSqr(origin.offset(15, 1, 4)) <= 16) {
            key = "nullarchive_ambient_obs"; message = "OBSERVATION // The glass shows you standing here. You are not standing here."; pitch = 0.55F;
        } else if (phase >= 3 && !data.getBoolean("nullarchive_ambient_corrupt") && pos.distSqr(origin.offset(26, 1, 4)) <= 16) {
            key = "nullarchive_ambient_corrupt"; message = "BASEMENT // The floor is breathing. The recorder insists this is ventilation."; pitch = 0.65F;
        } else if (phase >= 4 && !data.getBoolean("nullarchive_ambient_static") && pos.distSqr(origin.offset(37, 1, 4)) <= 16) {
            key = "nullarchive_ambient_static"; message = "INCIDENT LAB // A voice from the wall says: 'You already opened the door.'"; pitch = 0.45F;
        } else if (phase >= 4 && !data.getBoolean("nullarchive_ambient_gate") && pos.distSqr(origin.offset(65, 1, 4)) <= 25) {
            key = "nullarchive_ambient_gate"; message = "SEALED WING // Access requires authorization. Something on the other side is counting."; pitch = 0.5F;
        } else if (phase >= 5 && !data.getBoolean("nullarchive_ambient_rift") && pos.distSqr(origin.offset(70, 1, 4)) <= 16) {
            key = "nullarchive_ambient_rift"; message = "BREACH // The transmission is coming from your inventory."; pitch = 0.35F;
        }
        if (key == null && phase >= 3 && !data.getBoolean("nullarchive_presence_" + phase)
                && pos.distSqr(origin.offset(32, 1, 4)) <= 900) {
            // A sound is placed behind the player, creating a stalking cue without a fake entity.
            float yaw = player.getYRot() * ((float) Math.PI / 180F);
            BlockPos behind = pos.offset((int) Math.round(Math.sin(yaw) * 4),
                    0, (int) Math.round(Math.cos(yaw) * 4));
            player.level().playSound(null, behind, SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.AMBIENT, 0.45F, 0.45F + phase * 0.06F);
            data.putBoolean("nullarchive_presence_" + phase, true);
            data.putLong("nullarchive_ambient_cooldown", now + 100);
            player.displayClientMessage(Component.literal("SIGNAL // Footsteps behind you. The corridor is empty."), false);
            return;
        }
        // The Observer is a phase-gated encounter, represented by a short particle silhouette rather than a fake mob.
        if (phase >= 3 && phase <= 6 && !data.getBoolean("nullarchive_observer_" + phase)
                && now >= data.getLong("nullarchive_observer_cooldown")
                && pos.getX() >= origin.getX() && pos.getX() <= origin.getX() + 76
                && pos.getZ() >= origin.getZ() && pos.getZ() <= origin.getZ() + 8
                && player.getRandom().nextInt(900) == 0) {
            BlockPos observerPos;
            String observerMessage;
            if (phase >= 6) {
                observerPos = origin.offset(70, 1, 4);
                observerMessage = "OBSERVER // The silhouette is standing inside the Rift Frame. It has your outline.";
            } else if (phase >= 5) {
                float yaw = player.getYRot() * ((float) Math.PI / 180F);
                observerPos = pos.offset((int) Math.round(Math.sin(yaw) * 6), 0,
                        (int) Math.round(Math.cos(yaw) * 6));
                observerMessage = "OBSERVER // A shape waits behind you. When you turn, it is farther down the corridor.";
            } else {
                observerPos = origin.offset(48, 1, 4);
                observerMessage = "OBSERVER // A figure stands behind the Records glass. The room is empty when you reach it.";
            }
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SMOKE, observerPos.getX() + 0.5,
                        observerPos.getY() + 1.0, observerPos.getZ() + 0.5, 18, 0.25, 0.8, 0.25, 0.01);
                serverLevel.sendParticles(ParticleTypes.PORTAL, observerPos.getX() + 0.5,
                        observerPos.getY() + 1.0, observerPos.getZ() + 0.5, 12, 0.35, 0.9, 0.35, 0.2);
            }
            player.level().playSound(null, observerPos, ModSounds.SIGNAL_DISTORTION.get(),
                    SoundSource.AMBIENT, 0.65F, 0.35F + phase * 0.08F);
            data.putBoolean("nullarchive_observer_" + phase, true);
            data.putLong("nullarchive_observer_cooldown", now + 900);
            data.putInt("nullarchive_observer_count", data.getInt("nullarchive_observer_count") + 1);
            player.displayClientMessage(Component.literal(observerMessage), false);
        }
        // Rare, unscheduled micro-anomalies can occur during any facility visit.
        // Unscheduled, low-frequency micro-anomalies make revisits less predictable.
        long anomalyCooldown = data.getLong("nullarchive_random_anomaly_cooldown");
        boolean insideFacility = pos.getX() >= origin.getX() && pos.getX() <= origin.getX() + 76
                && pos.getZ() >= origin.getZ() && pos.getZ() <= origin.getZ() + 8;
        if (phase >= 2 && insideFacility && now >= anomalyCooldown && player.getRandom().nextInt(1600) == 0) {
            String[] anomalies = {
                    "SIGNAL // A recording clicks on. It contains three seconds of silence.",
                    "ARCHIVE // Your footsteps echo twice. The second echo is closer.",
                    "NULL // The room number on the wall changes when you look away."
            };
            String anomalyText = anomalies[player.getRandom().nextInt(anomalies.length)];
            data.putLong("nullarchive_random_anomaly_cooldown", now + 600);
            player.displayClientMessage(Component.literal(anomalyText), false);
            player.level().playSound(null, pos, ModSounds.TAPE_CLICK.get(), SoundSource.AMBIENT, 0.35F,
                    0.45F + player.getRandom().nextFloat() * 0.4F);
        }
        if (key == null) return;
        // Environmental changes make the anomaly persist in the facility.
        if (key.equals("nullarchive_ambient_obs")) {
            BlockPos wall = origin.offset(15, 1, 0);
            player.level().setBlock(wall, ModBlocks.BLACK_GLASS.get().defaultBlockState(), 3);
            player.level().setBlock(wall.above(), ModBlocks.BLACK_GLASS.get().defaultBlockState(), 3);
        } else if (key.equals("nullarchive_ambient_corrupt")) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                player.level().setBlock(origin.offset(26 + dx, 0, 4 + dz), ModBlocks.CORRUPTED_STONE.get().defaultBlockState(), 3);
        } else if (key.equals("nullarchive_ambient_static")) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                player.level().setBlock(origin.offset(37 + dx, 0, 4 + dz), ModBlocks.STATIC_BLOCK.get().defaultBlockState(), 3);
        } else if (key.equals("nullarchive_ambient_gate")) {
            // The sealed wing appears to gain a side passage, but the "opening" is a glass wall.
            for (int y = 1; y <= 3; y++) {
                player.level().setBlock(origin.offset(70, y, 0), ModBlocks.BLACK_GLASS.get().defaultBlockState(), 3);
            }
            player.level().setBlock(origin.offset(70, 2, 0), ModBlocks.NULL_BLOCK.get().defaultBlockState(), 3);
        } else if (key.equals("nullarchive_ambient_rift")) {
            player.level().setBlock(origin.offset(73, 1, 4), ModBlocks.NULL_BLOCK.get().defaultBlockState(), 3);
        }
        data.putBoolean(key, true);
        data.putLong("nullarchive_ambient_cooldown", now + 100);
        player.displayClientMessage(Component.literal(message), false);
        player.level().playSound(null, pos, ModSounds.SIGNAL_DISTORTION.get(), SoundSource.PLAYERS, 0.8F, pitch);

    }

    @SubscribeEvent
    public static void onRecorderUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.FIELD_RECORDER.get())) return;
        CompoundTag data = event.getEntity().getPersistentData();
        int phase = data.getInt(PHASE_KEY);
        int evidence = evidenceCount(data);

        if (phase >= 3 && phase < 5 && evidence < phase - 1) {
            event.getEntity().displayClientMessage(Component.literal(
                    "PLAYBACK LOCKED // Evidence fragments required: " + (phase - 1 - evidence)), false);
            return;
        }
        if (phase >= PHASE_MESSAGES.length) {
            event.getEntity().displayClientMessage(Component.literal("SIGNAL LOST // No further recordings available."), false);
            return;
        }

        event.getEntity().displayClientMessage(Component.literal(PHASE_MESSAGES[phase]), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(),
                ModSounds.TAPE_CLICK.get(), SoundSource.PLAYERS, 0.8F, 0.85F + phase * 0.04F);
        data.putInt(PHASE_KEY, phase + 1);
        increaseInstability(event.getEntity(), 5, "Tape playback accepted.");
        if (phase + 1 < 6) {
            event.getEntity().displayClientMessage(Component.literal("PLAYBACK // Recording accepted. Archive phase " + (phase + 2)
                    + " is now available. Use /archivejournal to review your next objective."), false);
            if (phase >= 2) event.getLevel().playSound(null, event.getEntity().blockPosition(),
                    ModSounds.ARCHIVE_PULSE.get(), SoundSource.PLAYERS, 0.6F, 0.7F + phase * 0.08F);
        } else {
            event.getEntity().displayClientMessage(Component.literal("PLAYBACK // Final recording accepted. The facility is no longer stable."), false);
            triggerFinalArchiveShift(event.getEntity());
        }
        NullArchive.LOGGER.info("Archive phase {} recorded for {}", phase + 1,
                event.getEntity().getGameProfile().getName());
    }

    private static void triggerFinalArchiveShift(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains("nullarchive_facility_x")
                || !data.getString("nullarchive_facility_dimension").equals(player.level().dimension().location().toString())) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = new BlockPos(data.getInt("nullarchive_facility_x"),
                data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
        // The final tape permanently rewrites the Rift Chamber.
        for (int y = 1; y <= 3; y++) {
            level.setBlock(origin.offset(74, y, 4), ModBlocks.NULL_BLOCK.get().defaultBlockState(), 3);
            level.setBlock(origin.offset(74, y, 5), ModBlocks.BLACK_GLASS.get().defaultBlockState(), 3);
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
            level.setBlock(origin.offset(70 + dx, 0, 4 + dz), ModBlocks.CORRUPTED_STONE.get().defaultBlockState(), 3);
        level.setBlock(origin.offset(73, 1, 4), ModBlocks.NULL_BLOCK.get().defaultBlockState(), 3);
        player.displayClientMessage(Component.literal("FACILITY SHIFT // The final tape rewrites the room. The breach is no longer contained by architecture."), false);
        level.playSound(null, player.blockPosition(), ModSounds.SIGNAL_DISTORTION.get(), SoundSource.PLAYERS, 1.0F, 0.3F);
    }

    @SubscribeEvent
    public static void onEvidenceUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        var stack = event.getItemStack();
        if (!stack.is(ModItems.EVIDENCE_TAPE.get()) && !stack.is(ModItems.DAMAGED_PHOTOGRAPH.get())
                && !stack.is(ModItems.MEMORY_FRAGMENT.get()) && !stack.is(ModItems.SEALED_DOCUMENT.get())) return;

        CompoundTag data = event.getEntity().getPersistentData();
        int flag = stack.is(ModItems.EVIDENCE_TAPE.get()) ? EVIDENCE_TAPE_FLAG
                : stack.is(ModItems.DAMAGED_PHOTOGRAPH.get()) ? PHOTO_FLAG
                : stack.is(ModItems.MEMORY_FRAGMENT.get()) ? MEMORY_FLAG : DOCUMENT_FLAG;
        if (!catalogueEvidence(data, flag)) {
            event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // This item is already catalogued. Duplicate record ignored."), false);
            return;
        }
        stack.shrink(1);
        increaseInstability(event.getEntity(), 8, "Unique evidence indexed.");
        event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // Unique fragment catalogued: " + evidenceCount(data) + "/8"), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(),
                ModSounds.ARCHIVE_PULSE.get(), SoundSource.PLAYERS, 0.65F, 1.0F);
    }

    @SubscribeEvent
    public static void onEmfUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.EMF_READER.get())) return;
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        int phase = data.getInt(PHASE_KEY);
        if (!data.contains("nullarchive_facility_x")
                || !data.getString("nullarchive_facility_dimension").equals(event.getLevel().dimension().location().toString())) {
            player.displayClientMessage(Component.literal("EMF // Baseline. No archive signal in this dimension."), false);
            event.getLevel().playSound(null, player.blockPosition(), ModSounds.ARCHIVE_PULSE.get(),
                    SoundSource.PLAYERS, 0.45F, 1.1F);
            return;
        }
        long now = event.getLevel().getGameTime();
        if (now < data.getLong("nullarchive_emf_cooldown")) {
            player.displayClientMessage(Component.literal("EMF // The receiver is still recalibrating. Wait for the needle to settle."), false);
            return;
        }
        data.putLong("nullarchive_emf_cooldown", now + 20);
        increaseInstability(player, 3, "EMF scan performed.");
        BlockPos origin = new BlockPos(data.getInt("nullarchive_facility_x"),
                data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
        BlockPos[] targets = {
                origin.offset(4, 1, 2), origin.offset(15, 1, 4), origin.offset(26, 1, 4),
                origin.offset(37, 1, 4), origin.offset(49, 0, 2), origin.offset(65, 1, 4),
                origin.offset(70, 1, 4), origin.offset(74, 1, 4)
        };
        String[] labels = {
                "INTAKE // Visitor log interference.", "OBSERVATION // Reflection signal.",
                "BASEMENT // Organic pulse beneath the floor.", "INCIDENT LAB // Static in the walls.",
                "RECORDS // A concealed index is nearby.", "SEALED WING // Lock resonance.",
                "RIFT // Displaced frame signal.", "NULL // The final recording is rewriting this room."
        };
        int[] gates = {1, 2, 3, 4, 4, 4, 5, 6};
        int bestIndex = -1;
        double bestDistance = 65.0;
        for (int i = 0; i < targets.length; i++) {
            if (phase < gates[i]) continue;
            double distance = player.blockPosition().distSqr(targets[i]);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        if (bestIndex < 0) {
            player.displayClientMessage(Component.literal("EMF // Signal below threshold. Advance the recordings or move deeper into the facility."), false);
            event.getLevel().playSound(null, player.blockPosition(), ModSounds.ARCHIVE_PULSE.get(),
                    SoundSource.PLAYERS, 0.5F, 0.9F);
            return;
        }
        BlockPos scanOrigin = player.blockPosition();
        BlockPos scanTarget = targets[bestIndex];
        // Detect actual anomalous blocks as well as authored landmarks, so mutations remain discoverable.
        if (phase >= 2) {
            for (int dx = -8; dx <= 8; dx++) for (int dy = -8; dy <= 8; dy++) for (int dz = -8; dz <= 8; dz++) {
                BlockPos scanPos = scanOrigin.offset(dx, dy, dz);
                double distance = scanOrigin.distSqr(scanPos);
                if (distance > 64 || distance >= bestDistance) continue;
                var scannedBlock = event.getLevel().getBlockState(scanPos).getBlock();
                if (scannedBlock.equals(ModBlocks.NULL_BLOCK.get())) {
                    bestDistance = distance; bestIndex = 7; scanTarget = scanPos;
                } else if (scannedBlock.equals(ModBlocks.STATIC_BLOCK.get())) {
                    bestDistance = distance; bestIndex = 3; scanTarget = scanPos;
                } else if (scannedBlock.equals(ModBlocks.CORRUPTED_STONE.get())) {
                    bestDistance = distance; bestIndex = 2; scanTarget = scanPos;
                } else if (scannedBlock.equals(ModBlocks.BLACK_GLASS.get())) {
                    bestDistance = distance; bestIndex = 1; scanTarget = scanPos;
                }
            }
        }
        int strength = bestDistance <= 4 ? 5 : bestDistance <= 9 ? 4 : bestDistance <= 16 ? 3 : bestDistance <= 36 ? 2 : 1;
        BlockPos target = scanTarget;
        String direction = target.getX() > scanOrigin.getX() ? "ahead"
                : target.getX() < scanOrigin.getX() ? "behind"
                : target.getY() > scanOrigin.getY() ? "above"
                : target.getY() < scanOrigin.getY() ? "below"
                : target.getZ() > scanOrigin.getZ() ? "to your right"
                : target.getZ() < scanOrigin.getZ() ? "to your left" : "underfoot";
        player.displayClientMessage(Component.literal("EMF // " + strength + "/5 // " + labels[bestIndex]
                + " Direction: " + direction + ". Signal does not catalogue evidence."), false);
        event.getLevel().playSound(null, scanOrigin,
                strength >= 4 || phase >= 5 ? ModSounds.SIGNAL_DISTORTION.get() : ModSounds.ARCHIVE_PULSE.get(),
                SoundSource.PLAYERS, 0.65F, 0.55F + strength * 0.12F);
    }

    @SubscribeEvent
    public static void onRoomZeroInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean("nullarchive_room_zero") || data.getBoolean("nullarchive_room_zero_clue")) return;
        if (!data.contains("nullarchive_facility_x")
                || !data.getString("nullarchive_facility_dimension").equals(player.level().dimension().location().toString())) return;
        BlockPos origin = new BlockPos(data.getInt("nullarchive_facility_x"),
                data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z"));
        if (!event.getPos().equals(origin.offset(81, 1, 4))) return;
        if (data.getInt(INSTABILITY_KEY) < 35 || data.getInt(PHASE_KEY) < 4) {
            player.displayClientMessage(Component.literal("ROOM 0 // The block is inert. The room is not ready to be remembered."), false);
            return;
        }
        data.putBoolean("nullarchive_room_zero_clue", true);
        player.displayClientMessage(Component.literal("ROOM 0 // RECORD 000: “There were seven rooms when you arrived. There are eight when you leave. Do not count the one that knows your name.” This clue is outside the standard evidence index."), false);
        player.level().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(), SoundSource.BLOCKS, 0.8F, 0.2F);
        if (player.level() instanceof ServerLevel serverLevel) serverLevel.sendParticles(ParticleTypes.PORTAL,
                event.getPos().getX() + 0.5, event.getPos().getY() + 1.0, event.getPos().getZ() + 0.5,
                24, 0.4, 0.5, 0.4, 0.1);
    }

    @SubscribeEvent
    public static void onArchiveBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        CompoundTag data = event.getEntity().getPersistentData();
        int phase = data.getInt(PHASE_KEY);
        int evidence = evidenceCount(data);

        if (block.equals(ModBlocks.SEALED_DOOR.get())) {
            if (phase < 4) {
                event.getEntity().displayClientMessage(Component.literal("SEALED ACCESS // Lock status: RED. Incident authorization missing."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                        SoundSource.BLOCKS, 0.45F, 0.35F);
            } else if (evidence < 3) {
                event.getEntity().displayClientMessage(Component.literal("SEALED ACCESS // Three indexed clues required for access."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.TAPE_CLICK.get(),
                        SoundSource.BLOCKS, 0.4F, 0.5F);
            } else if (!data.getBoolean("nullarchive_door_unlocked")
                    && !event.getItemStack().is(ModItems.ARCHIVE_KEY.get())) {
                event.getEntity().displayClientMessage(Component.literal("SEALED ACCESS // The lock accepts an Archive Key."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.TAPE_CLICK.get(),
                        SoundSource.BLOCKS, 0.5F, 0.35F);
            } else {
                boolean firstUnlock = !data.getBoolean("nullarchive_door_unlocked");
                if (firstUnlock) {
                    data.putBoolean("nullarchive_door_unlocked", true);
                    increaseInstability(event.getEntity(), 10, "Sealed access granted.");
                    if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(),
                            SoundSource.BLOCKS, 0.8F, 0.45F);
                }
                var state = event.getLevel().getBlockState(event.getPos());
                boolean opening = !state.getValue(ArchiveDoorBlock.OPEN);
                event.getLevel().setBlock(event.getPos(), state.setValue(ArchiveDoorBlock.OPEN, opening), 3);
                for (BlockPos linkedPos : new BlockPos[] { event.getPos().above(), event.getPos().below() }) {
                    var linked = event.getLevel().getBlockState(linkedPos);
                    if (linked.getBlock().equals(ModBlocks.SEALED_DOOR.get()))
                        event.getLevel().setBlock(linkedPos, linked.setValue(ArchiveDoorBlock.OPEN, opening), 3);
                }
                boolean firstSealRecord = catalogueEvidence(data, DOOR_FLAG);
                event.getEntity().displayClientMessage(Component.literal(opening
                        ? "SEALED ACCESS // Door opened. " + (firstSealRecord ? "EVIDENCE 07 recorded: the seal is older than the building." : "The corridor beyond is silent.")
                        : "SEALED ACCESS // Door closed. The lock engages from the other side."), false);
                event.getLevel().playSound(null, event.getPos(), opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                        SoundSource.BLOCKS, 1.0F, opening ? 0.7F : 0.6F);
            }
            event.setCanceled(true);
        } else if (block.equals(ModBlocks.OBSERVATION_GLASS.get())) {
            if (phase >= 2 && catalogueEvidence(data, GLASS_FLAG)) {
                increaseInstability(event.getEntity(), 8, "Observation clue indexed.");
                event.getEntity().displayClientMessage(Component.literal("EVIDENCE 05 // Observation glass recorded. Reflection timing is impossible."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(), SoundSource.BLOCKS, 0.7F, 0.65F);
            } else if (phase < 2) {
                event.getEntity().displayClientMessage(Component.literal("OBSERVATION // Nothing unusual. Complete the first two recordings."), false);
            } else {
                event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // Observation glass clue already recorded."), false);
            }
        } else if (block.equals(ModBlocks.MEMORY_BLOCK.get())) {
            var held = event.getItemStack();
            if (held.is(ModItems.TERMINAL_CARTRIDGE.get())) {
                if (phase < 2 || evidence < 2) {
                    event.getEntity().displayClientMessage(Component.literal("TERMINAL // Cartridge rejected. Two recordings and two indexed clues required."), false);
                } else {
                    boolean firstQuery = catalogueEvidence(data, TERMINAL_FLAG);
                    if (firstQuery) increaseInstability(event.getEntity(), 8, "Terminal clue indexed.");
                    if (firstQuery && !event.getEntity().getAbilities().instabuild) held.shrink(1);
                    event.getEntity().displayClientMessage(Component.literal(firstQuery
                            ? "TERMINAL QUERY // /SUBJECT/NULL/EXIT_LOG.txt // EVIDENCE 06 recorded. The subject field is blank."
                            : "TERMINAL QUERY // /SUBJECT/NULL/EXIT_LOG.txt // The file timestamp is tomorrow. Duplicate queries do not consume cartridges."), false);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(), SoundSource.BLOCKS, 0.8F, 0.55F);
                }
            } else {
                evidence = evidenceCount(data);
                if (phase >= 3 && !data.getBoolean("nullarchive_memory_echo")) {
                    data.putBoolean("nullarchive_memory_echo", true);
                    event.getEntity().displayClientMessage(Component.literal(
                            "MEMORY ECHO // A voice speaks from the block: \"You are not retrieving a file. You are returning one.\""), false);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                            SoundSource.BLOCKS, 0.7F, 0.35F);
                } else {
                    String output = phase < 2
                            ? "MEMORY TERMINAL // ACCESS DENIED. Complete the intake recordings."
                            : evidence < 2
                            ? "MEMORY TERMINAL // Empty index. Catalogue at least two clues before querying."
                            : data.getInt(EVIDENCE_FLAGS_KEY) != 0 && (data.getInt(EVIDENCE_FLAGS_KEY) & TERMINAL_FLAG) != 0
                            ? "MEMORY TERMINAL // /SUBJECT/NULL/EXIT_LOG.txt // The file timestamp is tomorrow. Use a cartridge to query the archive."
                            : "MEMORY TERMINAL // Indexed fragments: " + evidence + " // Cartridge interface required. Query locked.";
                    event.getEntity().displayClientMessage(Component.literal(output), false);
                }
            }
        } else if (block.equals(ModBlocks.BLACK_GLASS.get())
                && data.getBoolean("nullarchive_ending_complete")
                && data.contains("nullarchive_facility_x")
                && event.getPos().equals(new BlockPos(data.getInt("nullarchive_facility_x") + 70,
                        data.getInt("nullarchive_facility_y") + 1, data.getInt("nullarchive_facility_z") + 4))) {
            event.getEntity().displayClientMessage(Component.literal(
                    "ARCHIVE CLOSED // The glass is cold. The seal holds. There is no signal on the other side."), false);
            event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(),
                    SoundSource.BLOCKS, 0.35F, 0.4F);
            event.setCanceled(true);
        } else if (block.equals(ModBlocks.RIFT_FRAME.get())) {
            if (phase >= 5 && !data.getBoolean("nullarchive_rift_scan")) {
                data.putBoolean("nullarchive_rift_scan", true);
                increaseInstability(event.getEntity(), 15, "Rift resonance recorded.");
                event.getEntity().displayClientMessage(Component.literal("RIFT SCAN // Frame resonance detected. The missing segment is not missing; it is displaced. Signal recorded locally, but it is not part of the evidence index."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(), SoundSource.BLOCKS, 0.8F, 0.45F);
            }
            var held = event.getItemStack();
            if (held.is(ModItems.RIFT_KEY.get())) {
                if (data.getBoolean("nullarchive_ending_complete")) {
                    event.getEntity().displayClientMessage(Component.literal("NULL ARCHIVE // The breach is sealed. There is nothing left to unlock."), false);
                } else if (phase < 6 || evidence < 5) {
                    event.getEntity().displayClientMessage(Component.literal(
                            "RIFT LOCK // Final tape and at least five of eight clues required. Progress: " + phase + "/6 // Evidence: " + evidence + "/8"), false);
                } else {
                    data.putBoolean("nullarchive_ending_complete", true);
                    boolean unstableEnding = data.getInt(INSTABILITY_KEY) >= 80;
                    if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
                    // The instability threshold changes the physical seal and the ending text.
                    event.getLevel().setBlock(event.getPos(), (unstableEnding
                            ? ModBlocks.NULL_BLOCK.get().defaultBlockState()
                            : ModBlocks.BLACK_GLASS.get().defaultBlockState()), 3);
                    event.getLevel().setBlock(event.getPos().above(), (unstableEnding
                            ? ModBlocks.BLACK_GLASS.get().defaultBlockState()
                            : ModBlocks.NULL_BLOCK.get().defaultBlockState()), 3);
                    event.getLevel().setBlock(event.getPos().below(), ModBlocks.CORRUPTED_STONE.get().defaultBlockState(), 3);
                    event.getEntity().displayClientMessage(Component.literal(unstableEnding
                            ? "ENDING // ARCHIVE ERROR: SUBJECT OUTSIDE RECORD. The key turns in a lock that has no shape. Something is sealed, but it is not you."
                            : "ENDING // The key turns without touching the lock. The rift folds inward. On the other side, someone finishes your sentence. ARCHIVE CLOSED."), false);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                            SoundSource.BLOCKS, 1.2F, unstableEnding ? 0.2F : 0.35F);
                    event.setCanceled(true);
                }
            } else {
                String output = phase < 5
                        ? "RIFT MONITOR // No breach detected. Keep the recorder active."
                        : "RIFT MONITOR // BREACH CONFIRMED. The other side is transmitting your location.";
                event.getEntity().displayClientMessage(Component.literal(output), false);
                if (phase >= 5) event.getLevel().playSound(null, event.getPos(),
                        ModSounds.SIGNAL_DISTORTION.get(), SoundSource.BLOCKS, 1.0F, 0.5F);
            }
        } else if (block.equals(ModBlocks.ARCHIVE_TILE.get())) {
            boolean atRecordsSecret = data.contains("nullarchive_facility_x")
                    && data.getString("nullarchive_facility_dimension").equals(event.getLevel().dimension().location().toString())
                    && event.getPos().equals(new BlockPos(data.getInt("nullarchive_facility_x") + 49,
                            data.getInt("nullarchive_facility_y"), data.getInt("nullarchive_facility_z") + 2));
            if (atRecordsSecret && phase >= 4 && evidence >= 2 && !data.getBoolean("nullarchive_hidden_record")) {
                data.putBoolean("nullarchive_hidden_record", true);
                boolean first = catalogueEvidence(data, HIDDEN_FLAG);
                if (first) increaseInstability(event.getEntity(), 8, "Hidden record indexed.");
                event.getEntity().displayClientMessage(Component.literal(first
                        ? "HIDDEN RECORD // EVIDENCE 08 indexed. The shelf labels are dates. Every date is your next visit."
                        : "HIDDEN RECORD // The archive has already indexed this note."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                        SoundSource.BLOCKS, 0.7F, 0.4F);
            } else if (atRecordsSecret && phase < 4) {
                event.getEntity().displayClientMessage(Component.literal("RECORDS // The shelf labels are blank. Incident authorization required."), false);
            } else if (atRecordsSecret && evidence < 2) {
                event.getEntity().displayClientMessage(Component.literal("RECORDS // A hidden index is detectable, but the archive needs two catalogued clues."), false);
            } else if (phase > 0) {
                String inscriptionKey = "nullarchive_inscription_" + phase;
                if (!data.getBoolean(inscriptionKey)) {
                    String inscription = switch (phase) {
                        case 1 -> "INTAKE // The visitor log has one entry. The name field is empty.";
                        case 2 -> "BASEMENT // Maintenance note: footsteps detected below the lowest floor.";
                        case 3 -> "OBSERVATION // Do not trust reflections recorded after midnight.";
                        case 4 -> "INCIDENT // The door was sealed before the incident was reported.";
                        case 5 -> "BREACH // Containment is intact. The room is not.";
                        default -> "NULL // This inscription is dated tomorrow. It is signed with your name.";
                    };
                    data.putBoolean(inscriptionKey, true);
                    event.getEntity().displayClientMessage(Component.literal(inscription), false);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(),
                            SoundSource.BLOCKS, 0.45F, 0.55F + phase * 0.08F);
                } else {
                    event.getEntity().displayClientMessage(Component.literal("ARCHIVE // The surface is cold. The inscription has not changed."), false);
                }
            } else {
                event.getEntity().displayClientMessage(Component.literal("ARCHIVE // Intake record locked. Activate the field recorder."), false);
            }
        } else if (block.equals(ModBlocks.ARCHIVE_CONCRETE.get())) {
            boolean atIntakeLog = data.contains("nullarchive_facility_x")
                    && data.getString("nullarchive_facility_dimension").equals(event.getLevel().dimension().location().toString())
                    && event.getPos().equals(new BlockPos(data.getInt("nullarchive_facility_x") + 4,
                            data.getInt("nullarchive_facility_y") + 1, data.getInt("nullarchive_facility_z") + 2));
            if (atIntakeLog && phase >= 1) {
                boolean firstDiscovery = !data.getBoolean("nullarchive_intake_log");
                boolean lateReveal = phase >= 3 && !data.getBoolean("nullarchive_intake_log_reveal");
                if (firstDiscovery) data.putBoolean("nullarchive_intake_log", true);
                if (lateReveal) data.putBoolean("nullarchive_intake_log_reveal", true);
                if (firstDiscovery || lateReveal) {
                    String log = phase >= 3
                            ? "INTAKE LOG // Visitor: [YOUR NAME]. Arrival recorded three days before the facility opened."
                            : "INTAKE LOG // A visitor entry is embedded beneath the counter. The name field is unreadable.";
                    event.getEntity().displayClientMessage(Component.literal(log), false);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.TAPE_CLICK.get(),
                            SoundSource.BLOCKS, 0.6F, phase >= 3 ? 0.45F : 0.65F);
                } else {
                    event.getEntity().displayClientMessage(Component.literal("INTAKE LOG // The entry is still unreadable. The date field is scratched out."), false);
                }
            } else {
                event.getEntity().displayClientMessage(Component.literal("ARCHIVE // Cold concrete. No readable inscription."), false);
            }
        }
    }
    @SubscribeEvent
    public static void onAnomalyBlockUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        var block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!block.equals(ModBlocks.STATIC_BLOCK.get()) && !block.equals(ModBlocks.NULL_BLOCK.get())
                && !block.equals(ModBlocks.CORRUPTED_STONE.get())) return;

        CompoundTag data = event.getEntity().getPersistentData();
        if (data.getInt(PHASE_KEY) < 2) {
            event.getEntity().displayClientMessage(Component.literal("ANOMALY // Dormant. Complete the first recording."), false);
            return;
        }
        long now = event.getLevel().getGameTime();
        if (now < data.getLong("nullarchive_anomaly_cooldown")) {
            event.getEntity().displayClientMessage(Component.literal("ANOMALY // The surface is silent. Give the signal time to settle."), false);
            return;
        }
        data.putLong("nullarchive_anomaly_cooldown", now + 40);
        int anomaly = Math.min(data.getInt(ANOMALY_KEY) + 1, 3);
        data.putInt(ANOMALY_KEY, anomaly);
        String message = switch (anomaly) {
            case 1 -> "ANOMALY 01 // The surface is warm. It has no source of heat.";
            case 2 -> "ANOMALY 02 // Your shadow is facing the wrong direction.";
            default -> "ANOMALY 03 // Something is using your name from inside the wall.";
        };
        event.getEntity().displayClientMessage(Component.literal(message), false);
        event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                SoundSource.BLOCKS, 0.8F, 0.75F + anomaly * 0.1F);
    }
    @SubscribeEvent
    public static void onNullShardUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.NULL_SHARD.get())) return;
        CompoundTag data = event.getEntity().getPersistentData();
        long now = event.getLevel().getGameTime();
        if (now < data.getLong("nullarchive_shard_cooldown")) {
            event.getEntity().displayClientMessage(Component.literal("NULL SHARD // The resonance is fading. Wait before testing it again."), false);
            return;
        }
        data.putLong("nullarchive_shard_cooldown", now + 20);
        int phase = data.getInt(PHASE_KEY);
        int evidence = evidenceCount(data);
        String response = phase < 4
                ? "NULL SHARD // Cold. It refuses to resonate before the incident."
                : evidence < 3
                ? "NULL SHARD // It vibrates toward missing evidence."
                : phase < 6
                ? "NULL SHARD // A pulse travels down the corridor. It stops at a door that is already open."
                : "NULL SHARD // The shard is silent. Something nearby is listening.";
        event.getEntity().displayClientMessage(Component.literal(response), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(),
                ModSounds.SIGNAL_DISTORTION.get(), SoundSource.PLAYERS, 0.6F, phase < 4 ? 1.4F : 0.5F);
    }
    @SubscribeEvent
    public static void onKeyAndCartridgeUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        var stack = event.getItemStack();
        String response;
        CompoundTag data = event.getEntity().getPersistentData();
        if (stack.is(ModItems.ARCHIVE_KEY.get())) {
            response = data.getBoolean("nullarchive_door_unlocked")
                    ? "ARCHIVE KEY // The teeth are warm. The lock has already accepted you."
                    : data.getInt(PHASE_KEY) < 3
                    ? "ARCHIVE KEY // The teeth are worn smooth. It fits a lock you have not found."
                    : "ARCHIVE KEY // A lock clicks somewhere below the archive.";
        } else if (stack.is(ModItems.RIFT_KEY.get())) {
            response = data.getBoolean("nullarchive_ending_complete")
                    ? "RIFT KEY // The metal is inert. The seal is permanent."
                    : data.getInt(PHASE_KEY) < 5
                    ? "RIFT KEY // It is vibrating. The breach is not ready."
                    : "RIFT KEY // The metal bends toward the rift. Do not turn it.";
        } else if (stack.is(ModItems.TERMINAL_CARTRIDGE.get())) {
            response = evidenceCount(data) < 2
                    ? "CARTRIDGE // DATA CORRUPTED. More evidence required."
                    : (data.getInt(EVIDENCE_FLAGS_KEY) & TERMINAL_FLAG) != 0
                    ? "CARTRIDGE // Query recorded. The file timestamp is tomorrow."
                    : "CARTRIDGE // Recovered directory: /SUBJECT/NULL/EXIT_LOG.txt";
        } else return;
        event.getEntity().displayClientMessage(Component.literal(response), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(), ModSounds.TAPE_CLICK.get(),
                SoundSource.PLAYERS, 0.6F, stack.is(ModItems.RIFT_KEY.get()) ? 0.55F : 1.0F);
    }
}
