package com.ommods.ompd.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefenseTierTest {
    @Test
    void faithfulStrengthCurveAndNames() {
        float[] expected = {10, 20, 30, 50, 80};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], DefenseTier.values()[i].hardness());
            assertEquals(expected[i], DefenseTier.values()[i].blastResistance());
        }
        assertEquals("hardened_tier_one", DefenseTier.TIER1.registryName("hardened"));
        assertEquals("fence_tier_five", DefenseTier.TIER5.registryName("fence"));
        assertEquals(4, DefenseTier.TIER5.legacyMetadata());
    }
}
