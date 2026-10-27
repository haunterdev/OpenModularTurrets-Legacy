package com.ommods.ompd.content;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.Tags;

/** The 15 recipes from the final 1.12.2 jar, with metadata results flattened to block ids. */
public final class OMPDRecipes {
    private OMPDRecipes() {
    }

    public static void register(Registrate registrate) {
        registrate.addDataGenerator(ProviderType.RECIPE, OMPDRecipes::generate);
    }

    private static void generate(RegistrateRecipeProvider prov) {
        shaped(prov, hardened(DefenseTier.TIER1), 16, new String[]{"ABA", "BCB", "ABA"},
                'A', Items.GRAVEL, 'B', Tags.Items.COBBLESTONE, 'C', Items.SAND);
        shaped(prov, hardened(DefenseTier.TIER2), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', hardened(DefenseTier.TIER1), 'B', Items.STONE);
        shaped(prov, hardened(DefenseTier.TIER3), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', hardened(DefenseTier.TIER2), 'B', Items.BRICKS);
        shaped(prov, hardened(DefenseTier.TIER4), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', hardened(DefenseTier.TIER3), 'B', Items.NETHER_BRICK);
        shaped(prov, hardened(DefenseTier.TIER5), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', hardened(DefenseTier.TIER4), 'B', Tags.Items.OBSIDIAN);

        for (DefenseTier tier : DefenseTier.values()) {
            shaped(prov, wall(tier), 6, new String[]{"AAA", "AAA"},
                    'A', hardened(tier));
        }

        shaped(prov, fence(DefenseTier.TIER1), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', Items.IRON_BARS, 'B', Tags.Items.COBBLESTONE);
        shaped(prov, fence(DefenseTier.TIER2), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', Items.IRON_BARS, 'B', Tags.Items.INGOTS_IRON);
        shaped(prov, fence(DefenseTier.TIER3), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', Items.IRON_BARS, 'B', Tags.Items.INGOTS_GOLD);
        shaped(prov, fence(DefenseTier.TIER4), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', Items.IRON_BARS, 'B', Tags.Items.GEMS_DIAMOND);
        shaped(prov, fence(DefenseTier.TIER5), 16, new String[]{"ABA", "BAB", "ABA"},
                'A', Items.IRON_BARS, 'B', Tags.Items.OBSIDIAN);
    }

    @SuppressWarnings("unchecked")
    private static void shaped(RegistrateRecipeProvider prov, ItemLike result, int count,
                               String[] pattern, Object... keys) {
        ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, result, count);
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

    private static ItemLike hardened(DefenseTier tier) {
        return OMPDBlocks.HARDENED.get(tier).get();
    }

    private static ItemLike wall(DefenseTier tier) {
        return OMPDBlocks.WALLS.get(tier).get();
    }

    private static ItemLike fence(DefenseTier tier) {
        return OMPDBlocks.FENCES.get(tier).get();
    }
}
