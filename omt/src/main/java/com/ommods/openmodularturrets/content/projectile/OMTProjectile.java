package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.block.TurretBaseBlock;
import com.ommods.openmodularturrets.content.turret.OMTDamage;
import com.ommods.openmodularturrets.content.turret.OMTEvents;
import com.ommods.openmodularturrets.content.turret.TurretDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Base for the classic OMT turret projectiles, ported to the 1.20.1 {@link ThrowableProjectile}
 * entity model. It carries the two things every shot needs: the position of the turret base it was
 * fired from (so damage permissions - owner, trust, attack toggles - are re-checked against that base
 * when it strikes something) and the damage to deal.
 *
 * <p>Faithful to 1.12 {@code TurretProjectile}: the projectile lives at most 40 ticks, never collides
 * with another projectile or with the firing turret's own head block, is not itself attackable, and
 * does not survive a world reload. Each subclass implements what happens when it hits a solid block or
 * a target.
 */
public abstract class OMTProjectile extends ThrowableProjectile {

    private static final String BASE_TAG = "basePos";
    private static final String DAMAGE_TAG = "damage";

    private static final EntityDataAccessor<ItemStack> DATA_AMMO =
            SynchedEntityData.defineId(OMTProjectile.class, EntityDataSerializers.ITEM_STACK);

    protected float gravity = 0.0f;
    protected float damage = 1.0f;
    // Damage-amp addon: extra damage per point of the target's current health, faithful to 1.12
    // (amp_level * damageAmp). Zero when no amp addon is present.
    protected float ampPerHealth = 0.0f;
    // Fake drops addon level carried from the base at fire time (-1 none, 0-3 looting), and whether
    // the base lacked a Loot Deleter attachment; both faithful to 1.12 TurretProjectile's fields.
    protected int fakeDrops = -1;
    protected boolean dropLoot = true;
    protected ItemStack thrownAmmo = ItemStack.EMPTY;
    @Nullable
    private BlockPos basePos;

    protected OMTProjectile(EntityType<? extends OMTProjectile> type, Level level) {
        super(type, level);
    }

    /** Bind the projectile to the base that fired it and set its per-shot damage. */
    public void fireFrom(BlockPos basePos, float damage) {
        this.basePos = basePos;
        this.damage = damage;
    }

    /** The base this shot came from, for kill crediting; null if unknown (e.g. after reload). */
    @Nullable
    protected BlockPos basePos() {
        return basePos;
    }

    /**
     * The ammo item this shot consumed. Shots that physically throw their round (the disposable
     * turret) drop it as loot; the rest ignore it. It is synched because the disposable turret's
     * renderer draws this item in flight, exactly as 1.12's ItemProjectileRenderer drew the round it
     * had bound.
     */
    public void setThrownAmmo(ItemStack ammo) {
        this.thrownAmmo = ammo == null ? ItemStack.EMPTY : ammo.copy();
        this.entityData.set(DATA_AMMO, this.thrownAmmo);
    }

    /** The round this shot is carrying, readable on the client for rendering. */
    public ItemStack thrownAmmo() {
        return this.entityData.get(DATA_AMMO);
    }

    /** Set the damage-amp bonus per point of target health (0 for no amp addon). */
    public void setAmp(float ampPerHealth) {
        this.ampPerHealth = ampPerHealth;
    }

    /** Carry the base's kill-fidelity settings: fake drops looting level and loot-deleter state. */
    public void setKillSettings(int fakeDrops, boolean dropLoot) {
        this.fakeDrops = fakeDrops;
        this.dropLoot = dropLoot;
    }

    /**
     * The turret damage source for this shot, projectile-flagged; when the base carries a fake drops
     * addon the attacker is the OMT fake player so the kill counts as a player kill.
     */
    protected DamageSource turretDamage() {
        return OMTDamage.normal((ServerLevel) level(), this, fakeDrops, basePos());
    }

    /**
     * Mark a struck mob with the 1.12 command tags: every hit gets {@code turret_hit}; hits from a
     * loot-deleted base get {@code dont_drop_loot}, which cancels its drops on death (OMTEvents).
     */
    protected void tagTurretHit(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof net.minecraft.world.entity.player.Player) {
            return;
        }
        entity.addTag(OMTEvents.TAG_TURRET_HIT);
        if (fakeDrops > -1) {
            entity.addTag(OMTEvents.TAG_FAKE_DROPS + fakeDrops);
        }
        if (!dropLoot) {
            entity.addTag(OMTEvents.TAG_DONT_DROP_LOOT);
        }
    }

    /**
     * The damage to deal to {@code entity}, including the damage-amp addon's health-based bonus.
     * Faithful to 1.12: {@code damage + floor(currentHealth) * amp_level * damageAmp}.
     */
    protected float ampedDamage(Entity entity) {
        if (ampPerHealth > 0.0f && entity instanceof LivingEntity living) {
            return damage + (int) living.getHealth() * ampPerHealth;
        }
        return damage;
    }

    /** The firing base if it still exists, used to re-check who this shot may hurt. */
    @Nullable
    protected TurretBaseTileEntity base() {
        Level level = level();
        if (basePos == null || level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(basePos);
        return be instanceof TurretBaseTileEntity b ? b : null;
    }

    /** True if this shot is allowed to hurt the given entity under its base's targeting policy. */
    protected boolean canDamage(Entity entity) {
        TurretBaseTileEntity base = base();
        return base != null && entity instanceof LivingEntity living && TurretDamage.canDamage(living, base);
    }

    @Override
    protected float getGravity() {
        return gravity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_AMMO, ItemStack.EMPTY);
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return !(entity instanceof OMTProjectile) && super.canHitEntity(entity);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /**
     * 1.12 {@code TurretProjectile.onUpdate}, ported line for line. A turret round does NOT use
     * vanilla projectile collision: it ray traces blocks over its own step, and separately hands
     * <em>every</em> collidable entity inside its bounding box grown by 1.2x its motion to
     * {@link #onHitTarget}. That fat sweep is why the original hits at all; vanilla's single precise
     * ray misses a moving mob at these speeds. Movement, drag and gravity are applied after the hit
     * checks, in the original's order.
     */
    @Override
    public void tick() {
        if (tickCount > 40) {
            discard();
            return;
        }
        // Entity bookkeeping (tickCount, fire, portals) without ThrowableProjectile's own collision.
        baseTick();
        xo = getX();
        yo = getY();
        zo = getZ();

        Vec3 motion = getDeltaMovement();
        Vec3 from = position();
        Vec3 to = from.add(motion);
        BlockHitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));

        for (Entity entity : level().getEntities(this,
                getBoundingBox().expandTowards(motion.x * 1.2, motion.y * 1.2, motion.z * 1.2))) {
            if (!entity.isPickable()) {
                continue;
            }
            hitEntity(entity);
        }
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            hitBlock(blockHit.getBlockPos());
        }
        if (isRemoved()) {
            return;
        }

        // 1.12 moves by reading motionX/Y/Z back out of the fields here, AFTER the hit callbacks have
        // run, so a callback that zeroes or dampens the motion actually takes effect this tick. That
        // is the grenade's whole landing behaviour: onHitBlock stops it dead so it can sit and count
        // down its fuse, and onHitEntity bleeds its speed off so it settles against what it struck.
        // Reusing the pre-hit snapshot instead threw both away and left the grenade sailing on at
        // launch speed until the 35-tick fuse went off a hundred blocks past the target.
        motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        updateRotation();
        // 1.12: 0.99 drag in air, 0.8 in water with a bubble trail, then gravity.
        float drag = 0.99f;
        if (isInWater()) {
            for (int i = 0; i < 4; ++i) {
                level().addParticle(ParticleTypes.BUBBLE, getX() - motion.x * 0.25, getY() - motion.y * 0.25,
                        getZ() - motion.z * 0.25, motion.x, motion.y, motion.z);
            }
            drag = 0.8f;
        }
        setDeltaMovement(motion.scale(drag).subtract(0.0, isNoGravity() ? 0.0 : getGravity(), 0.0));
    }

    /** Server-side block strike, with 1.12's two universal guards applied before the subclass sees it. */
    private void hitBlock(BlockPos pos) {
        if (level().isClientSide()) {
            return;
        }
        BlockState state = level().getBlockState(pos);
        // Every 1.12 subclass opens onHitBlock with these same two checks: a turret head never stops
        // its own shot, and a block whose material is not opaque is flown straight through.
        //
        // The test is canOcclude(), not blocksMotion(). 1.12 used Material.isOpaque(), which is false
        // for glass, so a round flew through a window. blocksMotion() is true for glass, so the port
        // stopped rounds on it while the head's own line of sight check (which does use canOcclude)
        // happily targeted through it: the turret fired at a target it could see and shot the window.
        if (state.getBlock() instanceof TurretBaseBlock || !state.canOcclude()) {
            return;
        }
        onHitSolid(state, pos);
    }

    /** Server-side entity strike, with 1.12's shared guards: never another round, never once removed. */
    private void hitEntity(Entity entity) {
        if (level().isClientSide() || entity instanceof OMTProjectile || isRemoved()) {
            return;
        }
        onHitTarget(entity);
    }

    /** Handle striking a solid block that is not the firing turret's own head. */
    protected abstract void onHitSolid(BlockState state, BlockPos pos);

    /** Handle striking an entity (may be spared by {@link #canDamage}). */
    protected abstract void onHitTarget(Entity entity);

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (basePos != null) {
            tag.putLong(BASE_TAG, basePos.asLong());
        }
        tag.putFloat(DAMAGE_TAG, damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Faithful to 1.12: a turret projectile does not persist across a reload; drop it on load.
        discard();
    }
}
