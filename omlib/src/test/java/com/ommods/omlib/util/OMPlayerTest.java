package com.ommods.omlib.util;

import com.ommods.omlib.config.OMConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OMPlayerTest {
    private final UUID uuid = UUID.randomUUID();
    private final String name = "test";
    private final String team = "test";
    private final OMPlayer player = new OMPlayer(uuid, name, team);

    @AfterEach
    void resetConfig() {
        // The config is global static state, so a leaked true here changes later tests.
        OMConfig.Permission.offlineModeSupport = false;
    }

    @Test
    void equalPlayersShareAHashCodeInOfflineMode() {
        OMConfig.Permission.offlineModeSupport = true;
        OMPlayer cached = new OMPlayer(UUID.randomUUID(), "bob");
        OMPlayer offline = new OMPlayer(null, "bob");
        assertEquals(cached, offline);
        assertEquals(cached.hashCode(), offline.hashCode(),
                "equal objects must share a hash code, or every HashMap keyed by OMPlayer misses");
    }

    @Test
    void mapLookupFindsAnEqualPlayerInOfflineMode() {
        OMConfig.Permission.offlineModeSupport = true;
        Map<OMPlayer, String> shares = new HashMap<>();
        shares.put(new OMPlayer(UUID.randomUUID(), "bob"), "bobs list");
        // The share registry is keyed by OMPlayer, so this is the real failure mode.
        assertEquals("bobs list", shares.get(new OMPlayer(null, "bob")));
    }

    @Test
    void serializesAPlayerWithNoUuid() {
        OMConfig.Permission.offlineModeSupport = true;
        OMPlayer offline = new OMPlayer(null, "bob");
        CompoundTag tag = offline.serializeNBT();
        OMPlayer loaded = new OMPlayer(tag);
        assertEquals("bob", loaded.getName());
        assertNull(loaded.getUuid());
    }

    @Test
    void deserializesATagWithNoUuidWithoutThrowing() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", "bob");
        OMPlayer loaded = new OMPlayer(tag);
        assertEquals("bob", loaded.getName());
        assertNull(loaded.getUuid());
    }

    @Test
    void checkByteBuf() {
        OMConfig.Permission.offlineModeSupport = true;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        OMPlayer.writeToByteBuf(player, buf);
        assertEquals(player, OMPlayer.readFromByteBuf(buf));
        OMConfig.Permission.offlineModeSupport = false;
        buf = new FriendlyByteBuf(Unpooled.buffer());
        OMPlayer.writeToByteBuf(player, buf);
        assertEquals(player, OMPlayer.readFromByteBuf(buf));
    }

    @Test
    void checkNBT() {
        OMConfig.Permission.offlineModeSupport = true;
        CompoundTag tag = player.serializeNBT();
        assertEquals(player, new OMPlayer(tag));
        OMConfig.Permission.offlineModeSupport = false;
        tag = player.serializeNBT();
        assertEquals(player, new OMPlayer(tag));
    }


    @Test
    void getUuid() {
        assertEquals(uuid, player.getUuid());
    }

    @Test
    void getName() {
        assertEquals(name, player.getName());
    }

    @Test
    void getTeam() {
        assertEquals(team, player.getTeam());
    }
}