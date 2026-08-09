package com.ommods.openmodularturrets.content.turret;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The warning cooldown, faithful to 1.12 TurretHeadUtil.warnPlayers bookkeeping: a player near a
 * base is warned once, then not again for 12000 ticks, tracked per player and base pair.
 */
class WarnCooldownTest {

    private final UUID player = UUID.fromString("c5c97afa-fc98-44ab-944a-e67681a66b19");
    private final UUID other = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void firstEncounterWarns() {
        WarnCooldown cooldown = new WarnCooldown();
        assertTrue(cooldown.shouldWarn(player, 100L, 0L));
    }

    @Test
    void repeatWithinCooldownStaysQuiet() {
        WarnCooldown cooldown = new WarnCooldown();
        assertTrue(cooldown.shouldWarn(player, 100L, 0L));
        assertFalse(cooldown.shouldWarn(player, 100L, 1L));
        assertFalse(cooldown.shouldWarn(player, 100L, 12000L));
    }

    @Test
    void warnsAgainAfterTheCooldownExpires() {
        WarnCooldown cooldown = new WarnCooldown();
        assertTrue(cooldown.shouldWarn(player, 100L, 0L));
        assertTrue(cooldown.shouldWarn(player, 100L, 12001L));
    }

    @Test
    void cooldownIsPerPlayerAndBasePair() {
        WarnCooldown cooldown = new WarnCooldown();
        assertTrue(cooldown.shouldWarn(player, 100L, 0L));
        assertTrue(cooldown.shouldWarn(other, 100L, 0L));
        assertTrue(cooldown.shouldWarn(player, 200L, 0L));
    }
}
