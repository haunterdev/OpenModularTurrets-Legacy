package com.ommods.ompd.content;

import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.omlib.util.permission.IHasTrustManager;
import com.ommods.omlib.util.permission.TrustManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Owner and per-block trust state shared by hardened blocks, walls, and fences. */
public class OwnedDefenseBlockEntity extends OMBlockEntity implements IHasTrustManager {
    private static final String TIER_TAG = "tier";
    private static final String TRUST_TAG = "trust";

    private final TrustManager trustManager = new TrustManager();
    private DefenseTier tier;

    public OwnedDefenseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tier = state != null && state.getBlock() instanceof TieredDefenseBlock block
                ? block.tier()
                : DefenseTier.TIER1;
    }

    public DefenseTier tier() {
        return tier;
    }

    @Override
    public TrustManager getTrustManager() {
        return trustManager;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int metadata = tag.getInt(TIER_TAG);
        tier = metadata >= 0 && metadata < DefenseTier.values().length
                ? DefenseTier.values()[metadata]
                : DefenseTier.TIER1;
        if (tag.contains(TRUST_TAG)) {
            trustManager.readFromNBT(tag.getCompound(TRUST_TAG));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TIER_TAG, tier.legacyMetadata());
        tag.put(TRUST_TAG, trustManager.writeToNBT(new CompoundTag()));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (owner != null && owner.getUuid() != null) {
            tag.put("owner", owner.serializeNBT());
        }
        tag.putInt(TIER_TAG, tier.legacyMetadata());
        tag.put(TRUST_TAG, trustManager.writeToNBT(new CompoundTag()));
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
