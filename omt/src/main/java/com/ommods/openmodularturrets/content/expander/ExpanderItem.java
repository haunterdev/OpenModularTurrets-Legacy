package com.ommods.openmodularturrets.content.expander;

import com.ommods.openmodularturrets.content.item.ClassicTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The block item for an expander, carrying the 1.12 ItemBlockExpander shift tooltip: what the
 * expander lends its base (flat RF capacity, or nine slots with the tier's stack cap) plus the
 * tier's flavour line, word for word from the 1.12 lang file.
 */
public class ExpanderItem extends BlockItem {

    private static final String[] POWER_FLAVOUR = {
            "Now with flavour!",
            "Kinetic expansion.",
            "There, their and they're.",
            "Can be used as a sinking device.",
            "Asymmetrical warfare.",
    };
    private static final String[] INV_FLAVOUR = {
            "A little goes a long way.",
            "Supplies!",
            "Hair, heir and hare.",
            "Can be used as a floatation device.",
            "Very space, much inventory.",
    };

    public ExpanderItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!(getBlock() instanceof ExpanderBlock expander)) {
            return;
        }
        if (ClassicTooltips.hint(tooltip)) {
            return;
        }
        int tier = expander.tier();
        tooltip.add(Component.empty());
        if (expander.isPower()) {
            tooltip.add(ClassicTooltips.label("Turret Base RF Expander"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(
                    "Adds " + ExpanderStats.powerCapacity(tier) + " RF capacity to a turret base."));
            tooltip.add(Component.empty());
            tooltip.add(ClassicTooltips.flavour(POWER_FLAVOUR[tier - 1]));
        } else {
            tooltip.add(ClassicTooltips.label("Turret Base Inventory Expander"));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Adds 9 slots of inventory that the turret base can use."));
            tooltip.add(Component.literal(
                    "Each slot can only accept a max stack size of " + ExpanderStats.inventorySlotLimit(tier)));
            tooltip.add(Component.empty());
            tooltip.add(ClassicTooltips.flavour(INV_FLAVOUR[tier - 1]));
        }
    }
}
