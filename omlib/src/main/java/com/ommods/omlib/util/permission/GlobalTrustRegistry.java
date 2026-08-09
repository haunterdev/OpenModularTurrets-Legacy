package com.ommods.omlib.util.permission;

import com.ommods.omlib.util.OMPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A player's trust list that is not attached to any block: "trust this player on everything I own".
 * Parallel to {@link OwnerShareRegistry}, but the value carries per player access levels, so it is a
 * {@link TrustManager} rather than a flat list.
 *
 * Pure: no Level, no command source, no packets. Upstream's GlobalTrustRegister was an
 * ICapabilityProvider that called sender.sendMessage and sendMessageToAllPlayers from inside the
 * data structure, and left its worst defect there: the packet handlers mutated it with no
 * permission check. The split is deliberate. This returns a Result; the packet layer, which is the
 * trust boundary, decides who may call it and what to say.
 */
public class GlobalTrustRegistry implements INBTSerializable<CompoundTag> {
    private static final String LIST_TAG = "list";
    private static final String OWNER_TAG = "owner";
    private static final String TRUST_TAG = "trust";

    private final Map<OMPlayer, TrustManager> trustLists = new HashMap<>();

    /** What a mutation did. The caller phrases it. */
    public enum Result {
        ADDED,
        ALREADY_PRESENT,
        SELF,
        REMOVED,
        NOT_PRESENT,
        LEVEL_CHANGED,
        NO_SUCH_PLAYER
    }

    public Result addTrustedPlayer(OMPlayer owner, OMPlayer trusted, EnumAccessLevel level) {
        if (owner.equals(trusted)) {
            return Result.SELF;
        }
        TrustManager tm = trustLists.get(owner);
        // Dedup by name, not by TrustedPlayer.equals: equals also compares the access level, so the
        // same person at a different level would not count as a duplicate and would be added twice.
        if (tm != null && tm.getTrustedPlayer(trusted.getName()) != null) {
            return Result.ALREADY_PRESENT;
        }
        TrustManager target = tm != null ? tm : new TrustManager();
        if (!target.addTrustedPlayer(new TrustedPlayer(trusted, level))) {
            // The only remaining reason to fail is a null uuid rejected in online mode; nothing was
            // stored, so do not leave a fresh empty manager behind.
            return Result.ALREADY_PRESENT;
        }
        trustLists.put(owner, target);
        return Result.ADDED;
    }

    public Result removeTrustedPlayer(OMPlayer owner, OMPlayer trusted) {
        TrustManager tm = trustLists.get(owner);
        if (tm == null || !tm.removeTrustedPlayer(trusted.getName())) {
            return Result.NOT_PRESENT;
        }
        if (tm.getTrustedPlayers().isEmpty()) {
            trustLists.remove(owner);
        }
        return Result.REMOVED;
    }

    public Result changePermission(OMPlayer owner, OMPlayer trusted, EnumAccessLevel level) {
        TrustManager tm = trustLists.get(owner);
        if (tm == null) {
            return Result.NO_SUCH_PLAYER;
        }
        TrustedPlayer entry = tm.getTrustedPlayer(trusted.getName());
        if (entry == null) {
            return Result.NO_SUCH_PLAYER;
        }
        entry.setAccessLevel(level);
        return Result.LEVEL_CHANGED;
    }

    /** The owner's trust list, empty if none. Unmodifiable: mutate through this class. */
    public List<TrustedPlayer> getTrustedPlayers(OMPlayer owner) {
        TrustManager tm = trustLists.get(owner);
        return tm == null ? List.of() : Collections.unmodifiableList(tm.getTrustedPlayers());
    }

    /** The trust entry for candidate on owner's global list, offline aware, or null. */
    @Nullable
    public TrustedPlayer getTrustedPlayer(@Nullable OMPlayer owner, @Nullable OMPlayer candidate) {
        if (owner == null || candidate == null) {
            return null;
        }
        TrustManager tm = trustLists.get(owner);
        return tm == null ? null : tm.getTrustedPlayer(candidate);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (Map.Entry<OMPlayer, TrustManager> entry : trustLists.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            // Nested, unlike upstream, which flattened the owner and the trust list into one tag.
            entryTag.put(OWNER_TAG, entry.getKey().serializeNBT());
            entryTag.put(TRUST_TAG, entry.getValue().writeToNBT(new CompoundTag()));
            entries.add(entryTag);
        }
        tag.put(LIST_TAG, entries);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        trustLists.clear();
        ListTag entries = tag.getList(LIST_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            OMPlayer owner = new OMPlayer(entryTag.getCompound(OWNER_TAG));
            TrustManager tm = new TrustManager();
            tm.readFromNBT(entryTag.getCompound(TRUST_TAG));
            trustLists.put(owner, tm);
        }
    }
}
