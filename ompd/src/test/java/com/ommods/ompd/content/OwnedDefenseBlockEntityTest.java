package com.ommods.ompd.content;

import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.permission.EnumAccessLevel;
import com.ommods.omlib.util.permission.TrustedPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OwnedDefenseBlockEntityTest {
    static class TestEntity extends OwnedDefenseBlockEntity {
        TestEntity() {
            super(null, BlockPos.ZERO, null);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            saveAdditional(tag);
            return tag;
        }
    }

    @Test
    void ownerTierAndTrustSurviveSaveLoad() {
        OMConfig.Permission.offlineModeSupport = false;
        TestEntity saved = new TestEntity();
        saved.setOwner(new OMPlayer(UUID.randomUUID(), "owner"));
        saved.getTrustManager().addTrustedPlayer(new TrustedPlayer(
                new OMPlayer(UUID.randomUUID(), "friend"), EnumAccessLevel.OPEN_GUI));

        TestEntity loaded = new TestEntity();
        loaded.load(saved.save());

        assertNotNull(loaded.getOwner());
        assertEquals("owner", loaded.getOwner().getName());
        assertEquals(DefenseTier.TIER1, loaded.tier());
        assertNotNull(loaded.getTrustManager().getTrustedPlayer("friend"));
        assertEquals(EnumAccessLevel.OPEN_GUI,
                loaded.getTrustManager().getTrustedPlayer("friend").getAccessLevel());
    }
}
