package com.ommods.openmodularturrets.content.item;

import com.ommods.openmodularturrets.content.turret.TurretUpgrades;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A turret upgrade item that goes in a base's upgrade slots (11, 12). In 1.12 these were a single
 * meta item; here each is its own item carrying the same {@code meta} the {@link TurretUpgrades}
 * getters key on (accuracy 0, efficiency 1, fire rate 2, range 3, scatter shot 4). The shift
 * tooltip is the 1.12 UpgradeMetaItem text.
 */
public class UpgradeItem extends Item {

    private final int meta;

    public UpgradeItem(Properties properties, int meta) {
        super(properties);
        this.meta = meta;
    }

    public int meta() {
        return meta;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (ClassicTooltips.hint(tooltip)) {
            return;
        }
        tooltip.add(Component.empty());
        tooltip.add(ClassicTooltips.label("Turret Upgrade"));
        tooltip.add(Component.empty());
        if (meta == TurretUpgrades.UP_SCATTER) {
            tooltip.add(Component.literal("Adds 2 projectiles to each shot, at the cost of"));
            tooltip.add(Component.literal("+2 ammo usage, decr. accuracy and increased RF usage."));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("Stacks up to 4 times."));
            tooltip.add(Component.empty());
            tooltip.add(ClassicTooltips.flavour("You just need to shoot more bullets."));
            return;
        }
        tooltip.add(Component.literal("For effects see tooltips of turrets."));
        tooltip.add(Component.literal("Stacks up to 4 times."));
        tooltip.add(Component.empty());
        switch (meta) {
            case TurretUpgrades.UP_ACCURACY -> {
                tooltip.add(ClassicTooltips.flavour("Good eye sniper,"));
                tooltip.add(ClassicTooltips.flavour("I'll shoot, you run."));
            }
            case TurretUpgrades.UP_EFFICIENCY -> tooltip.add(ClassicTooltips.flavour("Eureka!"));
            case TurretUpgrades.UP_FIRE_RATE ->
                    tooltip.add(ClassicTooltips.flavour("Time consumer, time consuming."));
            case TurretUpgrades.UP_RANGE ->
                    tooltip.add(ClassicTooltips.flavour("Will you pass me the fork?"));
            default -> {
            }
        }
    }
}
