package com.ommods.openmodularturrets.content.turret;

import org.junit.jupiter.api.Test;

import com.ommods.openmodularturrets.content.turret.TurretUpgrades.Slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Faithful port of the 1.12 TurretHeadUtil upgrade/addon getters and the head's derived firing math.
 * The base's four modifier slots are modelled as (meta, count) pairs: slots 9 and 10 hold addons,
 * 11 and 12 hold upgrades. Tier 1 has no modifier slots at all; tiers 2 to 4 read one upgrade slot
 * (11) plus the two addon slots; tier 5 adds a second upgrade slot (12). Every number here is pinned
 * from the decompiled 1.12 source.
 */
class TurretUpgradesTest {

    private static final int TIER1 = 1;
    private static final int TIER3 = 3;
    private static final int TIER5 = 5;

    // --- Range: RANGE_PER_UPGRADE (2) * count of range-upgrade items (meta 3).

    @Test
    void tierOneIgnoresAllUpgrades() {
        Slot range = new Slot(TurretUpgrades.UP_RANGE, 4);
        assertEquals(0, TurretUpgrades.rangeBonus(TIER1, range, range));
    }

    @Test
    void rangeUpgradeInSlotElevenAddsTwoBlocksEach() {
        Slot range = new Slot(TurretUpgrades.UP_RANGE, 3);
        assertEquals(6, TurretUpgrades.rangeBonus(TIER3, range, null));
    }

    @Test
    void tierFiveReadsBothUpgradeSlots() {
        Slot range = new Slot(TurretUpgrades.UP_RANGE, 1);
        assertEquals(4, TurretUpgrades.rangeBonus(TIER5, range, range));
    }

    @Test
    void nonTierFiveIgnoresTheSecondUpgradeSlot() {
        Slot range = new Slot(TurretUpgrades.UP_RANGE, 2);
        // Slot 12 is only live on tier 5, so a tier-3 base only counts slot 11.
        assertEquals(4, TurretUpgrades.rangeBonus(TIER3, range, range));
    }

    @Test
    void aDifferentUpgradeInTheSlotDoesNotCountAsRange() {
        Slot fireRate = new Slot(TurretUpgrades.UP_FIRE_RATE, 5);
        assertEquals(0, TurretUpgrades.rangeBonus(TIER3, fireRate, null));
    }

    // --- Scattershot: raw count of scatter-shot items (meta 4), no magnitude.

    @Test
    void scattershotIsTheRawStackCount() {
        Slot scatter = new Slot(TurretUpgrades.UP_SCATTER, 2);
        assertEquals(2, TurretUpgrades.scattershot(TIER3, scatter, null));
    }

    // --- Accuracy / efficiency / fire-rate: per-upgrade magnitude * count.

    @Test
    void accuracyUpgradeIsTwoTenthsEach() {
        Slot acc = new Slot(TurretUpgrades.UP_ACCURACY, 3);
        assertEquals(0.6, TurretUpgrades.accuracyUpgrades(TIER3, acc, null), 1.0e-6);
    }

    @Test
    void efficiencyUpgradeIsEightHundredthsEach() {
        Slot eff = new Slot(TurretUpgrades.UP_EFFICIENCY, 2);
        assertEquals(0.16, TurretUpgrades.efficiencyUpgrades(TIER3, eff, null), 1.0e-6);
    }

    @Test
    void fireRateUpgradeIsOneTenthEach() {
        Slot rof = new Slot(TurretUpgrades.UP_FIRE_RATE, 2);
        assertEquals(0.2, TurretUpgrades.fireRateUpgrades(TIER3, rof, null), 1.0e-6);
    }

    // --- Addons live in slots 9 and 10, present on tier 2+.

    @Test
    void tierOneHasNoAddons() {
        Slot amp = new Slot(TurretUpgrades.AD_DAMAGE_AMP, 1);
        assertFalse(TurretUpgrades.hasDamageAmp(TIER1, amp, null));
        assertEquals(0, TurretUpgrades.ampLevel(TIER1, amp, null));
    }

    @Test
    void addonPresenceReadsEitherAddonSlot() {
        Slot solar = new Slot(TurretUpgrades.AD_SOLAR, 1);
        assertTrue(TurretUpgrades.hasSolar(TIER3, null, solar));
        assertTrue(TurretUpgrades.hasSolar(TIER3, solar, null));
        assertFalse(TurretUpgrades.hasSolar(TIER3, null, null));
    }

    @Test
    void redstoneAndConcealmentAddonsAreRecognised() {
        assertTrue(TurretUpgrades.hasRedstoneReactor(TIER3, new Slot(TurretUpgrades.AD_REDSTONE, 1), null));
        assertTrue(TurretUpgrades.hasConcealment(TIER3, new Slot(TurretUpgrades.AD_CONCEALER, 1), null));
    }

    @Test
    void ampLevelSumsBothSlots() {
        Slot amp = new Slot(TurretUpgrades.AD_DAMAGE_AMP, 2);
        assertEquals(4, TurretUpgrades.ampLevel(TIER3, amp, amp));
        assertTrue(TurretUpgrades.hasDamageAmp(TIER3, amp, null));
    }

    // --- Derived firing math (the head's getters, ported).

    @Test
    void actualAccuracyMatchesTheClassicFormula() {
        // machine gun base deviation 30, no upgrades, no scatter: 100 - min(70,100) = 30.
        assertEquals(30.0, TurretUpgrades.actualAccuracy(30.0, 0.0, 0), 1.0e-6);
    }

    @Test
    void perfectlyAccurateTurretStaysPerfect() {
        // relativistic base deviation 0: 100 - min(100,100) = 0 spread.
        assertEquals(0.0, TurretUpgrades.actualAccuracy(0.0, 0.0, 0), 1.0e-6);
    }

    @Test
    void projectileInaccuracyIsAccuracyOverTwenty() {
        assertEquals(1.5f, TurretUpgrades.projectileInaccuracy(30.0), 1.0e-6f);
    }

    @Test
    void targetedInaccuracyIsDeviationOverTenScaledByUpgrades() {
        // 1.12 doTargetedShot: baseDeviation / 10 * (1 - accuracyUpgrades) * (1 + scatter).
        // Machine gun base deviation 30, nothing installed.
        assertEquals(3.0f, TurretUpgrades.targetedInaccuracy(30.0, 0.0, 0), 1.0e-6f);
    }

    @Test
    void accuracyUpgradesTightenTheTargetedShot() {
        // Two accuracy upgrades (0.2 each): 30 / 10 * 0.6 = 1.8.
        assertEquals(1.8f, TurretUpgrades.targetedInaccuracy(30.0, 0.4, 0), 1.0e-6f);
    }

    @Test
    void scattershotWidensTheTargetedShotPerExtraRound() {
        // One scatter upgrade doubles the spread, two triple it. This is what fans a volley out; the
        // forceShot formula this used to borrow moved it by a few percent instead.
        assertEquals(6.0f, TurretUpgrades.targetedInaccuracy(30.0, 0.0, 1), 1.0e-6f);
        assertEquals(9.0f, TurretUpgrades.targetedInaccuracy(30.0, 0.0, 2), 1.0e-6f);
    }

    @Test
    void fireRateUpgradesShortenTheInterval() {
        // base 8 ticks, one fire-rate upgrade (0.1): 8 * 0.9 = 7.2, fires on the first whole tick past
        // that, so the interval rounds up to 8.
        assertEquals(8, TurretUpgrades.effectiveFireInterval(8, 0.1));
        // four upgrades on a 40-tick gun: 40 * 0.6 = 24 exactly.
        assertEquals(24, TurretUpgrades.effectiveFireInterval(40, 0.4));
    }

    @Test
    void powerPerShotDropsWithEfficiencyAndRisesWithScatter() {
        assertEquals(100, TurretUpgrades.powerPerShot(100, 0.0, 0));
        // 8% efficiency: 100 * 0.92 = 92.
        assertEquals(92, TurretUpgrades.powerPerShot(100, 0.08, 0));
        // one scatter doubles the cost: 100 * (1 + 1) = 200.
        assertEquals(200, TurretUpgrades.powerPerShot(100, 0.0, 1));
    }

    // --- Recycler addon (meta 3): present in either addon slot, tier 2+ only.

    @Test
    void recyclerFoundInEitherAddonSlotAboveTierOne() {
        Slot recycler = new Slot(TurretUpgrades.AD_RECYCLER, 1);
        assertTrue(TurretUpgrades.hasRecycler(TIER3, recycler, null));
        assertTrue(TurretUpgrades.hasRecycler(TIER3, null, recycler));
        assertFalse(TurretUpgrades.hasRecycler(TIER1, recycler, recycler));
        assertFalse(TurretUpgrades.hasRecycler(TIER3, null, null));
    }

    // --- Fake drops addon (meta 7): level = -1 + total count, capped at 3, from 1.12
    // getFakeDropsLevel. -1 means no addon; 0-3 is the looting level.

    @Test
    void fakeDropsLevelIsMinusOneWithoutAddon() {
        assertEquals(-1, TurretUpgrades.fakeDropsLevel(TIER3, null, null));
        Slot other = new Slot(TurretUpgrades.AD_SOLAR, 1);
        assertEquals(-1, TurretUpgrades.fakeDropsLevel(TIER3, other, null));
    }

    @Test
    void fakeDropsLevelSumsBothSlotsMinusOneCappedAtThree() {
        Slot one = new Slot(TurretUpgrades.AD_FAKEDROPS, 1);
        Slot three = new Slot(TurretUpgrades.AD_FAKEDROPS, 3);
        assertEquals(0, TurretUpgrades.fakeDropsLevel(TIER3, one, null));
        assertEquals(3, TurretUpgrades.fakeDropsLevel(TIER5, three, one));
        // 3 + 3 - 1 = 5 caps at 3.
        assertEquals(3, TurretUpgrades.fakeDropsLevel(TIER5, three, three));
        // Tier 1 has no addon slots.
        assertEquals(-1, TurretUpgrades.fakeDropsLevel(TIER1, three, three));
    }
}
