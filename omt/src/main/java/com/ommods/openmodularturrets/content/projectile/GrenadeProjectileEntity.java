package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.turret.OMTDamage;
import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The grenade, ported from 1.12 {@code GrenadeProjectile}: an arced throw that sticks where it lands
 * (a hit block stops its motion, a hit target makes it settle and arm) and detonates on a fuse at 35
 * ticks, damaging living entities within three blocks. The explosion breaks terrain like the original
 * when mob griefing is enabled.
 */
public class GrenadeProjectileEntity extends OMTProjectile {

    private static final int FUSE_TICKS = 35;
    private static final int ARMED_FUSE = 32;
    private static final double BLAST_RADIUS = 3.0;
    /**
     * 1.12: {@code canGrenadesDestroyBlocks ? 1.4f : 0.1f}, and that config is off by default. The
     * port hard-coded 1.4 and so cratered the ground on every shot, which the original does not do
     * unless you turn it on.
     */
    private static final float STRENGTH_DESTRUCTIVE = 1.4f;
    private static final float STRENGTH_HARMLESS = 0.1f;
    /** 1.12's detonation dust: EnumParticleTypes.REDSTONE tinted (1,1,1), i.e. white, at scale 1. */
    private static final DustParticleOptions BURST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);

    private boolean armed = false;

    public GrenadeProjectileEntity(EntityType<? extends GrenadeProjectileEntity> type, Level level) {
        super(type, level);
        setKind(ProjectileKind.GRENADE);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }
        if (level().isClientSide()) {
            // 1.12 spawns 20 white redstone dust at the grenade's exact position once the fuse is up,
            // which is the flash the player sees as it goes off. Not smoke: EnumParticleTypes.REDSTONE
            // with (1,1,1) as its colour triple.
            if (tickCount >= FUSE_TICKS) {
                for (int i = 0; i <= 20; i++) {
                    level().addParticle(BURST, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
                }
            }
            return;
        }
        if (tickCount >= FUSE_TICKS) {
            detonate();
        }
    }

    @Override
    protected void onHitSolid(BlockState state, BlockPos pos) {
        // Stick where it lands and keep counting down to the fuse.
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    protected void onHitTarget(Entity entity) {
        if (armed || !canDamage(entity)) {
            return;
        }
        // Settle against the target and bring the fuse forward, matching 1.12.
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x * 0.4, v.y * 1.2, v.z * 0.4);
        armed = true;
        tickCount = ARMED_FUSE;
    }

    private void detonate() {
        float strength = OMTConfig.Turrets.canGrenadesDestroyBlocks
                ? STRENGTH_DESTRUCTIVE : STRENGTH_HARMLESS;
        level().explode(null, getX(), getY(), getZ(), strength, Level.ExplosionInteraction.MOB);
        AABB box = getBoundingBox().inflate(BLAST_RADIUS);
        for (LivingEntity mob : level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (canDamage(mob)) {
                tagTurretHit(mob);
                // Faithful 1.12 split: 90 percent as a normal projectile hit, then 10 percent ignoring
                // armor, landed back to back with no invulnerability reset between them and the reset
                // only afterwards. That ordering matters: the second, smaller hit is refused by the
                // window the first one opened, exactly as upstream, so a grenade lands 90 percent of
                // its rating and the mob is left hittable again by the next round.
                float total = ampedDamage(mob);
                mob.hurt(turretDamage(), total * 0.9f);
                mob.hurt(OMTDamage.bypass((ServerLevel) level(), this, fakeDrops, basePos(),
                        kind.deathMessageId()), total * 0.1f);
                mob.invulnerableTime = -1;
            }
        }
        discard();
    }
}
