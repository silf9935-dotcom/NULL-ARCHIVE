package com.nullarchive.registry;

import com.nullarchive.NullArchive;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, NullArchive.MOD_ID);

    // Block items
    public static final RegistryObject<Item> ARCHIVE_CONCRETE = blockItem("archive_concrete", ModBlocks.ARCHIVE_CONCRETE);
    public static final RegistryObject<Item> ARCHIVE_TILE = blockItem("archive_tile", ModBlocks.ARCHIVE_TILE);
    public static final RegistryObject<Item> BLACK_GLASS = blockItem("black_glass", ModBlocks.BLACK_GLASS);
    public static final RegistryObject<Item> CORRUPTED_STONE = blockItem("corrupted_stone", ModBlocks.CORRUPTED_STONE);
    public static final RegistryObject<Item> NULL_BLOCK = blockItem("null_block", ModBlocks.NULL_BLOCK);
    public static final RegistryObject<Item> STATIC_BLOCK = blockItem("static_block", ModBlocks.STATIC_BLOCK);
    public static final RegistryObject<Item> MEMORY_BLOCK = blockItem("memory_block", ModBlocks.MEMORY_BLOCK);
    public static final RegistryObject<Item> RIFT_FRAME = blockItem("rift_frame", ModBlocks.RIFT_FRAME);
    public static final RegistryObject<Item> SEALED_DOOR = blockItem("sealed_door", ModBlocks.SEALED_DOOR);
    public static final RegistryObject<Item> OBSERVATION_GLASS = blockItem("observation_glass", ModBlocks.OBSERVATION_GLASS);

    // Investigation equipment and evidence
    public static final RegistryObject<Item> FIELD_RECORDER = simple("field_recorder");
    public static final RegistryObject<Item> EMF_READER = simple("emf_reader");
    public static final RegistryObject<Item> EVIDENCE_TAPE = simple("evidence_tape");
    public static final RegistryObject<Item> DAMAGED_PHOTOGRAPH = simple("damaged_photograph");
    public static final RegistryObject<Item> ARCHIVE_KEY = simple("archive_key");
    public static final RegistryObject<Item> RIFT_KEY = simple("rift_key");
    public static final RegistryObject<Item> TERMINAL_CARTRIDGE = simple("terminal_cartridge");
    public static final RegistryObject<Item> MEMORY_FRAGMENT = simple("memory_fragment");
    public static final RegistryObject<Item> NULL_SHARD = simple("null_shard");
    public static final RegistryObject<Item> SEALED_DOCUMENT = simple("sealed_document");

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static <T extends net.minecraft.world.level.block.Block> RegistryObject<Item> blockItem(String name, RegistryObject<T> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
