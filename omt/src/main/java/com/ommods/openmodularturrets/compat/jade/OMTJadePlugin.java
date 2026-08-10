package com.ommods.openmodularturrets.compat.jade;

import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.compat.probe.TurretProbeInfo;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.block.TurretBaseBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade integration, ported from the 1.12 HWYLA handlers (OMTWailaWrapper, WailaTurretBaseHandler,
 * WailaTurretHandler). Looking at a turret base shows its redstone mode, whether it is active and who
 * owns it; looking at a head adds the loaded ammo, the damage boost, the accuracy and the rate of
 * fire. The text itself comes from {@link TurretProbeInfo}.
 *
 * <p>None of that is readable on the client, so it travels the way 1.12's handlers sent it: a server
 * data provider packs the fields into the tag Jade ships with the block, and the component provider
 * unpacks them. Jade is a compile-only dependency and finds this class by its own annotation scan, so
 * it is only ever loaded when Jade is installed.
 */
@WailaPlugin
public class OMTJadePlugin implements IWailaPlugin {

    private static final ResourceLocation BASE_UID =
            new ResourceLocation(OpenModularTurrets.MOD_ID, "turret_base");
    private static final ResourceLocation HEAD_UID =
            new ResourceLocation(OpenModularTurrets.MOD_ID, "turret_head");

    private static final Provider BASE = new Provider(BASE_UID);
    private static final Provider HEAD = new Provider(HEAD_UID);

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(BASE, TurretBaseTileEntity.class);
        registration.registerBlockDataProvider(HEAD, TurretBaseBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(BASE, com.ommods.openmodularturrets.content.base.TurretBaseBlock.class);
        registration.registerBlockComponent(HEAD, com.ommods.openmodularturrets.content.block.TurretBaseBlock.class);
    }

    /**
     * One class for both halves of the round trip. Which block it is deciding what to gather keeps the
     * base and the head from needing separate providers, since neither reports a field the other could
     * misread.
     */
    private record Provider(ResourceLocation uid) implements IServerDataProvider<BlockAccessor>,
            IBlockComponentProvider {

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            TurretProbeInfo.gather(tag, accessor.getBlockEntity(), accessor.getLevel(), accessor.getPosition());
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            for (Component line : TurretProbeInfo.lines(accessor.getServerData())) {
                tooltip.add(line);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }
    }
}
