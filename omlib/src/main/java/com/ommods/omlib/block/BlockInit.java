package com.ommods.omlib.block;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Block registration.
 *
 * Registrate replaces the 1.12 GameRegistry plus InitHelper pattern and registers the block, its
 * item and its default model together.
 */
public class BlockInit {

    public static BlockEntry<CableBlock> NETWORK_CABLE;
    public static BlockEntityEntry<BlockEntityCable> NETWORK_CABLE_ENTITY;

    public static void register(Registrate registrate) {
        NETWORK_CABLE = registrate.object("network_cable")
                .block(CableBlock::new)
                .initialProperties(() -> Blocks.IRON_BLOCK)
                .properties(p -> p
                        .mapColor(MapColor.METAL)
                        .strength(1.0F)
                        .sound(SoundType.METAL))
                // The cable borrows the vanilla iron block texture instead of carrying art of its
                // own. Registrate's default model would look for omlib:block/network_cable, which
                // this mod does not ship, and the block would render as missing.
                .blockstate((ctx, prov) -> prov.simpleBlock(ctx.getEntry(),
                        prov.models().cubeAll(ctx.getName(),
                                new ResourceLocation("minecraft", "block/iron_block"))))
                .simpleItem()
                .register();

        NETWORK_CABLE_ENTITY = registrate.blockEntity("network_cable", BlockEntityCable::new)
                .validBlock(NETWORK_CABLE)
                .register();
    }

    private BlockInit() {
    }
}
