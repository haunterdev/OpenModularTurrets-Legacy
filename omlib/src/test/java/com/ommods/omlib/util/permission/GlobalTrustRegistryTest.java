package com.ommods.omlib.util.permission;

import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalTrustRegistryTest {

    private final OMPlayer alice = new OMPlayer(UUID.randomUUID(), "alice");
    private final OMPlayer bob = new OMPlayer(UUID.randomUUID(), "bob");

    @AfterEach
    void resetConfig() {
        OMConfig.Permission.offlineModeSupport = false;
    }

    @Test
    void addThenAddAgainReportsDuplicate() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        assertEquals(GlobalTrustRegistry.Result.ADDED,
                reg.addTrustedPlayer(alice, bob, EnumAccessLevel.OPEN_GUI));
        assertEquals(GlobalTrustRegistry.Result.ALREADY_PRESENT,
                reg.addTrustedPlayer(alice, bob, EnumAccessLevel.OPEN_GUI));
    }

    @Test
    void addingYourselfIsRefused() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        assertEquals(GlobalTrustRegistry.Result.SELF,
                reg.addTrustedPlayer(alice, alice, EnumAccessLevel.ADMIN));
    }

    @Test
    void addStoresTheAccessLevel() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, bob, EnumAccessLevel.CHANGE_SETTINGS);
        TrustedPlayer stored = reg.getTrustedPlayer(alice, bob);
        assertNotNull(stored);
        assertEquals(EnumAccessLevel.CHANGE_SETTINGS, stored.getAccessLevel());
    }

    @Test
    void removeThenRemoveAgainReportsAbsent() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, bob, EnumAccessLevel.NONE);
        assertEquals(GlobalTrustRegistry.Result.REMOVED, reg.removeTrustedPlayer(alice, bob));
        assertEquals(GlobalTrustRegistry.Result.NOT_PRESENT, reg.removeTrustedPlayer(alice, bob));
    }

    @Test
    void emptyingAnOwnerDropsThatOwner() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, bob, EnumAccessLevel.NONE);
        reg.removeTrustedPlayer(alice, bob);
        assertTrue(reg.getTrustedPlayers(alice).isEmpty());
    }

    @Test
    void changePermissionUpdatesAKnownPlayer() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, bob, EnumAccessLevel.NONE);
        assertEquals(GlobalTrustRegistry.Result.LEVEL_CHANGED,
                reg.changePermission(alice, bob, EnumAccessLevel.ADMIN));
        assertEquals(EnumAccessLevel.ADMIN, reg.getTrustedPlayer(alice, bob).getAccessLevel());
    }

    @Test
    void changePermissionOnAnUnknownPlayerReports() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        assertEquals(GlobalTrustRegistry.Result.NO_SUCH_PLAYER,
                reg.changePermission(alice, bob, EnumAccessLevel.ADMIN));
    }

    @Test
    void trustLookupMatchesByNameInOfflineMode() {
        OMConfig.Permission.offlineModeSupport = true;
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, new OMPlayer(UUID.randomUUID(), "bob"), EnumAccessLevel.OPEN_GUI);
        // The querying player is the same person offline, with no cached uuid.
        assertNotNull(reg.getTrustedPlayer(alice, new OMPlayer(null, "bob")));
    }

    @Test
    void getTrustedPlayersIsUnmodifiable() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, bob, EnumAccessLevel.NONE);
        assertThrows(UnsupportedOperationException.class,
                () -> reg.getTrustedPlayers(alice).clear());
    }

    @Test
    void nbtRoundTripPreservesOwnerTrustAndLevel() {
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        reg.addTrustedPlayer(alice, bob, EnumAccessLevel.CHANGE_SETTINGS);

        CompoundTag tag = reg.serializeNBT();
        GlobalTrustRegistry loaded = new GlobalTrustRegistry();
        loaded.deserializeNBT(tag);

        TrustedPlayer stored = loaded.getTrustedPlayer(alice, bob);
        assertNotNull(stored);
        assertEquals("bob", stored.getName());
        assertEquals(EnumAccessLevel.CHANGE_SETTINGS, stored.getAccessLevel());
    }

    // The persisted structure the owner actually hits in a single-player (offline) world: owner and
    // trusted player both have null uuids. This whole entry used to vanish on reload.
    @Test
    void nbtRoundTripKeepsOfflineEntries() {
        OMConfig.Permission.offlineModeSupport = true;
        GlobalTrustRegistry reg = new GlobalTrustRegistry();
        OMPlayer owner = new OMPlayer(null, "host");
        reg.addTrustedPlayer(owner, new OMPlayer(null, "guest"), EnumAccessLevel.OPEN_GUI);

        GlobalTrustRegistry loaded = new GlobalTrustRegistry();
        loaded.deserializeNBT(reg.serializeNBT());

        TrustedPlayer stored = loaded.getTrustedPlayer(owner, new OMPlayer(null, "guest"));
        assertNotNull(stored, "offline owner's offline trust entry must survive reload");
        assertEquals(EnumAccessLevel.OPEN_GUI, stored.getAccessLevel());
    }
}
