package com.ommods.openmodularturrets.content.projectile;

import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.content.turret.ProjectileKind;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

/**
 * Registers the six classic turret projectile entity types and turns a {@link ProjectileKind} into a
 * fresh entity. Hitscan turrets have no entity, so {@link #create} returns null for
 * {@link ProjectileKind#NONE}.
 */
public final class OMTEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenModularTurrets.MOD_ID);

    public static final RegistryObject<EntityType<BulletProjectileEntity>> BULLET =
            ENTITIES.register("bullet", () -> projectile(BulletProjectileEntity::new, 0.25f));
    public static final RegistryObject<EntityType<RocketProjectileEntity>> ROCKET =
            ENTITIES.register("rocket", () -> projectile(RocketProjectileEntity::new, 0.5f));
    public static final RegistryObject<EntityType<GrenadeProjectileEntity>> GRENADE =
            ENTITIES.register("grenade", () -> projectile(GrenadeProjectileEntity::new, 0.5f));
    public static final RegistryObject<EntityType<PotatoProjectileEntity>> POTATO =
            ENTITIES.register("potato", () -> projectile(PotatoProjectileEntity::new, 0.25f));
    public static final RegistryObject<EntityType<DisposableProjectileEntity>> DISPOSABLE =
            ENTITIES.register("disposable", () -> projectile(DisposableProjectileEntity::new, 0.25f));
    public static final RegistryObject<EntityType<BlazingClayProjectileEntity>> BLAZING_CLAY =
            ENTITIES.register("blazing_clay", () -> projectile(BlazingClayProjectileEntity::new, 0.25f));

    private static <T extends OMTProjectile> EntityType<T> projectile(EntityType.EntityFactory<T> factory, float size) {
        // noSave: turret projectiles do not persist across a reload (faithful to 1.12), and it also
        // lets build take a null id without the datafixer lookup that would NPE on a saved type.
        return EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(size, size)
                .noSummon()
                .noSave()
                .build(null);
    }

    /** A fresh projectile entity for the given kind, or null for hitscan ({@link ProjectileKind#NONE}). */
    @Nullable
    public static OMTProjectile create(ProjectileKind kind, Level level) {
        return switch (kind) {
            case NONE -> null;
            case BULLET -> BULLET.get().create(level);
            case ROCKET -> ROCKET.get().create(level);
            case GRENADE -> GRENADE.get().create(level);
            case POTATO -> POTATO.get().create(level);
            case DISPOSABLE -> DISPOSABLE.get().create(level);
            case BLAZING_CLAY -> BLAZING_CLAY.get().create(level);
        };
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }

    private OMTEntities() {
    }
}
