package com.nullarchive;

import com.mojang.logging.LogUtils;
import com.nullarchive.registry.ModBlocks;
import com.nullarchive.registry.ModCreativeTabs;
import com.nullarchive.registry.ModItems;
import com.nullarchive.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(NullArchive.MOD_ID)
public final class NullArchive {
    public static final String MOD_ID = "nullarchive";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NullArchive() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModSounds.register(modEventBus);
        LOGGER.info("NULL ARCHIVE initialized. The archive is listening.");
    }
}
