package com.ommods.openmodularturrets.content.block;

import com.ommods.openmodularturrets.content.turret.TurretType;
import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

import java.util.EnumMap;
import java.util.Map;

/**
 * OMT block registration. Each {@link TurretType} becomes its own block, but all of them share a
 * single block entity type (valid for every turret block) so the ticker and firing loop are written
 * once. Mirrors omlib's {@code BlockInit}: Registrate registers the block, its item and its default
 * model together.
 */
public class OMTBlockInit {

    public static final Map<TurretType, BlockEntry<TurretBaseBlock>> TURRETS = new EnumMap<>(TurretType.class);
    public static BlockEntityEntry<TurretBaseBlockEntity> TURRET_ENTITY;

    public static BlockEntry<EnergizerBlock> ENERGIZER;
    public static BlockEntityEntry<EnergizerBlockEntity> ENERGIZER_ENTITY;

    public static BlockEntry<LeverBlock> LEVER;
    public static BlockEntityEntry<LeverBlockEntity> LEVER_ENTITY;

    public static void register(Registrate registrate) {
        for (TurretType type : TurretType.values()) {
            TURRETS.put(type, registrate.object(type.registryName())
                    .block(p -> new TurretBaseBlock(p, type))
                    .lang(type.displayName())
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p
                            .mapColor(mapColor(type))
                            // The 1.12 default (turretBreakable=false): heads can't be mined or
                            // blown up, the base GUI's Drop turrets button is the removal path.
                            .strength(-1.0F, 600_000.0F)
                            .isValidSpawn((state, level, pos, entityType) -> false)
                            .sound(SoundType.METAL)
                            // Not a solid cube: it renders through the block entity renderer, so it
                            // must not occlude light (its own position would go black) or cull faces.
                            .noOcclusion())
                    // The item renders the 3D head via a BlockEntityWithoutLevelRenderer, so its
                    // model is builtin/entity (which marks it a custom renderer) and TurretHeadItem
                    // supplies the renderer on the client.
                    .item(TurretHeadItem::new)
                    // Unchecked because builtin/entity is a virtual model with no json file; the
                    // existence-checking withExistingParent would reject it at datagen.
                    .model((ctx, prov) -> prov.getBuilder(ctx.getName())
                            .parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(
                                    new ResourceLocation("builtin/entity"))))
                    .build()
                    .register());
        }

        // One entity type, valid for every turret head block. Named turret_head so it does not
        // collide with the turret_base entity in BaseInit; the collision bound the base tile to this
        // head renderer and crashed on render.
        var builder = registrate.blockEntity("turret_head", TurretBaseBlockEntity::new);
        for (TurretType type : TurretType.values()) {
            builder.validBlock(TURRETS.get(type));
        }
        TURRET_ENTITY = builder.register();

        ENERGIZER = registrate.object("energizer")
                .block(EnergizerBlock::new)
                // Not 1.12 content: our creative infinite power stand in, named so players do not
                // mistake it for a survival generator. EnergizerItem carries the matching tooltip.
                .lang("Creative Energy Cube")
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p
                        .mapColor(MapColor.COLOR_YELLOW)
                        // 1.12 BlockBaseAttachment: hardness 3, setResistance(3) = 1.8 in 1.20 units.
                        .strength(3.0F, 1.8F)
                        .isValidSpawn((state, level, pos, entityType) -> false)
                        .sound(SoundType.METAL))
                .item(EnergizerItem::new)
                .build()
                .register();

        ENERGIZER_ENTITY = registrate.blockEntity("energizer", EnergizerBlockEntity::new)
                .validBlock(ENERGIZER)
                .register();

        // The tier 1 hand crank. Drawn by a block entity renderer (the crank spins), so like the
        // turret heads its block model is never shown in world; the item shows the flat crank art.
        LEVER = registrate.object("lever_block")
                .block(LeverBlock::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p
                        .mapColor(MapColor.METAL)
                        // 1.12 LeverBlock: hardness 2, setResistance(15) = 9 in 1.20 units.
                        .strength(2.0F, 9.0F)
                        .isValidSpawn((state, level, pos, entityType) -> false)
                        .sound(SoundType.STONE)
                        .noOcclusion())
                .lang("Basic Turret Base Crank")
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().getBuilder(ctx.getName())
                                .texture("particle", new ResourceLocation("openmodularturrets", "block/lever_block"))))
                .item(LeverItem::new)
                // builtin/entity: the item is drawn by LeverItemRenderer (a BEWLR), like the turret
                // heads, so the icon is the 3D crank instead of a flat sheet of the model atlas.
                .model((ctx, prov) -> prov.getBuilder(ctx.getName())
                        .parent(new net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile(
                                new ResourceLocation("builtin/entity"))))
                .build()
                .register();

        LEVER_ENTITY = registrate.blockEntity("lever", LeverBlockEntity::new)
                .validBlock(LEVER)
                .register();
    }

    /** The block shown as the creative tab icon. */
    public static BlockEntry<TurretBaseBlock> iconTurret() {
        return TURRETS.get(TurretType.MACHINE_GUN);
    }

    private static MapColor mapColor(TurretType type) {
        return switch (type) {
            case LASER, RELATIVISTIC -> MapColor.COLOR_LIGHT_BLUE;
            case RAILGUN -> MapColor.COLOR_BLACK;
            case INCENDIARY, ROCKET -> MapColor.COLOR_RED;
            case GRENADE -> MapColor.COLOR_GREEN;
            default -> MapColor.METAL;
        };
    }

    private OMTBlockInit() {
    }
}
