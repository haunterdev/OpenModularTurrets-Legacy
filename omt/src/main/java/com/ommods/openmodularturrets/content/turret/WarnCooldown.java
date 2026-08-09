package com.ommods.openmodularturrets.content.turret;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Bookkeeping for the turret proximity warning, faithful to the 1.12 TurretHeadUtil.warnedPlayers
 * map: each player and base pair is warned once, then goes quiet for {@link #COOLDOWN_TICKS} game
 * ticks (10 minutes), after which the warning fires again. Expired entries are dropped on the next
 * lookup, exactly as 1.12 pruned them.
 */
public class WarnCooldown {

    public static final long COOLDOWN_TICKS = 12000L;

    private final Map<Key, Long> warnedUntil = new HashMap<>();

    /** True when the player should be warned now; arms the cooldown for the pair when it is. */
    public boolean shouldWarn(UUID player, long basePos, long gameTime) {
        Key key = new Key(player, basePos);
        Long until = warnedUntil.get(key);
        if (until != null && until >= gameTime) {
            return false;
        }
        warnedUntil.put(key, gameTime + COOLDOWN_TICKS);
        return true;
    }

    private record Key(UUID player, long basePos) {
    }
}
