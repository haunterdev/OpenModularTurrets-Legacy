package com.ommods.omlib.network;

import com.ommods.omlib.api.network.INetworkTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Walks the cable graph.
 *
 * Pure: it reaches the world only through TileLookup, so the tests run it against a HashMap.
 * Upstream's equivalent was recursive with no visited set, so any cable loop recursed until the
 * stack blew, and its split check was inverted.
 */
public final class NetworkTopology {

    /**
     * Resolves a position to a network tile, or null if there is none there.
     *
     * This is the seam that keeps traversal testable. In game it wraps Level.getBlockEntity; in
     * tests it wraps a map.
     */
    @FunctionalInterface
    public interface TileLookup {
        @Nullable
        INetworkTile at(BlockPos pos);
    }

    private NetworkTopology() {
    }

    /**
     * Every network tile reachable from start, including start itself.
     *
     * Breadth first and iterative, so a loop terminates and a long cable run cannot overflow.
     *
     * @return positions mapped to tiles, empty if start holds no network tile
     */
    public static Map<BlockPos, INetworkTile> collectConnected(BlockPos start, TileLookup lookup) {
        Map<BlockPos, INetworkTile> found = new LinkedHashMap<>();
        INetworkTile origin = lookup.at(start);
        if (origin == null) {
            return found;
        }
        found.put(start, origin);

        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.addLast(start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                // The visited check is what upstream lacked.
                if (found.containsKey(next)) {
                    continue;
                }
                INetworkTile tile = lookup.at(next);
                if (tile == null) {
                    continue;
                }
                found.put(next, tile);
                queue.addLast(next);
            }
        }
        return found;
    }

    /**
     * True when devices no longer form one connected component, for example after a cable in the
     * middle was mined.
     */
    public static boolean isSplit(Set<BlockPos> devices, TileLookup lookup) {
        if (devices.size() <= 1) {
            return false;
        }
        BlockPos start = devices.iterator().next();
        Map<BlockPos, INetworkTile> reachable = collectConnected(start, lookup);
        return !reachable.keySet().containsAll(devices);
    }
}
