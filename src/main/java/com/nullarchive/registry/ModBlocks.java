package com.nullarchive.registry;

import com.nullarchive.NullArchive;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, NullArchive.MOD_ID);

    public static final RegistryObject<Block> ARCHIVE_CONCRETE = register("archive_concrete", 3.5F, 6.0F);
    public static final RegistryObject<Block> ARCHIVE_TILE = register("archive_tile", 2.0F, 6.0F);
    public static final RegistryObject<Block> BLACK_GLASS = register("black_glass", 0.3F, 0.3F);
    public static final RegistryObject<Block> CORRUPTED_STONE = register("corrupted_stone", 3.0F, 6.0F);
    public static final RegistryObject<Block> NULL_BLOCK = register("null_block", 4.0F, 12.0F);
    public static final RegistryObject<Block> STATIC_BLOCK = register("static_block", 1.5F, 3.0F);
    public static final RegistryObject<Block> MEMORY_BLOCK = register("memory_block", 2.0F, 5.0F);
    public static final RegistryObject<Block> RIFT_FRAME = register("rift_frame", 4.0F, 12.0F);
    public static final RegistryObject<Block> SEALED_DOOR = register("sealed_door", 4.0F, 8.0F);
    public static final RegistryObject<Block> OBSERVATION_GLASS = register("observation_glass", 0.3F, 0.3F);

    private static RegistryObject<Block> register(String name, float hardness, float resistance) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .strength(hardness, resistance)
                .sound(SoundType.STONE)));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
