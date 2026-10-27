package com.ommods.ompd.client;

import com.ommods.ompd.OMPD;
import com.ommods.ompd.content.CamoTrapBlockEntity;
import com.ommods.ompd.content.DefenseTier;
import com.ommods.ompd.content.OMPDBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only model and cutout setup for the original transparent fences and camouflage trap. */
@Mod.EventBusSubscriber(modid = OMPD.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OMPDClient {
    private OMPDClient() {
    }

    @SuppressWarnings("deprecation")
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (DefenseTier tier : DefenseTier.values()) {
                ItemBlockRenderTypes.setRenderLayer(OMPDBlocks.FENCES.get(tier).get(), RenderType.cutout());
            }
        });
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation location = new ModelResourceLocation(OMPD.MOD_ID, "camo_trap", "");
        BakedModel original = event.getModels().get(location);
        if (original != null) {
            event.getModels().put(location, new CamoTrapBakedModel(original));
        }
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, getter, pos, tintIndex) -> {
            if (getter != null && pos != null
                    && getter.getBlockEntity(pos) instanceof CamoTrapBlockEntity trap
                    && trap.getCamoState() != null) {
                return Minecraft.getInstance().getBlockColors()
                        .getColor(trap.getCamoState(), getter, pos, tintIndex);
            }
            return -1;
        }, OMPDBlocks.CAMO_TRAP.get());
    }
}
