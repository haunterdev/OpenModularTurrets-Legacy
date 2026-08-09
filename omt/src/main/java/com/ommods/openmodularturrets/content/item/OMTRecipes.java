package com.ommods.openmodularturrets.content.item;

import com.ommods.openmodularturrets.content.base.BaseInit;
import com.ommods.openmodularturrets.content.base.BaseTier;
import com.ommods.openmodularturrets.content.block.OMTBlockInit;
import com.ommods.openmodularturrets.content.expander.ExpanderInit;
import com.ommods.openmodularturrets.content.turret.TurretType;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;

/**
 * The faithful 1.12 vanilla crafting recipes, ported one to one from the jar's recipe JSONs (the
 * {@code *_vanilla.json} set; EnderIO/Mekanism variants do not apply without those mods). Ore
 * dictionary keys become the matching Forge tags. Recipes whose results are not ported (ferro slug,
 * recycler/serial port/fake drops addons, base addon, lever, memory card) are left out with them.
 */
public final class OMTRecipes {

    private OMTRecipes() {
    }

    public static void register(Registrate registrate) {
        registrate.addDataGenerator(ProviderType.RECIPE, OMTRecipes::generate);
    }

    private static void generate(RegistrateRecipeProvider prov) {
        // Intermediates. The IO Bus is the recurring component of everything above tier 1.
        shaped(prov, OMTItems.IO_BUS.get(), 1, new String[]{" A ", "BBB", " C "},
                'A', Tags.Items.INGOTS_GOLD, 'B', Tags.Items.DUSTS_REDSTONE, 'C', Tags.Items.INGOTS_IRON);

        // Sensors tier 1-5: each tier wraps the previous in the next material.
        shaped(prov, sensor(1), 1, new String[]{" A ", "ABA", " A "},
                'A', Tags.Items.DUSTS_REDSTONE, 'B', ItemTags.PLANKS);
        shaped(prov, sensor(2), 1, new String[]{" A ", "ABA", " C "},
                'A', Tags.Items.INGOTS_IRON, 'B', sensor(1), 'C', ioBus());
        shaped(prov, sensor(3), 1, new String[]{" C ", "ABA", " C "},
                'A', Tags.Items.INGOTS_GOLD, 'B', sensor(2), 'C', ioBus());
        shaped(prov, sensor(4), 1, new String[]{"ADA", "CBC", "EDE"},
                'A', Tags.Items.INGOTS_GOLD, 'B', sensor(3), 'C', ioBus(),
                'D', Tags.Items.GEMS_DIAMOND, 'E', Tags.Items.GEMS_QUARTZ);
        shaped(prov, sensor(5), 1, new String[]{"ADA", "CBC", "EDE"},
                'A', Tags.Items.INGOTS_GOLD, 'B', sensor(4), 'C', ioBus(),
                'D', Items.GLOWSTONE_DUST, 'E', Tags.Items.OBSIDIAN);

        // Chambers tier 1-5.
        shaped(prov, chamber(1), 1, new String[]{"AAA", " BC", "AAA"},
                'A', Tags.Items.COBBLESTONE, 'B', ItemTags.PLANKS, 'C', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, chamber(2), 1, new String[]{"AAA", " BC", "AAA"},
                'A', Tags.Items.INGOTS_IRON, 'B', chamber(1), 'C', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, chamber(3), 1, new String[]{"AAA", " BC", "AAA"},
                'A', Tags.Items.INGOTS_GOLD, 'B', chamber(2), 'C', ioBus());
        shaped(prov, chamber(4), 1, new String[]{"DAD", " BC", "DAD"},
                'A', Tags.Items.GEMS_DIAMOND, 'B', chamber(3), 'C', ioBus(), 'D', Tags.Items.GEMS_QUARTZ);
        shaped(prov, chamber(5), 1, new String[]{"ADA", " BC", "ADA"},
                'A', Tags.Items.OBSIDIAN, 'B', chamber(4), 'C', ioBus(), 'D', Tags.Items.GEMS_QUARTZ);

        // Barrels tier 1-5.
        shaped(prov, barrel(1), 1, new String[]{"AAA", " B ", "AAA"},
                'A', Tags.Items.COBBLESTONE, 'B', ItemTags.PLANKS);
        shaped(prov, barrel(2), 1, new String[]{"AAA", " B ", "AAA"},
                'A', Tags.Items.INGOTS_IRON, 'B', barrel(1));
        shaped(prov, barrel(3), 1, new String[]{"AAA", " B ", "AAA"},
                'A', Tags.Items.INGOTS_GOLD, 'B', barrel(2));
        shaped(prov, barrel(4), 1, new String[]{"CAC", " B ", "CAC"},
                'A', Tags.Items.GEMS_DIAMOND, 'B', barrel(3), 'C', Tags.Items.GEMS_QUARTZ);
        shaped(prov, barrel(5), 1, new String[]{"AAA", "CBC", "AAA"},
                'A', Tags.Items.OBSIDIAN, 'B', barrel(4), 'C', Items.GLOWSTONE_DUST);

        // Turret bases: tier 1 from raw materials, each later tier around the previous base.
        shaped(prov, base(BaseTier.TIER1), 1, new String[]{"ABA", "BCB", "ABA"},
                'A', Tags.Items.COBBLESTONE, 'B', ItemTags.PLANKS, 'C', sensor(1));
        shaped(prov, base(BaseTier.TIER2), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.INGOTS_IRON, 'B', base(BaseTier.TIER1), 'C', sensor(2), 'D', ioBus());
        shaped(prov, base(BaseTier.TIER3), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.INGOTS_GOLD, 'B', base(BaseTier.TIER2), 'C', sensor(3), 'D', ioBus());
        shaped(prov, base(BaseTier.TIER4), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.GEMS_DIAMOND, 'B', base(BaseTier.TIER3), 'C', sensor(4), 'D', ioBus());
        shaped(prov, base(BaseTier.TIER5), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.OBSIDIAN, 'B', base(BaseTier.TIER4), 'C', sensor(5), 'D', ioBus());

        // Turret heads, each from its tier's barrel and chamber.
        shaped(prov, turret(TurretType.DISPOSABLE), 1, new String[]{" A ", "CBC", "CDC"},
                'A', barrel(1), 'B', chamber(1), 'C', Tags.Items.COBBLESTONE, 'D', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, turret(TurretType.POTATO_CANNON), 1, new String[]{"CAC", "CAC", "DBD"},
                'A', barrel(1), 'B', chamber(1), 'C', Tags.Items.COBBLESTONE, 'D', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, turret(TurretType.MACHINE_GUN), 1, new String[]{" A ", "CAC", "DBD"},
                'A', barrel(2), 'B', chamber(2), 'C', Tags.Items.INGOTS_IRON, 'D', ioBus());
        shaped(prov, turret(TurretType.INCENDIARY), 1, new String[]{"A A", "BCB", "DCD"},
                'A', barrel(2), 'B', chamber(2), 'C', Tags.Items.INGOTS_IRON, 'D', ioBus());
        shaped(prov, turret(TurretType.GRENADE), 1, new String[]{" A ", "CBC", "CDC"},
                'A', barrel(3), 'B', chamber(3), 'C', Tags.Items.INGOTS_GOLD, 'D', ioBus());
        shaped(prov, turret(TurretType.RELATIVISTIC), 1, new String[]{"CAC", "ABA", "CDC"},
                'A', Items.ENDER_PEARL, 'B', sensor(3), 'C', Tags.Items.INGOTS_GOLD, 'D', ioBus());
        shaped(prov, turret(TurretType.ROCKET), 1, new String[]{"CAC", "ABA", "EDE"},
                'A', barrel(4), 'B', chamber(4), 'C', Tags.Items.GEMS_QUARTZ, 'D', ioBus(),
                'E', Tags.Items.GEMS_DIAMOND);
        shaped(prov, turret(TurretType.TELEPORTER), 1, new String[]{"CEC", "ABA", "CDC"},
                'A', Tags.Items.GEMS_DIAMOND, 'B', sensor(4), 'C', Items.ENDER_EYE, 'D', ioBus(),
                'E', Tags.Items.GEMS_QUARTZ);
        shaped(prov, turret(TurretType.LASER), 1, new String[]{" A ", "CBC", "DCD"},
                'A', barrel(5), 'B', chamber(5), 'C', Tags.Items.OBSIDIAN, 'D', ioBus());
        shaped(prov, turret(TurretType.RAILGUN), 1, new String[]{"CAC", "CAC", "DBD"},
                'A', barrel(5), 'B', chamber(5), 'C', Tags.Items.OBSIDIAN, 'D', ioBus());

        // Ammo, with the 1.12 output counts.
        shaped(prov, OMTItems.AMMO_BLAZING_CLAY.get(), 32, new String[]{"BCB", "CAC", "BCB"},
                'A', Items.BLAZE_POWDER, 'B', Items.CLAY_BALL, 'C', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, OMTItems.AMMO_BULLET.get(), 64, new String[]{" A ", "BC ", " A "},
                'A', Tags.Items.INGOTS_IRON, 'B', Items.GUNPOWDER, 'C', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, OMTItems.AMMO_GRENADE.get(), 32, new String[]{" C ", "ABA", " A "},
                'A', Tags.Items.INGOTS_IRON, 'B', Items.GUNPOWDER, 'C', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, OMTItems.AMMO_ROCKET.get(), 32, new String[]{" A ", "ABA", "ACA"},
                'A', Tags.Items.INGOTS_IRON, 'B', Items.TNT, 'C', Tags.Items.DUSTS_REDSTONE);
        shaped(prov, OMTItems.AMMO_FERRO_SLUG.get(), 16, new String[]{" C ", "CBC", " A "},
                'A', Tags.Items.INGOTS_IRON, 'B', Items.FLINT, 'C', Tags.Items.DUSTS_REDSTONE);

        // Upgrades: all quartz around a signature item over an IO Bus.
        upgradeRecipe(prov, OMTItems.UPGRADE_ACCURACY.get(), Tags.Items.INGOTS_GOLD);
        upgradeRecipe(prov, OMTItems.UPGRADE_EFFICIENCY.get(), Items.ENDER_EYE);
        upgradeRecipe(prov, OMTItems.UPGRADE_FIRE_RATE.get(), Items.BLAZE_POWDER);
        upgradeRecipe(prov, OMTItems.UPGRADE_RANGE.get(), Tags.Items.GEMS_DIAMOND);
        upgradeRecipe(prov, OMTItems.UPGRADE_SCATTER_SHOT.get(), Items.FLINT);

        // Addons.
        shaped(prov, OMTItems.ADDON_CONCEALER.get(), 1, new String[]{"ABA", "BCD", "ABA"},
                'A', Tags.Items.INGOTS_IRON, 'B', Tags.Items.GEMS_QUARTZ, 'C', Tags.Items.CHESTS_WOODEN,
                'D', ioBus());
        shaped(prov, OMTItems.ADDON_RECYCLER.get(), 1, new String[]{"ABA", "BCD", "ABA"},
                'A', Tags.Items.INGOTS_GOLD, 'B', Items.MAGMA_CREAM, 'C', Items.ENDER_CHEST, 'D', ioBus());
        shaped(prov, OMTItems.ADDON_FAKE_DROPS.get(), 1, new String[]{"CAC", "ABD", "CAC"},
                'A', Tags.Items.STORAGE_BLOCKS_LAPIS, 'B', Items.ENDER_EYE, 'C', Tags.Items.GEMS_QUARTZ,
                'D', ioBus());
        shaped(prov, OMTItems.ADDON_DAMAGE_AMP.get(), 1, new String[]{"AAA", "BCB", "AAA"},
                'A', Tags.Items.INGOTS_IRON, 'B', Items.ENDER_EYE, 'C', Tags.Items.STORAGE_BLOCKS_REDSTONE);
        shaped(prov, OMTItems.ADDON_REDSTONE_REACTOR.get(), 1, new String[]{"CAC", "ABD", "CAC"},
                'A', Tags.Items.STORAGE_BLOCKS_REDSTONE, 'B', Items.ENDER_EYE, 'C', Tags.Items.GEMS_QUARTZ,
                'D', ioBus());
        shaped(prov, OMTItems.ADDON_SOLAR_PANEL.get(), 1, new String[]{"AAA", "CBC", "DED"},
                'A', Tags.Items.GLASS_PANES, 'B', Tags.Items.STORAGE_BLOCKS_LAPIS,
                'C', Tags.Items.DUSTS_REDSTONE, 'D', Tags.Items.INGOTS_IRON, 'E', ioBus());

        // The Loot Deleter base attachment, the crank and the memory card (1.12 base_addon_meta_0,
        // lever_block and usable_meta_2; the card's dye meta 4 was lapis).
        shaped(prov, ExpanderInit.LOOT_DELETER.get(), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.OBSIDIAN, 'B', Tags.Items.INGOTS_IRON, 'C', Tags.Items.DUSTS_REDSTONE,
                'D', ioBus());
        shaped(prov, OMTBlockInit.LEVER.get(), 1, new String[]{"AAA", "A  ", "A  "},
                'A', Tags.Items.COBBLESTONE);
        shaped(prov, OMTItems.MEMORY_CARD.get(), 1, new String[]{"BAB", "CEC", "FDF"},
                'A', Tags.Items.INGOTS_GOLD, 'B', Tags.Items.DUSTS_REDSTONE, 'C', Tags.Items.INGOTS_IRON,
                'D', ioBus(), 'E', Items.PAPER, 'F', Tags.Items.GEMS_LAPIS);

        // Expanders. ExpanderInit.ALL is ordered inventory tier 1-5 then power tier 1-5, matching the
        // 1.12 metas 0-9. Inventory expanders chain around a chest, power ones around redstone.
        shaped(prov, expander(0), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.COBBLESTONE, 'B', ItemTags.PLANKS, 'C', Tags.Items.CHESTS_WOODEN, 'D', ioBus());
        shaped(prov, expander(1), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.INGOTS_IRON, 'B', expander(0), 'C', Tags.Items.CHESTS_WOODEN, 'D', ioBus());
        shaped(prov, expander(2), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.INGOTS_GOLD, 'B', expander(1), 'C', Tags.Items.CHESTS_WOODEN, 'D', ioBus());
        shaped(prov, expander(3), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.GEMS_DIAMOND, 'B', expander(2), 'C', Tags.Items.CHESTS_WOODEN, 'D', ioBus());
        shaped(prov, expander(4), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.OBSIDIAN, 'B', expander(3), 'C', Tags.Items.CHESTS_WOODEN, 'D', ioBus());
        shaped(prov, expander(5), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.COBBLESTONE, 'B', ItemTags.PLANKS, 'C', Tags.Items.DUSTS_REDSTONE, 'D', ioBus());
        shaped(prov, expander(6), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.INGOTS_IRON, 'B', expander(5), 'C', Tags.Items.STORAGE_BLOCKS_REDSTONE, 'D', ioBus());
        shaped(prov, expander(7), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.INGOTS_GOLD, 'B', expander(6), 'C', Tags.Items.STORAGE_BLOCKS_REDSTONE, 'D', ioBus());
        shaped(prov, expander(8), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.GEMS_DIAMOND, 'B', expander(7), 'C', Tags.Items.STORAGE_BLOCKS_REDSTONE, 'D', ioBus());
        shaped(prov, expander(9), 1, new String[]{"ABA", "DCD", "ADA"},
                'A', Tags.Items.OBSIDIAN, 'B', expander(8), 'C', Tags.Items.STORAGE_BLOCKS_REDSTONE, 'D', ioBus());
    }

    // Every upgrade shares the 1.12 pattern: quartz cross, its signature item in the middle, IO Bus below.
    private static void upgradeRecipe(RegistrateRecipeProvider prov, ItemLike result, Object signature) {
        shaped(prov, result, 1, new String[]{" A ", "ABA", " C "},
                'A', Tags.Items.GEMS_QUARTZ, 'B', signature, 'C', ioBus());
    }

    /**
     * Builds one shaped recipe. {@code keys} alternates between a pattern character and its
     * ingredient, which is either an {@link ItemLike} or a {@code TagKey<Item>}. The unlock criterion
     * is having the first keyed ingredient, enough to surface the tree in the recipe book.
     */
    @SuppressWarnings("unchecked")
    private static void shaped(RegistrateRecipeProvider prov, ItemLike result, int count, String[] pattern,
                               Object... keys) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count);
        for (String row : pattern) {
            builder.pattern(row);
        }
        CriterionTriggerInstance unlock = null;
        for (int i = 0; i < keys.length; i += 2) {
            char symbol = (Character) keys[i];
            Object ingredient = keys[i + 1];
            if (ingredient instanceof ItemLike item) {
                builder.define(symbol, item);
                if (unlock == null) {
                    unlock = InventoryChangeTrigger.TriggerInstance.hasItems(item);
                }
            } else {
                TagKey<Item> tag = (TagKey<Item>) ingredient;
                builder.define(symbol, tag);
                if (unlock == null) {
                    unlock = InventoryChangeTrigger.TriggerInstance.hasItems(
                            ItemPredicate.Builder.item().of(tag).build());
                }
            }
        }
        builder.unlockedBy("has_ingredient", unlock);
        builder.save(prov);
    }

    private static Item ioBus() {
        return OMTItems.IO_BUS.get();
    }

    private static Item sensor(int tier) {
        return OMTItems.SENSORS.get(tier).get();
    }

    private static Item chamber(int tier) {
        return OMTItems.CHAMBERS.get(tier).get();
    }

    private static Item barrel(int tier) {
        return OMTItems.BARRELS.get(tier).get();
    }

    private static ItemLike base(BaseTier tier) {
        return BaseInit.BASES.get(tier).get();
    }

    private static ItemLike turret(TurretType type) {
        return OMTBlockInit.TURRETS.get(type).get();
    }

    private static ItemLike expander(int meta) {
        return ExpanderInit.ALL.get(meta).get();
    }
}
