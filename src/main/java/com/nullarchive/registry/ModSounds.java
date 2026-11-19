package com.nullarchive.registry;

import com.nullarchive.NullArchive;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, NullArchive.MOD_ID);

    public static final RegistryObject<SoundEvent> ARCHIVE_PULSE = register("archive_pulse");
    public static final RegistryObject<SoundEvent> SIGNAL_DISTORTION = register("signal_distortion");
    public static final RegistryObject<SoundEvent> TAPE_CLICK = register("tape_click");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NullArchive.MOD_ID, name);
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUNDS.register(eventBus);
    }
}
