package com.ommods.omlib.block;

import com.ommods.omlib.api.network.INetworkCable;
import com.ommods.omlib.api.network.INetworkTile;
import com.ommods.omlib.network.NetworkRegistry;
import com.ommods.omlib.network.OMLibNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class BlockEntityCable extends OMBlockEntity implements INetworkCable {
    private final Map<Direction, Boolean> connections = new EnumMap<>(Direction.class);

    @Nullable
    private OMLibNetwork network;

    // Registrate's BlockEntityFactory is create(BlockEntityType<T>, BlockPos, BlockState), so the
    // type arrives as an argument. Do not reach back into BlockInit for it.
    public BlockEntityCable(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (Direction direction : Direction.values()) {
            connections.put(direction, false);
        }
    }

    @Override
    public boolean shouldConnect(Direction side) {
        return Boolean.TRUE.equals(connections.get(side));
    }

    @Nullable
    @Override
    public OMLibNetwork getNetwork() {
        return network;
    }

    @Override
    public void setNetwork(@Nullable OMLibNetwork network) {
        this.network = network;
    }

    @Nonnull
    @Override
    public String getDeviceName() {
        return "network_cable";
    }

    @Nonnull
    @Override
    public BlockPos getPosition() {
        return this.getBlockPos();
    }

    /** Recomputes which sides have a network neighbour. */
    public void refreshConnections() {
        Level level = this.getLevel();
        if (level == null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = this.getBlockPos().relative(direction);
            connections.put(direction,
                    level.hasChunkAt(neighbour)
                            && level.getBlockEntity(neighbour) instanceof INetworkTile);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        refreshConnections();
        if (network == null) {
            NetworkRegistry.join(level, this);
        }
    }

    /** Called by the block when this cable is broken, so the rest of the run can re-form. */
    public void onRemoved() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide() || network == null) {
            return;
        }
        OMLibNetwork previous = network;
        previous.removeDevice(this);
        this.network = null;
        NetworkRegistry.rebuild(level, previous);
    }

    /**
     * One line for the debug readout. Null safe: network is null between placement and the first
     * join, and upstream dereferenced it here.
     */
    public String describeNetwork() {
        if (network == null) {
            return "no network";
        }
        String name = network.getName().isEmpty() ? "unnamed" : network.getName();
        return name + " " + network.getUuid() + " (" + network.getAllDevices().size() + " devices)";
    }
}
