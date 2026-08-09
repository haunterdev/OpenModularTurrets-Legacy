package com.ommods.openmodularturrets;

import com.ommods.omlib.OMLib;
import com.ommods.omlib.block.BlockInit;
import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.base.BaseInit;
import com.ommods.openmodularturrets.content.base.BaseTier;
import com.ommods.openmodularturrets.content.block.OMTBlockInit;
import com.ommods.openmodularturrets.content.OMTSounds;
import com.ommods.openmodularturrets.content.expander.ExpanderInit;
import com.ommods.openmodularturrets.content.item.OMTItems;
import com.ommods.openmodularturrets.content.item.OMTRecipes;
import com.ommods.openmodularturrets.content.projectile.OMTEntities;
import com.ommods.openmodularturrets.content.turret.TurretType;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(OpenModularTurrets.MOD_ID)
public class OpenModularTurrets {
    public static final String MOD_ID = "openmodularturrets";

    public static final NonNullSupplier<Registrate> REGISTRATE =
            NonNullSupplier.lazy(() -> Registrate.create(MOD_ID));

    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Open Modular Turrets"))
                    .icon(() -> new ItemStack(BaseInit.BASES.get(BaseTier.TIER1).get()))
                    .displayItems((params, output) -> {
                        for (BaseTier tier : BaseTier.values()) {
                            output.accept(BaseInit.BASES.get(tier).get());
                        }
                        for (TurretType type : TurretType.values()) {
                            output.accept(OMTBlockInit.TURRETS.get(type).get());
                        }
                        output.accept(OMTBlockInit.ENERGIZER.get());
                        output.accept(OMTBlockInit.LEVER.get());
                        ExpanderInit.ALL.forEach(entry -> output.accept(entry.get()));
                        output.accept(ExpanderInit.LOOT_DELETER.get());
                        output.accept(OMTItems.AMMO_BULLET.get());
                        output.accept(OMTItems.AMMO_GRENADE.get());
                        output.accept(OMTItems.AMMO_ROCKET.get());
                        output.accept(OMTItems.AMMO_BLAZING_CLAY.get());
                        output.accept(OMTItems.AMMO_FERRO_SLUG.get());
                        // DROPPED: the throwable bullet and grenade. 1.12 put usable_meta 0 and 1 in
                        // the tab, but in this port they exist only as the in-flight model a round is
                        // drawn with; they have no recipe, no use and no ammo value, so listing them
                        // just adds two dead items a player can pick up and do nothing with.
                        OMTItems.modifiers().forEach(entry -> output.accept(entry.get()));
                        output.accept(OMTItems.MEMORY_CARD.get());
                        OMTItems.intermediates().forEach(entry -> output.accept(entry.get()));
                        // omlib's network cable is deliberately absent: 1.12 registers it with no
                        // creative tab and ships no model or texture for it, so it was never a block
                        // players could obtain.
                    })
                    .build());

    public OpenModularTurrets() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, OMTConfig.serverConfigSpec);
        // Define the Registrate content before the registry events fire; Registrate hooks the bus
        // itself, as it does in omlib.
        OMTBlockInit.register(REGISTRATE.get());
        BaseInit.register(REGISTRATE.get());
        ExpanderInit.register(REGISTRATE.get());
        OMTItems.register(REGISTRATE.get());
        OMTRecipes.register(REGISTRATE.get());
        // Death message for the turret damage types (1.12 never localized one; this port does).
        REGISTRATE.get().addRawLang("death.attack.turret", "%1$s was shot down by a turret");
        REGISTRATE.get().addRawLang("death.attack.turret.player", "%1$s was shot down by a turret while fighting %2$s");
        // The proximity warning and permission refusal lines, word for word from the 1.12 en_US.lang.
        REGISTRATE.get().addRawLang("status.warning", "You are entering a turret-protected area. Continue at your own risk.");
        REGISTRATE.get().addRawLang("status.ownership", "You do not own this turret, or you don't have permission to do that.");
        registerSubtitles();
        OMTEntities.register(modEventBus);
        OMTSounds.register(modEventBus);
        TABS.register(modEventBus);
        OMLib.getLogger().info("OpenModularTurrets loaded against omlib");
    }

    /**
     * Subtitles for every sound in sounds.json. 1.12 predates subtitles and shipped none, so these
     * are written for this port: without them a player with subtitles on hears a turret open fire and
     * sees nothing, which is the one accessibility gap the sound port left.
     */
    private static void registerSubtitles() {
        subtitle("machine_gun", "Gun Turret fires");
        subtitle("grenade", "Grenade Launcher Turret fires");
        subtitle("rocket", "Rocket Launcher Turret fires");
        subtitle("incendiary", "Incendiary Turret fires");
        subtitle("potato", "Potato Cannon Turret fires");
        subtitle("disposable", "Disposable Item Turret fires");
        subtitle("laser", "Laser Turret fires");
        subtitle("rail_gun", "Rail Gun Turret fires");
        subtitle("relativistic", "Relativistic Turret pulses");
        subtitle("teleport", "Teleporter Turret warps a target");
        subtitle("bullet_hit", "Bullet hits");
        subtitle("laser_hit", "Laser burns");
        subtitle("rail_gun_hit", "Slug punches through");
        subtitle("amped", "Turret amplifier charges");
        subtitle("windup", "Turret winds up");
        subtitle("warning", "Turret warning alarm");
        subtitle("turret_deploy", "Turret deploys");
        subtitle("turret_retract", "Turret retracts");
    }

    private static void subtitle(String sound, String text) {
        REGISTRATE.get().addRawLang("subtitles." + MOD_ID + "." + sound, text);
    }
}
