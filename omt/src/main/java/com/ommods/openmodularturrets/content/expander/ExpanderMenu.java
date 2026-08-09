package com.ommods.openmodularturrets.content.expander;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * The inventory expander's menu: a 3x3 ammo grid over the player inventory, ported from 1.12
 * {@code ExpanderInvContainer}. Slot coordinates match upstream so the ported GUI texture lines up.
 * Only ammo is accepted, and each slot's stack limit comes from the expander's tier via the handler.
 */
public class ExpanderMenu extends AbstractContainerMenu {

    private final ExpanderBlockEntity expander;

    /** Client side: the expander position rides in the buffer. */
    public ExpanderMenu(MenuType<?> type, int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(type, windowId, playerInventory, resolve(playerInventory, buf));
    }

    /** Server side and the shared build. */
    public ExpanderMenu(MenuType<?> type, int windowId, Inventory playerInventory, ExpanderBlockEntity expander) {
        super(type, windowId);
        this.expander = expander;

        // 3x3 ammo grid, expander slots 0-8, at the upstream coordinates (62,17).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(expander.getInventory(), col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }
        // Player inventory (3x9) and hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + col + row * 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static ExpanderBlockEntity resolve(Inventory playerInventory, FriendlyByteBuf buf) {
        return playerInventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof ExpanderBlockEntity e
                ? e : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return expander != null && !expander.isRemoved()
                && player.distanceToSqr(expander.getBlockPos().getX() + 0.5,
                expander.getBlockPos().getY() + 0.5, expander.getBlockPos().getZ() + 0.5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();
        int ammoSlots = ExpanderStats.EXPANDER_SLOTS;
        if (index < ammoSlots) {
            // From the expander into the player inventory.
            if (!moveItemStackTo(stackInSlot, ammoSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stackInSlot, 0, ammoSlots, false)) {
            // From the player inventory into the expander (rejects non-ammo).
            return ItemStack.EMPTY;
        }
        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
