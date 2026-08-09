package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.content.OMTSounds;
import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The machine gun's bullet, ported from 1.12 {@code BulletProjectile}: flies dead straight, stops on
 * any solid block, and on a valid target deals its damage once and vanishes. It is drawn as the
 * throwable bullet item by its renderer, exactly as 1.12's RenderSnowball did; the round itself emits
 * no particles.
 */
public class BulletProjectileEntity extends OMTProjectile {

    public BulletProjectileEntity(EntityType<? extends BulletProjectileEntity> type, Level level) {
        super(type, level);
        this.gravity = ProjectileKind.BULLET.gravity();
    }

    @Override
    protected void onHitSolid(BlockState state, BlockPos pos) {
        playHitSound();
        discard();
    }

    @Override
    protected void onHitTarget(Entity entity) {
        if (!canDamage(entity)) {
            return;
        }
        tagTurretHit(entity);
        entity.hurt(turretDamage(), ampedDamage(entity));
        // 1.12 clears the invulnerability window AFTER the hit, never before. Clearing it first let
        // hurt() set its own 20-tick window straight afterwards, so with the gun turret's 8-tick
        // cadence two rounds in three were swallowed and the turret did a third of its rated damage.
        entity.invulnerableTime = -1;
        // 1.12 plays the ricochet only on a player hit and on a block hit, never on a mob.
        if (entity instanceof Player) {
            playHitSound();
        }
        discard();
    }

    // 1.12 BulletProjectile.playSound: the ricochet, on the AMBIENT category, pitched 0.5 to 1.5.
    private void playHitSound() {
        level().playSound(null, BlockPos.containing(getX(), getY(), getZ()), OMTSounds.BULLET_HIT.get(),
                SoundSource.AMBIENT, OMTSounds.volume(), level().random.nextFloat() + 0.5F);
    }
}
