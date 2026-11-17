package com.nullarchive.registry;

import com.nullarchive.NullArchive;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NullArchive.MOD_ID);

    public static final RegistryObject<CreativeModeTab> NULL_ARCHIVE_TAB = TABS.register("null_archive",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.nullarchive"))
                    .icon(() -> new ItemStack(ModItems.FIELD_RECORDER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ARCHIVE_CONCRETE.get());
                        output.accept(ModItems.ARCHIVE_TILE.get());
                        output.accept(ModItems.BLACK_GLASS.get());
                        output.accept(ModItems.CORRUPTED_STONE.get());
                        output.accept(ModItems.NULL_BLOCK.get());
                        output.accept(ModItems.STATIC_BLOCK.get());
                        output.accept(ModItems.MEMORY_BLOCK.get());
                        output.accept(ModItems.RIFT_FRAME.get());
                        output.accept(ModItems.SEALED_DOOR.get());
                        output.accept(ModItems.OBSERVATION_GLASS.get());
                        output.accept(ModItems.FIELD_RECORDER.get());
                        output.accept(ModItems.EMF_READER.get());
                        output.accept(ModItems.EVIDENCE_TAPE.get());
                        output.accept(ModItems.DAMAGED_PHOTOGRAPH.get());
                        output.accept(ModItems.ARCHIVE_KEY.get());
                        output.accept(ModItems.RIFT_KEY.get());
                        output.accept(ModItems.TERMINAL_CARTRIDGE.get());
                        output.accept(ModItems.MEMORY_FRAGMENT.get());
                        output.accept(ModItems.NULL_SHARD.get());
                        output.accept(ModItems.SEALED_DOCUMENT.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
