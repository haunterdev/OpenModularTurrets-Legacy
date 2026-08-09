package com.ommods.omlib.util.permission;

import com.ommods.omlib.util.OMPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Who has shared ownership of whose blocks. This is omlib's trust list, and the reason omlib stays
 * one shared module instead of being shaded into each consumer.
 *
 * Pure: no Level, no command source, no packets. The whole list is unit testable without a game.
 *
 * Upstream called sender.sendMessage from inside the data structure, so storage, presentation and
 * networking were one class. That is the entanglement that kept the network layer untested for
 * years, so the split is deliberate: this returns a Result, and the command layer decides what to
 * say about it.
 */
public class OwnerShareRegistry implements INBTSerializable<CompoundTag> {
    private static final String LIST_TAG = "list";
    private static final String OWNER_TAG = "owner";
    private static final String SHARE_PLAYERS_TAG = "share_players";

    private final Map<OMPlayer, List<OMPlayer>> shares = new HashMap<>();

    /** What a mutation did. The caller phrases it. */
    public enum Result {
        ADDED,
        ALREADY_PRESENT,
        SELF,
        REMOVED,
        NOT_PRESENT
    }

    public Result addSharePlayer(OMPlayer owner, OMPlayer sharePlayer) {
        if (owner.equals(sharePlayer)) {
            return Result.SELF;
        }
        List<OMPlayer> sharePlayers = shares.computeIfAbsent(owner, key -> new ArrayList<>());
        if (sharePlayers.contains(sharePlayer)) {
            return Result.ALREADY_PRESENT;
        }
        sharePlayers.add(sharePlayer);
        return Result.ADDED;
    }

    public Result removeSharePlayer(OMPlayer owner, OMPlayer sharePlayer) {
        List<OMPlayer> sharePlayers = shares.get(owner);
        if (sharePlayers == null || !sharePlayers.remove(sharePlayer)) {
            return Result.NOT_PRESENT;
        }
        if (sharePlayers.isEmpty()) {
            shares.remove(owner);
        }
        return Result.REMOVED;
    }

    /** The owner's share list, empty if they have none. Unmodifiable: mutate through this class. */
    public List<OMPlayer> getSharePlayers(OMPlayer owner) {
        List<OMPlayer> sharePlayers = shares.get(owner);
        return sharePlayers == null ? List.of() : Collections.unmodifiableList(sharePlayers);
    }

    /**
     * Whether candidate has shared ownership of owner's blocks.
     *
     * False when they are the same player: an owner already has access by being the owner, and
     * answering true here would mask a caller that forgot to check ownership.
     */
    public boolean isPlayerSharedOwner(@Nullable OMPlayer owner, @Nullable OMPlayer candidate) {
        if (owner == null || candidate == null) {
            return false;
        }
        List<OMPlayer> sharePlayers = shares.get(owner);
        return sharePlayers != null && sharePlayers.contains(candidate);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (Map.Entry<OMPlayer, List<OMPlayer>> entry : shares.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            // Nested, unlike upstream, which flattened the owner's fields into the same tag that
            // holds share_players and left them free to collide.
            entryTag.put(OWNER_TAG, entry.getKey().serializeNBT());
            ListTag sharePlayers = new ListTag();
            for (OMPlayer sharePlayer : entry.getValue()) {
                sharePlayers.add(sharePlayer.serializeNBT());
            }
            entryTag.put(SHARE_PLAYERS_TAG, sharePlayers);
            entries.add(entryTag);
        }
        tag.put(LIST_TAG, entries);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        shares.clear();
        ListTag entries = tag.getList(LIST_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            OMPlayer owner = new OMPlayer(entryTag.getCompound(OWNER_TAG));
            ListTag sharePlayersTag = entryTag.getList(SHARE_PLAYERS_TAG, Tag.TAG_COMPOUND);
            List<OMPlayer> sharePlayers = new ArrayList<>();
            for (int j = 0; j < sharePlayersTag.size(); j++) {
                sharePlayers.add(new OMPlayer(sharePlayersTag.getCompound(j)));
            }
            shares.put(owner, sharePlayers);
        }
    }
}
