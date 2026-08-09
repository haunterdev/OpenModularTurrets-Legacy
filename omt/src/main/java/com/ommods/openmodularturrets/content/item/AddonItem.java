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
 * A turret addon item that goes in a base's addon slots (9, 10). In 1.12 these were a single meta
 * item; here each is its own item carrying the same {@code meta} the {@link TurretUpgrades} getters
 * key on (concealer 0, damage amp 1, redstone reactor 4, solar panel 6). The shift tooltip is the
 * 1.12 AddonMetaItem text; the generator numbers are the compiled-in 1.12 config defaults.
 */
public class AddonItem extends Item {

    private final int meta;

    public AddonItem(Properties properties, int meta) {
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
        tooltip.add(ClassicTooltips.label("Turret Addon"));
        tooltip.add(Component.empty());
        switch (meta) {
            case TurretUpgrades.AD_CONCEALER -> {
                tooltip.add(Component.literal("Conceals turrets when they aren't shooting."));
                tooltip.add(Component.empty());
                tooltip.add(ClassicTooltips.flavour("DiggyDiggy Hole!"));
            }
            case TurretUpgrades.AD_DAMAGE_AMP -> {
                tooltip.add(Component.literal("Adds a few % of maxHP of target"));
                tooltip.add(Component.literal("(dep. on turret) as damage per projectile."));
                tooltip.add(Component.empty());
                tooltip.add(ClassicTooltips.flavour("Here we are juggernaught."));
            }
            case TurretUpgrades.AD_RECYCLER -> {
                tooltip.add(Component.literal("Adds a chance for a turret to not use ammo,"));
                tooltip.add(Component.literal("as well as a chance to generate ammo, per shot."));
                tooltip.add(Component.empty());
                tooltip.add(ClassicTooltips.flavour("Reduce, reuse, reassert."));
            }
            case TurretUpgrades.AD_REDSTONE -> {
                tooltip.add(Component.literal("Generates 1600 RF per"));
                tooltip.add(Component.literal("redstone dust in Ammo inventory."));
                tooltip.add(Component.empty());
                tooltip.add(ClassicTooltips.flavour("Sublimation, refined."));
            }
            case TurretUpgrades.AD_SOLAR -> {
                tooltip.add(Component.literal("Generates 10 RF/t in sunlight."));
                tooltip.add(Component.empty());
                tooltip.add(ClassicTooltips.flavour("Good Helios!"));
            }
            case TurretUpgrades.AD_FAKEDROPS -> {
                tooltip.add(Component.literal("Makes mobs killed by turrets"));
                tooltip.add(Component.literal("drop loot as if a player killed them."));
                tooltip.add(Component.empty());
                tooltip.add(ClassicTooltips.flavour("Turn on the swag!"));
            }
            default -> {
            }
        }
    }
}
