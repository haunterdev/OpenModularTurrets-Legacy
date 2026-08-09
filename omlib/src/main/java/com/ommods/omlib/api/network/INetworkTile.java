package com.ommods.omlib.api.network;

import com.ommods.omlib.network.OMLibNetwork;
import net.minecraft.core.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A block entity that can join a cable network.
 *
 * Upstream put scan, createNetwork and recursAddDevice here as default methods. They were
 * untestable, recursAddDevice never recursed or added anything, and the traversal blew the stack
 * on any cable loop. Traversal lives in NetworkTopology now.
 */
public interface INetworkTile {

    /** The network this tile currently belongs to, or null before it has joined one. */
    @Nullable
    OMLibNetwork getNetwork();

    void setNetwork(@Nullable OMLibNetwork network);

    @Nonnull
    String getDeviceName();

    @Nonnull
    BlockPos getPosition();
}
