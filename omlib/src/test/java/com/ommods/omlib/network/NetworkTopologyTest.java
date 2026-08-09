package com.ommods.omlib.network;

import com.ommods.omlib.api.network.INetworkTile;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NetworkTopologyTest {

    static class Tile implements INetworkTile {
        private final BlockPos pos;

        Tile(BlockPos pos) {
            this.pos = pos;
        }

        @Override public OMLibNetwork getNetwork() { return null; }
        @Override public void setNetwork(OMLibNetwork network) { }
        @Nonnull @Override public String getDeviceName() { return "cable"; }
        @Nonnull @Override public BlockPos getPosition() { return pos; }
    }

    /** An in memory world. This is the whole point of TileLookup: no Level needed to test. */
    static class FakeWorld implements NetworkTopology.TileLookup {
        final Map<BlockPos, INetworkTile> tiles = new HashMap<>();

        FakeWorld put(BlockPos pos) {
            tiles.put(pos, new Tile(pos));
            return this;
        }

        @Override
        public INetworkTile at(BlockPos pos) {
            return tiles.get(pos);
        }
    }

    @Test
    void collectsASingleTile() {
        FakeWorld world = new FakeWorld().put(BlockPos.ZERO);

        Map<BlockPos, INetworkTile> found = NetworkTopology.collectConnected(BlockPos.ZERO, world);

        assertEquals(1, found.size());
    }

    @Test
    void collectsAStraightRun() {
        FakeWorld world = new FakeWorld();
        for (int x = 0; x < 5; x++) {
            world.put(new BlockPos(x, 0, 0));
        }

        Map<BlockPos, INetworkTile> found = NetworkTopology.collectConnected(BlockPos.ZERO, world);

        assertEquals(5, found.size());
    }

    @Test
    void stopsAtAGap() {
        FakeWorld world = new FakeWorld()
                .put(new BlockPos(0, 0, 0))
                .put(new BlockPos(1, 0, 0))
                // gap at x=2
                .put(new BlockPos(3, 0, 0));

        Map<BlockPos, INetworkTile> found = NetworkTopology.collectConnected(BlockPos.ZERO, world);

        assertEquals(2, found.size());
        assertFalse(found.containsKey(new BlockPos(3, 0, 0)));
    }

    @Test
    void aLoopTerminates() {
        // A 2x2 ring. Upstream's recursion had no visited set, so this ran until StackOverflow.
        FakeWorld world = new FakeWorld()
                .put(new BlockPos(0, 0, 0))
                .put(new BlockPos(1, 0, 0))
                .put(new BlockPos(0, 0, 1))
                .put(new BlockPos(1, 0, 1));

        Map<BlockPos, INetworkTile> found =
                assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                        () -> NetworkTopology.collectConnected(BlockPos.ZERO, world));

        assertEquals(4, found.size());
    }

    @Test
    void aLongRunDoesNotBlowTheStack() {
        // 5000 cables in a line. Upstream recursed once per cable and would StackOverflow here.
        FakeWorld world = new FakeWorld();
        for (int x = 0; x < 5000; x++) {
            world.put(new BlockPos(x, 0, 0));
        }

        Map<BlockPos, INetworkTile> found = NetworkTopology.collectConnected(BlockPos.ZERO, world);

        assertEquals(5000, found.size());
    }

    @Test
    void anIntactNetworkIsNotSplit() {
        FakeWorld world = new FakeWorld()
                .put(new BlockPos(0, 0, 0))
                .put(new BlockPos(1, 0, 0))
                .put(new BlockPos(2, 0, 0));

        // Upstream's isSplitted() returned true here, so splitNetwork() tore down healthy networks.
        assertFalse(NetworkTopology.isSplit(new HashSet<>(world.tiles.keySet()), world));
    }

    @Test
    void aBrokenNetworkIsSplit() {
        FakeWorld world = new FakeWorld()
                .put(new BlockPos(0, 0, 0))
                .put(new BlockPos(1, 0, 0))
                .put(new BlockPos(3, 0, 0));
        Set<BlockPos> devices = new HashSet<>(world.tiles.keySet());
        // The cable at x=2 was mined, so x=3 is stranded.

        assertTrue(NetworkTopology.isSplit(devices, world));
    }

    @Test
    void aSingleDeviceIsNeverSplit() {
        FakeWorld world = new FakeWorld().put(BlockPos.ZERO);

        assertFalse(NetworkTopology.isSplit(new HashSet<>(world.tiles.keySet()), world));
    }

    @Test
    void anEmptyLookupCollectsNothing() {
        FakeWorld world = new FakeWorld();

        assertTrue(NetworkTopology.collectConnected(BlockPos.ZERO, world).isEmpty());
    }
}
