package com.ommods.openmodularturrets.content.turret;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Which turret fires which classic projectile, and the two gravity numbers each one carries. In 1.12
 * the projectile's own gravity (how it flies) and the turret's lead gravity (how far it leads the
 * target) are set separately: a machine gun bullet flies dead straight yet the gun leads as if arced,
 * so both values are pinned here from the 1.12 turret classes.
 */
class ProjectileKindTest {

    @Test
    void projectileTurretsMapToTheirClassicProjectile() {
        assertEquals(ProjectileKind.BULLET, TurretType.MACHINE_GUN.projectile());
        assertEquals(ProjectileKind.ROCKET, TurretType.ROCKET.projectile());
        assertEquals(ProjectileKind.GRENADE, TurretType.GRENADE.projectile());
        assertEquals(ProjectileKind.POTATO, TurretType.POTATO_CANNON.projectile());
        assertEquals(ProjectileKind.DISPOSABLE, TurretType.DISPOSABLE.projectile());
        assertEquals(ProjectileKind.BLAZING_CLAY, TurretType.INCENDIARY.projectile());
    }

    @Test
    void hitscanTurretsHaveNoProjectile() {
        assertEquals(ProjectileKind.NONE, TurretType.LASER.projectile());
        assertEquals(ProjectileKind.NONE, TurretType.RAILGUN.projectile());
        assertEquals(ProjectileKind.NONE, TurretType.RELATIVISTIC.projectile());
        assertFalse(TurretType.LASER.isProjectile());
        assertTrue(TurretType.MACHINE_GUN.isProjectile());
    }

    @Test
    void bulletFliesStraightButItsGunLeadsAsIfArced() {
        assertEquals(0.0f, ProjectileKind.BULLET.gravity(), 0.0f);
        assertTrue(ProjectileKind.BULLET.leadGravity());
    }

    @Test
    void arcedProjectilesFallUnderGravity() {
        assertEquals(0.03f, ProjectileKind.GRENADE.gravity(), 0.0f);
        assertEquals(0.03f, ProjectileKind.POTATO.gravity(), 0.0f);
        assertEquals(0.03f, ProjectileKind.DISPOSABLE.gravity(), 0.0f);
    }

    @Test
    void rocketAndIncendiaryLeadOnAStraightLine() {
        assertFalse(ProjectileKind.ROCKET.leadGravity());
        assertFalse(ProjectileKind.BLAZING_CLAY.leadGravity());
    }
}
