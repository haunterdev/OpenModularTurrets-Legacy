package com.ommods.ompd.content;

import com.ommods.ompd.OMPD;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registers the hardened blocks, walls, fences, camouflage trap and their block entities. */
public final class OMPDBlocks {
    public static final Map<DefenseTier, BlockEntry<HardenedBlock>> HARDENED =
            new EnumMap<>(DefenseTier.class);
    public static final Map<DefenseTier, BlockEntry<DefenseWallBlock>> WALLS =
            new EnumMap<>(DefenseTier.class);
    public static final Map<DefenseTier, BlockEntry<DefenseFenceBlock>> FENCES =
            new EnumMap<>(DefenseTier.class);
    public static BlockEntry<CamoTrapBlock> CAMO_TRAP;
    public static BlockEntityEntry<OwnedDefenseBlockEntity> OWNED_DEFENSE_ENTITY;
    public static BlockEntityEntry<CamoTrapBlockEntity> CAMO_TRAP_ENTITY;

    private OMPDBlocks() {
    }

    public static void register(Registrate registrate) {
        for (DefenseTier tier : DefenseTier.values()) {
            registerHardened(registrate, tier);
            registerWall(registrate, tier);
            registerFence(registrate, tier);
        }
        registerCamoTrap(registrate);

        var entity = registrate.blockEntity("owned_defense", OwnedDefenseBlockEntity::new);
        for (DefenseTier tier : DefenseTier.values()) {
            entity.validBlock(HARDENED.get(tier));
            entity.validBlock(WALLS.get(tier));
            entity.validBlock(FENCES.get(tier));
        }
        OWNED_DEFENSE_ENTITY = entity.register();
        CAMO_TRAP_ENTITY = registrate.blockEntity("camo_trap", CamoTrapBlockEntity::new)
                .validBlock(CAMO_TRAP)
                .register();

        registrate.addRawLang("status.ownership",
                "You do not own this block, or you don't have permission to do that.");
    }

    private static void registerHardened(Registrate registrate, DefenseTier tier) {
        String name = tier.registryName("hardened");
        HARDENED.put(tier, registrate.block(name, properties -> new HardenedBlock(tier, properties))
                .initialProperties(() -> Blocks.STONE)
                .properties(properties -> properties
                        .strength(tier.hardness(), tier.blastResistance())
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops())
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().cubeAll(ctx.getName(),
                                ResourceLocation.fromNamespaceAndPath(OMPD.MOD_ID, "block/" + name))))
                .lang("Hardened Block (Tier " + tier.tier() + ")")
                .defaultLoot()
                .simpleItem()
                .register());
    }

    private static void registerWall(Registrate registrate, DefenseTier tier) {
        String name = tier.registryName("wall");
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(
                OMPD.MOD_ID, "block/" + tier.registryName("hardened"));
        WALLS.put(tier, registrate.block(name, properties -> new DefenseWallBlock(tier, properties))
                .initialProperties(() -> Blocks.COBBLESTONE_WALL)
                .properties(properties -> properties
                        .strength(tier.hardness(), tier.blastResistance())
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops())
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
                .blockstate((ctx, prov) -> prov.wallBlock(ctx.getEntry(), texture))
                .lang("Hardened Wall (Tier " + tier.tier() + ")")
                .defaultLoot()
                .item()
                .model((ctx, prov) -> prov.wallInventory(ctx.getName(), texture))
                .build()
                .register());
    }

    private static void registerFence(Registrate registrate, DefenseTier tier) {
        String name = tier.registryName("fence");
        ResourceLocation pane = ResourceLocation.fromNamespaceAndPath(OMPD.MOD_ID, "block/" + name);
        ResourceLocation edge = ResourceLocation.fromNamespaceAndPath(OMPD.MOD_ID, "block/fence_top");
        FENCES.put(tier, registrate.block(name, properties -> new DefenseFenceBlock(tier, properties))
                .initialProperties(() -> Blocks.IRON_BARS)
                .properties(properties -> properties
                        .strength(tier.hardness(), tier.blastResistance())
                        .sound(SoundType.STONE)
                        .requiresCorrectToolForDrops())
                .tag(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.NEEDS_IRON_TOOL)
                .blockstate((ctx, prov) -> prov.paneBlock(ctx.getEntry(), pane, edge))
                .lang("Fence (Tier " + tier.tier() + ")")
                .defaultLoot()
                .item()
                .model((ctx, prov) -> prov.panePost(ctx.getName(), pane, edge))
                .build()
                .register());
    }

    private static void registerCamoTrap(Registrate registrate) {
        CAMO_TRAP = registrate.block("camo_trap", CamoTrapBlock::new)
                .properties(properties -> properties
                        .mapColor(MapColor.WOOD)
                        .strength(0.0F, 0.0F)
                        .sound(SoundType.GRASS)
                        .requiresCorrectToolForDrops())
                .tag(BlockTags.MINEABLE_WITH_AXE, BlockTags.NEEDS_IRON_TOOL)
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().cubeAll(ctx.getName(),
                                ResourceLocation.fromNamespaceAndPath(OMPD.MOD_ID, "block/camo_trap"))))
                .lang("Camouflage Trap")
                .defaultLoot()
                .simpleItem()
                .register();
    }

    public static List<BlockEntry<? extends Block>> allItems() {
        List<BlockEntry<? extends Block>> entries = new ArrayList<>();
        entries.addAll(HARDENED.values());
        entries.addAll(WALLS.values());
        entries.addAll(FENCES.values());
        entries.add(CAMO_TRAP);
        return entries;
    }
}
