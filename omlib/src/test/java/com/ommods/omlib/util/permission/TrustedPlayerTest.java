package com.ommods.omlib.util.permission;

import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TrustedPlayerTest {

    @AfterEach
    void resetConfig() {
        OMConfig.Permission.offlineModeSupport = false;
    }

    // Same contract break OMPlayer had: equals drops the uuid in offline mode but hashCode hashed
    // it, so two equal players hashed apart and any hash structure of TrustedPlayer missed.
    @Test
    void equalPlayersShareAHashCodeInOfflineMode() {
        OMConfig.Permission.offlineModeSupport = true;
        TrustedPlayer cached = new TrustedPlayer(new OMPlayer(UUID.randomUUID(), "bob"));
        TrustedPlayer offline = new TrustedPlayer(new OMPlayer(null, "bob"));
        assertEquals(cached, offline);
        assertEquals(cached.hashCode(), offline.hashCode(),
                "equal objects must share a hash code");
    }

    @Test
    void differentAccessLevelsAreNotEqual() {
        UUID uuid = UUID.randomUUID();
        TrustedPlayer none = new TrustedPlayer(new OMPlayer(uuid, "bob"), EnumAccessLevel.NONE);
        TrustedPlayer admin = new TrustedPlayer(new OMPlayer(uuid, "bob"), EnumAccessLevel.ADMIN);
        assertNotEquals(none, admin);
    }
}
