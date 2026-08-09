package com.ommods.openmodularturrets.content.base;

import com.ommods.openmodularturrets.client.gui.ConfigureScreen;
import com.ommods.openmodularturrets.client.gui.TurretBaseScreen;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.MenuEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registers the five turret base tiers. All tiers share one block entity type. Kept in the base
 * package so its {@code TurretBaseBlock} does not collide with the head block of the same simple
 * name in the block package.
 */
public final class BaseInit {

    public static final Map<BaseTier, BlockEntry<TurretBaseBlock>> BASES = new EnumMap<>(BaseTier.class);
    public static BlockEntityEntry<TurretBaseTileEntity> TURRET_BASE_ENTITY;
    public static MenuEntry<TurretBaseMenu> TURRET_BASE_MENU;
    public static MenuEntry<ConfigureMenu> CONFIGURE_MENU;

    private BaseInit() {
    }

    public static void register(Registrate registrate) {
        for (BaseTier tier : BaseTier.values()) {
            BASES.put(tier, registrate.object(tier.registryName())
                    .block(p -> new TurretBaseBlock(p, tier))
                    .lang(tier.displayName())
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p
                            .mapColor(MapColor.METAL)
                            // The 1.12 default (baseBreakable=false): bases can't be mined, the
                            // GUI's Drop base button is the removal path. Upstream returns -1 for
                            // the explosion resistance too, so bases have no blast protection.
                            .strength(-1.0F, 0.0F)
                            .isValidSpawn((state, level, pos, type) -> false)
                            .sound(SoundType.METAL))
                    // Faithful base skin at openmodularturrets:block/turret_base_tierN. The 1.12 art
                    // is a 32x32 face atlas; a cube_all approximation until the base model is ported.
                    .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                            prov.models().cubeAll(ctx.getName(),
                                    new ResourceLocation("openmodularturrets", "block/" + tier.registryName()))))
                    // TurretBaseItem carries the 1.12 shift tooltip (energy stats, slots, flavour).
                    .item(TurretBaseItem::new).build()
                    .register());
        }

        var builder = registrate.blockEntity("turret_base", TurretBaseTileEntity::new);
        for (BaseTier tier : BaseTier.values()) {
            builder.validBlock(BASES.get(tier));
        }
        TURRET_BASE_ENTITY = builder.register();

        // Registrate registers the menu types and wires the screens on the client for us.
        TURRET_BASE_MENU = registrate.menu("turret_base", TurretBaseMenu::new, () -> TurretBaseScreen::new)
                .register();
        CONFIGURE_MENU = registrate.menu("turret_base_configure", ConfigureMenu::new, () -> ConfigureScreen::new)
                .register();
    }
}
