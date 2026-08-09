package com.ommods.omlib.util.permission;

import com.ommods.omlib.util.OMPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Puts the global trust list on disk, one per save.
 *
 * The thin shell around {@link GlobalTrustRegistry}, the twin of {@link OwnerShareSavedData}:
 * everything with logic is in the registry, which needs no game; this only knows where the data
 * lives and when it is dirty.
 *
 * Stored on the overworld because global trust is not per dimension. Upstream attached its
 * capability to a dimension for the same reason (the End, oddly; the overworld is the natural home).
 */
public class GlobalTrustSavedData extends SavedData {
    private static final String DATA_NAME = "omlib_global_trust";
    private static final String TRUST_TAG = "trust";

    private final GlobalTrustRegistry registry = new GlobalTrustRegistry();

    /** The one instance for this save, created on first use. Server side only. */
    public static GlobalTrustSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage()
                .computeIfAbsent(GlobalTrustSavedData::load, GlobalTrustSavedData::new, DATA_NAME);
    }

    /**
     * Convenience for callers holding a level. Null on the client, and null when the level has no
     * server, which is what unit tests hand us.
     */
    @Nullable
    public static GlobalTrustSavedData get(@Nullable Level level) {
        if (level == null || level.isClientSide()) {
            return null;
        }
        MinecraftServer server = level.getServer();
        return server == null ? null : get(server);
    }

    private static GlobalTrustSavedData load(CompoundTag tag) {
        GlobalTrustSavedData data = new GlobalTrustSavedData();
        data.registry.deserializeNBT(tag.getCompound(TRUST_TAG));
        return data;
    }

    public GlobalTrustRegistry getRegistry() {
        return registry;
    }

    public List<TrustedPlayer> getTrustedPlayers(OMPlayer owner) {
        return registry.getTrustedPlayers(owner);
    }

    @Nullable
    public TrustedPlayer getTrustedPlayer(@Nullable OMPlayer owner, @Nullable OMPlayer candidate) {
        return registry.getTrustedPlayer(owner, candidate);
    }

    /** Mutations go through here so nothing can change the list without marking it dirty. */
    public GlobalTrustRegistry.Result addTrustedPlayer(OMPlayer owner, OMPlayer trusted,
                                                       EnumAccessLevel level) {
        GlobalTrustRegistry.Result result = registry.addTrustedPlayer(owner, trusted, level);
        if (result == GlobalTrustRegistry.Result.ADDED) {
            setDirty();
        }
        return result;
    }

    public GlobalTrustRegistry.Result removeTrustedPlayer(OMPlayer owner, OMPlayer trusted) {
        GlobalTrustRegistry.Result result = registry.removeTrustedPlayer(owner, trusted);
        if (result == GlobalTrustRegistry.Result.REMOVED) {
            setDirty();
        }
        return result;
    }

    public GlobalTrustRegistry.Result changePermission(OMPlayer owner, OMPlayer trusted,
                                                       EnumAccessLevel level) {
        GlobalTrustRegistry.Result result = registry.changePermission(owner, trusted, level);
        if (result == GlobalTrustRegistry.Result.LEVEL_CHANGED) {
            setDirty();
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(TRUST_TAG, registry.serializeNBT());
        return tag;
    }
}
