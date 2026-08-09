package com.ommods.openmodularturrets.content.expander;

/**
 * The pure storage maths for expanders, ported from 1.12 {@code Expander.getSlotLimit} and
 * {@code TurretHeadUtil.getPowerExtenderCapacityValue}. An expander comes in two kinds (inventory or
 * power) and five tiers. An inventory expander adds nine ammo slots whose per-slot stack limit
 * doubles per tier; a power expander adds a flat energy capacity to the base it touches. No game
 * types here, so it is unit tested.
 */
public final class ExpanderStats {

    /** Ammo slots on an inventory expander, matching the base's 3x3 ammo grid. */
    public static final int EXPANDER_SLOTS = 9;

    // Power-expander flat capacity per tier, index 1..5, from the 1.12 OMTConfig MISCELLANEOUS
    // defaults (expanderPowerTierOneCapacity .. expanderPowerTierFiveCapacity).
    private static final int[] POWER_CAPACITY = {0, 2_500, 25_000, 75_000, 250_000, 5_000_000};

    private ExpanderStats() {
    }

    /**
     * Per-slot stack limit of an inventory expander's ammo slots. 1.12 stores the tier as the block
     * meta 0..4 and returns {@code 2^(meta+1)}; for display tiers 1..5 that is {@code 2^tier}.
     */
    public static int inventorySlotLimit(int tier) {
        checkTier(tier);
        return 1 << tier;
    }

    /** Flat energy capacity a single power expander of the given tier adds to its base. */
    public static int powerCapacity(int tier) {
        checkTier(tier);
        return POWER_CAPACITY[tier];
    }

    /** Sum of {@link #powerCapacity(int)} over every touching power expander, faithful to 1.12. */
    public static int totalPowerCapacity(int... tiers) {
        int total = 0;
        for (int tier : tiers) {
            total += powerCapacity(tier);
        }
        return total;
    }

    private static void checkTier(int tier) {
        if (tier < 1 || tier > 5) {
            throw new IllegalArgumentException("expander tier out of range: " + tier);
        }
    }
}
