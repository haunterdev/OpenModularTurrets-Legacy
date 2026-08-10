package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The rocket, ported from 1.12 {@code RocketProjectile}: flies trailing smoke, and on hitting a block
 * or a valid target explodes, damaging every living entity within five blocks. Two config switches
 * change how it behaves: {@code canRocketsHome} makes it steer onto its target for the whole flight,
 * and {@code canRocketsDestroyBlocks} picks the blast strength.
 */
public class RocketProjectileEntity extends OMTProjectile {

    private static final double BLAST_RADIUS = 5.0;
    /** 1.12: {@code canRocketsDestroyBlocks ? 2.3f : 0.1f}, and that config is off by default. */
    private static final float STRENGTH_DESTRUCTIVE = 2.3f;
    private static final float STRENGTH_HARMLESS = 0.1f;
    /** 1.12 homing speed, the third argument to its per-tick shoot() call. */
    private static final float HOMING_SPEED = 0.24f;

    // The entity this rocket was fired at, for the homing mode. Not saved: a turret round never
    // survives a reload (see OMTProjectile.readAdditionalSaveData).
    @Nullable
    private Entity homingTarget;

    public RocketProjectileEntity(EntityType<? extends RocketProjectileEntity> type, Level level) {
        super(type, level);
        setKind(ProjectileKind.ROCKET);
    }

    /** Bind the rocket to what the turret was aiming at, so the homing config has something to chase. */
    public void setHomingTarget(@Nullable Entity target) {
        this.homingTarget = target;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            // 1.12 RocketProjectile spawns 20 smoke puffs per tick, each offset by a gaussian tenth of
            // a block. That density is what makes the rocket read as a thick exhaust plume rather than
            // a dotted line, so the count is kept as upstream had it.
            for (int i = 0; i <= 20; i++) {
                double ox = random.nextGaussian() / 10.0;
                double oy = random.nextGaussian() / 10.0;
                double oz = random.nextGaussian() / 10.0;
                level().addParticle(ParticleTypes.SMOKE, getX() + ox, getY() + oy, getZ() + oz, 0.0, 0.0, 0.0);
            }
        } else if (OMTConfig.Turrets.canRocketsHome) {
            // 1.12 onEntityUpdate: with homing on, the rocket re-aims at its target every tick at a
            // fixed 0.24 speed and no spread, and destroys itself outright if the target is gone.
            if (homingTarget == null || !homingTarget.isAlive()) {
                discard();
                return;
            }
            shoot(homingTarget.getX() - getX(),
                    homingTarget.getY() + homingTarget.getEyeHeight() - 1.1F - getY(),
                    homingTarget.getZ() - getZ(),
                    HOMING_SPEED, 0.0F);
        }
        super.tick();
    }

    @Override
    protected void onHitSolid(BlockState state, BlockPos pos) {
        explode();
        discard();
    }

    @Override
    protected void onHitTarget(Entity entity) {
        if (!canDamage(entity)) {
            return;
        }
        explode();
        discard();
    }

    private void explode() {
        float strength = OMTConfig.Turrets.canRocketsDestroyBlocks
                ? STRENGTH_DESTRUCTIVE : STRENGTH_HARMLESS;
        level().explode(null, getX(), getY(), getZ(), strength, Level.ExplosionInteraction.MOB);
        AABB box = getBoundingBox().inflate(BLAST_RADIUS);
        for (LivingEntity mob : level().getEntitiesOfClass(LivingEntity.class, box)) {
            // 1.12 damageEntityLivingBase applies the damage amp addon per victim, off that victim's
            // own health. The port used the flat damage here, so an amped rocket turret hit for its
            // base rating no matter how many amp addons the base carried.
            float damage = ampedDamage(mob);
            if (mob instanceof EnderDragon && OMTConfig.Turrets.canRocketsHurtEnderDragon) {
                // The dragon refuses ordinary damage sources, so 1.12 subtracts the health directly.
                mob.setHealth(mob.getHealth() - damage);
                mob.invulnerableTime = -1;
                continue;
            }
            if (!canDamage(mob)) {
                continue;
            }
            tagTurretHit(mob);
            mob.hurt(turretDamage(), damage);
            mob.invulnerableTime = -1;
        }
    }
}
