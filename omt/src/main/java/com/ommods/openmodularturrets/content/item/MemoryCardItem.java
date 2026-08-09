package com.ommods.openmodularturrets.content.item;

import com.ommods.omlib.util.MachineMode;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Turret Memory Card, faithful to 1.12 {@code UsableMetaItem} meta 2: sneak right click a turret
 * base to store its settings on the card, right click another base to apply them, sneak right click
 * into the air to wipe the card. The base interaction lives in {@code TurretBaseBlock.use}; this item
 * only holds the stored data and the wipe.
 */
public class MemoryCardItem extends Item {

    private static final String DATA_TAG = "data";

    public MemoryCardItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean hasData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(DATA_TAG);
    }

    @Nullable
    public static CompoundTag getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(DATA_TAG) ? tag.getCompound(DATA_TAG) : null;
    }

    public static void setData(ItemStack stack, CompoundTag data) {
        stack.getOrCreateTag().put(DATA_TAG, data);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Sneak-use away from a base wipes the card, matching 1.12's keySet.clear().
        if (player.isShiftKeyDown() && hasData(stack)) {
            stack.setTag(null);
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("Memory card wiped"));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return super.use(level, player, hand);
    }

    /**
     * The 1.12 UsableMetaItem tooltip: the three usage lines, then the stored settings readout
     * (range, mode, multi-targeting, attack flags, camo block) when the card carries data.
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal("Shift-RightClick base stores data"));
        tooltip.add(Component.literal("Shift-RightClick anywhere else to clear"));
        tooltip.add(Component.literal("RightClick base copies data from card"));
        CompoundTag data = getData(stack);
        if (data == null) {
            return;
        }
        tooltip.add(stored("Range", String.valueOf(data.getInt("currentMaxRange"))));
        tooltip.add(stored("Mode", MachineMode.byOrdinal(data.getInt("mode")).label()));
        tooltip.add(Component.literal("Multi-Targeting: ").withStyle(ChatFormatting.GOLD)
                .append(yesNo(data.getBoolean("multiTargeting"))));
        tooltip.add(Component.literal("Attack Mobs: ").withStyle(ChatFormatting.GOLD)
                .append(yesNo(data.getBoolean("attacksMobs"))));
        tooltip.add(Component.literal("Attack Neutrals: ").withStyle(ChatFormatting.GOLD)
                .append(yesNo(data.getBoolean("attacksNeutrals"))));
        tooltip.add(Component.literal("Attack Players: ").withStyle(ChatFormatting.GOLD)
                .append(yesNo(data.getBoolean("attacksPlayers"))));
        if (data.contains("camoSettings")) {
            BlockState camo = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),
                    data.getCompound("camoSettings"));
            tooltip.add(Component.literal("Camo: ").withStyle(ChatFormatting.GOLD)
                    .append(camo.getBlock().getName().withStyle(ChatFormatting.AQUA)));
        }
    }

    private static Component stored(String label, String value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(value).withStyle(ChatFormatting.AQUA));
    }

    private static Component yesNo(boolean value) {
        return Component.literal(value ? "yes" : "no")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
