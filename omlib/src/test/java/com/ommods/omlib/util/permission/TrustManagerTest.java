package com.ommods.omlib.util.permission;

import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrustManagerTest {

    @AfterEach
    void resetConfig() {
        // offlineModeSupport is global static state; a leak flips later tests.
        OMConfig.Permission.offlineModeSupport = false;
    }

    private TrustedPlayer at(UUID uuid, String name, EnumAccessLevel level) {
        return new TrustedPlayer(new OMPlayer(uuid, name), level);
    }

    // Defect: getTrustedPlayer(UUID) dereferences every entry's uuid. Offline mode allows a
    // null-uuid entry, so one such entry NPEs the lookup before it can reach the target.
    @Test
    void findByUuidSkipsNullUuidEntries() {
        OMConfig.Permission.offlineModeSupport = true;
        TrustManager tm = new TrustManager();
        tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(null, "offline")));
        UUID target = UUID.randomUUID();
        tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(target, "online")));

        TrustedPlayer found = tm.getTrustedPlayer(target);
        assertNotNull(found);
        assertEquals("online", found.getName());
    }

    @Test
    void findByUuidReturnsNullWhenAbsent() {
        TrustManager tm = new TrustManager();
        tm.addTrustedPlayer(at(UUID.randomUUID(), "bob", EnumAccessLevel.NONE));
        assertNull(tm.getTrustedPlayer(UUID.randomUUID()));
    }

    @Test
    void addRejectsADuplicate() {
        UUID uuid = UUID.randomUUID();
        TrustManager tm = new TrustManager();
        assertTrue(tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(uuid, "bob"))));
        assertFalse(tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(uuid, "bob"))));
    }

    // Online mode has no identity without a uuid, so a null-uuid add must be refused.
    @Test
    void addRejectsNullUuidWhenNotOffline() {
        TrustManager tm = new TrustManager();
        assertFalse(tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(null, "ghost"))));
    }

    @Test
    void addAcceptsNullUuidInOfflineMode() {
        OMConfig.Permission.offlineModeSupport = true;
        TrustManager tm = new TrustManager();
        assertTrue(tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(null, "offline"))));
    }

    @Test
    void removeByName() {
        TrustManager tm = new TrustManager();
        tm.addTrustedPlayer(at(UUID.randomUUID(), "bob", EnumAccessLevel.NONE));
        assertTrue(tm.removeTrustedPlayer("bob"));
        assertNull(tm.getTrustedPlayer("bob"));
        assertFalse(tm.removeTrustedPlayer("bob"));
    }

    @Test
    void changePermissionStepsUpAndClampsAtAdmin() {
        TrustManager tm = new TrustManager();
        tm.addTrustedPlayer(at(UUID.randomUUID(), "bob", EnumAccessLevel.NONE));
        assertTrue(tm.changePermission("bob", 1));
        assertEquals(EnumAccessLevel.OPEN_GUI, tm.getTrustedPlayer("bob").getAccessLevel());
        tm.changePermission("bob", 1);
        tm.changePermission("bob", 1);
        assertEquals(EnumAccessLevel.ADMIN, tm.getTrustedPlayer("bob").getAccessLevel());
        assertFalse(tm.changePermission("bob", 1), "already ADMIN, cannot step up");
    }

    @Test
    void changePermissionClampsAtNone() {
        TrustManager tm = new TrustManager();
        tm.addTrustedPlayer(at(UUID.randomUUID(), "bob", EnumAccessLevel.NONE));
        assertFalse(tm.changePermission("bob", -1), "already NONE, cannot step down");
        assertEquals(EnumAccessLevel.NONE, tm.getTrustedPlayer("bob").getAccessLevel());
    }

    // Defect: readFromNBT dropped any entry it could not resolve to a uuid. In offline mode every
    // entry has a null uuid and none is in the UsernameCache, so the whole list vanished on the
    // first save and reload. Offline trust must survive a round trip.
    @Test
    void nbtRoundTripKeepsOfflineEntries() {
        OMConfig.Permission.offlineModeSupport = true;
        TrustManager tm = new TrustManager();
        tm.addTrustedPlayer(new TrustedPlayer(new OMPlayer(null, "offline"), EnumAccessLevel.CHANGE_SETTINGS));

        TrustManager loaded = new TrustManager();
        loaded.readFromNBT(tm.writeToNBT(new CompoundTag()));

        TrustedPlayer survivor = loaded.getTrustedPlayer("offline");
        assertNotNull(survivor, "offline entry must survive save and reload");
        assertEquals(EnumAccessLevel.CHANGE_SETTINGS, survivor.getAccessLevel());
    }
}
