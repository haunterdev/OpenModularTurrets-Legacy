package com.ommods.openmodularturrets.content.expander;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Faithful port of the 1.12 Expander storage maths and TurretHeadUtil's power-expander capacity
 * lookup. Every number is pinned from the decompiled 1.12 source: an inventory expander's per-slot
 * limit is {@code 2^(meta+1)} with meta 0..4 (so tier 1..5 gives 2, 4, 8, 16, 32), and a power
 * expander adds a flat capacity per tier read from the 1.12 config defaults.
 */
class ExpanderStatsTest {

    // --- Nine ammo slots on every inventory expander, like the base's ammo grid.

    @Test
    void everyExpanderHasNineSlots() {
        assertEquals(9, ExpanderStats.EXPANDER_SLOTS);
    }

    // --- Inventory expander per-slot stack limit: 1.12 getSlotLimit = 2^(tier+1), tier = meta 0..4,
    // which for display tiers 1..5 is 2^tier.

    @Test
    void inventorySlotLimitDoublesPerTier() {
        assertEquals(2, ExpanderStats.inventorySlotLimit(1));
        assertEquals(4, ExpanderStats.inventorySlotLimit(2));
        assertEquals(8, ExpanderStats.inventorySlotLimit(3));
        assertEquals(16, ExpanderStats.inventorySlotLimit(4));
        assertEquals(32, ExpanderStats.inventorySlotLimit(5));
    }

    // --- Power expander flat capacity per tier, from OMTConfig MISCELLANEOUS defaults.

    @Test
    void powerCapacityMatchesConfigDefaults() {
        assertEquals(2_500, ExpanderStats.powerCapacity(1));
        assertEquals(25_000, ExpanderStats.powerCapacity(2));
        assertEquals(75_000, ExpanderStats.powerCapacity(3));
        assertEquals(250_000, ExpanderStats.powerCapacity(4));
        assertEquals(5_000_000, ExpanderStats.powerCapacity(5));
    }

    // --- Total extra capacity is the sum over the touching power expanders, faithful to
    // getPowerExpanderTotalExtraCapacity.

    @Test
    void totalPowerCapacitySumsEachExpander() {
        assertEquals(0, ExpanderStats.totalPowerCapacity());
        assertEquals(5_000, ExpanderStats.totalPowerCapacity(1, 1));
        assertEquals(5_075_000, ExpanderStats.totalPowerCapacity(3, 5));
    }
}
