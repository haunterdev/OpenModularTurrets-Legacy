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
 * Puts the trust list on disk and keeps it in one place per save.
 *
 * The thin shell around {@link OwnerShareRegistry}, in the same spirit as NetworkRegistry around
 * NetworkGraph: everything with logic in it is in the registry, which needs no game; this only
 * knows where the data lives and when it is dirty.
 *
 * Stored on the overworld because the trust list is global, not per dimension. Upstream attached
 * its capability to dimension 0 for the same reason.
 *
 * Unlike the network graphs, this is persisted. Phase 2 refused SavedData for networks because a
 * network is derived from topology and can be rebuilt by a scan. A trust list is derived from
 * nothing, so the same reasoning points the other way.
 */
public class OwnerShareSavedData extends SavedData {
    private static final String DATA_NAME = "omlib_owner_shares";
    private static final String SHARES_TAG = "shares";

    private final OwnerShareRegistry registry = new OwnerShareRegistry();

    /** The one instance for this save, created on first use. Server side only. */
    public static OwnerShareSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage()
                .computeIfAbsent(OwnerShareSavedData::load, OwnerShareSavedData::new, DATA_NAME);
    }

    /**
     * Convenience for callers holding a level. Null on the client, and null when the level has no
     * server, which is what unit tests hand us.
     */
    @Nullable
    public static OwnerShareSavedData get(@Nullable Level level) {
        if (level == null || level.isClientSide()) {
            return null;
        }
        MinecraftServer server = level.getServer();
        return server == null ? null : get(server);
    }

    private static OwnerShareSavedData load(CompoundTag tag) {
        OwnerShareSavedData data = new OwnerShareSavedData();
        data.registry.deserializeNBT(tag.getCompound(SHARES_TAG));
        return data;
    }

    public OwnerShareRegistry getRegistry() {
        return registry;
    }

    public List<OMPlayer> getSharePlayers(OMPlayer owner) {
        return registry.getSharePlayers(owner);
    }

    public boolean isPlayerSharedOwner(@Nullable OMPlayer owner, @Nullable OMPlayer candidate) {
        return registry.isPlayerSharedOwner(owner, candidate);
    }

    /** Mutations go through here so nothing can change the list without marking it dirty. */
    public OwnerShareRegistry.Result addSharePlayer(OMPlayer owner, OMPlayer sharePlayer) {
        OwnerShareRegistry.Result result = registry.addSharePlayer(owner, sharePlayer);
        if (result == OwnerShareRegistry.Result.ADDED) {
            setDirty();
        }
        return result;
    }

    public OwnerShareRegistry.Result removeSharePlayer(OMPlayer owner, OMPlayer sharePlayer) {
        OwnerShareRegistry.Result result = registry.removeSharePlayer(owner, sharePlayer);
        if (result == OwnerShareRegistry.Result.REMOVED) {
            setDirty();
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(SHARES_TAG, registry.serializeNBT());
        return tag;
    }
}
