package com.ommods.openmodularturrets.content.turret;

import com.ommods.omlib.util.OMPlayer;
import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.turret.TurretTargeting.Category;
import com.ommods.openmodularturrets.content.turret.TurretTargeting.Protection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * The game-seam glue that runs the pure {@link TurretTargeting} policy against a live entity and a
 * base: classify the entity, read the base's attack toggles, and resolve which protections apply.
 *
 * <p>Three entry points, matching the three things 1.12 asked at three different moments:
 * {@link #canAcquire} is the full target-selection filter from {@code TurretHeadUtil.getTarget},
 * {@link #canDamage} is the on-hit check a fired round makes from {@code OMTUtil.canDamageEntity},
 * and {@link #protectionOf} is the player exclusion set both are built on.
 */
public final class TurretDamage {

    /**
     * Target-selection permission, the full 1.12 {@code getTarget} filter: the blacklist, the pet and
     * tamed-horse exclusions, the entity's category, and the player protections.
     *
     * <p>This is what a head must call when picking a target, not {@link #canTarget}. Selecting on the
     * category alone let a head lock onto a pet wolf it was then forbidden to shoot, so it sat there
     * aiming at the dog forever while the creepers walked past.
     */
    public static boolean canAcquire(LivingEntity entity, TurretBaseTileEntity base) {
        if (isBlacklisted(entity)) {
            return false;
        }
        // 1.12 skipped a tamed horse outright, whoever tamed it, and skipped any owned entity whose
        // owner is the base's owner or a trusted player.
        if (entity instanceof AbstractHorse horse && horse.isTamed()) {
            return false;
        }
        if (entity instanceof OwnableEntity owned && ownerIsProtected(owned, base)) {
            return false;
        }
        return canTarget(entity, base);
    }

    /**
     * On-hit permission, from 1.12 {@code OMTUtil.canDamageEntity}: a tamed pet inherits its owner's
     * protection, and anything else is judged on its own category and protections.
     */
    public static boolean canDamage(LivingEntity entity, TurretBaseTileEntity base) {
        if (entity instanceof TamableAnimal pet && pet.getOwner() instanceof Player owner) {
            return canTarget(owner, base);
        }
        return canTarget(entity, base);
    }

    /** The entity's category is enabled on this base (and server-wide) and no protection spares it. */
    public static boolean canTarget(LivingEntity entity, TurretBaseTileEntity base) {
        return TurretTargeting.canTarget(classify(entity), base.attackFlags(), protectionOf(entity, base));
    }

    /** True if the entity's registry id is on the config blacklist (the armor stand by default). */
    public static boolean isBlacklisted(LivingEntity entity) {
        if (OMTConfig.General.mobBlacklist.isEmpty()) {
            return false;
        }
        return OMTConfig.General.mobBlacklist.contains(
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }

    /**
     * The 1.12 list membership: hostile was isCreatureType(MONSTER) which means the IMob marker
     * ({@link Enemy} today, so slimes, ghasts and phantoms count); neutral was EntityAnimal or
     * EntityAmbientCreature; players are players; everything else (villager, squid, armor stand)
     * matched no list and was never targeted.
     */
    public static Category classify(LivingEntity entity) {
        if (entity instanceof Player) {
            return Category.PLAYER;
        }
        if (entity instanceof Enemy) {
            return Category.HOSTILE;
        }
        if (entity instanceof Animal || entity instanceof AmbientCreature) {
            return Category.NEUTRAL;
        }
        return Category.UNLISTED;
    }

    /**
     * The 1.12 exclusions, from {@code OMTUtil.canDamagePlayer} plus the creative check its callers
     * apply: the base's owner, a trusted player (unless {@code turretDamageTrustedPlayers} is on),
     * a player sharing the owner's scoreboard team, and anyone in creative or spectator.
     *
     * <p>Operator status is deliberately not one of them. An earlier version of this port spared
     * anyone with permission level 2, which no 1.12 code path does, and which in single player spares
     * the host permanently: the world owner is permission level 4, so no turret could ever shoot the
     * only player in the world.
     */
    public static Protection protectionOf(LivingEntity entity, TurretBaseTileEntity base) {
        if (!(entity instanceof Player player)) {
            return Protection.NONE;
        }
        OMPlayer owner = base.getOwner();
        boolean isOwner = owner != null && owner.getUuid() != null && player.getUUID().equals(owner.getUuid());
        if (OMTConfig.Turrets.turretsAttackOwner) {
            // Testing switch, off by default: lets a single player stand in front of their own turret.
            isOwner = false;
        }
        boolean isTrusted = !OMTConfig.Turrets.turretDamageTrustedPlayers
                && base.getTrustManager().getTrustedPlayer(player.getUUID()) != null;
        boolean isCreative = player.isCreative() || player.isSpectator();
        // 1.12 spared anyone whose scoreboard team matches the owner's, so a team's turrets never
        // shoot their own side. An empty team name never matches, as upstream.
        String team = new OMPlayer(player).getTeam();
        boolean sameTeam = owner != null && team != null && !team.isEmpty() && team.equals(owner.getTeam());
        return new Protection(isOwner, isTrusted, isCreative, sameTeam);
    }

    /** True if this owned entity's owner is the base's owner or one of its trusted players. */
    private static boolean ownerIsProtected(OwnableEntity owned, TurretBaseTileEntity base) {
        UUID ownerId = owned.getOwnerUUID();
        if (ownerId == null) {
            return false;
        }
        OMPlayer baseOwner = base.getOwner();
        if (baseOwner != null && ownerId.equals(baseOwner.getUuid())) {
            return true;
        }
        // 1.12 resolved the pet's owner to a live EntityPlayer first, so a pet whose owner had logged
        // out lost its protection. This port matches by UUID instead, which keeps the pet safe while
        // its owner is offline: an owner is not present to notice their wolf being shot.
        return base.getTrustManager().getTrustedPlayer(ownerId) != null;
    }

    private TurretDamage() {
    }
}
