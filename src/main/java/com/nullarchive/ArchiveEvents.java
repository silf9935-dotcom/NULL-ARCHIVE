package com.nullarchive;

import com.nullarchive.registry.ModBlocks;
import com.nullarchive.registry.ModItems;
import com.nullarchive.registry.ModSounds;
import net.minecraft.network.chat.Component;
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
    public static void onRecorderUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide() || !event.getItemStack().is(ModItems.FIELD_RECORDER.get())) return;
        CompoundTag data = event.getEntity().getPersistentData();
        int phase = data.getInt(PHASE_KEY);
        int evidence = data.getInt(EVIDENCE_KEY);

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
        NullArchive.LOGGER.info("Archive phase {} recorded for {}", phase + 1,
                event.getEntity().getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onEvidenceUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        var stack = event.getItemStack();
        if (!stack.is(ModItems.EVIDENCE_TAPE.get()) && !stack.is(ModItems.DAMAGED_PHOTOGRAPH.get())
                && !stack.is(ModItems.MEMORY_FRAGMENT.get()) && !stack.is(ModItems.SEALED_DOCUMENT.get())) return;

        CompoundTag data = event.getEntity().getPersistentData();
        int evidence = data.getInt(EVIDENCE_KEY);
        if (evidence >= 5) {
            event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // Archive limit reached."), false);
            return;
        }
        data.putInt(EVIDENCE_KEY, evidence + 1);
        event.getEntity().displayClientMessage(Component.literal("EVIDENCE INDEX // Fragment catalogued: " + (evidence + 1) + "/5"), false);
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
        int evidence = data.getInt(EVIDENCE_KEY);

        if (block.equals(ModBlocks.MEMORY_BLOCK.get())) {
            String output = phase < 2
                    ? "MEMORY TERMINAL // ACCESS DENIED. Complete the intake recordings."
                    : evidence < 1
                    ? "MEMORY TERMINAL // Empty index. Catalogue evidence before querying."
                    : "MEMORY TERMINAL // Indexed fragments: " + evidence + " // Current tape: " + phase + "/6 // Query result: SUBJECT NOT FOUND.";
            event.getEntity().displayClientMessage(Component.literal(output), false);
        } else if (block.equals(ModBlocks.SEALED_DOOR.get())) {
            String output = phase < 4
                    ? "SEALED ACCESS // Lock status: RED. Incident authorization missing."
                    : evidence < 3
                    ? "SEALED ACCESS // Authorization accepted. Evidence index incomplete."
                    : "SEALED ACCESS // Seal is intact. Door state: OPEN (impossible).";
            event.getEntity().displayClientMessage(Component.literal(output), false);
            if (phase >= 4 && evidence >= 3) {
                event.getLevel().playSound(null, event.getPos(), ModSounds.SIGNAL_DISTORTION.get(),
                        SoundSource.BLOCKS, 1.0F, 0.6F);
            }
        } else if (block.equals(ModBlocks.RIFT_FRAME.get())) {
            String output = phase < 5
                    ? "RIFT MONITOR // No breach detected. Keep the recorder active."
                    : "RIFT MONITOR // BREACH CONFIRMED. The other side is transmitting your location.";
            event.getEntity().displayClientMessage(Component.literal(output), false);
            if (phase >= 5) event.getLevel().playSound(null, event.getPos(),
                    ModSounds.SIGNAL_DISTORTION.get(), SoundSource.BLOCKS, 1.0F, 0.5F);
        }
    }
}
