package com.ommods.omlib.network;

import com.ommods.omlib.api.network.INetworkTile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Owns one NetworkGraph per level and wires it to the Forge events.
 *
 * Deliberately thin. Everything with logic in it lives in NetworkGraph, which needs no Level and
 * so can be unit tested; this shell cannot be, because a ResourceKey cannot be constructed
 * without a bootstrapped game.
 *
 * Upstream held every network of every dimension in a single list on a singleton and never
 * cleared it, so a dedicated server mixed levels together and each world reload leaked the
 * previous world's networks. Graphs are per level and dropped when the level unloads.
 *
 * Server side only. Networks are a server concept; the client is told what it needs by the
 * cable's own block entity sync.
 */
public final class NetworkRegistry {
    private static final Map<ResourceKey<Level>, NetworkGraph> GRAPHS = new HashMap<>();

    private NetworkRegistry() {
    }

    public static NetworkGraph get(Level level) {
        return GRAPHS.computeIfAbsent(level.dimension(), key -> new NetworkGraph());
    }

    /** Resolves positions to network tiles in this level. The seam NetworkTopology traverses. */
    public static NetworkTopology.TileLookup lookupFor(Level level) {
        return pos -> {
            // Never load a chunk to answer a topology question. hasChunkAt is the 1.20.1 name;
            // there is no isLoaded on LevelReader.
            if (!level.hasChunkAt(pos)) {
                return null;
            }
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof INetworkTile ? (INetworkTile) be : null;
        };
    }

    /** Convenience for block entities: join the graph of this level. */
    public static OMLibNetwork join(Level level, INetworkTile tile) {
        return get(level).joinOrCreate(lookupFor(level), tile);
    }

    /** Convenience for block entities: re-form what is left of a network after a removal. */
    public static void rebuild(Level level, OMLibNetwork previous) {
        get(level).rebuildAround(lookupFor(level), previous);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) {
            return;
        }
        NetworkGraph graph = GRAPHS.get(event.level.dimension());
        if (graph != null) {
            graph.tickAll();
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        // Upstream never did this, so every reload leaked the previous world's networks.
        GRAPHS.remove(((Level) event.getLevel()).dimension());
    }
}
