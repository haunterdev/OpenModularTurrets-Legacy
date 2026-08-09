package com.ommods.openmodularturrets.content.base;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The block item for a turret base, carrying the 1.12 ItemBlockTurretBase shift tooltip: the energy
 * stats, the tier's modifier slots, and the flavour line, word for word from the 1.12 lang file.
 */
public class TurretBaseItem extends BlockItem {

    public TurretBaseItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!(getBlock() instanceof TurretBaseBlock baseBlock)) {
            return;
        }
        BaseTier tier = baseBlock.tier();
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.literal("Press ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Shift").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" for details.").withStyle(ChatFormatting.GRAY)));
            return;
        }
        tooltip.add(Component.literal("The core of a modular turret."));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("--Energy--").withStyle(ChatFormatting.AQUA));
        tooltip.add(stat("Max capacity", String.valueOf(tier.capacity())));
        tooltip.add(stat("Max IO", String.valueOf(tier.maxTransfer())));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("--Extras--").withStyle(ChatFormatting.GREEN));
        switch (tier.tier()) {
            case 1 -> tooltip.add(Component.literal("None").withStyle(ChatFormatting.GRAY));
            case 5 -> {
                tooltip.add(Component.literal("2x Addon slots").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("2x Upgrade slots").withStyle(ChatFormatting.GRAY));
            }
            default -> {
                tooltip.add(Component.literal("2x Addon slots").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("1x Upgrade slots").withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.empty());
        for (String line : flavour(tier)) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component stat(String label, String value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static String[] flavour(BaseTier tier) {
        return switch (tier.tier()) {
            case 1 -> new String[]{"There are trees, everywhere..."};
            case 2 -> new String[]{"I'll draw your blood."};
            case 3 -> new String[]{"I said good day sir!"};
            case 4 -> new String[]{"Press the attack!"};
            default -> new String[]{"I wonder if this thing", "can do a barrel-roll..."};
        };
    }
}
