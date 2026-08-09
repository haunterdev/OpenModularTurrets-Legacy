package com.ommods.openmodularturrets.content.turret;

import org.jetbrains.annotations.Nullable;

/**
 * Faithful port of the 1.12 {@code TurretHeadUtil} upgrade/addon getters and the head's derived
 * firing math. Pure: it models the base's four modifier slots as {@link Slot} (meta, count) pairs, so
 * every number can be unit tested without bootstrapping Minecraft. The block entity turns real
 * {@code ItemStack}s into these slots.
 *
 * <p>Layout, from the 1.12 base inventory: slots 0-8 are ammo, 9 and 10 hold addons
 * ({@code AddonMetaItem}), 11 and 12 hold upgrades ({@code UpgradeMetaItem}). Tier 1 has no modifier
 * slots; tiers 2 to 4 read upgrade slot 11 plus both addon slots; tier 5 also reads upgrade slot 12.
 *
 * <p>The four upgrade magnitudes are identical across every 1.12 {@code TurretSetting}, so they are
 * shared constants here rather than per-turret fields. The per-turret numbers that do vary (base
 * accuracy deviation, damage amp) live on {@link TurretType}.
 */
public final class TurretUpgrades {

    // Uniform 1.12 config magnitudes (the same in every TurretSetting).
    public static final int RANGE_PER_UPGRADE = 2;
    public static final double FIRE_RATE_PER_UPGRADE = 0.1;
    public static final double ACCURACY_PER_UPGRADE = 0.2;
    public static final double EFFICIENCY_PER_UPGRADE = 0.08;

    // UpgradeMetaItem sub-item order (slots 11, 12).
    public static final int UP_ACCURACY = 0;
    public static final int UP_EFFICIENCY = 1;
    public static final int UP_FIRE_RATE = 2;
    public static final int UP_RANGE = 3;
    public static final int UP_SCATTER = 4;

    // AddonMetaItem sub-item order (slots 9, 10).
    public static final int AD_CONCEALER = 0;
    public static final int AD_DAMAGE_AMP = 1;
    public static final int AD_RECYCLER = 3;
    public static final int AD_REDSTONE = 4;
    public static final int AD_SOLAR = 6;
    public static final int AD_FAKEDROPS = 7;

    /** One modifier slot's contents: the sub-item {@code meta} and its stack {@code count}. */
    public record Slot(int meta, int count) {
    }

    private TurretUpgrades() {
    }

    // --- Upgrade slots (11, 12). Tier 1 has none; slot 12 is only live on tier 5.

    private static int sumUpgrades(int tier, @Nullable Slot s11, @Nullable Slot s12, int wantMeta) {
        if (tier == 1) {
            return 0;
        }
        int total = 0;
        if (tier == 5 && s12 != null && s12.meta() == wantMeta) {
            total += s12.count();
        }
        if (s11 != null && s11.meta() == wantMeta) {
            total += s11.count();
        }
        return total;
    }

    /** Extra range in blocks: {@link #RANGE_PER_UPGRADE} per range-upgrade item. */
    public static int rangeBonus(int tier, @Nullable Slot s11, @Nullable Slot s12) {
        return RANGE_PER_UPGRADE * sumUpgrades(tier, s11, s12, UP_RANGE);
    }

    /** Number of extra shots per volley: the raw scatter-shot stack count. */
    public static int scattershot(int tier, @Nullable Slot s11, @Nullable Slot s12) {
        return sumUpgrades(tier, s11, s12, UP_SCATTER);
    }

    public static double accuracyUpgrades(int tier, @Nullable Slot s11, @Nullable Slot s12) {
        return ACCURACY_PER_UPGRADE * sumUpgrades(tier, s11, s12, UP_ACCURACY);
    }

    public static double efficiencyUpgrades(int tier, @Nullable Slot s11, @Nullable Slot s12) {
        return EFFICIENCY_PER_UPGRADE * sumUpgrades(tier, s11, s12, UP_EFFICIENCY);
    }

    public static double fireRateUpgrades(int tier, @Nullable Slot s11, @Nullable Slot s12) {
        return FIRE_RATE_PER_UPGRADE * sumUpgrades(tier, s11, s12, UP_FIRE_RATE);
    }

    // --- Addon slots (9, 10). Tier 1 has none; presence is checked in either slot.

    private static boolean hasAddon(int tier, @Nullable Slot s9, @Nullable Slot s10, int wantMeta) {
        if (tier == 1) {
            return false;
        }
        return (s9 != null && s9.meta() == wantMeta) || (s10 != null && s10.meta() == wantMeta);
    }

    public static boolean hasConcealment(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        return hasAddon(tier, s9, s10, AD_CONCEALER);
    }

    public static boolean hasDamageAmp(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        return hasAddon(tier, s9, s10, AD_DAMAGE_AMP);
    }

    public static boolean hasRedstoneReactor(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        return hasAddon(tier, s9, s10, AD_REDSTONE);
    }

    public static boolean hasSolar(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        return hasAddon(tier, s9, s10, AD_SOLAR);
    }

    public static boolean hasRecycler(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        return hasAddon(tier, s9, s10, AD_RECYCLER);
    }

    /**
     * The fake drops addon level, faithful to 1.12 getFakeDropsLevel: -1 (no addon) plus the total
     * fake-drops count across both addon slots, capped at 3. -1 means kills stay turret kills; 0-3
     * credits kills to the fake player with that looting level.
     */
    public static int fakeDropsLevel(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        if (tier == 1) {
            return -1;
        }
        int level = -1;
        if (s9 != null && s9.meta() == AD_FAKEDROPS) {
            level += s9.count();
        }
        if (s10 != null && s10.meta() == AD_FAKEDROPS) {
            level += s10.count();
        }
        return Math.min(level, 3);
    }

    /** Total damage-amp addon count across both addon slots. */
    public static int ampLevel(int tier, @Nullable Slot s9, @Nullable Slot s10) {
        if (tier == 1) {
            return 0;
        }
        int level = 0;
        if (s9 != null && s9.meta() == AD_DAMAGE_AMP) {
            level += s9.count();
        }
        if (s10 != null && s10.meta() == AD_DAMAGE_AMP) {
            level += s10.count();
        }
        return level;
    }

    // --- Derived firing math, ported from the 1.12 TurretHead getters.

    /**
     * The spread magnitude passed to a shot, from {@code TurretHead.getActualTurretAccuracy}: a bigger
     * base deviation or accuracy upgrade widens it; scatter-shot widens it further.
     */
    public static double actualAccuracy(double baseDeviation, double accuracyUpgrades, int scatter) {
        return 100.0 - Math.min(
                (100.0 - baseDeviation) * (1.0 + accuracyUpgrades) * (1.0 - scatter / 25.0), 100.0);
    }

    /**
     * Projectile inaccuracy for a shot at a target, from 1.12 {@code ProjectileTurret.doTargetedShot}:
     * {@code baseAccuracyDeviation / 10 * (1 - accuracyUpgrades) * (1 + scattershot)}.
     *
     * <p>Note the two things that separate this from {@link #actualAccuracy}: accuracy upgrades
     * <em>divide</em> the spread here rather than feeding the displayed percentage, and scatter-shot
     * multiplies it outright, which is what fans a scatter volley out instead of stacking every round
     * on one point. This port previously fed {@code actualAccuracy / 20} into every shot, which is the
     * {@code forceShot} path's number (the redstone auto-fire with no target), and it barely moves
     * with scatter: one scatter upgrade widened the spread by about a tenth instead of doubling it.
     */
    public static float targetedInaccuracy(double baseDeviation, double accuracyUpgrades, int scatter) {
        return (float) (baseDeviation / 10.0 * (1.0 - accuracyUpgrades) * (1 + scatter));
    }

    /**
     * Projectile inaccuracy for an untargeted auto-fire shot, from 1.12 {@code ProjectileTurret
     * .forceShot}: {@code getActualTurretAccuracy() / 20}.
     */
    public static float projectileInaccuracy(double actualAccuracy) {
        return (float) (actualAccuracy / 20.0);
    }

    /**
     * Ticks between shots after fire-rate upgrades. 1.12 fires once {@code ticks} reaches
     * {@code baseFireRate * (1 - fireRateUpgrades)}, and {@code ticks} is a whole number, so the
     * effective interval is that threshold rounded up.
     */
    public static int effectiveFireInterval(int baseFireRate, double fireRateUpgrades) {
        return (int) Math.ceil(baseFireRate * (1.0 - fireRateUpgrades));
    }

    /**
     * Energy per shot after efficiency and scatter, from {@code TurretHead.getPowerRequiredForNextShot}:
     * {@code round(powerUsage * (1 - efficiencyUpgrades) * (1 + scatter))}.
     */
    public static int powerPerShot(int powerUsage, double efficiencyUpgrades, int scatter) {
        return Math.round((float) (powerUsage * (1.0 - efficiencyUpgrades) * (1 + scatter)));
    }
}
