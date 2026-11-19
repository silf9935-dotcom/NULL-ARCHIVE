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

/**
 * Server-authoritative starter gameplay loop: tapes, evidence, memory terminal,
 * and phase-sensitive EMF readings.
 */
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
        if (event.getLevel().isClientSide()) return;
        if (!event.getItemStack().is(ModItems.FIELD_RECORDER.get())) return;

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
        if (!stack.is(ModItems.EVIDENCE_TAPE.get())
                && !stack.is(ModItems.DAMAGED_PHOTOGRAPH.get())
                && !stack.is(ModItems.MEMORY_FRAGMENT.get())
                && !stack.is(ModItems.SEALED_DOCUMENT.get())) return;

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
        NullArchive.LOGGER.info("Evidence fragment {} catalogued for {}", evidence + 1,
                event.getEntity().getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onEmfUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()) return;
        if (!event.getItemStack().is(ModItems.EMF_READER.get())) return;

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
    public static void onMemoryTerminal(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.MEMORY_BLOCK.get())) return;

        CompoundTag data = event.getEntity().getPersistentData();
        int phase = data.getInt(PHASE_KEY);
        int evidence = data.getInt(EVIDENCE_KEY);
        String output;
        if (phase < 2) {
            output = "MEMORY TERMINAL // ACCESS DENIED. Complete the intake recordings.";
        } else if (evidence < 1) {
            output = "MEMORY TERMINAL // Empty index. Catalogue evidence before querying.";
        } else {
            output = "MEMORY TERMINAL // Indexed fragments: " + evidence
                    + " // Current tape: " + phase + "/6 // Query result: SUBJECT NOT FOUND.";
        }
        event.getEntity().displayClientMessage(Component.literal(output), false);
    }
}
