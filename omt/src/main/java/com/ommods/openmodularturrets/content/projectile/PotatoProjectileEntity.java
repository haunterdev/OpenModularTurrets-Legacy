package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The potato cannon shot, ported from 1.12 {@code PotatoProjectile}: an arced throw that spawns a real
 * potato item flying alongside it (so a hit leaves the potato on the ground), and on a valid target
 * deals its damage once. The bound item is removed when the shot ends so it does not double up.
 */
public class PotatoProjectileEntity extends OMTProjectile {

    @Nullable
    private ItemEntity boundItem;
    private boolean spawnedItem = false;

    public PotatoProjectileEntity(EntityType<? extends PotatoProjectileEntity> type, Level level) {
        super(type, level);
        setKind(ProjectileKind.POTATO);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved() || level().isClientSide() || spawnedItem) {
            return;
        }
        // 1.12 binds the consumed ammo stack itself, not a fresh potato, and cancels the shot's own
        // gravity on the item's first tick so the two stay together in flight.
        Vec3 motion = getDeltaMovement();
        boundItem = new ItemEntity(level(), getX(), getY() - 0.2, getZ(), thrownAmmo.copy());
        boundItem.setDeltaMovement(motion.x, motion.y + gravity, motion.z);
        boundItem.setPickUpDelay(10000);
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
