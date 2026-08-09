package com.ommods.openmodularturrets.content.turret;

/**
 * Faithful per-turret stats from OMT 1.12 (docs/OMT-1.12-REFERENCE.md). Pure data: no Minecraft
 * imports, so it is unit tested without bootstrapping the game. The visual side (particle, sound)
 * lives in the block entity, which is a game seam; this enum only carries the numbers.
 *
 * These are the hitscan-appropriate turrets. Projectile turrets (rocket, grenade, potato) need
 * projectile entities and land in a later step.
 */
public enum TurretType {
    // Faithful OMT 1.12 TurretSetting stats (range, fireRate ticks, damage, power/shot, base accuracy
    // deviation, damage-amp per level). Machine gun/grenade/rocket consume ammo; the ray tracing and
    // effect turrets are energy only.
    DISPOSABLE("disposable", 10.0D, 25, 2.0F, 2, AmmoType.ANY, 50.0D, 0.05D),
    POTATO_CANNON("potato_cannon", 15.0D, 35, 3.0F, 10, AmmoType.POTATO, 30.0D, 0.05D),
    MACHINE_GUN("machine_gun", 18.0D, 8, 2.0F, 100, AmmoType.BULLET, 30.0D, 0.06D),
    INCENDIARY("incendiary", 12.0D, 25, 2.0F, 250, AmmoType.BLAZING_CLAY, 30.0D, 0.05D),
    GRENADE("grenade", 20.0D, 40, 8.0F, 3000, AmmoType.GRENADE, 30.0D, 0.08D),
    RELATIVISTIC("relativistic", 20.0D, 25, 0.0F, 5000, AmmoType.NONE, 0.0D, 0.0D),
    ROCKET("rocket", 30.0D, 30, 10.0F, 5000, AmmoType.ROCKET, 10.0D, 0.08D),
    // The teleporter deals no damage and fires no projectile: it yanks its target onto the turret.
    TELEPORTER("teleporter", 20.0D, 100, 0.0F, 15000, AmmoType.NONE, 0.0D, 0.0D),
    LASER("laser", 25.0D, 10, 4.0F, 8000, AmmoType.NONE, 10.0D, 0.06D),
    RAILGUN("railgun", 30.0D, 100, 25.0F, 25000, AmmoType.FERRO_SLUG, 3.0D, 0.1D);

    private final String blockId;
    private final double range;
    private final int fireInterval;
    private final float damage;
    private final int energyPerShot;
    private final AmmoType ammo;
    private final double accuracyDeviation;
    private final double damageAmp;

    TurretType(String blockId, double range, int fireInterval, float damage, int energyPerShot,
               AmmoType ammo, double accuracyDeviation, double damageAmp) {
        this.blockId = blockId;
        this.range = range;
        this.fireInterval = fireInterval;
        this.damage = damage;
        this.energyPerShot = energyPerShot;
        this.ammo = ammo;
        this.accuracyDeviation = accuracyDeviation;
        this.damageAmp = damageAmp;
    }

    /** Base accuracy deviation (spread) before upgrades; 0 for a pinpoint turret. */
    public double accuracyDeviation() {
        return accuracyDeviation;
    }

    /** Per-amp-level bonus damage as a fraction of the target's current health. */
    public double damageAmp() {
        return damageAmp;
    }

    /** The ammo this head consumes per shot, or NONE for an energy-only head. */
    public AmmoType ammo() {
        return ammo;
    }

    /**
     * The classic projectile this turret launches, or {@link ProjectileKind#NONE} for the hitscan
     * turrets (laser, railgun, relativistic) that damage instantly instead of firing an entity.
     */
    public ProjectileKind projectile() {
        return switch (this) {
            case MACHINE_GUN -> ProjectileKind.BULLET;
            case ROCKET -> ProjectileKind.ROCKET;
            case GRENADE -> ProjectileKind.GRENADE;
            case POTATO_CANNON -> ProjectileKind.POTATO;
            case DISPOSABLE -> ProjectileKind.DISPOSABLE;
            case INCENDIARY -> ProjectileKind.BLAZING_CLAY;
            case LASER, RAILGUN, RELATIVISTIC, TELEPORTER -> ProjectileKind.NONE;
        };
    }

    /** True if this turret fires a flying projectile rather than a hitscan shot. */
    public boolean isProjectile() {
        return projectile() != ProjectileKind.NONE;
    }

    /**
     * True for the two heads that apply an effect instead of shooting: the teleporter and the
     * relativistic turret. In 1.12 both extend {@code TurretHead} directly rather than
     * {@code ProjectileTurret} or {@code RayTracingTurret}, so neither has the scatter-shot round loop
     * and neither deals damage. They still pay the scattershot energy multiplier, as upstream did.
     */
    public boolean isSingleEffect() {
        return this == TELEPORTER || this == RELATIVISTIC;
    }

    /**
     * True for the teleporter, whose shot teleports the target onto the turret instead of damaging it.
     * It fires no projectile and does no hitscan, so the block entity handles it as its own effect.
     */
    public boolean isTeleport() {
        return this == TELEPORTER;
    }

    /**
     * The closest a target may be and still be fired on. Faithful to 1.12's getTargetWithMinimumRange:
     * only the teleporter ignores targets within 3 blocks (so it does not endlessly yank a mob already
     * on top of it); every other head shoots from point blank.
     */
    public double minTargetRange() {
        return this == TELEPORTER ? 3.0D : 0.0D;
    }

    /**
     * How many heads of this type one base carries at once, the 1.12 TurretSetting maxSimultaneous:
     * placement is refused past it (so one base never mounts two teleporters).
     */
    public int maxSimultaneous() {
        return switch (this) {
            case TELEPORTER -> 1;
            case RAILGUN -> 2;
            case GRENADE, ROCKET -> 3;
            default -> 4;
        };
    }

    /**
     * The minimum base tier this head runs on, faithful to the 1.12 turretTier each tile entity
     * passed to super(): a head on a lower-tier base destroys itself and drops.
     */
    public int requiredTier() {
        return switch (this) {
            case DISPOSABLE, POTATO_CANNON -> 1;
            case MACHINE_GUN, INCENDIARY -> 2;
            case GRENADE, RELATIVISTIC -> 3;
            case ROCKET, TELEPORTER -> 4;
            case LASER, RAILGUN -> 5;
        };
    }

    /** Registry path for the block, e.g. {@code turret_<blockId>}. */
    public String blockId() {
        return blockId;
    }

    public String registryName() {
        return "turret_" + blockId;
    }

    /** The 1.12 display name, verbatim from the en_US.lang tile entries. */
    public String displayName() {
        return switch (this) {
            case DISPOSABLE -> "Disposable Item Turret";
            case POTATO_CANNON -> "Potato Cannon Turret";
            case MACHINE_GUN -> "Gun Turret";
            case INCENDIARY -> "Incendiary Turret";
            case GRENADE -> "Grenade Launcher Turret";
            case RELATIVISTIC -> "Relativistic Turret";
            case ROCKET -> "Rocket Launcher Turret";
            case TELEPORTER -> "Teleporter Turret";
            case LASER -> "Laser Turret";
            case RAILGUN -> "Rail Gun Turret";
        };
    }

    /** Target search radius in blocks. */
    public double range() {
        return range;
    }

    /** Ticks between shots. */
    public int fireInterval() {
        return fireInterval;
    }

    /** Damage per shot. */
    public float damage() {
        return damage;
    }

    /** Energy spent per shot (not gated in v1, but drained when present). */
    public int energyPerShot() {
        return energyPerShot;
    }
}
