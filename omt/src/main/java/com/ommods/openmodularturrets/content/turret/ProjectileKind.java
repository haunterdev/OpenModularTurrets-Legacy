package com.ommods.openmodularturrets.content.turret;

/**
 * The classic OMT projectiles a turret can fire, plus the two gravity numbers each carries. Pure data
 * (no Minecraft imports) like {@link TurretType}: the entity spawning that turns a kind into a real
 * {@code ThrowableProjectile} is a game seam handled in the block entity.
 *
 * <p>{@code gravity} is how the projectile itself falls in flight; {@code leadGravity} is whether the
 * turret leads the target as an arc. In 1.12 these are set separately (a machine gun bullet flies dead
 * straight yet the gun leads as if arced), so both come straight from the 1.12 turret and projectile
 * classes.
 */
public enum ProjectileKind {
    /** Hitscan turrets (laser, railgun, relativistic) fire no projectile. */
    NONE(0.0f, false, "turret"),
    BULLET(0.0f, true, "bullet"),
    ROCKET(0.0f, false, "rocket"),
    GRENADE(0.03f, true, "grenade"),
    POTATO(0.03f, true, "disposable"),
    DISPOSABLE(0.03f, true, "disposable"),
    // 1.12's BlazingClayProjectile built its damage source with the name "bullet", so an incendiary
    // kill read as a bullet kill and death.attack.blazingclay was never reachable. Kept as it was.
    BLAZING_CLAY(0.0f, false, "bullet");

    private final float gravity;
    private final boolean leadGravity;
    private final String deathMessageId;

    ProjectileKind(float gravity, boolean leadGravity, String deathMessageId) {
        this.gravity = gravity;
        this.leadGravity = leadGravity;
        this.deathMessageId = deathMessageId;
    }

    /**
     * The name 1.12 passed to the damage source constructor, which vanilla turns into the
     * {@code death.attack.<id>} chat line when the shot kills something.
     */
    public String deathMessageId() {
        return deathMessageId;
    }

    /** Downward acceleration the projectile falls at per tick; 0 means it flies straight. */
    public float gravity() {
        return gravity;
    }

    /** True if the turret leads a moving target with the slower arced flight time. */
    public boolean leadGravity() {
        return leadGravity;
    }
}
