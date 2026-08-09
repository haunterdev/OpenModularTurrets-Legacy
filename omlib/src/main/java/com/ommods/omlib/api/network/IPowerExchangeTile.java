package com.ommods.omlib.api.network;

import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * A network device that produces or consumes energy.
 *
 * Upstream returned its own OMEnergyStorage here, wrapping three power backends. 1.20.1 has Forge
 * Energy natively, so this is IEnergyStorage and the rf/ic2/tesla wrappers are gone.
 */
public interface IPowerExchangeTile extends INetworkTile {

    /** True if this device wants energy from the network. */
    boolean requiresEnergy();

    /** True if this device offers energy to the network. */
    boolean deliversEnergy();

    /** The backing storage, or null if this device has none yet. */
    @Nullable
    IEnergyStorage getEnergyStorage();
}
