package com.ommods.ompd.content;

/** Pure decisions at the game seam, kept separate so the original collision rules stay testable. */
public final class DefensePolicies {
    private DefensePolicies() {
    }

    /** 1.12 fence rule: item entities and the owner are safe; everything else takes cactus damage. */
    public static boolean fenceDamages(boolean itemEntity, boolean playerEntity, boolean owner) {
        return !itemEntity && !(playerEntity && owner);
    }

    /** 1.12 trap rule: only its owner gets a collision box. */
    public static boolean trapIsSolid(boolean playerEntity, boolean owner) {
        return playerEntity && owner;
    }
}
