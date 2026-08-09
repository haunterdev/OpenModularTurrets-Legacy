package com.ommods.openmodularturrets.content.block;

import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.omlib.network.OMLibChannel;
import com.ommods.omlib.network.PacketRenderRay;
import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.OMTSounds;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.projectile.OMTEntities;
import com.ommods.openmodularturrets.content.projectile.OMTProjectile;
import com.ommods.openmodularturrets.content.turret.OMTDamage;
import com.ommods.openmodularturrets.content.turret.OMTEvents;
import com.ommods.openmodularturrets.content.turret.ProjectileAim;
import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import com.ommods.openmodularturrets.content.turret.TurretDamage;
import com.ommods.openmodularturrets.content.turret.TurretTargeting;
import com.ommods.openmodularturrets.content.turret.TurretType;
import com.ommods.openmodularturrets.content.turret.TurretUpgrades;
import com.ommods.openmodularturrets.content.turret.WarnCooldown;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A turret head. It attaches to a {@link TurretBaseTileEntity} on one of its six sides and draws all
 * its resources from that base: energy to fire, the attack toggles, and the owner and trusted
 * players for target exclusions. With no adjacent base, or with the base out of energy, it does not
 * fire. Its stats come from {@link TurretType}, so every head type shares this one firing loop.
 */
public class TurretBaseBlockEntity extends OMBlockEntity {

    private final TurretType type;

    // 1.12 AbstractDirectedTurret kept two counters, and both run every tick regardless of what else
    // the head does. `ticks` is the shot timer: it climbs on every tick and the head fires the moment
    // it passes the type's fire interval, then resets. `targetingTicks` paces the target search while
    // the head has nobody to shoot.
    //
    // The port used to hold a `cooldown` that returned out of tick() at the very top, which skipped
    // everything else the tick does: the head stopped tracking between shots (a railgun at 100 ticks
    // aimed once every five seconds and snapped), stopped warning approaching players, stopped
    // concealing, and would not notice its base had gone.
    private int ticks = 0;
    private int targetingTicks = 0;

    // Aim angles in degrees toward the current target, synced to the client so the renderer can
    // turn the head. Both follow 1.12's TurretHeadUtil conventions: yaw is atan2(dZ, dX) in 0..360
    // and pitch is measured from straight up, so 90 degrees means a level target.
    private float renderYaw = 0.0F;
    // 1.12 AbstractDirectedTurret leaves pitch and yaw at zero until the first target, which is
    // what makes a freshly placed head sit pointing straight up.
    private float renderPitch = 0.0F;

    // Proximity warning, faithful to 1.12 warnPlayers: untrusted players within range plus the
    // configured warning distance (default 5) get the alarm sound and a red chat message, then a
    // shared 12000-tick cooldown per player and base. The map was static in 1.12 too.
    private static final WarnCooldown WARN_COOLDOWN = new WarnCooldown();

    // This head's current pick, transient, so sibling heads on a multi-targeting base can spread
    // their fire (1.12 isTargetAlreadyTargeted walked the heads' target fields the same way).
    @Nullable
    private LivingEntity currentTarget;

    // Concealer addon state, faithful to 1.12 concealmentChecks: after 40 idle ticks the head
    // retracts (the renderer hides it) and it pops back out the moment a target appears.
    private static final int CONCEAL_IDLE_TICKS = 40;
    private boolean concealed = false;
    private int ticksWithoutTarget = 0;

    public TurretBaseBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state) {
        super(beType, pos, state);
        this.type = state.getBlock() instanceof TurretBaseBlock tb ? tb.turretType() : TurretType.MACHINE_GUN;
    }

    public TurretType turretType() {
        return type;
    }

    /**
     * The head's server tick, ported from 1.12 {@code AbstractDirectedTurret.update}. Every step below
     * runs on every tick; only the shot itself is gated, on the {@code ticks} timer. That ordering is
     * the whole point of the method: a head that skips its tick between shots stops tracking, warning
     * and concealing, and a head that re-picks its target from scratch every tick flips between
     * whoever happens to be nearest instead of finishing one off.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, TurretBaseBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        TurretBaseTileEntity base = be.findBase(level, pos);
        if (base == null) {
            // 1.12 setSide() bailed out without touching the head when the base side was not loaded
            // yet, and only destroyed the head once it had latched a base that later vanished. On a
            // chunk load the head can tick before the base's block entity exists, so a bare "no base
            // entity, destroy" test deletes turrets on world load. Look for the base block itself.
            if (be.hasAdjacentBaseBlock(level, pos)) {
                return;
            }
            level.destroyBlock(pos, true);
            return;
        }
        if (base.tier().tier() < be.type.requiredTier()) {
            // A head on a base below its minimum tier pops off and drops, matching 1.12.
            level.destroyBlock(pos, true);
            return;
        }
        // Redstone mode gate, faithful to 1.12: an inactive base freezes its heads before any other
        // work, concealment included.
        if (!base.isActive()) {
            be.currentTarget = null;
            return;
        }
        be.ticks++;
        // 1.12 ran concealmentChecks before the energy test, on the target it was holding, so a head
        // that runs dry stays deployed as long as it still has someone in its sights.
        be.updateConcealment((ServerLevel) level, pos, base.shouldConcealTurrets(), be.currentTarget != null);

        IEnergyStorage energy = base.getEnergyStorageImpl();
        // Scatter-shot fires several rounds per volley for more energy; efficiency upgrades cut the
        // cost. The whole volley is gated on affording that combined cost, faithful to 1.12.
        int scatter = base.scattershot();
        int power = TurretUpgrades.powerPerShot(be.type.energyPerShot(), base.efficiencyUpgrades(), scatter);
        if (energy.getEnergyStored() < power) {
            return;
        }
        // 1.12 warned players whenever a powered head was set to attack them, before targeting ran.
        if (base.isAttacksPlayers() && OMTConfig.Turrets.globalCanTargetPlayers) {
            be.warnPlayers((ServerLevel) level, pos, base);
        }

        // 1.12's targeting cadence: with no target the head only searches every turretTargetSearchTicks
        // ticks; with one it re-validates that same target every tick and keeps it until it dies,
        // leaves range, or breaks line of sight.
        if (be.currentTarget == null && be.targetingTicks < OMTConfig.Turrets.turretTargetSearchTicks) {
            be.targetingTicks++;
        } else {
            be.targetingChecks(level, pos, base);
            if (be.currentTarget != null) {
                be.aimAt((ServerLevel) level, pos, be.currentTarget);
            }
            be.targetingTicks = 0;
        }
        LivingEntity target = be.currentTarget;
        if (target == null) {
            return;
        }
        if (be.ticks < TurretUpgrades.effectiveFireInterval(be.type.fireInterval(), base.fireRateUpgrades())) {
            return;
        }

        float spread = TurretUpgrades.targetedInaccuracy(
                be.type.accuracyDeviation(), base.accuracyUpgrades(), scatter);
        float ampPerHealth = (float) (be.type.damageAmp() * base.ampLevel());

        boolean firedAny = false;
        // The teleporter and the relativistic turret are not scatter-shot capable in 1.12: neither
        // extends ProjectileTurret or RayTracingTurret, so neither has the round loop. They still pay
        // the scattershot energy multiplier, which is upstream's behaviour, not a port slip.
        int rounds = be.type.isSingleEffect() ? 0 : scatter;
        for (int shot = 0; shot <= rounds; shot++) {
            // Consume ammo per round; an energy-only head (NONE) always passes with an empty stack. The
            // volley stops early once the base runs dry.
            ItemStack ammo = base.consumeAmmo(be.type.ammo());
            if (ammo == null) {
                break;
            }
            be.fireOne((ServerLevel) level, pos, target, base, ammo, spread, ampPerHealth);
            firedAny = true;
        }
        if (firedAny) {
            energy.extractEnergy(power, false);
            be.ticks = 0;
            // 1.12's teleporter and relativistic heads end their shot with target = null, so each
            // pulse re-runs the filtered search. Without it the teleporter yanks the same mob onto
            // itself again every cycle and the relativistic turret re-slows what it already slowed.
            if (be.type.isSingleEffect()) {
                be.currentTarget = null;
            }
        }
    }

    /**
     * True if one of the six neighbours is a turret base block, whether or not its block entity has
     * been built yet. Used only to tell "my base is gone" from "my base has not loaded yet".
     */
    private boolean hasAdjacentBaseBlock(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos side = pos.relative(direction);
            if (!level.isLoaded(side)) {
                return true;
            }
            if (level.getBlockState(side).getBlock()
                    instanceof com.ommods.openmodularturrets.content.base.TurretBaseBlock) {
                return true;
            }
        }
        return false;
    }

    /**
     * 1.12 {@code TurretHead.targetingChecks}. The head keeps the target it already has: it only looks
     * for a new one when the current pick is gone or dead, and it then re-tests the pick it is holding
     * against the player exclusions, line of sight and the base's range cap, dropping it if any fails.
     */
    private void targetingChecks(Level level, BlockPos pos, TurretBaseTileEntity base) {
        LivingEntity target = currentTarget;
        if (target == null || target.isRemoved() || !target.isAlive() || target.getHealth() <= 0.0F) {
            target = findTarget(level, pos, base);
        }
        if (target == null) {
            currentTarget = null;
            return;
        }
        if (!TurretDamage.canAcquire(target, base) || !suitsThisHead(target, pos)) {
            currentTarget = null;
            return;
        }
        if (!hasLineOfSight(level, muzzle(pos), target)) {
            currentTarget = null;
            return;
        }
        // 1.12 chebyshevDistance: the range cap is tested as a box, not a sphere, so a target in the
        // corner of the search cube stays valid out to the cap on every axis.
        if (outsideChebyshevRange(pos, base, target)) {
            currentTarget = null;
            return;
        }
        currentTarget = target;
    }

    /**
     * The two type-specific acquisition rules, re-tested on the target the head is holding rather than
     * only when it picks one. 1.12 got this for free because its teleporter and relativistic heads drop
     * their target the moment they fire, so they always re-acquired through the filtered search; a head
     * that keeps its target has to check again or it teleports the same mob onto itself forever and
     * re-slows the one it just slowed.
     */
    private boolean suitsThisHead(LivingEntity target, BlockPos pos) {
        if (type == TurretType.RELATIVISTIC && target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            return false;
        }
        double minRange = type.minTargetRange();
        return minRange <= 0.0 || target.distanceToSqr(muzzle(pos)) >= minRange * minRange;
    }

    /** 1.12 {@code chebyshevDistance}: the largest per-axis gap exceeds the base's range cap. */
    private static boolean outsideChebyshevRange(BlockPos pos, TurretBaseTileEntity base, LivingEntity target) {
        double dx = target.getX() - pos.getX();
        double dy = target.getY() - pos.getY();
        double dz = target.getZ() - pos.getZ();
        return absMax(absMax(dx, dy), dz) > base.getCurrentMaxRange();
    }

    public float renderYaw() {
        return renderYaw;
    }

    public float renderPitch() {
        return renderPitch;
    }

    /** True while the concealer addon has this head tucked away; the renderer then draws nothing. */
    public boolean isConcealed() {
        return concealed;
    }

    // Faithful 1.12 concealmentChecks: 40 target-less ticks retract the head with the classic
    // retract sound; a target (or removing the addon) deploys it again immediately.
    private void updateConcealment(ServerLevel level, BlockPos pos, boolean enabled, boolean hasTarget) {
        if (!enabled || hasTarget) {
            ticksWithoutTarget = 0;
            if (concealed) {
                concealed = false;
                if (enabled) {
                    level.playSound(null, pos, OMTSounds.TURRET_DEPLOY.get(), SoundSource.BLOCKS,
                            OMTSounds.volume(), level.random.nextFloat() + 0.5F);
                }
                level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 2);
            }
            return;
        }
        if (!concealed && ++ticksWithoutTarget >= CONCEAL_IDLE_TICKS) {
            ticksWithoutTarget = 0;
            concealed = true;
            level.playSound(null, pos, OMTSounds.TURRET_RETRACT.get(), SoundSource.BLOCKS,
                    OMTSounds.volume(), level.random.nextFloat() + 0.5F);
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 2);
        }
    }

    // Faithful 1.12 TurretHeadUtil.getAimYaw/getAimPitch: yaw in degrees 0..360 from atan2(dZ, dX)
    // about the block position; pitch measured from straight up over the block-truncated target
    // position, with the same half-block offsets 1.12 used. The renderer converts these to model
    // rotations the same way MathUtil did.
    private void aimAt(ServerLevel level, BlockPos pos, LivingEntity target) {
        double dX = target.getX() - pos.getX();
        double dZ = target.getZ() - pos.getZ();
        float yaw = (float) Math.atan2(dZ, dX);
        if (yaw < 0.0F) {
            yaw += (float) (Math.PI * 2.0);
        }
        float yawDegrees = yaw / (float) Math.PI * 180.0F;

        BlockPos targetBlock = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        double pdX = targetBlock.getX() - 0.5F - (pos.getX() + 0.5F);
        double pdY = targetBlock.getY() + 0.5F - (pos.getY() - 0.5F);
        double pdZ = targetBlock.getZ() - 0.5F - (pos.getZ() + 0.5F);
        float pitch = (float) Math.atan2(Math.sqrt(pdZ * pdZ + pdX * pdX), pdY);
        if (pitch < 0.0F) {
            pitch += (float) (Math.PI * 2.0);
        }
        float pitchDegrees = pitch / (float) Math.PI * 180.0F;

        // Only resync when the aim shifts enough, to avoid a packet every tick.
        if (Math.abs(Mth.degreesDifference(yawDegrees, renderYaw)) > 2.0F
                || Math.abs(Mth.degreesDifference(pitchDegrees, renderPitch)) > 2.0F) {
            renderYaw = yawDegrees;
            renderPitch = pitchDegrees;
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 2);
        }
    }

    /** The base this head sits against, or null if none of the six neighbours is a base. */
    @Nullable
    public TurretBaseTileEntity findBase(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(pos.relative(direction));
            if (neighbour instanceof TurretBaseTileEntity base) {
                return base;
            }
        }
        return null;
    }

    /**
     * The 1.12 setSide search order. It is deliberately not {@link Direction#values()}: setSide
     * probed east, west, south, north, up, then down, and that order decides which base a head
     * mounts against when it touches more than one. The renderer's mount rotation follows it.
     */
    private static final Direction[] MOUNT_SEARCH_ORDER = {
            Direction.EAST, Direction.WEST, Direction.SOUTH,
            Direction.NORTH, Direction.UP, Direction.DOWN};

    /** The direction of the base this head is mounted on, in the 1.12 setSide order. */
    @Nullable
    public Direction baseDirection() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        for (Direction direction : MOUNT_SEARCH_ORDER) {
            if (level.getBlockEntity(getBlockPos().relative(direction)) instanceof TurretBaseTileEntity) {
                return direction;
            }
        }
        return null;
    }

    // Faithful 1.12 TurretHeadUtil.warnPlayers: every survival player in the warning box who is not
    // the owner, trusted, or creative hears the alarm and reads the red warning, at most once per
    // cooldown window per base.
    private void warnPlayers(ServerLevel level, BlockPos pos, TurretBaseTileEntity base) {
        // 1.12 passed the raw turret base range here (no upgrades, no current-max-range cap).
        double reach = type.range() + OMTConfig.Turrets.turretWarningDistance;
        AABB box = new AABB(pos).inflate(reach);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            // 1.12 warnPlayers checked owner, trusted and creative only; a team mate still gets the
            // warning even though the turret would not shoot them.
            TurretTargeting.Protection protection = TurretDamage.protectionOf(player, base);
            if (protection.owner() || protection.trusted() || protection.creative()) {
                continue;
            }
            if (!WARN_COOLDOWN.shouldWarn(player.getUUID(), base.getBlockPos().asLong(), level.getGameTime())) {
                continue;
            }
            // 1.12 dispatchWarnMessage sends it through EntityPlayerMP.playSound, which overrides
            // Entity.playSound to reach only that player, on their own sound category (PLAYERS). Both
            // halves of the warning have their own config switch, as upstream.
            if (OMTConfig.Turrets.turretAlarmSound) {
                player.playNotifySound(OMTSounds.WARNING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (OMTConfig.Turrets.turretWarnMessage) {
                player.sendSystemMessage(Component.translatable("status.warning").withStyle(ChatFormatting.RED));
            }
        }
    }

    /**
     * Targeting radius, faithful to 1.12: range upgrades extend the head's reach, capped by the
     * base's player-adjustable current max range (the GUI's +/- buttons).
     */
    private double searchRange(TurretBaseTileEntity base) {
        return Math.min(type.range() + base.upgradeRangeBonus(), base.getCurrentMaxRange());
    }

    @Nullable
    private LivingEntity findTarget(Level level, BlockPos pos, TurretBaseTileEntity base) {
        Vec3 muzzle = muzzle(pos);
        double range = searchRange(base);
        AABB box = new AABB(pos).inflate(range);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive);
        LivingEntity nearest = null;
        double nearestSqr = range * range;
        // The teleporter ignores anything closer than its minimum range, faithful to 1.12; other heads
        // leave this at zero and target from point blank.
        double minSqr = type.minTargetRange() * type.minTargetRange();
        for (LivingEntity candidate : candidates) {
            // The full 1.12 getTarget filter, not just the category test: it also drops blacklisted
            // entities, tamed horses and pets belonging to the base's owner or a trusted player. A
            // head that acquires one of those locks onto something it may never shoot.
            if (!TurretDamage.canAcquire(candidate, base)) {
                continue;
            }
            double distSqr = candidate.distanceToSqr(muzzle);
            if (distSqr < minSqr) {
                continue;
            }
            // 1.12 TurretHeadUtil.getTargetWithoutSlowEffect: the relativistic turret is the only head
            // that skips a target it has already hit, so it does not re-slow one mob forever.
            if (type == TurretType.RELATIVISTIC && candidate.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
                continue;
            }
            // Multi-targeting on spreads fire: skip whoever a sibling head already picked, faithful
            // to 1.12 isTargetAlreadyTargeted.
            if (base.isMultiTargeting() && targetedBySibling(level, base, candidate)) {
                continue;
            }
            if (distSqr <= nearestSqr && hasLineOfSight(level, muzzle, candidate)) {
                nearestSqr = distSqr;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private boolean targetedBySibling(Level level, TurretBaseTileEntity base, LivingEntity candidate) {
        for (Direction direction : Direction.values()) {
            BlockEntity neighbour = level.getBlockEntity(base.getBlockPos().relative(direction));
            if (neighbour instanceof TurretBaseBlockEntity head && head != this && head.currentTarget == candidate) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ported line for line from 1.12 {@code TurretHeadUtil.canTurretSeeTarget}. It is not a single
     * ray: the trace is walked in up to ten hops, and each hop that lands on the turret's own block,
     * or on a non-occluding block while still more than a block from the target, restarts the trace
     * from the hit point. Anything else blocks the shot.
     *
     * <p>The stepping is what makes it work at all. The ray starts inside the head's own collider, so
     * a plain {@code level.clip} returns that collider as its first hit every single time; treating
     * that one hit as "clear" (which this used to do) disabled the check outright and let every turret
     * shoot straight through walls.
     *
     * <p>1.12 tested {@code Material.isOpaque()} on the struck block. 1.20.1 has no Material, so this
     * uses {@link BlockState#canOcclude()}, the nearest survivor: full solid blocks stop the shot and
     * glass does not, as before. Non-cube colliders (fences, walls) counted as opaque in 1.12 and do
     * not here, which is the one behaviour difference the mapping leaves.
     */
    private boolean hasLineOfSight(Level level, Vec3 from, LivingEntity target) {
        Vec3 end = target.getEyePosition();
        Vec3 delta = end.subtract(from);
        if (delta.lengthSqr() < 1.0e-8) {
            return true;
        }
        Vec3 step = delta.normalize();
        Vec3 start = from;
        for (int i = 0; i < 10; i++) {
            start = start.add(step);
            BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, target));
            if (hit.getType() != HitResult.Type.BLOCK) {
                return true;
            }
            boolean ownBlock = hit.getBlockPos().equals(getBlockPos());
            boolean seeThrough = !level.getBlockState(hit.getBlockPos()).canOcclude()
                    && absMax(absMax(start.x - end.x, start.y - end.y), start.z - end.z) > 1.0;
            if (!ownBlock && !seeThrough) {
                return false;
            }
            start = hit.getLocation();
        }
        return false;
    }

    /** 1.12 {@code MathHelper.absMax}: the larger of two magnitudes. */
    private static double absMax(double a, double b) {
        return Math.max(Math.abs(a), Math.abs(b));
    }

    // Projectile turrets launch a flying entity; the laser and railgun hit instantly along a rendered
    // beam; the teleporter and the relativistic turret apply their own effect and deal no damage.
    // Split faithful to 1.12's ProjectileTurret / RayTracingTurret / bare TurretHead subclassing.
    // Called once per round in a scatter-shot volley, and 1.12 plays the launch sound per round too.
    private void fireOne(ServerLevel level, BlockPos pos, LivingEntity target, TurretBaseTileEntity base,
                         ItemStack ammo, float spread, float ampPerHealth) {
        if (type.isTeleport()) {
            teleport(level, pos, target);
            playEffectTurretSound(level, pos);
        } else if (type == TurretType.RELATIVISTIC) {
            slowTarget(target);
            playEffectTurretSound(level, pos);
        } else if (type.isProjectile()) {
            // 1.12 ProjectileTurret.shootProjectile plays the amp chirp per round on an amped base.
            if (base.ampLevel() != 0) {
                level.playSound(null, pos, OMTSounds.AMPED.get(), SoundSource.BLOCKS,
                        OMTSounds.volume(), level.random.nextFloat() + 0.5F);
            }
            launchProjectile(level, pos, target, base, ammo, spread, ampPerHealth);
            playLaunchSound(level, pos);
        } else {
            hitscan(level, pos, target, base, ampPerHealth);
            playLaunchSound(level, pos);
        }
    }

    // 1.12 plays every launch on the BLOCKS category at the config's turretSoundVolume, pitched
    // randomly over 0.5 to 1.5 (new Random().nextFloat() + 0.5f).
    private void playLaunchSound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, fireSound(), SoundSource.BLOCKS, OMTSounds.volume(),
                level.random.nextFloat() + 0.5F);
    }

    // The teleporter and relativistic tile entities do not use the config volume: both end their tick
    // with playSound(..., SoundCategory.BLOCKS, 0.6f, 1.0f), a fixed quiet, unpitched cue.
    private void playEffectTurretSound(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, fireSound(), SoundSource.BLOCKS, 0.6F, 1.0F);
    }

    // Faithful to 1.12 TeleporterTurretTileEntity: the shot teleports the target onto the top of the
    // turret (x+0.5, y+1, z+0.5) and deals no damage.
    //
    // The portal burst is 1.12 BlockTeleporterTurret.randomDisplayTick, which the tile entity triggers
    // by setting shouldAnimate on the block: 26 portal particles scattered a gaussian block around the
    // head's centre with a +/-0.1 drift. Sent straight from the head here, because upstream's flag
    // lives on the block singleton, so it fires the burst at whichever teleporter head the client
    // happens to render next rather than the one that shot.
    private void teleport(ServerLevel level, BlockPos pos, LivingEntity target) {
        target.teleportTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                26, 1.0, 1.0, 1.0, 0.1);
    }

    // Faithful to 1.12 RelativisticTurretTileEntity: it deals no damage and fires nothing. It hangs
    // Slowness IV and Weakness IV on the target for 200 ticks (amplifier 3, not ambient, particles
    // shown), and its target search skips anything already slowed, so it never re-applies.
    private void slowTarget(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 3, false, true));
    }

    // Faithful to 1.12 ProjectileTurret.doTargetedShot: aim the shot with lead, spawn it at the head
    // centre, and hand the direction and speed to the projectile. Damage permissions are re-checked by
    // the projectile against this base when it strikes. The consumed ammo is handed on so a shot that
    // throws its round (the disposable turret) lands it as a drop; the damage-amp addon rides along.
    private void launchProjectile(ServerLevel level, BlockPos pos, LivingEntity target, TurretBaseTileEntity base,
                                  ItemStack ammo, float spread, float ampPerHealth) {
        ProjectileKind kind = type.projectile();
        OMTProjectile projectile = OMTEntities.create(kind, level);
        if (projectile == null) {
            return;
        }
        Vec3 targetVel = leadVelocity(target);
        ProjectileAim.Solution aim = ProjectileAim.solve(
                pos.getX(), pos.getY(), pos.getZ(),
                target.getX(), target.getY(), target.getZ(), target.getBbHeight(),
                targetVel.x, targetVel.y, targetVel.z, kind.leadGravity());
        projectile.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        projectile.fireFrom(base.getBlockPos(), type.damage());
        projectile.setThrownAmmo(ammo);
        projectile.setAmp(ampPerHealth);
        projectile.setKillSettings(base.fakeDropsLevel(), base.dropsLoot());
        // 1.12 handed the target to the rocket's constructor so its homing mode had something to
        // chase; every other round ignores it.
        if (projectile instanceof com.ommods.openmodularturrets.content.projectile.RocketProjectileEntity rocket) {
            rocket.setHomingTarget(target);
        }
        projectile.shoot(aim.dirX(), aim.dirY(), aim.dirZ(), aim.velocity(), spread);
        level.addFreshEntity(projectile);
    }

    /**
     * How far the target moved over the last tick, which is what a projectile shot leads by.
     *
     * <p>1.12 read this two ways: {@code posX - prevPosX} for a mob, and for a player a delta the
     * turret tracked itself across ticks, because a player's motion fields are meaningless on the
     * server. Both come to the same thing, and 1.20.1 stamps {@code xOld} on every entity at the top
     * of its tick, so one formula covers both. Reading {@code getDeltaMovement()} instead (which this
     * used to do) returns near enough zero for a walking player, so a turret never led one.
     */
    private static Vec3 leadVelocity(LivingEntity target) {
        return new Vec3(target.getX() - target.xOld, target.getY() - target.yOld, target.getZ() - target.zOld);
    }

    /**
     * The laser and railgun shot, ported from 1.12 {@code RayTracingTurret}. A hitscan turret spawns
     * no particles at all: what the player sees is a beam, a coloured line rendered from the barrel to
     * whatever the ray struck, held for a few ticks (omlib's {@code Ray} / {@code MessageRenderRay}).
     *
     * <p>Damage is not the flat setting either. Each turret scales its base damage by an armour-aware
     * modifier and then routes the result through a normal / armour-bypassing split:
     * the laser does {@code baseDamage * ((30 - armour)/20 + 0.1)} as ordinary damage, so plate armour
     * blunts it twice over; the railgun does {@code baseDamage * (armour/20 + 0.6)} and bypasses armour
     * entirely, so it is the anti-armour turret. The damage-amp addon rides on top of the modifier.
     */
    private void hitscan(ServerLevel level, BlockPos pos, LivingEntity target, TurretBaseTileEntity base,
                         float ampPerHealth) {
        // 1.12 doTargetedShot aims the ray at the target's eyes in world coordinates, with no lead.
        Vec3 aim = new Vec3(target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
        Vec3 origin = new Vec3(pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5);

        // A beam turret is not a guaranteed hit. 1.12 scatters the aim point by a gaussian that grows
        // with distance (half the range, plus a 0.3 floor) and by half again for a target under half a
        // block tall, then traces to wherever that lands. Without this every laser and railgun shot in
        // the port struck dead centre no matter the range, which is both stronger than the original
        // and why the beam always ended on the target rather than sometimes on the scenery.
        double accuracy = TurretUpgrades.actualAccuracy(
                type.accuracyDeviation(), base.accuracyUpgrades(), base.scattershot());
        double reach = type.range() + base.upgradeRangeBonus();
        double deviationModifier = (target.getBbHeight() < 0.5 ? 1.5 : 1.0)
                * (aim.distanceTo(origin) * 0.5 / reach + 0.3);
        double dev = 0.035 * accuracy * deviationModifier;
        Vec3 end = aim.add(level.random.nextGaussian() * dev,
                level.random.nextGaussian() * dev,
                level.random.nextGaussian() * dev);
        // The trace starts 0.75 blocks out along the shot so it clears the head's own model.
        Vec3 start = origin.add(end.subtract(origin).normalize().scale(0.75));

        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, target));
        boolean hitsBlock = blockHit.getType() == HitResult.Type.BLOCK;
        double blockRange = hitsBlock ? blockHit.getLocation().distanceTo(start) : 500.0;

        // Whoever the beam crosses first, in order, up to the block that stops it. That is how a
        // bystander who walks into the line takes the shot meant for someone behind them.
        for (Entity entity : entitiesAlong(level, start, end, blockRange)) {
            if (onBeamHitEntity(level, entity, base, ampPerHealth)) {
                renderRay(level, start, end);
                return;
            }
        }
        if (hitsBlock) {
            renderRay(level, start, blockHit.getLocation());
            handleBlockHit(level, blockHit.getBlockPos());
            // DROPPED: 1.12 then falls through and draws a second beam that carries on past the block
            // it just stopped at, because the overshoot render sits outside the if/else instead of
            // inside the else. That is a copy/paste slip upstream, and porting it draws a beam
            // straight through the wall the shot was blocked by.
            return;
        }
        // Nothing hit: the beam runs on past the aim point so it reads as a miss going off into the
        // distance rather than stopping in mid air.
        renderRay(level, start, end.add(end.subtract(start).scale(2.0)));
    }

    /** Entities the segment crosses, nearest first, no further out than the block that stops it. */
    private List<Entity> entitiesAlong(ServerLevel level, Vec3 start, Vec3 end, double maxDistance) {
        record Crossing(double distance, Entity entity) {
        }
        List<Crossing> crossings = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, new AABB(start, end).inflate(1.0),
                Entity::isPickable)) {
            Optional<Vec3> where = entity.getBoundingBox().inflate(entity.getPickRadius()).clip(start, end);
            if (where.isEmpty()) {
                continue;
            }
            double distance = start.distanceTo(where.get());
            if (distance > maxDistance) {
                continue;
            }
            crossings.add(new Crossing(distance, entity));
        }
        crossings.sort(Comparator.comparingDouble(Crossing::distance));
        return crossings.stream().map(Crossing::entity).toList();
    }

    /**
     * 1.12 {@code RayTracingTurret.onHitEntity}: damage whoever the beam struck, clear their
     * invulnerability so the next shot lands on the turret's cadence, and play the hit sound at them.
     * Returns false for anyone this base may not hurt, which lets the beam carry on to the next entity.
     */
    private boolean onBeamHitEntity(ServerLevel level, Entity entity, TurretBaseTileEntity base,
                                    float ampPerHealth) {
        if (!(entity instanceof LivingEntity living) || !TurretDamage.canDamage(living, base)) {
            return false;
        }
        // Kill fidelity rides the damage source: fake drops credit the fake player, and a hit from a
        // loot-deleted base marks the mob so its drops are cancelled (OMTEvents).
        int fakeDrops = base.fakeDropsLevel();
        if (!(living instanceof net.minecraft.world.entity.player.Player)) {
            living.addTag(OMTEvents.TAG_TURRET_HIT);
            if (fakeDrops > -1) {
                living.addTag(OMTEvents.TAG_FAKE_DROPS + fakeDrops);
            }
            if (!base.dropsLoot()) {
                living.addTag(OMTEvents.TAG_DONT_DROP_LOOT);
            }
        }
        // 1.12 damageEntity always makes both calls. The zero-factor one lands first for the railgun,
        // which opens the invulnerability window at zero damage so the armour-bypassing half that
        // follows still goes through in full; for the laser it is the second call and is refused.
        float damage = type.damage() * hitscanDamageModifier(living);
        if (ampPerHealth > 0.0F) {
            damage += (int) living.getHealth() * ampPerHealth;
        }
        BlockPos basePos = base.getBlockPos();
        float normalFactor = type == TurretType.RAILGUN ? 0.0F : 1.0F;
        float bypassFactor = type == TurretType.RAILGUN ? 1.0F : 0.0F;
        living.hurt(OMTDamage.normal(level, null, fakeDrops, basePos), damage * normalFactor);
        living.hurt(OMTDamage.bypass(level, null, fakeDrops, basePos), damage * bypassFactor);
        living.invulnerableTime = -1;
        // The hit sound, on the AMBIENT category at the target's position, pitched 0.5 to 1.5.
        level.playSound(null, living.blockPosition(), hitSound(), SoundSource.AMBIENT,
                OMTSounds.volume(), level.random.nextFloat() + 0.5F);
        return true;
    }

    /**
     * 1.12 {@code handleBlockHit}: the laser's is empty, and the railgun's breaks what it struck when
     * {@code canRailgunDestroyBlocks} is on and the block is softer than hardness 200. Both skip a
     * turret head and any non-occluding block, as every other 1.12 block-hit handler does.
     */
    private void handleBlockHit(ServerLevel level, BlockPos hitPos) {
        if (type != TurretType.RAILGUN || !OMTConfig.Turrets.canRailgunDestroyBlocks) {
            return;
        }
        BlockState state = level.getBlockState(hitPos);
        if (state.getBlock() instanceof TurretBaseBlock || !state.canOcclude()) {
            return;
        }
        if (state.getDestroySpeed(level, hitPos) < 200.0F) {
            level.destroyBlock(hitPos, false);
        }
    }

    /**
     * 1.12 starts the beam at (x+0.5, y+0.6, z+0.5) of the head and sends it to everyone within 120
     * blocks. The laser is {@code ColorOM(1.0f, 0.1f, 0.0f, 0.38f)} for 5 ticks and the railgun is
     * {@code ColorOM(1.0f, 0.5f, 0.0f, 0.2f)} for 3.
     */
    private void renderRay(ServerLevel level, Vec3 from, Vec3 to) {
        if (type == TurretType.RAILGUN) {
            OMLibChannel.sendToNear(new PacketRenderRay(from, to, 1.0F, 0.5F, 0.0F, 0.2F, 3),
                    level, from, 120.0);
        } else {
            OMLibChannel.sendToNear(new PacketRenderRay(from, to, 1.0F, 0.1F, 0.0F, 0.38F, 5),
                    level, from, 120.0);
        }
    }

    // 1.12 getDamageModifier: the laser is blunted by armour and the railgun is sharpened by it.
    private float hitscanDamageModifier(LivingEntity target) {
        int armor = target.getArmorValue();
        return type == TurretType.RAILGUN
                ? armor / 20.0F + 0.6F
                : (30 - armor) / 20.0F + 0.1F;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putFloat("yaw", renderYaw);
        tag.putFloat("pitch", renderPitch);
        tag.putBoolean("concealed", concealed);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("yaw")) {
            renderYaw = tag.getFloat("yaw");
        }
        if (tag.contains("pitch")) {
            renderPitch = tag.getFloat("pitch");
        }
        if (tag.contains("concealed")) {
            concealed = tag.getBoolean("concealed");
        }
    }

    // The 1.12 getHitSound for the two beam turrets. Only they have one.
    private SoundEvent hitSound() {
        return type == TurretType.RAILGUN ? OMTSounds.RAIL_GUN_HIT.get() : OMTSounds.LASER_HIT.get();
    }

    // The classic 1.12 launch sound for each head, from the ported ogg set.
    private SoundEvent fireSound() {
        return switch (type) {
            case DISPOSABLE -> OMTSounds.DISPOSABLE.get();
            case POTATO_CANNON -> OMTSounds.POTATO.get();
            case MACHINE_GUN -> OMTSounds.MACHINE_GUN.get();
            case INCENDIARY -> OMTSounds.INCENDIARY.get();
            case GRENADE -> OMTSounds.GRENADE.get();
            case RELATIVISTIC -> OMTSounds.RELATIVISTIC.get();
            case ROCKET -> OMTSounds.ROCKET.get();
            case TELEPORTER -> OMTSounds.TELEPORT.get();
            case LASER -> OMTSounds.LASER.get();
            case RAILGUN -> OMTSounds.RAIL_GUN.get();
        };
    }

    private static Vec3 muzzle(BlockPos pos) {
        // 1.12 traces line of sight from the head block's exact centre (pos + 0.5 on every axis).
        return Vec3.atCenterOf(pos);
    }
}
