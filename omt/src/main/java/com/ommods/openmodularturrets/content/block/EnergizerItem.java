package com.ommods.openmodularturrets.content.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Creative Energy Cube's item. It is not 1.12 content: it is our creative power stand in, an
 * infinite source you wire to a turret base while the real power addons and tiers are ported. The
 * tooltip says so, so it is not mistaken for a survival generator.
 */
public class EnergizerItem extends BlockItem {

    public EnergizerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Creative infinite power source.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Wire it to a turret base to keep it charged.")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
