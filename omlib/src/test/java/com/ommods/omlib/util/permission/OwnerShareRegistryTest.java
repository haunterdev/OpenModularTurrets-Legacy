package com.ommods.omlib.util.permission;

import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnerShareRegistryTest {
    private final OwnerShareRegistry registry = new OwnerShareRegistry();
    private final OMPlayer owner = new OMPlayer(UUID.randomUUID(), "owner");
    private final OMPlayer friend = new OMPlayer(UUID.randomUUID(), "friend");
    private final OMPlayer stranger = new OMPlayer(UUID.randomUUID(), "stranger");

    @AfterEach
    void resetConfig() {
        OMConfig.Permission.offlineModeSupport = false;
    }

    @Test
    void addsAShare() {
        assertEquals(OwnerShareRegistry.Result.ADDED, registry.addSharePlayer(owner, friend));
        assertEquals(java.util.List.of(friend), registry.getSharePlayers(owner));
    }

    @Test
    void refusesToAddTheSamePlayerTwice() {
        registry.addSharePlayer(owner, friend);
        assertEquals(OwnerShareRegistry.Result.ALREADY_PRESENT, registry.addSharePlayer(owner, friend));
        assertEquals(1, registry.getSharePlayers(owner).size());
    }

    @Test
    void refusesToShareWithYourself() {
        assertEquals(OwnerShareRegistry.Result.SELF, registry.addSharePlayer(owner, owner));
        assertTrue(registry.getSharePlayers(owner).isEmpty());
    }

    @Test
    void removesAShare() {
        registry.addSharePlayer(owner, friend);
        assertEquals(OwnerShareRegistry.Result.REMOVED, registry.removeSharePlayer(owner, friend));
        assertTrue(registry.getSharePlayers(owner).isEmpty());
    }

    @Test
    void reportsRemovingSomeoneWhoIsNotOnTheList() {
        assertEquals(OwnerShareRegistry.Result.NOT_PRESENT, registry.removeSharePlayer(owner, friend));
    }

    @Test
    void hasNoSharesForAnUnknownOwner() {
        assertTrue(registry.getSharePlayers(stranger).isEmpty());
    }

    @Test
    void reportsASharedOwner() {
        registry.addSharePlayer(owner, friend);
        assertTrue(registry.isPlayerSharedOwner(owner, friend));
        assertFalse(registry.isPlayerSharedOwner(owner, stranger));
    }

    @Test
    void doesNotTreatTheOwnerAsTheirOwnSharedOwner() {
        // The owner already has access by being the owner. Answering true here would hide a bug
        // in a caller that forgot to check ownership.
        assertFalse(registry.isPlayerSharedOwner(owner, owner));
    }

    @Test
    void sharesAreNotSharedInTheOtherDirection() {
        registry.addSharePlayer(owner, friend);
        assertFalse(registry.isPlayerSharedOwner(friend, owner));
    }

    @Test
    void theReturnedShareListCannotBeMutatedByCallers() {
        registry.addSharePlayer(owner, friend);
        assertThrows(UnsupportedOperationException.class,
                () -> registry.getSharePlayers(owner).add(stranger));
    }

    @Test
    void findsASharedOwnerInOfflineModeWhereTheUuidIsNull() {
        // Regression for the equals/hashCode split: offline players have no uuid, and the registry
        // is a HashMap keyed by OMPlayer, so this lookup used to miss.
        OMConfig.Permission.offlineModeSupport = true;
        OMPlayer offlineOwner = new OMPlayer(null, "owner");
        OMPlayer offlineFriend = new OMPlayer(null, "friend");
        registry.addSharePlayer(offlineOwner, offlineFriend);
        assertTrue(registry.isPlayerSharedOwner(new OMPlayer(null, "owner"), new OMPlayer(null, "friend")));
    }

    @Test
    void survivesASaveAndLoad() {
        registry.addSharePlayer(owner, friend);
        registry.addSharePlayer(owner, stranger);
        CompoundTag tag = registry.serializeNBT();

        OwnerShareRegistry loaded = new OwnerShareRegistry();
        loaded.deserializeNBT(tag);

        assertEquals(2, loaded.getSharePlayers(owner).size());
        assertTrue(loaded.isPlayerSharedOwner(owner, friend));
        assertTrue(loaded.isPlayerSharedOwner(owner, stranger));
    }

    @Test
    void savesAndLoadsAnOfflineOwnerWithNoUuid() {
        OMConfig.Permission.offlineModeSupport = true;
        OMPlayer offlineOwner = new OMPlayer(null, "owner");
        OMPlayer offlineFriend = new OMPlayer(null, "friend");
        registry.addSharePlayer(offlineOwner, offlineFriend);

        OwnerShareRegistry loaded = new OwnerShareRegistry();
        loaded.deserializeNBT(registry.serializeNBT());

        assertTrue(loaded.isPlayerSharedOwner(offlineOwner, offlineFriend));
    }

    @Test
    void loadingReplacesWhatWasThereBefore() {
        registry.addSharePlayer(owner, friend);
        registry.deserializeNBT(new OwnerShareRegistry().serializeNBT());
        assertTrue(registry.getSharePlayers(owner).isEmpty());
    }
}
