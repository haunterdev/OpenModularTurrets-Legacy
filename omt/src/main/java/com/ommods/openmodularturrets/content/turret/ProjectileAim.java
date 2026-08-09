package com.ommods.openmodularturrets.content.turret;

/**
 * Faithful port of OMT 1.12 {@code ProjectileTurret.doTargetedShot}: given a turret block and a
 * target, work out the launch direction and speed for a projectile. Pure math (no Minecraft imports)
 * so it unit tests without the game, mirroring {@link TurretType} and {@link TurretTargeting}.
 *
 * The original aims at the target's mid height, leads it by the projectile's flight time, then scales
 * the launch speed by how far that lead stretched the aim vector. Non-gravity shots (bullets,
 * rockets, incendiary) fly at speed 3.0 and cover distance in {@code dist/3.0} ticks; arced shots
 * (grenade, potato, disposable) use {@code dist/1.6}. The block entity feeds the resulting direction
 * and speed straight into the projectile's {@code shoot}, which normalises the direction and applies
 * the random inaccuracy, exactly as the 1.12 turret did.
 */
public final class ProjectileAim {

    /** Launch direction (un-normalised, as 1.12 handed it to shoot) and the speed to fire at. */
    public record Solution(double dirX, double dirY, double dirZ, float velocity) {
    }

    /**
     * @param bx block X (integer origin; the +0.5 to reach the block centre is applied here)
     * @param by block Y
     * @param bz block Z
     * @param tx target X (world position)
     * @param ty target Y (feet)
     * @param tz target Z
     * @param targetHeight target bounding-box height; the shot aims at {@code ty + height*0.5}
     * @param vx target velocity X per tick (its lead)
     * @param vy target velocity Y per tick
     * @param vz target velocity Z per tick
     * @param gravity true for arced ammo (grenade/potato/disposable), false for straight shots
     */
    public static Solution solve(double bx, double by, double bz,
                                 double tx, double ty, double tz, double targetHeight,
                                 double vx, double vy, double vz, boolean gravity) {
        double d0 = tx - (bx + 0.5);
        double d1 = ty + targetHeight * 0.5 - (by + 0.5);
        double d2 = tz - (bz + 0.5);
        double dist = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        double time = dist / (gravity ? 1.6 : 3.0);
        double adjX = d0 + vx * time;
        double adjY = d1 + vy * time;
        double adjZ = d2 + vz * time;
        double dist2 = Math.sqrt(adjX * adjX + adjY * adjY + adjZ * adjZ);
        float speedFactor = (float) (dist2 / dist);
        return new Solution(adjX, adjY - 0.1, adjZ, 3.0F * speedFactor);
    }

    private ProjectileAim() {
    }
}
