package com.ommods.openmodularturrets.content.turret;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the faithful 1.12 turret stats (docs/OMT-1.12-REFERENCE.md) against typos. TurretType is
 * pure data on purpose: no Minecraft imports, so it unit tests without bootstrapping the game.
 */
class TurretTypeTest {

    @Test
    void machineGunMatchesFaithfulStats() {
        TurretType t = TurretType.MACHINE_GUN;
        assertEquals(18.0D, t.range());
        assertEquals(8, t.fireInterval());
        assertEquals(2.0F, t.damage());
        assertEquals(100, t.energyPerShot());
    }

    @Test
    void laserMatchesFaithfulStats() {
        TurretType t = TurretType.LASER;
        assertEquals(25.0D, t.range());
        assertEquals(10, t.fireInterval());
        assertEquals(4.0F, t.damage());
        assertEquals(8000, t.energyPerShot());
    }

    @Test
    void railgunMatchesFaithfulStats() {
        TurretType t = TurretType.RAILGUN;
        assertEquals(30.0D, t.range());
        assertEquals(100, t.fireInterval());
        assertEquals(25.0F, t.damage());
        assertEquals(25000, t.energyPerShot());
    }

    @Test
    void blockIdIsStableAndLowerCase() {
        assertEquals("machine_gun", TurretType.MACHINE_GUN.blockId());
        assertEquals("laser", TurretType.LASER.blockId());
        assertEquals("railgun", TurretType.RAILGUN.blockId());
    }

    @Test
    void projectileTurretsRequireTheir112Ammo() {
        // Faithful 1.12: each projectile turret's getAmmo. Potato eats potatoes, incendiary blazing
        // clay, disposable throws whatever ammo is loaded, the rest their named rounds.
        assertEquals(AmmoType.BULLET, TurretType.MACHINE_GUN.ammo());
        assertEquals(AmmoType.GRENADE, TurretType.GRENADE.ammo());
        assertEquals(AmmoType.ROCKET, TurretType.ROCKET.ammo());
        assertEquals(AmmoType.POTATO, TurretType.POTATO_CANNON.ammo());
        assertEquals(AmmoType.BLAZING_CLAY, TurretType.INCENDIARY.ammo());
        assertEquals(AmmoType.ANY, TurretType.DISPOSABLE.ammo());
    }

    @Test
    void hitscanTurretsNeedNoAmmoExceptTheRailgun() {
        assertEquals(AmmoType.NONE, TurretType.LASER.ammo());
        assertEquals(AmmoType.NONE, TurretType.RELATIVISTIC.ammo());
        // 1.12 RailGunTurretTileEntity requiresAmmo and requiresSpecificAmmo both return true and its
        // getAmmo is ammo_meta:2, the ferro-magnetic slug. It is the one hitscan head that eats ammo.
        assertEquals(AmmoType.FERRO_SLUG, TurretType.RAILGUN.ammo());
    }

    @Test
    void teleporterMatchesFaithfulStats() {
        // Faithful 1.12 OMTConfig teleporter_turret: range 20, fireRate 100, no damage, 15000 FE/shot,
        // no ammo. It does not shoot; it yanks the target onto the turret.
        TurretType t = TurretType.TELEPORTER;
        assertEquals(20.0D, t.range());
        assertEquals(100, t.fireInterval());
        assertEquals(0.0F, t.damage());
        assertEquals(15000, t.energyPerShot());
        assertEquals(AmmoType.NONE, t.ammo());
        assertEquals("teleporter", t.blockId());
    }

    @Test
    void teleporterFiresNoProjectileAndTeleports() {
        assertEquals(ProjectileKind.NONE, TurretType.TELEPORTER.projectile());
        assertFalse(TurretType.TELEPORTER.isProjectile());
        assertTrue(TurretType.TELEPORTER.isTeleport());
        assertFalse(TurretType.MACHINE_GUN.isTeleport());
        assertFalse(TurretType.LASER.isTeleport());
    }

    @Test
    void teleporterAloneHasAMinimumTargetRange() {
        // Faithful 1.12 getTargetWithMinimumRange: teleporter ignores targets within 3 blocks. Every
        // other head targets from point blank.
        assertEquals(3.0D, TurretType.TELEPORTER.minTargetRange());
        assertEquals(0.0D, TurretType.MACHINE_GUN.minTargetRange());
        assertEquals(0.0D, TurretType.RAILGUN.minTargetRange());
    }
}
