package com.ommods.openmodularturrets.content.turret;

/**
 * The ammo a turret head needs per shot, or NONE for energy-only heads (the ray tracing turrets like
 * the laser). Pure: the mapping from these to actual items lives in the item registry at the game
 * seam, so {@link TurretType} stays Minecraft-free.
 */
public enum AmmoType {
    /** Energy-only heads (the ray tracing turrets like the laser) consume no ammo. */
    NONE,
    BULLET,
    GRENADE,
    ROCKET,
    /** The potato cannon eats vanilla potatoes. */
    POTATO,
    /** The incendiary turret burns blazing clay. */
    BLAZING_CLAY,
    /** The railgun fires ferro-magnetic slugs. */
    FERRO_SLUG,
    /** The disposable turret throws whatever loaded ammo it finds, matching its 1.12 non-specific ammo. */
    ANY
}
