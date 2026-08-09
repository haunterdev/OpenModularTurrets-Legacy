package com.ommods.openmodularturrets.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A turret ammo item with the 1.12 AmmoMetaItem shift tooltip: the blue "Turret Ammunition" label
 * and the per-round description line, word for word from the 1.12 lang file.
 */
public class AmmoItem extends Item {

    private final String description;

    public AmmoItem(Properties properties, String description) {
        super(properties);
        this.description = description;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (ClassicTooltips.hint(tooltip)) {
            return;
        }
        tooltip.add(Component.empty());
        tooltip.add(ClassicTooltips.label("Turret Ammunition"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(description));
    }
}
