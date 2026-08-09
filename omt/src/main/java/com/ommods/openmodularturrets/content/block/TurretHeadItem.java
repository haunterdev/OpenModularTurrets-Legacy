package com.ommods.openmodularturrets.content.block;

import com.ommods.openmodularturrets.client.OMTClient;
import com.ommods.openmodularturrets.content.turret.TurretType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * The block item for a turret head. Overrides the client renderer so the inventory icon is the 3D
 * head model, not the flat texture sheet (initializeClient is only invoked on the client, so the
 * reference to the client renderer never loads on a dedicated server), and carries the 1.12 shift
 * tooltip: the stat block every ItemBlock*Turret printed, word for word from the 1.12 lang file.
 */
public class TurretHeadItem extends BlockItem {

    public TurretHeadItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!(getBlock() instanceof TurretBaseBlock headBlock)) {
            return;
        }
        TurretType type = headBlock.turretType();
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.literal("Press ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("Shift").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" for details.").withStyle(ChatFormatting.GRAY)));
            return;
        }
        int tier = type.requiredTier();
        tooltip.add(Component.literal("A turret head, place on a turret base."));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("--Info--").withStyle(ChatFormatting.GOLD));
        tooltip.add(stat("Tier", String.valueOf(tier)));
        tooltip.add(stat("Range (in blocks)", String.valueOf((int) type.range())));
        tooltip.add(stat("Accuracy", accuracyWord(type)));
        tooltip.add(stat("Ammo Type", ammoLabel(type)));
        tooltip.add(stat("Base Minimum Tier", "Tier " + tier + " Turret Base"));
        tooltip.add(Component.empty());
        tooltip.add(Component.literal("--Damage Output--").withStyle(ChatFormatting.DARK_PURPLE));
        // 1.12 divided the config damage by two for a hearts readout. (Upstream's grenade tooltip
        // read the laser's damage by a copy-paste slip; this shows each turret's own number.)
        tooltip.add(stat("Projectile Damage", type.damage() / 2.0F + " Hearts"));
        tooltip.add(stat("AOE Radius", String.valueOf(aoeRadius(type))));
        tooltip.add(stat("Shots/s", shotsPerSecond(type)));
        tooltip.add(stat("Energy usage per shot", type.energyPerShot() + " RF"));
        tooltip.add(Component.empty());
        for (String line : flavour(type)) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component stat(String label, String value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static String shotsPerSecond(TurretType type) {
        float perSecond = 20.0F / type.fireInterval();
        // The 1.12 DecimalFormat("#.##") readout.
        String text = String.format(java.util.Locale.ROOT, "%.2f", perSecond);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    // The per-turret tooltip words each 1.12 ItemBlock*Turret hardcoded.

    private static String accuracyWord(TurretType type) {
        return switch (type) {
            case DISPOSABLE -> "Low";
            case POTATO_CANNON, MACHINE_GUN, INCENDIARY, GRENADE -> "Medium";
            case RELATIVISTIC, TELEPORTER, LASER -> "High";
            case ROCKET, RAILGUN -> "Pin Point";
        };
    }

    private static String ammoLabel(TurretType type) {
        return switch (type) {
            case DISPOSABLE -> "See config file, default: cobble/planks";
            case POTATO_CANNON -> "Potato";
            case MACHINE_GUN -> "Bullet";
            case INCENDIARY -> "Blazing Clay";
            case GRENADE -> "Grenade";
            case ROCKET -> "Homing Rocket";
            case RAILGUN -> "Ferro-Magnetic Slugs";
            case RELATIVISTIC, TELEPORTER, LASER -> "None";
        };
    }

    private static int aoeRadius(TurretType type) {
        return switch (type) {
            case INCENDIARY, ROCKET -> 5;
            case GRENADE -> 3;
            default -> 0;
        };
    }

    private static String[] flavour(TurretType type) {
        return switch (type) {
            case DISPOSABLE -> new String[]{"Poor man's sleeping aid."};
            case MACHINE_GUN -> new String[]{"TRRRAA!! TRRAATRRAA!!"};
            case GRENADE -> new String[]{"Another example is the inductive", "form of the horse paradox."};
            case ROCKET -> new String[]{"Red mist."};
            case LASER -> new String[]{"Go ahead, Newton doesn't care."};
            case RAILGUN -> new String[]{"BFG."};
            case POTATO_CANNON -> new String[]{"A PVC masterpiece."};
            case INCENDIARY -> new String[]{"Flaming Disjuncture."};
            case RELATIVISTIC -> new String[]{"Point of reference."};
            case TELEPORTER -> new String[]{"It's like long strands of spaghetti,", "but in 11 dimensions."};
        };
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return OMTClient.turretItemRenderer();
            }
        });
    }
}
