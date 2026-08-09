package com.ommods.openmodularturrets.content.base;

/**
 * The five turret base tiers, faithful to 1.12 BaseSetting: energy capacity, energy transfer rate,
 * and how many heads the base carries at once. (1.12 has no per-tier range cap; the range ceiling
 * comes from the attached heads via the base's adjustable current max range. The per-tier
 * hardness/blast resistance in BaseSetting only applies under the non-default baseBreakable
 * config; the default base is unbreakable, so those values are not carried here.)
 */
public enum BaseTier {
    TIER1(1, 500, 50, 1),
    TIER2(2, 50_000, 100, 1),
    TIER3(3, 150_000, 500, 2),
    TIER4(4, 500_000, 1_500, 3),
    TIER5(5, 10_000_000, 7_500, 4);

    private final int tier;
    private final int capacity;
    private final int maxTransfer;
    private final int maxTurrets;

    BaseTier(int tier, int capacity, int maxTransfer, int maxTurrets) {
        this.tier = tier;
        this.capacity = capacity;
        this.maxTransfer = maxTransfer;
        this.maxTurrets = maxTurrets;
    }

    public int tier() {
        return tier;
    }

    public int capacity() {
        return capacity;
    }

    public int maxTransfer() {
        return maxTransfer;
    }

    /** How many turret heads this tier's base carries at once (1.12 baseMaxTurrets). */
    public int maxTurrets() {
        return maxTurrets;
    }

    /** Registry path for the block, e.g. {@code turret_base_tier1}. */
    public String registryName() {
        return "turret_base_tier" + tier;
    }

    /** The 1.12 display name, verbatim from the en_US.lang tile entries. */
    public String displayName() {
        return "Turret Base (Tier " + tier + ")";
    }
}
