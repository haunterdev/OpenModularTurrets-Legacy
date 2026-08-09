package com.ommods.omlib.network;

import com.ommods.omlib.api.network.IPowerExchangeTile;
import net.minecraft.core.BlockPos;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PowerDistributorTest {

    /** Test double. IPowerExchangeTile has no Minecraft dependency beyond BlockPos. */
    static class Tile implements IPowerExchangeTile {
        private final EnergyStorage storage;
        private final boolean delivers;
        private final BlockPos pos;

        Tile(EnergyStorage storage, boolean delivers, int x) {
            this.storage = storage;
            this.delivers = delivers;
            this.pos = new BlockPos(x, 0, 0);
        }

        @Override public boolean requiresEnergy() { return !delivers; }
        @Override public boolean deliversEnergy() { return delivers; }
        @Override public IEnergyStorage getEnergyStorage() { return storage; }
        @Override public OMLibNetwork getNetwork() { return null; }
        @Override public void setNetwork(OMLibNetwork network) { }
        @Nonnull @Override public String getDeviceName() { return "test"; }
        @Nonnull @Override public BlockPos getPosition() { return pos; }
    }

    private static Tile producer(int stored, int x) {
        return new Tile(new EnergyStorage(10000, 10000, 10000, stored), true, x);
    }

    private static Tile consumer(int stored, int x) {
        return new Tile(new EnergyStorage(10000, 10000, 10000, stored), false, x);
    }

    @Test
    void fullProducerDeliversItsStoredEnergy() {
        // Deliberately full: capacity equals stored. Upstream sized a producer's output from
        // getMaxEnergyStored() - getEnergyStored(), which is free space, so for this storage it
        // computed 0 and a full battery delivered nothing at all.
        Tile p = new Tile(new EnergyStorage(1000, 10000, 10000, 1000), true, 0);
        Tile c = consumer(0, 1);

        int moved = PowerDistributor.distribute(List.of(p, c));

        assertEquals(1000, moved);
        assertEquals(0, p.getEnergyStorage().getEnergyStored());
        assertEquals(1000, c.getEnergyStorage().getEnergyStored());
    }

    @Test
    void energyIsConservedNotCreated() {
        Tile p = producer(5000, 0);
        Tile c = consumer(100, 1);
        int before = p.getEnergyStorage().getEnergyStored() + c.getEnergyStorage().getEnergyStored();

        PowerDistributor.distribute(List.of(p, c));

        int after = p.getEnergyStorage().getEnergyStored() + c.getEnergyStorage().getEnergyStored();
        // Upstream never extracted from producers, so the total grew every tick.
        assertEquals(before, after);
    }

    @Test
    void movesNoMoreThanDemand() {
        Tile p = producer(10000, 0);
        Tile c = new Tile(new EnergyStorage(500, 10000, 10000, 0), false, 1);

        int moved = PowerDistributor.distribute(List.of(p, c));

        assertEquals(500, moved);
        assertEquals(9500, p.getEnergyStorage().getEnergyStored());
        assertEquals(500, c.getEnergyStorage().getEnergyStored());
    }

    @Test
    void movesNoMoreThanSupply() {
        Tile p = producer(200, 0);
        Tile c = consumer(0, 1);

        int moved = PowerDistributor.distribute(List.of(p, c));

        assertEquals(200, moved);
        assertEquals(0, p.getEnergyStorage().getEnergyStored());
        assertEquals(200, c.getEnergyStorage().getEnergyStored());
    }

    @Test
    void withoutConsumersNothingLeavesTheProducer() {
        Tile p = producer(1000, 0);

        int moved = PowerDistributor.distribute(List.of(p));

        assertEquals(0, moved);
        assertEquals(1000, p.getEnergyStorage().getEnergyStored());
    }

    @Test
    void withoutProducersConsumersGetNothing() {
        Tile c = consumer(0, 0);

        int moved = PowerDistributor.distribute(List.of(c));

        assertEquals(0, moved);
        assertEquals(0, c.getEnergyStorage().getEnergyStored());
    }

    @Test
    void twoProducersFillOneConsumer() {
        Tile p1 = producer(300, 0);
        Tile p2 = producer(300, 1);
        Tile c = consumer(0, 2);

        int moved = PowerDistributor.distribute(List.of(p1, p2, c));

        assertEquals(600, moved);
        assertEquals(600, c.getEnergyStorage().getEnergyStored());
        assertEquals(0, p1.getEnergyStorage().getEnergyStored() + p2.getEnergyStorage().getEnergyStored());
    }

    @Test
    void tilesWithoutStorageAreIgnored() {
        Tile p = producer(500, 0);
        Tile broken = new Tile(null, false, 1);

        assertDoesNotThrow(() -> PowerDistributor.distribute(List.of(p, broken)));
        assertEquals(500, p.getEnergyStorage().getEnergyStored());
    }
}
