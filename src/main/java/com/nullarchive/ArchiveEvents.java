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
    private static final int GLASS_FLAG = 16, TERMINAL_FLAG = 32, DOOR_FLAG = 64;

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

    private ArchiveEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nullarchive")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    CompoundTag data = player.getPersistentData();
                    player.displayClientMessage(Component.literal("NULL ARCHIVE // Phase " + data.getInt(PHASE_KEY)
                            + "/6 // Evidence " + evidenceCount(data) + "/7 // Anomaly " + data.getInt(ANOMALY_KEY)
                            + " // Ending " + (data.getBoolean("nullarchive_ending_complete") ? "COMPLETE" : "INCOMPLETE")), false);
                    return 1;
                }))
                .then(Commands.literal("generate").executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos origin = player.blockPosition().offset(0, -1, 0);
                    generateFacility(level, origin);
                    CompoundTag playerData = player.getPersistentData();
                    playerData.putInt("nullarchive_facility_x", origin.getX());
                    playerData.putInt("nullarchive_facility_y", origin.getY());
                    playerData.putInt("nullarchive_facility_z", origin.getZ());
                    playerData.putString("nullarchive_facility_dimension", level.dimension().location().toString());
                    playerData.remove("nullarchive_ambient_obs");
                    playerData.remove("nullarchive_ambient_corrupt");
                    playerData.remove("nullarchive_ambient_static");
                    playerData.remove("nullarchive_ambient_gate");
                    playerData.remove("nullarchive_ambient_rift");
                    playerData.remove("nullarchive_ambient_cooldown");
                    player.displayClientMessage(Component.literal("NULL ARCHIVE // Facility generated. Start in Intake and follow the corridor."), false);
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    Player player = context.getSource().getPlayerOrException();
                    CompoundTag data = player.getPersistentData();
                    data.remove(PHASE_KEY);
                    data.remove(EVIDENCE_KEY);
                    data.remove(EVIDENCE_FLAGS_KEY);
                    data.remove(ANOMALY_KEY);
                    data.remove("nullarchive_ending_complete");
                    data.remove("nullarchive_door_unlocked");
                    data.remove("nullarchive_facility_x");
                    data.remove("nullarchive_facility_y");
                    data.remove("nullarchive_facility_z");
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
        if (phase + 1 == 6) triggerFinalArchiveShift(event.getEntity());
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
        event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // Unique fragment catalogued: " + evidenceCount(data) + "/7"), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(),
                ModSounds.ARCHIVE_PULSE.get(), SoundSource.PLAYERS, 0.65F, 1.0F);
    }

    @SubscribeEvent
    public static void onEmfUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.EMF_READER.get())) return;
        int phase = event.getEntity().getPersistentData().getInt(PHASE_KEY);
        String reading = phase < 2 ? "EMF // Baseline. No signal."
                : phase < 4 ? "EMF // Weak pulse detected. Direction unstable."
                : phase < 5 ? "EMF // CRITICAL. Signal originates inside the recorder."
                : "EMF // ERROR: Receiver is reading its own serial number.";
        event.getEntity().displayClientMessage(Component.literal(reading), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(),
                phase >= 4 ? ModSounds.SIGNAL_DISTORTION.get() : ModSounds.ARCHIVE_PULSE.get(),
                SoundSource.PLAYERS, 0.7F, phase >= 4 ? 0.65F : 1.0F);
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
            } else if (evidence < 3) {
                event.getEntity().displayClientMessage(Component.literal("SEALED ACCESS // Three indexed clues required for access."), false);
            } else if (!data.getBoolean("nullarchive_door_unlocked")
                    && !event.getItemStack().is(ModItems.ARCHIVE_KEY.get())) {
                event.getEntity().displayClientMessage(Component.literal("SEALED ACCESS // The lock accepts an Archive Key."), false);
            } else {
                if (!data.getBoolean("nullarchive_door_unlocked")) {
                    data.putBoolean("nullarchive_door_unlocked", true);
                    if (!event.getEntity().getAbilities().instabuild) event.getItemStack().shrink(1);
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
                event.getEntity().displayClientMessage(Component.literal("EVIDENCE 05 // Observation glass recorded. Reflection timing is impossible."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(), SoundSource.BLOCKS, 0.7F, 0.65F);
            } else if (phase < 2) {
                event.getEntity().displayClientMessage(Component.literal("OBSERVATION // Nothing unusual. Complete the first two recordings."), false);
            } else {
                event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // Observation glass clue already recorded."), false);
            }
        } else if (block.equals(ModBlocks.MEMORY_BLOCK.get())) {
            if (phase >= 2 && catalogueEvidence(data, TERMINAL_FLAG)) {
                event.getEntity().displayClientMessage(Component.literal("EVIDENCE 06 // Terminal memory recorded. The subject field is blank."), false);
                event.getLevel().playSound(null, event.getPos(), ModSounds.ARCHIVE_PULSE.get(), SoundSource.BLOCKS, 0.7F, 0.8F);
            }
            evidence = evidenceCount(data);
            String output = phase < 2
                    ? "MEMORY TERMINAL // ACCESS DENIED. Complete the intake recordings."
                    : evidence < 1
                    ? "MEMORY TERMINAL // Empty index. Catalogue evidence before querying."
                    : "MEMORY TERMINAL // Indexed fragments: " + evidence + " // Current tape: " + phase + "/6 // Query result: SUBJECT NOT FOUND.";
            event.getEntity().displayClientMessage(Component.literal(output), false);
        } else if (block.equals(ModBlocks.RIFT_FRAME.get())) {
            var held = event.getItemStack();
            if (held.is(ModItems.RIFT_KEY.get())) {
                if (data.getBoolean("nullarchive_ending_complete")) {
                    event.getEntity().displayClientMessage(Component.literal("NULL ARCHIVE // The breach is sealed. There is nothing left to unlock."), false);
                } else if (phase < 6 || evidence < 5) {
                    event.getEntity().displayClientMessage(Component.literal(
                            "RIFT LOCK // Final tape and at least five of seven clues required. Progress: " + phase + "/6 // Evidence: " + evidence + "/7"), false);
                } else {
                    data.putBoolean("nullarchive_ending_complete", true);
                    if (!event.getEntity().getAbilities().instabuild) held.shrink(1);
                    event.getEntity().displayClientMessage(Component.literal(
                            "ENDING // The key turns without touching the lock. The rift folds inward. On the other side, someone finishes your sentence. ARCHIVE CLOSED."), false);
                    event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                            SoundSource.BLOCKS, 1.2F, 0.35F);
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
        int phase = data.getInt(PHASE_KEY);
        int evidence = evidenceCount(data);
        String response = phase < 4
                ? "NULL SHARD // Cold. It refuses to resonate before the incident."
                : evidence < 3
                ? "NULL SHARD // It vibrates toward missing evidence."
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
        if (stack.is(ModItems.ARCHIVE_KEY.get())) {
            response = event.getEntity().getPersistentData().getInt(PHASE_KEY) < 3
                    ? "ARCHIVE KEY // The teeth are worn smooth. It fits a lock you have not found."
                    : "ARCHIVE KEY // A lock clicks somewhere below the archive.";
        } else if (stack.is(ModItems.RIFT_KEY.get())) {
            response = event.getEntity().getPersistentData().getInt(PHASE_KEY) < 5
                    ? "RIFT KEY // It is vibrating. The breach is not ready."
                    : "RIFT KEY // The metal bends toward the rift. Do not turn it.";
        } else if (stack.is(ModItems.TERMINAL_CARTRIDGE.get())) {
            response = evidenceCount(event.getEntity().getPersistentData()) < 2
                    ? "CARTRIDGE // DATA CORRUPTED. More evidence required."
                    : "CARTRIDGE // Recovered directory: /SUBJECT/NULL/EXIT_LOG.txt";
        } else return;
        event.getEntity().displayClientMessage(Component.literal(response), false);
        event.getLevel().playSound(null, event.getEntity().blockPosition(), ModSounds.TAPE_CLICK.get(),
                SoundSource.PLAYERS, 0.6F, stack.is(ModItems.RIFT_KEY.get()) ? 0.55F : 1.0F);
    }
}
