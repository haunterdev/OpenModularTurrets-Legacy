package com.ommods.openmodularturrets.content.turret;

/**
 * The faithful OMT targeting policy, ported from 1.12 TurretHeadUtil.getTarget plus the
 * targetingChecks exclusions. Pure data and rules, no Minecraft: the block entity classifies the
 * candidate and resolves its protection (owner/trusted/creative/op) at the game seam, and this
 * decides whether the candidate may be shot.
 */
public final class TurretTargeting {

    /** How the turret classifies a candidate entity. */
    public enum Category {
        HOSTILE,
        NEUTRAL,
        PLAYER,
        /** Matches none of the 1.12 lists (villager, squid, armor stand): never a target. */
        UNLISTED
    }

    /** Which categories the base is set to attack. */
    public record Flags(boolean hostile, boolean neutral, boolean player) {
    }

    /**
     * Why a candidate might be off limits. Any true field spares the candidate.
     *
     * <p>These are 1.12's four and only four exclusions, from {@code OMTUtil.canDamagePlayer} plus the
     * creative check its callers apply: the base's owner, a trusted player, a player sharing the
     * owner's scoreboard team, and anyone in creative. Operator status is not among them: no 1.12 code
     * path checks permission level when picking a target, and treating it as protection makes a single
     * player world unshootable, since its host is always permission level 4.
     */
    public record Protection(boolean owner, boolean trusted, boolean creative, boolean sameTeam) {
        public static final Protection NONE = new Protection(false, false, false, false);

        /** True if any exclusion applies, so the candidate may not be shot. */
        public boolean spared() {
            return owner || trusted || creative || sameTeam;
        }
    }

    private TurretTargeting() {
    }

    public static boolean canTarget(Category category, Flags flags, Protection protection) {
        boolean enabled = switch (category) {
            case HOSTILE -> flags.hostile();
            case NEUTRAL -> flags.neutral();
            case PLAYER -> flags.player();
            case UNLISTED -> false;
        };
        if (!enabled) {
            return false;
        }
        return !protection.spared();
    }
}
