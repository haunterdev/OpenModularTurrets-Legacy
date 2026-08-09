package com.ommods.omlib.network;

import com.ommods.omlib.api.network.INetworkTile;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Network formation, bridging and splitting.
 *
 * This is the behaviour the in game gate would check by hand: place cables, watch them join, mine
 * one out of the middle, watch the run split. It runs here instead because NetworkGraph reaches
 * the world only through a TileLookup.
 */
class NetworkGraphTest {

    static class Cable implements INetworkTile {
        private final BlockPos pos;
        private OMLibNetwork network;

        Cable(BlockPos pos) {
            this.pos = pos;
        }

        @Override public OMLibNetwork getNetwork() { return network; }
        @Override public void setNetwork(OMLibNetwork network) { this.network = network; }
        @Nonnull @Override public String getDeviceName() { return "cable"; }
        @Nonnull @Override public BlockPos getPosition() { return pos; }
    }

    /** An in memory world standing in for a Level. */
    static class FakeWorld implements NetworkTopology.TileLookup {
        final Map<BlockPos, INetworkTile> tiles = new HashMap<>();

        Cable place(int x) {
            return place(new BlockPos(x, 0, 0));
        }

        Cable place(BlockPos pos) {
            Cable cable = new Cable(pos);
            tiles.put(pos, cable);
            return cable;
        }

        void mine(Cable cable) {
            tiles.remove(cable.getPosition());
        }

        @Override
        public INetworkTile at(BlockPos pos) {
            return tiles.get(pos);
        }
    }

    private NetworkGraph graph;
    private FakeWorld world;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        world = new FakeWorld();
    }

    @Test
    void aLoneCableGetsItsOwnNetwork() {
        Cable cable = world.place(0);

        OMLibNetwork network = graph.joinOrCreate(world, cable);

        assertNotNull(network);
        assertSame(network, cable.getNetwork());
        assertEquals(1, network.getAllDevices().size());
        assertEquals(1, graph.networkCount());
    }

    @Test
    void aNeighbouringCableJoinsTheSameNetwork() {
        Cable first = world.place(0);
        graph.joinOrCreate(world, first);
        Cable second = world.place(1);
        graph.joinOrCreate(world, second);

        assertSame(first.getNetwork(), second.getNetwork());
        assertEquals(2, first.getNetwork().getAllDevices().size());
        assertEquals(1, graph.networkCount());
    }

    @Test
    void separatedCablesGetSeparateNetworks() {
        Cable left = world.place(0);
        graph.joinOrCreate(world, left);
        Cable right = world.place(2);
        graph.joinOrCreate(world, right);

        assertNotSame(left.getNetwork(), right.getNetwork());
        assertEquals(2, graph.networkCount());
    }

    @Test
    void aCableBridgingTwoNetworksMergesThem() {
        Cable left = world.place(0);
        graph.joinOrCreate(world, left);
        Cable right = world.place(2);
        graph.joinOrCreate(world, right);
        assertEquals(2, graph.networkCount());

        Cable bridge = world.place(1);
        graph.joinOrCreate(world, bridge);

        assertSame(left.getNetwork(), right.getNetwork());
        assertSame(left.getNetwork(), bridge.getNetwork());
        assertEquals(3, left.getNetwork().getAllDevices().size());
        assertEquals(1, graph.networkCount());
    }

    @Test
    void breakingAMiddleCableSplitsTheNetworkInTwo() {
        Cable a = world.place(0);
        Cable b = world.place(1);
        Cable middle = world.place(2);
        Cable c = world.place(3);
        Cable d = world.place(4);
        for (Cable cable : new Cable[]{a, b, middle, c, d}) {
            graph.joinOrCreate(world, cable);
        }
        assertEquals(1, graph.networkCount());
        OMLibNetwork original = a.getNetwork();

        // Mine the middle cable, as CableBlock.onRemove does.
        world.mine(middle);
        original.removeDevice(middle);
        middle.setNetwork(null);
        graph.rebuildAround(world, original);

        assertNotSame(a.getNetwork(), c.getNetwork());
        assertSame(a.getNetwork(), b.getNetwork());
        assertSame(c.getNetwork(), d.getNetwork());
        assertEquals(2, a.getNetwork().getAllDevices().size());
        assertEquals(2, c.getNetwork().getAllDevices().size());
        assertEquals(2, graph.networkCount());
    }

    @Test
    void breakingAnEndCableLeavesOneNetwork() {
        Cable a = world.place(0);
        Cable b = world.place(1);
        Cable end = world.place(2);
        for (Cable cable : new Cable[]{a, b, end}) {
            graph.joinOrCreate(world, cable);
        }
        OMLibNetwork original = a.getNetwork();

        world.mine(end);
        original.removeDevice(end);
        end.setNetwork(null);
        graph.rebuildAround(world, original);

        assertSame(a.getNetwork(), b.getNetwork());
        assertEquals(2, a.getNetwork().getAllDevices().size());
        assertEquals(1, graph.networkCount());
    }

    @Test
    void breakingTheOnlyCableLeavesNoNetwork() {
        Cable only = world.place(0);
        graph.joinOrCreate(world, only);
        OMLibNetwork original = only.getNetwork();

        world.mine(only);
        original.removeDevice(only);
        only.setNetwork(null);
        graph.rebuildAround(world, original);

        assertEquals(0, graph.networkCount());
    }

    @Test
    void aRingOfCablesFormsOneNetwork() {
        // 2x2 ring. This is the shape that recursed until StackOverflow upstream.
        Cable[] ring = {
                world.place(new BlockPos(0, 0, 0)),
                world.place(new BlockPos(1, 0, 0)),
                world.place(new BlockPos(0, 0, 1)),
                world.place(new BlockPos(1, 0, 1))
        };
        for (Cable cable : ring) {
            graph.joinOrCreate(world, cable);
        }

        assertEquals(1, graph.networkCount());
        assertEquals(4, ring[0].getNetwork().getAllDevices().size());
    }

    @Test
    void breakingOneCableOfARingKeepsItOneNetwork() {
        Cable corner = world.place(new BlockPos(0, 0, 0));
        Cable b = world.place(new BlockPos(1, 0, 0));
        Cable c = world.place(new BlockPos(0, 0, 1));
        Cable d = world.place(new BlockPos(1, 0, 1));
        for (Cable cable : new Cable[]{corner, b, c, d}) {
            graph.joinOrCreate(world, cable);
        }
        OMLibNetwork original = corner.getNetwork();

        // The ring becomes an L. Still fully connected, so still one network.
        world.mine(corner);
        original.removeDevice(corner);
        corner.setNetwork(null);
        graph.rebuildAround(world, original);

        assertEquals(1, graph.networkCount());
        assertSame(b.getNetwork(), d.getNetwork());
        assertSame(c.getNetwork(), d.getNetwork());
        assertEquals(3, b.getNetwork().getAllDevices().size());
    }

    @Test
    void aLongRunJoinsAsOneNetwork() {
        Cable[] run = new Cable[200];
        for (int x = 0; x < run.length; x++) {
            run[x] = world.place(x);
        }
        for (Cable cable : run) {
            graph.joinOrCreate(world, cable);
        }

        assertEquals(1, graph.networkCount());
        assertEquals(200, run[0].getNetwork().getAllDevices().size());
    }

    @Test
    void tickAllDoesNotThrowOnAnEmptyGraph() {
        assertDoesNotThrow(() -> graph.tickAll());
    }
}
