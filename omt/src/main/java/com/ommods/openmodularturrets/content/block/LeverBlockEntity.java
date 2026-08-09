package com.ommods.openmodularturrets.content.block;

import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The hand crank's state, faithful to 1.12 {@code LeverTileEntity}: while turning, the crank spins 20
 * degrees a tick through one full revolution, then stops. It only attaches to a tier 1 base on one of
 * the four horizontal sides and pops off when the base goes. The energy grant happens in
 * {@link LeverBlock#use} at the start of a crank, like the 1.12 block did.
 */
public class LeverBlockEntity extends OMBlockEntity {

    public float rotation = 0.0f;
    public boolean isTurning = false;

    public LeverBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state) {
        super(beType, pos, state);
    }

    /** The tier 1 base this crank feeds, from the four horizontal neighbours. */
    @Nullable
    public TurretBaseTileEntity findBase() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(getBlockPos().relative(facing)) instanceof TurretBaseTileEntity base
                    && base.tier().tier() == 1) {
                return base;
            }
        }
        return null;
    }

    /** The direction from the crank toward its base, for the renderer; null with no base. */
    @Nullable
    public Direction baseDirection() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(getBlockPos().relative(facing)) instanceof TurretBaseTileEntity base
                    && base.tier().tier() == 1) {
                return facing;
            }
        }
        return null;
    }

    // Runs on both sides: the server owns the crank cycle, the client mirrors it for the renderer.
    public static void tick(Level level, BlockPos pos, BlockState state, LeverBlockEntity be) {
        if (be.isTurning) {
            be.rotation += 20.0f;
            if (be.rotation >= 360.0f) {
                be.isTurning = false;
                be.rotation = 0.0f;
            }
        }
        if (!level.isClientSide() && level.getGameTime() % 15L == 0L && be.findBase() == null) {
            level.destroyBlock(pos, true);
        }
    }

    /** Server side: start a crank turn. Returns true if it started (was not already turning). */
    public boolean startTurning() {
        if (isTurning) {
            return false;
        }
        isTurning = true;
        setChanged();
        Level level = getLevel();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
        return true;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("turning", isTurning);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("turning")) {
            isTurning = tag.getBoolean("turning");
        }
    }
}
