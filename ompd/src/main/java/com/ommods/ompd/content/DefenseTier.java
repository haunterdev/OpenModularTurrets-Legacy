package com.ommods.ompd.content;

/** The five shipped OMPD tiers and their exact 1.12 hardness/blast values. */
public enum DefenseTier {
    TIER1(1, "one", 10.0F),
    TIER2(2, "two", 20.0F),
    TIER3(3, "three", 30.0F),
    TIER4(4, "four", 50.0F),
    TIER5(5, "five", 80.0F);

    private final int tier;
    private final String word;
    private final float strength;

    DefenseTier(int tier, String word, float strength) {
        this.tier = tier;
        this.word = word;
        this.strength = strength;
    }

    public int tier() {
        return tier;
    }

    /** The old metadata value persisted by TileEntityPassiveTiered. */
    public int legacyMetadata() {
        return tier - 1;
    }

    public float hardness() {
        return strength;
    }

    public float blastResistance() {
        return strength;
    }

    public String registryName(String kind) {
        return kind + "_tier_" + word;
    }
}
