package com.ommods.ompd.compat.jade;

import com.ommods.ompd.OMPD;
import com.ommods.ompd.compat.probe.OMPDProbeInfo;
import com.ommods.ompd.content.DefenseFenceBlock;
import com.ommods.ompd.content.DefenseWallBlock;
import com.ommods.ompd.content.HardenedBlock;
import com.ommods.ompd.content.OwnedDefenseBlockEntity;
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

/** Jade port of the final jar's HWYLA owner readout. */
@WailaPlugin
public class OMPDJadePlugin implements IWailaPlugin {
    private static final Provider PROVIDER = new Provider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PROVIDER, OwnedDefenseBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PROVIDER, HardenedBlock.class);
        registration.registerBlockComponent(PROVIDER, DefenseWallBlock.class);
        registration.registerBlockComponent(PROVIDER, DefenseFenceBlock.class);
    }

    private static final class Provider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
        private static final ResourceLocation UID =
                ResourceLocation.fromNamespaceAndPath(OMPD.MOD_ID, "owned_defense");

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof OwnedDefenseBlockEntity defense) {
                OMPDProbeInfo.gather(tag, defense);
            }
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            for (Component line : OMPDProbeInfo.lines(accessor.getServerData())) {
                tooltip.add(line);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
