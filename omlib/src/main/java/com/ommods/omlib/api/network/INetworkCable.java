package com.ommods.omlib.api.network;

import net.minecraft.core.Direction;

public interface INetworkCable extends INetworkTile {

    /** True if the cable should render and link a connection on this side. */
    boolean shouldConnect(Direction side);
}
