package com.ommods.openmodularturrets.content.projectile;


import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The disposable turret shot, ported from 1.12 {@code DisposableTurretProjectile}: a cheap arced round
 * that throws whatever ammo the base was loaded with, so the item flies alongside the shot and lands as
 * loot, and deals its damage to a valid target. The bound item is removed when the shot ends so it does
 * not double up.
 */
public class DisposableProjectileEntity extends OMTProjectile {

    @Nullable
    private ItemEntity boundItem;
    private boolean spawnedItem = false;

    public DisposableProjectileEntity(EntityType<? extends DisposableProjectileEntity> type, Level level) {
        super(type, level);
        this.gravity = ProjectileKind.DISPOSABLE.gravity();
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved() || level().isClientSide() || spawnedItem || thrownAmmo.isEmpty()) {
            return;
        }
        // Bind the thrown ammo to the shot so it lands as a real, briefly un-pickupable drop. 1.12
        // cancels the shot's own gravity on the item's first tick so the two stay together in flight.
        Vec3 motion = getDeltaMovement();
        boundItem = new ItemEntity(level(), getX(), getY() - 0.2, getZ(), thrownAmmo.copy());
        boundItem.setDeltaMovement(motion.x, motion.y + gravity, motion.z);
        boundItem.setPickUpDelay(100000);
        boundItem.lifespan = 40;
        level().addFreshEntity(boundItem);
        spawnedItem = true;
    }

    @Override
    protected void onHitSolid(BlockState state, BlockPos pos) {
        clearItem();
        discard();
    }

    @Override
    protected void onHitTarget(Entity entity) {
        if (!canDamage(entity)) {
            return;
        }
        tagTurretHit(entity);
        entity.hurt(turretDamage(), ampedDamage(entity));
        // 1.12 clears the invulnerability window after the hit, not before; see BulletProjectileEntity.
        entity.invulnerableTime = -1;
        clearItem();
        discard();
    }

    private void clearItem() {
        if (boundItem != null) {
            boundItem.discard();
        }
    }
}
