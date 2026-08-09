package com.ommods.openmodularturrets.content;

import com.ommods.openmodularturrets.OpenModularTurrets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The classic OMT sounds, ported straight from the 1.12 jar (the same ogg files and sounds.json
 * ids). One event per 1.12 ModSounds entry; the launch sounds replace the vanilla placeholders the
 * firing loop used before these were ported.
 */
public final class OMTSounds {

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OpenModularTurrets.MOD_ID);

    public static final RegistryObject<SoundEvent> DISPOSABLE = sound("disposable");
    public static final RegistryObject<SoundEvent> POTATO = sound("potato");
    public static final RegistryObject<SoundEvent> MACHINE_GUN = sound("machine_gun");
    public static final RegistryObject<SoundEvent> INCENDIARY = sound("incendiary");
    public static final RegistryObject<SoundEvent> GRENADE = sound("grenade");
    public static final RegistryObject<SoundEvent> RELATIVISTIC = sound("relativistic");
    public static final RegistryObject<SoundEvent> ROCKET = sound("rocket");
    public static final RegistryObject<SoundEvent> TELEPORT = sound("teleport");
    public static final RegistryObject<SoundEvent> LASER = sound("laser");
    public static final RegistryObject<SoundEvent> RAIL_GUN = sound("rail_gun");

    public static final RegistryObject<SoundEvent> BULLET_HIT = sound("bullet_hit");
    public static final RegistryObject<SoundEvent> LASER_HIT = sound("laser_hit");
    public static final RegistryObject<SoundEvent> RAIL_GUN_HIT = sound("rail_gun_hit");

    public static final RegistryObject<SoundEvent> TURRET_DEPLOY = sound("turret_deploy");
    public static final RegistryObject<SoundEvent> TURRET_RETRACT = sound("turret_retract");
    public static final RegistryObject<SoundEvent> WARNING = sound("warning");
    public static final RegistryObject<SoundEvent> AMPED = sound("amped");
    public static final RegistryObject<SoundEvent> WINDUP = sound("windup");

    // Faithful 1.12 OMTConfig.TURRETS.turretSoundVolume default.
    public static final float TURRET_VOLUME = 4.0F;

    /**
     * The volume every turret sound plays at, from the config. 1.12 read
     * {@code OMTConfig.TURRETS.turretSoundVolume} at each playSound call rather than caching it, so a
     * config reload takes effect on the next shot.
     */
    public static float volume() {
        return (float) com.ommods.openmodularturrets.config.OMTConfig.Turrets.turretSoundVolume;
    }

    private OMTSounds() {
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                new ResourceLocation(OpenModularTurrets.MOD_ID, name)));
    }

    public static void register(IEventBus modEventBus) {
        SOUNDS.register(modEventBus);
    }
}
