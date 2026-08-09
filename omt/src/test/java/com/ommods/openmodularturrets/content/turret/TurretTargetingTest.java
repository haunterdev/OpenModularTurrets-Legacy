package com.ommods.openmodularturrets.content.turret;

import com.ommods.openmodularturrets.content.turret.TurretTargeting.Category;
import com.ommods.openmodularturrets.content.turret.TurretTargeting.Flags;
import com.ommods.openmodularturrets.content.turret.TurretTargeting.Protection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The faithful OMT targeting policy (TurretHeadUtil.getTarget + targetingChecks): a candidate is a
 * valid target only if its category is enabled AND it is not the owner, a trusted player, in
 * creative, or an op. Pure so it unit tests without the game.
 */
class TurretTargetingTest {

    private static final Flags HOSTILE_ONLY = new Flags(true, false, false);
    private static final Flags ALL = new Flags(true, true, true);

    @Test
    void hostileTargetedWhenEnabledAndUnprotected() {
        assertTrue(TurretTargeting.canTarget(Category.HOSTILE, HOSTILE_ONLY, Protection.NONE));
    }

    @Test
    void hostileIgnoredWhenDisabled() {
        Flags off = new Flags(false, true, true);
        assertFalse(TurretTargeting.canTarget(Category.HOSTILE, off, Protection.NONE));
    }

    @Test
    void neutralIgnoredWhenDisabled() {
        assertFalse(TurretTargeting.canTarget(Category.NEUTRAL, HOSTILE_ONLY, Protection.NONE));
    }

    @Test
    void playerTargetedWhenEnabledAndUnprotected() {
        assertTrue(TurretTargeting.canTarget(Category.PLAYER, ALL, Protection.NONE));
    }

    @Test
    void unlistedNeverTargetedEvenWithEveryFlagOn() {
        // 1.12 only shot IMob entities, animals and ambient creatures (and players): a villager,
        // squid or armor stand matched no list and was never a target.
        assertFalse(TurretTargeting.canTarget(Category.UNLISTED, ALL, Protection.NONE));
    }

    @Test
    void ownerNeverTargetedEvenWhenPlayersEnabled() {
        Protection owner = new Protection(true, false, false, false);
        assertFalse(TurretTargeting.canTarget(Category.PLAYER, ALL, owner));
    }

    @Test
    void trustedNeverTargeted() {
        Protection trusted = new Protection(false, true, false, false);
        assertFalse(TurretTargeting.canTarget(Category.PLAYER, ALL, trusted));
    }

    @Test
    void creativeNeverTargeted() {
        Protection creative = new Protection(false, false, true, false);
        assertFalse(TurretTargeting.canTarget(Category.PLAYER, ALL, creative));
    }

    @Test
    void teamMateNeverTargeted() {
        // 1.12 OMTUtil.canDamagePlayer spared anyone whose scoreboard team matched the base owner's.
        Protection teamMate = new Protection(false, false, false, true);
        assertFalse(TurretTargeting.canTarget(Category.PLAYER, ALL, teamMate));
    }

    @Test
    void unprotectedPlayerIsTargetedRegardlessOfPermissionLevel() {
        // 1.12 never spared operators. Protection carries no op field, so an ordinary player with
        // none of the three exclusions is a valid target even when they run the server.
        assertTrue(TurretTargeting.canTarget(Category.PLAYER, ALL, Protection.NONE));
    }
}
