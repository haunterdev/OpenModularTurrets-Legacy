package com.ommods.openmodularturrets.content.expander;

import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.item.OMTItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity for an expander, an attachment placed next to a turret base, faithful to 1.12
 * {@code Expander}. A power expander has no inventory; it only lends its {@link ExpanderStats#powerCapacity}
 * to the base it touches, which the base reads each tick. An inventory expander holds nine extra ammo
 * slots whose per-slot limit doubles per tier, and the base pulls ammo and reactor fuel from them the
 * same way it does its own slots. The link is pure adjacency: an expander finds its base among its six
 * neighbours, and destroys itself if none remains.
 */
public class ExpanderBlockEntity extends OMBlockEntity {

    private static final String INVENTORY_TAG = "inventory";

    private final boolean power;
    private final int tier;

    // Inventory expanders only. Nine ammo slots, per-slot limit ExpanderStats.inventorySlotLimit(tier).
    @Nullable
    private final ItemStackHandler inventory;
    @Nullable
    private final LazyOptional<IItemHandler> itemCap;

    public ExpanderBlockEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state) {
        super(beType, pos, state);
        if (state.getBlock() instanceof ExpanderBlock eb) {
            this.power = eb.isPower();
            this.tier = eb.tier();
        } else {
            this.power = false;
            this.tier = 1;
        }
        if (power) {
            this.inventory = null;
            this.itemCap = null;
        } else {
            this.inventory = new ItemStackHandler(ExpanderStats.EXPANDER_SLOTS) {
                @Override
                protected void onContentsChanged(int slot) {
                    setChanged();
                }

                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    // Same as the base's ammo slots: real ammo, plus redstone dust/block as reactor fuel.
                    return OMTItems.isAmmoItem(stack.getItem())
                            || stack.getItem() == Items.REDSTONE
                            || stack.getItem() == Blocks.REDSTONE_BLOCK.asItem();
                }

                @Override
                public int getSlotLimit(int slot) {
                    return ExpanderStats.inventorySlotLimit(tier);
                }
            };
            this.itemCap = LazyOptional.of(() -> inventory);
        }
    }

    public boolean isPower() {
        return power;
    }

    public int tier() {
        return tier;
    }

    /** The extra energy capacity this expander lends its base, or 0 for an inventory expander. */
    public int powerCapacity() {
        return power ? ExpanderStats.powerCapacity(tier) : 0;
    }

    @Nullable
    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** The turret base among this expander's six neighbours, or null if it has been removed. */
    @Nullable
    public TurretBaseTileEntity findBase() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(getBlockPos().relative(facing)) instanceof TurretBaseTileEntity base) {
                return base;
            }
        }
        return null;
    }

    // Every so often, an expander that has lost its base drops itself, matching 1.12's dropBlock tick.
    public static void serverTick(Level level, BlockPos pos, BlockState state, ExpanderBlockEntity be) {
        if (level.getGameTime() % 15L == 0L && be.findBase() == null) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (inventory != null && tag.contains(INVENTORY_TAG)) {
            inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (inventory != null) {
            tag.put(INVENTORY_TAG, inventory.serializeNBT());
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && itemCap != null) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (itemCap != null) {
            itemCap.invalidate();
        }
    }
}
