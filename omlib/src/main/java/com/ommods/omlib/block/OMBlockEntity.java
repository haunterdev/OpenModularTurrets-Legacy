package com.ommods.omlib.block;

import com.ommods.omlib.util.OMPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public abstract class OMBlockEntity extends BlockEntity {
    private static final String OWNER_TAG = "owner";

    @Nullable
    protected OMPlayer owner;

    public OMBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Nullable
    public OMPlayer getOwner() {
        return owner;
    }

    public void setOwner(@Nullable OMPlayer owner) {
        this.owner = owner;
    }

    // load and saveAdditional are the hooks vanilla's chunk load and save call. The 1.19.2 base
    // overrode serializeNBT/deserializeNBT instead, which Forge defines as delegating inward to
    // these, so nothing it wrote ever reached disk.
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(OWNER_TAG)) {
            OMPlayer loaded = new OMPlayer();
            loaded.deserializeNBT(tag.getCompound(OWNER_TAG));
            this.owner = loaded;
        } else {
            this.owner = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // An unowned block is normal, and OMPlayer.serializeNBT dereferences its uuid.
        if (owner != null && owner.getUuid() != null) {
            tag.put(OWNER_TAG, owner.serializeNBT());
        }
    }
}
