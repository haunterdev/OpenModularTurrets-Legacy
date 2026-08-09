package com.ommods.omlib.block;

import com.ommods.omlib.util.OMPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.energy.EnergyStorage;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the energy and saving half of BlockEntityStorageEnergy.
 *
 * The inventory half is deliberately not covered here, and cannot be. ItemStack's static
 * initializer builds a codec against the item registry, so touching ItemStack.EMPTY, or
 * constructing an ItemStackHandler, throws ExceptionInInitializerError. Bootstrapping vanilla with
 * Bootstrap.bootStrap() does not help either: it reaches NetworkHooks.init() and fails because FML
 * is not running under plain JUnit. The inventory belongs in a Forge GameTest, and there is no
 * concrete subclass with an inventory yet to write one against.
 */
class BlockEntityStorageEnergyTest {

    /**
     * Minimal concrete subclass. BlockEntity tolerates a null type and state in unit tests.
     *
     * inventory stays null on purpose: allocating an ItemStackHandler would drag in ItemStack and
     * break every test in this class. Nothing here calls a slot method.
     */
    static class TestStorage extends BlockEntityStorageEnergy {
        TestStorage(EnergyStorage energy) {
            super(null, BlockPos.ZERO, null, energy);
        }

        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    @Test
    void energyStorageIsExposed() {
        TestStorage te = new TestStorage(new EnergyStorage(1000, 100, 100, 250));
        assertEquals(250, te.getEnergyStorage().getEnergyStored());
    }

    @Test
    void energyAndOwnerSurviveSaveAndLoad() {
        TestStorage saved = new TestStorage(new EnergyStorage(1000, 100, 100, 500));
        saved.setOwner(new OMPlayer(UUID.randomUUID(), "owner", "test"));

        CompoundTag tag = new CompoundTag();
        // saveAdditional is the hook vanilla's chunk save actually calls. The 1.19.2 base wrote
        // this data from serializeNBT, which vanilla never calls on save, so it never reached disk.
        saved.saveAdditional(tag);

        TestStorage loaded = new TestStorage(new EnergyStorage(1000, 100, 100, 0));
        loaded.load(tag);

        assertEquals(500, loaded.getEnergyStorage().getEnergyStored());
        assertNotNull(loaded.getOwner());
        assertEquals("owner", loaded.getOwner().getName());
    }

    @Test
    void savingWithoutAnOwnerDoesNotThrow() {
        TestStorage te = new TestStorage(new EnergyStorage(1000));
        CompoundTag tag = new CompoundTag();

        // Upstream's serializeNBT dereferenced a null owner here.
        assertDoesNotThrow(() -> te.saveAdditional(tag));
        assertDoesNotThrow(() -> new TestStorage(new EnergyStorage(1000)).load(tag));
    }

    @Test
    void loadingWithoutAnOwnerTagLeavesNoOwner() {
        TestStorage te = new TestStorage(new EnergyStorage(1000, 100, 100, 0));
        te.setOwner(new OMPlayer(UUID.randomUUID(), "stale", "test"));

        te.load(new CompoundTag());

        assertNull(te.getOwner());
    }
}
