package com.ommods.ompd;

import com.ommods.omlib.OMLib;
import com.ommods.ompd.content.DefenseTier;
import com.ommods.ompd.content.OMPDBlocks;
import com.ommods.ompd.content.OMPDRecipes;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(OMPD.MOD_ID)
public class OMPD {
    public static final String MOD_ID = "ompd";

    public static final NonNullSupplier<Registrate> REGISTRATE =
            NonNullSupplier.lazy(() -> Registrate.create(MOD_ID));

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Open Modular Passive Defense"))
                    .icon(() -> new ItemStack(OMPDBlocks.HARDENED.get(DefenseTier.TIER1).get()))
                    .displayItems((params, output) -> OMPDBlocks.allItems()
                            .forEach(entry -> output.accept(entry.get())))
                    .build());

    public OMPD() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        OMPDBlocks.register(REGISTRATE.get());
        OMPDRecipes.register(REGISTRATE.get());
        TABS.register(modEventBus);
        OMLib.getLogger().info("OMPD loaded against omlib");
    }
}
