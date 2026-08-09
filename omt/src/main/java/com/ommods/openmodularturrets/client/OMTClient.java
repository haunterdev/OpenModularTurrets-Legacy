package com.ommods.openmodularturrets.client;

import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.client.projectile.EmptyProjectileRenderer;
import com.ommods.openmodularturrets.client.projectile.ItemProjectileRenderer;
import com.ommods.openmodularturrets.client.projectile.RocketProjectileRenderer;
import com.ommods.openmodularturrets.client.turret.AddonModels;
import com.ommods.openmodularturrets.client.turret.LeverItemRenderer;
import com.ommods.openmodularturrets.client.turret.LeverRenderer;
import com.ommods.openmodularturrets.client.turret.TurretItemRenderer;
import com.ommods.openmodularturrets.client.turret.TurretModels;
import com.ommods.openmodularturrets.client.turret.TurretRenderer;
import com.ommods.openmodularturrets.content.base.BaseInit;
import com.ommods.openmodularturrets.content.base.BaseTier;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.block.OMTBlockInit;
import com.ommods.openmodularturrets.content.item.OMTItems;
import com.ommods.openmodularturrets.content.projectile.BlazingClayProjectileEntity;
import com.ommods.openmodularturrets.content.projectile.BulletProjectileEntity;
import com.ommods.openmodularturrets.content.projectile.DisposableProjectileEntity;
import com.ommods.openmodularturrets.content.projectile.GrenadeProjectileEntity;
import com.ommods.openmodularturrets.content.projectile.OMTEntities;
import com.ommods.openmodularturrets.content.turret.TurretType;
import net.minecraft.world.item.ItemStack;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client only setup: registers the turret head model layers and the block entity renderer that draws
 * them. Marked Dist.CLIENT so the class and everything it references (the render classes) never load
 * on a dedicated server.
 */
@Mod.EventBusSubscriber(modid = OpenModularTurrets.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OMTClient {

    private static TurretItemRenderer turretItemRenderer;
    private static LeverItemRenderer leverItemRenderer;

    private OMTClient() {
    }

    /** Lazily built client only item renderer shared by every turret head item. */
    public static TurretItemRenderer turretItemRenderer() {
        if (turretItemRenderer == null) {
            turretItemRenderer = new TurretItemRenderer();
        }
        return turretItemRenderer;
    }

    /** Lazily built client only item renderer for the crank lever item. */
    public static LeverItemRenderer leverItemRenderer() {
        if (leverItemRenderer == null) {
            leverItemRenderer = new LeverItemRenderer();
        }
        return leverItemRenderer;
    }

    /** Wrap the five base models so a camouflaged base renders as its disguise block. */
    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (BaseTier tier : BaseTier.values()) {
            ModelResourceLocation location =
                    new ModelResourceLocation(OpenModularTurrets.MOD_ID, tier.registryName(), "");
            BakedModel original = event.getModels().get(location);
            if (original != null) {
                event.getModels().put(location, new CamoBakedModel(original));
            }
        }
    }

    /** Delegate block tints (grass, leaves) to the camo block so its colors carry over. */
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, getter, pos, tintIndex) -> {
            if (getter != null && pos != null
                    && getter.getBlockEntity(pos) instanceof TurretBaseTileEntity base
                    && base.getCamoState() != null) {
                return Minecraft.getInstance().getBlockColors()
                        .getColor(base.getCamoState(), getter, pos, tintIndex);
            }
            return -1;
        }, BaseInit.BASES.values().stream().map(BlockEntry::get).toArray(Block[]::new));
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (TurretType type : TurretType.values()) {
            event.registerLayerDefinition(TurretModels.layerFor(type), () -> TurretModels.createLayer(type));
        }
        event.registerLayerDefinition(LeverRenderer.LAYER, LeverRenderer::createLayer);
        event.registerLayerDefinition(AddonModels.DAMAGE_AMP, AddonModels::createDamageAmp);
        event.registerLayerDefinition(AddonModels.SOLAR_PANEL, AddonModels::createSolarPanel);
        event.registerLayerDefinition(AddonModels.REDSTONE_REACTOR, AddonModels::createRedstoneReactor);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OMTBlockInit.TURRET_ENTITY.get(), TurretRenderer::new);
        event.registerBlockEntityRenderer(OMTBlockInit.LEVER_ENTITY.get(), LeverRenderer::new);
        // Projectile renderers, matching 1.12 ProjectileRenderers one for one: three rounds are drawn
        // as a fixed item, the disposable draws whatever round it threw, the rocket has its own model,
        // and the potato gets no renderer at all, so what the player sees is the item bound to the shot.
        event.registerEntityRenderer(OMTEntities.BULLET.get(),
                context -> new ItemProjectileRenderer<BulletProjectileEntity>(context,
                        e -> new ItemStack(OMTItems.THROWABLE_BULLET.get())));
        event.registerEntityRenderer(OMTEntities.GRENADE.get(),
                context -> new ItemProjectileRenderer<GrenadeProjectileEntity>(context,
                        e -> new ItemStack(OMTItems.AMMO_GRENADE.get())));
        event.registerEntityRenderer(OMTEntities.BLAZING_CLAY.get(),
                context -> new ItemProjectileRenderer<BlazingClayProjectileEntity>(context,
                        e -> new ItemStack(OMTItems.AMMO_BLAZING_CLAY.get())));
        event.registerEntityRenderer(OMTEntities.DISPOSABLE.get(),
                context -> new ItemProjectileRenderer<DisposableProjectileEntity>(context,
                        DisposableProjectileEntity::thrownAmmo));
        event.registerEntityRenderer(OMTEntities.ROCKET.get(), RocketProjectileRenderer::new);
        event.registerEntityRenderer(OMTEntities.POTATO.get(), EmptyProjectileRenderer::new);
    }
}
