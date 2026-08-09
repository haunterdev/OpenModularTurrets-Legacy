package com.ommods.openmodularturrets.content.base;

import net.minecraftforge.energy.EnergyStorage;

/**
 * The base's energy buffer. It behaves like a normal Forge {@link EnergyStorage} for cables and
 * generators (receive is capped by the tier transfer rate, export is disabled), but adds
 * {@link #generate(int)} so the solar and redstone-reactor addons can inject their own power directly,
 * bypassing the transfer cap - faithful to 1.12, where the reactor called {@code modifyEnergyStored}
 * rather than the throttled receive path.
 */
public class BaseEnergyStorage extends EnergyStorage {

    public BaseEnergyStorage(int capacity, int maxReceive) {
        super(capacity, maxReceive, 0);
    }

    /** Add power from an internal addon, ignoring the transfer rate, clamped to capacity. */
    public void generate(int amount) {
        this.energy = Math.min(this.energy + amount, this.capacity);
    }

    /**
     * Resize the buffer, e.g. when power expanders are added or removed. Faithful to 1.12's per-tick
     * {@code storage.setCapacity(getMaxEnergyStorageWithExtenders())}: stored energy is clamped down if
     * the new capacity is smaller.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        if (this.energy > capacity) {
            this.energy = capacity;
        }
    }
}
