package com.ommods.omlib.network;

import com.ommods.omlib.api.network.IPowerExchangeTile;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Moves energy from producers to consumers across one network tick.
 *
 * Pure: no Level, no block entities beyond the interface, so it is unit tested against real
 * Forge EnergyStorage instances.
 *
 * Upstream got two things wrong here. It sized a producer's output from its free space rather
 * than its stored energy, so a full battery delivered nothing. And it called receiveEnergy on
 * consumers without ever calling extractEnergy on producers, so every tick created energy from
 * nothing. This uses the IEnergyStorage simulate contract on both sides and moves only what it
 * has actually taken.
 */
public final class PowerDistributor {

    private PowerDistributor() {
    }

    /**
     * @param tiles every device on the network, in any order
     * @return the energy actually moved
     */
    public static int distribute(List<IPowerExchangeTile> tiles) {
        List<IEnergyStorage> producers = new ArrayList<>();
        List<IEnergyStorage> consumers = new ArrayList<>();

        for (IPowerExchangeTile tile : tiles) {
            IEnergyStorage storage = tile.getEnergyStorage();
            if (storage == null) {
                continue;
            }
            if (tile.deliversEnergy()) {
                producers.add(storage);
            } else if (tile.requiresEnergy()) {
                consumers.add(storage);
            }
        }

        if (producers.isEmpty() || consumers.isEmpty()) {
            return 0;
        }

        int supply = 0;
        for (IEnergyStorage producer : producers) {
            supply += producer.extractEnergy(Integer.MAX_VALUE, true);
        }
        int demand = 0;
        for (IEnergyStorage consumer : consumers) {
            demand += consumer.receiveEnergy(Integer.MAX_VALUE, true);
        }

        int budget = Math.min(supply, demand);
        if (budget <= 0) {
            return 0;
        }

        int taken = move(producers, budget, false);
        int given = move(consumers, taken, true);

        int undelivered = taken - given;
        if (undelivered > 0) {
            // A consumer accepted less than it simulated. Give it back rather than destroy it.
            move(producers, undelivered, true);
        }
        return given;
    }

    /**
     * Fills or drains storages in order until budget is met.
     *
     * The fill is sequential rather than an even split. Upstream attempted an even split with
     * integer division plus a running subtraction that skewed the shares; sequential conserves
     * energy exactly and is simple. Even distribution can come later if it is ever asked for.
     *
     * @param receiving true to push energy in, false to pull it out
     * @return the amount actually moved
     */
    private static int move(List<IEnergyStorage> storages, int budget, boolean receiving) {
        int moved = 0;
        for (IEnergyStorage storage : storages) {
            if (moved >= budget) {
                break;
            }
            int remaining = budget - moved;
            moved += receiving
                    ? storage.receiveEnergy(remaining, false)
                    : storage.extractEnergy(remaining, false);
        }
        return moved;
    }
}
