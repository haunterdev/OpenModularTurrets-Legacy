package com.ommods.openmodularturrets.content.expander;

import com.ommods.openmodularturrets.client.gui.ExpanderScreen;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.MenuEntry;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.model.generators.ConfiguredModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers the ten expander variants: an inventory and a power expander for each of five tiers,
 * faithful to the ten metas of 1.12 {@code BlockExpander}. All ten share one block entity type; only
 * the inventory expanders use the shared expander menu. Kept in its own package alongside the pure
 * {@link ExpanderStats}.
 */
public final class ExpanderInit {

    // Texture files use word tiers (expander_inv_tier_one_side), so map the number across.
    private static final String[] TIER_WORD = {"", "one", "two", "three", "four", "five"};

    public static final List<BlockEntry<ExpanderBlock>> ALL = new ArrayList<>();
    public static BlockEntityEntry<ExpanderBlockEntity> EXPANDER_ENTITY;
    public static MenuEntry<ExpanderMenu> EXPANDER_MENU;
    public static BlockEntry<LootDeleterBlock> LOOT_DELETER;

    private ExpanderInit() {
    }

    public static void register(Registrate registrate) {
        for (boolean power : new boolean[]{false, true}) {
            for (int tier = 1; tier <= 5; tier++) {
                ALL.add(registerOne(registrate, power, tier));
            }
        }

        var builder = registrate.blockEntity("expander", ExpanderBlockEntity::new);
        for (BlockEntry<ExpanderBlock> block : ALL) {
            builder.validBlock(block);
        }
        EXPANDER_ENTITY = builder.register();

        EXPANDER_MENU = registrate.menu("expander_inv", ExpanderMenu::new, () -> ExpanderScreen::new)
                .register();

        // The Loot Deleter base attachment, faithful to 1.12 BlockBaseAttachment meta 0; it shares
        // the expander's facing pad model (the 1.12 base_addon blockstate reuses block/expander).
        LOOT_DELETER = registrate.object("base_addon_loot_deleter")
                .block(LootDeleterBlock::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p
                        .mapColor(MapColor.METAL)
                        // 1.12 BlockBaseAttachment: hardness 3, setResistance(3) = 1.8 in 1.20 units.
                        .strength(3.0F, 1.8F)
                        .isValidSpawn((state, level, pos, entityType) -> false)
                        .sound(SoundType.METAL)
                        .noOcclusion())
                .lang("Base Addon - Loot Deleter")
                .blockstate((ctx, prov) -> padBlockstate(ctx, prov,
                        "block/base_addon_loot_deleter_side", "block/base_addon_loot_deleter_top"))
                .simpleItem()
                .register();
    }

    /**
     * The 1.12 attachment pad blockstate: one model per block (the shared expander_pad geometry with
     * this block's textures), rotated so the pad hugs the face toward the base. The rotation table is
     * vanilla's multiface mapping for a north-hugging model.
     */
    private static void padBlockstate(DataGenContext<Block, ? extends Block> ctx,
                                      RegistrateBlockstateProvider prov, String side, String top) {
        var model = prov.models().withExistingParent(ctx.getName(), prov.modLoc("block/expander_pad"))
                .texture("side", new ResourceLocation("openmodularturrets", side))
                .texture("top", new ResourceLocation("openmodularturrets", top));
        prov.getVariantBuilder(ctx.getEntry()).forAllStates(state -> {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            int x = switch (facing) {
                case DOWN -> 90;
                case UP -> 270;
                default -> 0;
            };
            int y = switch (facing) {
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
    }

    private static BlockEntry<ExpanderBlock> registerOne(Registrate registrate, boolean power, int tier) {
        String kind = power ? "power" : "inv";
        String name = "expander_" + kind + "_tier" + tier;
        String side = "block/expander_" + kind + "_tier_" + TIER_WORD[tier] + "_side";
        String top = "block/expander_" + kind + "_tier_" + TIER_WORD[tier] + "_top";
        String label = "Base " + (power ? "Power" : "Inventory") + " Expander (Tier " + tier + ")";
        return registrate.object(name)
                .block(p -> new ExpanderBlock(p, power, tier))
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p
                        .mapColor(MapColor.METAL)
                        // 1.12 BlockExpander: hardness 3, setResistance(3) = 1.8 in 1.20 units.
                        .strength(3.0F, 1.8F)
                        .isValidSpawn((state, level, pos, entityType) -> false)
                        .sound(SoundType.METAL)
                        .noOcclusion())
                .lang(label)
                // A 12x12x6 pad on the base's face: the tier's top texture fills the face, the side
                // texture wraps the rim.
                .blockstate((ctx, prov) -> padBlockstate(ctx, prov, side, top))
                // ExpanderItem carries the 1.12 shift tooltip (capacity/slots and flavour).
                .item(ExpanderItem::new).build()
                .register();
    }
}
