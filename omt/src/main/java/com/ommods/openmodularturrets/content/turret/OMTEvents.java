package com.ommods.openmodularturrets.content.turret;

import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge (game bus) event handlers, faithful to 1.12 OMTEventHandler.lootEvent: a mob tagged by a
 * turret whose base touches a Loot Deleter attachment drops nothing when it dies. The tag rides the
 * entity so the cancellation holds even when the killing blow lands after the shot that marked it.
 */
@Mod.EventBusSubscriber(modid = OpenModularTurrets.MOD_ID)
public final class OMTEvents {

    /** Command tag set on mobs a loot-deleted turret has hit. */
    public static final String TAG_DONT_DROP_LOOT = "openmodularturrets:dont_drop_loot";
    /** Command tag marking any mob a turret has hit, matching 1.12 for pack compatibility. */
    public static final String TAG_TURRET_HIT = "openmodularturrets:turret_hit";
    /**
     * Prefix of the fake drops tag 1.12 stamped on a mob shot by a base carrying that addon, the
     * looting level appended ({@code openmodularturrets:fake_drops_2}). The looting itself rides the
     * damage source in this port, but the tag still marks the kill for the loot override below and for
     * any pack that reacted to it.
     */
    public static final String TAG_FAKE_DROPS = "openmodularturrets:fake_drops_";

    private OMTEvents() {
    }

    /**
     * 1.12 {@code OMTEventHandler.lootEvent}, in full. Two separate reasons cancel a mob's drops: the
     * base that shot it touches a Loot Deleter, or the server turned turret-kill loot off entirely
     * (in which case a fake drops addon still restores it, unless that override is off too).
     *
     * <p>The port only carried the first of those, so both {@code doTurretsKillsDropMobLoot} and
     * {@code doLootAddonsOverrideMobLootSetting} did nothing at all.
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        var tags = event.getEntity().getTags();
        if (tags.contains(TAG_DONT_DROP_LOOT)) {
            event.setCanceled(true);
            return;
        }
        if (!tags.contains(TAG_TURRET_HIT) || OMTConfig.General.doTurretsKillsDropMobLoot) {
            return;
        }
        boolean hasLootAddon = tags.stream().anyMatch(tag -> tag.startsWith(TAG_FAKE_DROPS));
        if (!hasLootAddon || !OMTConfig.General.doLootAddonsOverrideMobLootSetting) {
            event.setCanceled(true);
        }
    }

    // Faithful 1.12 entityDeathEvent: a death by a turret damage source bumps the firing base's
    // kill counter, and the player-kill counter when the victim was a player.
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource() instanceof OMTDamageSource source) || source.basePos() == null) {
            return;
        }
        if (event.getEntity().level().getBlockEntity(source.basePos()) instanceof TurretBaseTileEntity base) {
            base.increaseKillCounter();
            if (event.getEntity() instanceof Player) {
                base.increasePlayerKillCounter();
            }
        }
    }
}
