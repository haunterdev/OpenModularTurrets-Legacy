package com.ommods.openmodularturrets.config;

import com.ommods.openmodularturrets.OpenModularTurrets;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OMT's server config, covering every 1.12 {@code OMTConfig} switch that changes turret behaviour.
 * Values are baked into plain static fields on load and reload, the same shape omlib's
 * {@code OMConfig} uses, so callers read a primitive and a missed bake degrades to the default rather
 * than throwing.
 *
 * <p>What is deliberately absent from the 1.12 set: the per-turret {@code TurretSetting} and per-tier
 * {@code BaseSetting} blocks (this port holds those numbers in {@code TurretType} and
 * {@code BaseTier}), {@code recipes} (the port ships one vanilla recipe set), {@code allowBaseCamo},
 * {@code baseBreakable} and {@code turretBreakable} (block properties are fixed at registration in
 * 1.20.1, so a config could not move them without a restart-only reload path), the expander capacity
 * and addon generation numbers, and {@code useWhitelistForAmmo}/{@code stringAmmoAllowList} (the port
 * enforces the whitelist through the ammo slot's own item filter instead).
 */
@Mod.EventBusSubscriber(modid = OpenModularTurrets.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OMTConfig {

    public static final ForgeConfigSpec serverConfigSpec;

    private static final Server SERVER;

    static {
        Pair<Server, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = pair.getKey();
        serverConfigSpec = pair.getValue();
    }

    public static final class Turrets {
        /**
         * Off by default, which is the 1.12 rule: a turret never shoots the player who placed its
         * base, so there is no way to stand in front of your own turret and see it fire at you. Turn
         * this on to test the "attack players" toggle in single player without a second account.
         *
         * <p>This one has no 1.12 counterpart. Everything else in this class does.
         */
        public static boolean turretsAttackOwner = false;

        /** 1.12 {@code canRocketsHome}: a rocket steers toward its target for its whole flight. */
        public static boolean canRocketsHome = false;

        /** 1.12 {@code canRocketsHurtEnderDragon}: rockets bypass the dragon's damage immunity. */
        public static boolean canRocketsHurtEnderDragon = false;

        /** 1.12 {@code canRocketsDestroyBlocks}: blast strength 2.3 when on, 0.1 when off. */
        public static boolean canRocketsDestroyBlocks = false;

        /** 1.12 {@code canGrenadesDestroyBlocks}: blast strength 1.4 when on, 0.1 when off. */
        public static boolean canGrenadesDestroyBlocks = false;

        /**
         * 1.12 {@code canRailgunDestroyBlocks}: a slug that strikes a block with a hardness under 200
         * breaks it (without dropping it).
         */
        public static boolean canRailgunDestroyBlocks = false;

        /** 1.12 {@code canTurretsConcealWithoutAddon}: every base conceals, addon or not. */
        public static boolean canTurretsConcealWithoutAddon = false;

        /** 1.12 {@code globalCanTargetPlayers}: server-wide veto over the base's attack toggle. */
        public static boolean globalCanTargetPlayers = true;

        /** 1.12 {@code globalCanTargetNeutrals}: server-wide veto over the base's attack toggle. */
        public static boolean globalCanTargetNeutrals = true;

        /** 1.12 {@code globalCanTargetMobs}: server-wide veto over the base's attack toggle. */
        public static boolean globalCanTargetMobs = true;

        /** 1.12 {@code turretAlarmSound}: play the alarm with the proximity warning. */
        public static boolean turretAlarmSound = true;

        /** 1.12 {@code turretWarnMessage}: send the red chat line with the proximity warning. */
        public static boolean turretWarnMessage = true;

        /** 1.12 {@code turretWarningDistance}: blocks past the turret's range that still warn. */
        public static int turretWarningDistance = 5;

        /**
         * 1.12 {@code turretDamageTrustedPlayers}: when on, a trusted player loses their protection
         * and can be shot like anyone else.
         */
        public static boolean turretDamageTrustedPlayers = false;

        /** 1.12 {@code turretSoundVolume}: the volume every turret sound plays at. */
        public static double turretSoundVolume = 4.0;

        /**
         * 1.12 {@code turretTargetSearchTicks}: how many ticks a head waits between target searches
         * while it has nobody to shoot. It still tracks and fires on a target it already holds every
         * tick; this only paces the search.
         */
        public static int turretTargetSearchTicks = 10;

        private Turrets() {
        }
    }

    public static final class General {
        /** 1.12 {@code doTurretsKillsDropMobLoot}: turret kills drop the mob's usual loot. */
        public static boolean doTurretsKillsDropMobLoot = true;

        /**
         * 1.12 {@code doLootAddonsOverrideMobLootSetting}: with loot off above, a fake drops addon
         * still restores drops for the base carrying it.
         */
        public static boolean doLootAddonsOverrideMobLootSetting = true;

        /**
         * 1.12 {@code stringMobBlackList}: entity ids no turret ever targets. Upstream defaulted to
         * the armor stand, written as its 1.12 name; this port uses the 1.20.1 registry id.
         */
        public static Set<String> mobBlacklist = Set.of("minecraft:armor_stand");

        private General() {
        }
    }

    private static final class Server {
        private final ForgeConfigSpec.BooleanValue turretsAttackOwner;
        private final ForgeConfigSpec.BooleanValue canRocketsHome;
        private final ForgeConfigSpec.BooleanValue canRocketsHurtEnderDragon;
        private final ForgeConfigSpec.BooleanValue canRocketsDestroyBlocks;
        private final ForgeConfigSpec.BooleanValue canGrenadesDestroyBlocks;
        private final ForgeConfigSpec.BooleanValue canRailgunDestroyBlocks;
        private final ForgeConfigSpec.BooleanValue canTurretsConcealWithoutAddon;
        private final ForgeConfigSpec.BooleanValue globalCanTargetPlayers;
        private final ForgeConfigSpec.BooleanValue globalCanTargetNeutrals;
        private final ForgeConfigSpec.BooleanValue globalCanTargetMobs;
        private final ForgeConfigSpec.BooleanValue turretAlarmSound;
        private final ForgeConfigSpec.BooleanValue turretWarnMessage;
        private final ForgeConfigSpec.IntValue turretWarningDistance;
        private final ForgeConfigSpec.BooleanValue turretDamageTrustedPlayers;
        private final ForgeConfigSpec.DoubleValue turretSoundVolume;
        private final ForgeConfigSpec.IntValue turretTargetSearchTicks;

        private final ForgeConfigSpec.BooleanValue doTurretsKillsDropMobLoot;
        private final ForgeConfigSpec.BooleanValue doLootAddonsOverrideMobLootSetting;
        private final ForgeConfigSpec.ConfigValue<List<? extends String>> mobBlacklist;

        private Server(ForgeConfigSpec.Builder builder) {
            builder.push("TURRETS");
            turretsAttackOwner = builder
                    .comment("May a turret shoot the player who owns its base?",
                            "Off matches the original mod, where the owner is always spared.",
                            "On is a testing switch: it lets you check the 'attack players' toggle",
                            "on your own turret in single player.")
                    .define("turretsAttackOwner", false);
            canRocketsHome = builder
                    .comment("Do rocket turret rounds steer toward their target in flight?")
                    .define("canRocketsHome", false);
            canRocketsHurtEnderDragon = builder
                    .comment("May rocket turret rounds hurt the ender dragon?")
                    .define("canRocketsHurtEnderDragon", false);
            canRocketsDestroyBlocks = builder
                    .comment("May rocket turret rounds blow up terrain?")
                    .define("canRocketsDestroyBlocks", false);
            canGrenadesDestroyBlocks = builder
                    .comment("May grenade turret rounds blow up terrain?")
                    .define("canGrenadesDestroyBlocks", false);
            canRailgunDestroyBlocks = builder
                    .comment("May railgun turret slugs break the blocks they strike?")
                    .define("canRailgunDestroyBlocks", false);
            canTurretsConcealWithoutAddon = builder
                    .comment("Do turrets retract while idle even without a concealment addon?")
                    .define("canTurretsConcealWithoutAddon", false);
            globalCanTargetPlayers = builder
                    .comment("If turrets can target players")
                    .define("globalCanTargetPlayers", true);
            globalCanTargetNeutrals = builder
                    .comment("If turrets can target neutrals (cow, sheep etc.)")
                    .define("globalCanTargetNeutrals", true);
            globalCanTargetMobs = builder
                    .comment("If turrets can target hostile mobs")
                    .define("globalCanTargetMobs", true);
            turretAlarmSound = builder
                    .comment("Play the alarm when a player walks into a turret's warning range?")
                    .define("turretAlarmSound", true);
            turretWarnMessage = builder
                    .comment("Send the chat warning when a player walks into a turret's range?")
                    .define("turretWarnMessage", true);
            turretWarningDistance = builder
                    .comment("How many blocks past its range a turret still warns players.")
                    .defineInRange("turretWarningDistance", 5, 0, 128);
            turretDamageTrustedPlayers = builder
                    .comment("May turrets shoot players trusted on their base?")
                    .define("turretDamageTrustedPlayers", false);
            turretSoundVolume = builder
                    .comment("Volume of every turret sound.")
                    .defineInRange("turretSoundVolume", 4.0, 0.0, 16.0);
            turretTargetSearchTicks = builder
                    .comment("Ticks a turret waits between target searches while it has no target.")
                    .defineInRange("turretTargetSearchTicks", 10, 1, 200);
            builder.pop();

            builder.push("GENERAL");
            doTurretsKillsDropMobLoot = builder
                    .comment("If turret kills drop loot")
                    .define("doTurretsKillsDropMobLoot", true);
            doLootAddonsOverrideMobLootSetting = builder
                    .comment("If loot is disabled, do loot addons enable loot anyway?")
                    .define("doLootAddonsOverrideMobLootSetting", true);
            mobBlacklist = builder
                    .comment("Which entities should not be targeted by turrets?")
                    .defineList("mobBlacklist", List.of("minecraft:armor_stand"),
                            entry -> entry instanceof String);
            builder.pop();
        }
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        bake(event.getConfig().getSpec());
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        bake(event.getConfig().getSpec());
    }

    private static void bake(Object spec) {
        if (spec != serverConfigSpec) {
            return;
        }
        Turrets.turretsAttackOwner = SERVER.turretsAttackOwner.get();
        Turrets.canRocketsHome = SERVER.canRocketsHome.get();
        Turrets.canRocketsHurtEnderDragon = SERVER.canRocketsHurtEnderDragon.get();
        Turrets.canRocketsDestroyBlocks = SERVER.canRocketsDestroyBlocks.get();
        Turrets.canGrenadesDestroyBlocks = SERVER.canGrenadesDestroyBlocks.get();
        Turrets.canRailgunDestroyBlocks = SERVER.canRailgunDestroyBlocks.get();
        Turrets.canTurretsConcealWithoutAddon = SERVER.canTurretsConcealWithoutAddon.get();
        Turrets.globalCanTargetPlayers = SERVER.globalCanTargetPlayers.get();
        Turrets.globalCanTargetNeutrals = SERVER.globalCanTargetNeutrals.get();
        Turrets.globalCanTargetMobs = SERVER.globalCanTargetMobs.get();
        Turrets.turretAlarmSound = SERVER.turretAlarmSound.get();
        Turrets.turretWarnMessage = SERVER.turretWarnMessage.get();
        Turrets.turretWarningDistance = SERVER.turretWarningDistance.get();
        Turrets.turretDamageTrustedPlayers = SERVER.turretDamageTrustedPlayers.get();
        Turrets.turretSoundVolume = SERVER.turretSoundVolume.get();
        Turrets.turretTargetSearchTicks = SERVER.turretTargetSearchTicks.get();

        General.doTurretsKillsDropMobLoot = SERVER.doTurretsKillsDropMobLoot.get();
        General.doLootAddonsOverrideMobLootSetting = SERVER.doLootAddonsOverrideMobLootSetting.get();
        General.mobBlacklist = new HashSet<>(new ArrayList<>(SERVER.mobBlacklist.get()));
    }

    private OMTConfig() {
    }
}
