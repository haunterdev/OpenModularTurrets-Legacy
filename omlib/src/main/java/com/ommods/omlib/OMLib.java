package com.ommods.omlib;

import com.ommods.omlib.block.BlockInit;
import com.ommods.omlib.command.OMLibCommands;
import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.network.NetworkRegistry;
import com.ommods.omlib.network.OMLibChannel;
import com.ommods.omlib.reference.Reference;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Reference.MOD_ID)
public class OMLib {
    private static final Logger LOGGER = LoggerFactory.getLogger(Reference.NAME);

    public static final NonNullSupplier<Registrate> REGISTRATE =
            NonNullSupplier.lazy(() -> Registrate.create(Reference.MOD_ID));

    public OMLib() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OMConfig.commonConfigSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, OMConfig.serverConfigSpec);
        // NetworkRegistry's handlers are static and live on the Forge bus, not the mod bus.
        MinecraftForge.EVENT_BUS.register(NetworkRegistry.class);
        MinecraftForge.EVENT_BUS.register(OMLibCommands.class);
        OMLibChannel.register();
        BlockInit.register(REGISTRATE.get());
        LOGGER.info("{} {} loading", Reference.NAME, Reference.VERSION);
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
