package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The incendiary turret's blazing clay shot, ported from 1.12 {@code BlazingClayProjectile}: flies
 * straight and, on hitting a block or a valid target, burns every living entity within five blocks,
 * dealing its damage and setting them alight for five seconds.
 */
public class BlazingClayProjectileEntity extends OMTProjectile {

    private static final double BLAST_RADIUS = 5.0;
    private static final int BURN_SECONDS = 5;

    public BlazingClayProjectileEntity(EntityType<? extends BlazingClayProjectileEntity> type, Level level) {
        super(type, level);
        setKind(ProjectileKind.BLAZING_CLAY);
    }

    // DROPPED: no tick override. 1.12 BlazingClayProjectile spawns no particles of any kind; the
    // round is drawn only as its blazing clay item by the renderer. An earlier flame trail here was
    // invented by this port.

    @Override
    protected void onHitSolid(BlockState state, BlockPos pos) {
        // A round that ends on terrain has nobody to read an amp bonus off, so it splashes for its
        // flat damage, exactly as 1.12's onHitBlock does.
        burn(damage);
        discard();
    }

    @Override
    protected void onHitTarget(Entity entity) {
        if (!canDamage(entity)) {
            return;
        }
        burn(ampedDamage(entity));
        discard();
    }

    /**
     * The splash. {@code amped} carries the damage the struck entity earned: 1.12 works the damage-amp
     * bonus out once, off whatever the round actually hit, then deals that same figure to everything in
     * the blast. The port passed the flat damage instead, so an amped incendiary turret never gained
     * anything from its addons.
     */
    private void burn(float amped) {
        AABB box = getBoundingBox().inflate(BLAST_RADIUS);
        for (LivingEntity mob : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (canDamage(mob)) {
                tagTurretHit(mob);
                mob.hurt(turretDamage(), amped);
                mob.setSecondsOnFire(BURN_SECONDS);
                // 1.12 clears the invulnerability window after the hit, never before.
                mob.invulnerableTime = 0;
            }
        }
    }
}
