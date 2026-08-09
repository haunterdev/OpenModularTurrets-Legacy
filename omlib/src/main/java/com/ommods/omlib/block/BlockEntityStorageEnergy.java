package com.ommods.omlib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public abstract class BlockEntityStorageEnergy extends OMBlockEntity implements IItemHandler {
    private static final String ENERGY_TAG = "energyStorage";

    protected ItemStackHandler inventory;
    protected EnergyStorage energyStorage;

    private final LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> this);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    public BlockEntityStorageEnergy(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState,
                                    EnergyStorage energyStorage) {
        super(pType, pPos, pBlockState);
        this.energyStorage = energyStorage;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY_TAG)) {
            this.energyStorage.deserializeNBT(tag.get(ENERGY_TAG));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ENERGY_TAG, this.energyStorage.serializeNBT());
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < inventory.getSlots();
    }

    @Override
    public abstract int getSlots();

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        // This delegates to the inventory. The 1.19.2 base called itself here.
        return isValidSlot(slot) ? inventory.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        // IItemHandler requires that the incoming stack is never mutated and the remainder is
        // returned. The 1.19.2 base mutated the caller's stack even when simulating.
        if (!isValidSlot(slot) || !isItemValid(slot, stack)) {
            return stack;
        }
        return inventory.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isValidSlot(slot)) {
            return ItemStack.EMPTY;
        }
        return inventory.extractItem(slot, amount, simulate);
    }

    @Override
    public abstract int getSlotLimit(int slot);

    @Override
    public abstract boolean isItemValid(int slot, @NotNull ItemStack stack);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        } else if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
        energyCapability.invalidate();
    }
}
