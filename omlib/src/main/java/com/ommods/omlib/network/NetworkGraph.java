package com.ommods.omlib.network;

import com.ommods.omlib.api.network.INetworkTile;
import com.ommods.omlib.network.NetworkTopology.TileLookup;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every network of one level, and the rules for joining, bridging and splitting them.
 *
 * Pure: it reaches the world only through TileLookup, so all of this is unit tested without a
 * Level. NetworkRegistry is the thin shell that owns one of these per level and wires the events.
 *
 * The split from NetworkRegistry exists because a ResourceKey cannot be built in a unit test
 * (ResourceKey.createRegistryKey builds a MappedRegistry, which throws "Not bootstrapped"), so
 * anything keyed by level is untestable. The logic worth testing lives here instead.
 */
public class NetworkGraph {
    private final Map<UUID, OMLibNetwork> networks = new HashMap<>();

    public void register(OMLibNetwork network) {
        networks.put(network.getUuid(), network);
    }

    public void remove(OMLibNetwork network) {
        networks.remove(network.getUuid());
    }

    @Nullable
    public OMLibNetwork getNetwork(UUID uuid) {
        return networks.get(uuid);
    }

    public int networkCount() {
        return networks.size();
    }

    public Collection<OMLibNetwork> getNetworks() {
        return networks.values();
    }

    /**
     * Puts a tile on a network: joins the neighbouring one, merges several if the tile bridges
     * them, or starts a new one.
     *
     * @return the network the tile now belongs to
     */
    public OMLibNetwork joinOrCreate(TileLookup lookup, INetworkTile tile) {
        List<OMLibNetwork> neighbours = new ArrayList<>();
        for (INetworkTile connected :
                NetworkTopology.collectConnected(tile.getPosition(), lookup).values()) {
            OMLibNetwork network = connected.getNetwork();
            if (network != null && !neighbours.contains(network)) {
                neighbours.add(network);
            }
        }

        OMLibNetwork target;
        if (neighbours.isEmpty()) {
            target = new OMLibNetwork();
            register(target);
        } else {
            target = neighbours.get(0);
            // The tile bridged separate networks, so fold them into the first.
            for (int i = 1; i < neighbours.size(); i++) {
                OMLibNetwork absorbed = neighbours.get(i);
                target.mergeNetwork(absorbed);
                remove(absorbed);
            }
        }

        if (target.addDevice(tile)) {
            tile.setNetwork(target);
        }
        return target;
    }

    /**
     * Recomputes the networks left behind after a tile was removed.
     *
     * Every surviving tile is detached and rejoined, which naturally splits a severed run into
     * separate networks. Upstream tried to detect the split first and got the check backwards, so
     * it tore down healthy networks and never split broken ones.
     */
    public void rebuildAround(TileLookup lookup, OMLibNetwork previous) {
        List<INetworkTile> survivors = new ArrayList<>(previous.getAllDevices());
        remove(previous);
        for (INetworkTile tile : survivors) {
            tile.setNetwork(null);
        }
        for (INetworkTile tile : survivors) {
            if (tile.getNetwork() == null) {
                joinOrCreate(lookup, tile);
            }
        }
    }

    /** One energy tick for every network here. */
    public void tickAll() {
        // Copy: a tick may merge or drop networks.
        for (OMLibNetwork network : new ArrayList<>(networks.values())) {
            network.tick();
        }
    }
}
