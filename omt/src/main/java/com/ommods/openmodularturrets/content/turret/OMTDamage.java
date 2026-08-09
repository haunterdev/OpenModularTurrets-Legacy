package com.ommods.openmodularturrets.content.turret;

import com.mojang.authlib.GameProfile;
import com.ommods.openmodularturrets.OpenModularTurrets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The turret damage sources, faithful to 1.12 {@code NormalDamageSource}/{@code ArmorBypassDamageSource}:
 * both are projectile-flagged, the bypass variant ignores armor (via the damage type tags in
 * data/), and when the base carries a fake drops addon the attacker is the OMT fake player so kills
 * count as player kills and drop player-only loot. The fake player keeps the 1.12 profile UUID.
 */
public final class OMTDamage {

    public static final ResourceKey<DamageType> TURRET_NORMAL =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(OpenModularTurrets.MOD_ID, "turret_normal"));
    public static final ResourceKey<DamageType> TURRET_BYPASS =
            ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(OpenModularTurrets.MOD_ID, "turret_bypass"));

    // The 1.12 OMTFakePlayer profile UUID, name adjusted (a colon is not a valid profile name).
    private static final GameProfile FAKE_PROFILE = new GameProfile(
            UUID.fromString("c5c97afa-fc98-44ab-944a-e67681a66b19"), "omt_turret");

    private OMTDamage() {
    }

    public static FakePlayer fakePlayer(ServerLevel level) {
        return FakePlayerFactory.get(level, FAKE_PROFILE);
    }

    /**
     * A normal turret shot. {@code fakeDrops} is the fake drops addon level: -1 for none (the turret
     * itself is the attacker), 0-3 to credit the kill to the fake player with that looting level
     * (looting is applied by the LootingLevelEvent handler in OMTEvents). {@code basePos} is the
     * firing base, so a kill can bump its counters; null when unknown.
     */
    public static DamageSource normal(ServerLevel level, @Nullable Entity directEntity, int fakeDrops,
                                      @Nullable BlockPos basePos) {
        return source(level, TURRET_NORMAL, directEntity, fakeDrops, basePos);
    }

    /** The armor-ignoring share of a shot (the grenade's 10 percent slice). */
    public static DamageSource bypass(ServerLevel level, @Nullable Entity directEntity, int fakeDrops,
                                      @Nullable BlockPos basePos) {
        return source(level, TURRET_BYPASS, directEntity, fakeDrops, basePos);
    }

    private static DamageSource source(ServerLevel level, ResourceKey<DamageType> type,
                                       @Nullable Entity directEntity, int fakeDrops, @Nullable BlockPos basePos) {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type);
        Entity attacker = null;
        if (fakeDrops > -1) {
            // The fake player holds a looting sword matching the addon level, the same trick 1.12's
            // OMTFakePlayer.getSword used: vanilla reads the killer's held item for looting at death.
            FakePlayer fp = fakePlayer(level);
            fp.setItemInHand(InteractionHand.MAIN_HAND, swordFor(fakeDrops));
            attacker = fp;
        }
        return new OMTDamageSource(holder, directEntity, attacker, basePos);
    }

    private static ItemStack swordFor(int looting) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        if (looting > 0) {
            sword.enchant(Enchantments.MOB_LOOTING, Math.min(looting, 3));
        }
        return sword;
    }
}
