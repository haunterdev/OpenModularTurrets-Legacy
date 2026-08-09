package com.ommods.openmodularturrets.content.block;

import com.ommods.omlib.api.network.IPowerExchangeTile;
import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.omlib.network.NetworkRegistry;
import com.ommods.omlib.network.OMLibNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A creative power source: it generates a large, steady amount of energy every tick and delivers it
 * to the network, so turrets wired to it over cables charge up. Faithful OMT powers turrets with
 * base addons (solar panel 10/tick, redstone reactor 1600/tick); this is a stand in strong enough to
 * see the energy move while the real addons and base tiers are ported.
 */
public class EnergizerBlockEntity extends OMBlockEntity implements IPowerExchangeTile {
    private static final String ENERGY_TAG = "energy";
    private static final int GEN_RATE = 1000;
    private static final int CAPACITY = 100_000;

    // maxReceive = GEN_RATE so the internal generate call can top it up; maxExtract = GEN_RATE so it
    // can hand a tick's worth to the network. It never accepts energy from outside (it is a producer
    // on the network, so the distributor only ever extracts from it).
    private final EnergyStorage energyStorage = new EnergyStorage(CAPACITY, GEN_RATE, GEN_RATE);
    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> energyStorage);

    @Nullable
    private OMLibNetwork network;

    public EnergizerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, EnergizerBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        // Generate: receiveEnergy respects capacity, so this tops up without overfilling. The
        // network's tick then extracts and distributes it to consumers.
        be.energyStorage.receiveEnergy(GEN_RATE, false);
    }

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public boolean requiresEnergy() {
        return false;
    }

    @Override
    public boolean deliversEnergy() {
        return true;
    }

    @Nullable
    @Override
    public OMLibNetwork getNetwork() {
        return network;
    }

    @Override
    public void setNetwork(@Nullable OMLibNetwork network) {
        this.network = network;
    }

    @Override
    public String getDeviceName() {
        return "energizer";
    }

    @Override
    public BlockPos getPosition() {
        return getBlockPos();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level level = getLevel();
        if (level != null && !level.isClientSide() && network == null) {
            NetworkRegistry.join(level, this);
        }
    }

    /** Called by the block when broken, so the rest of the network can re-form. */
    public void onRemoved() {
        Level level = getLevel();
        if (level == null || level.isClientSide() || network == null) {
            return;
        }
        OMLibNetwork previous = network;
        previous.removeDevice(this);
        this.network = null;
        NetworkRegistry.rebuild(level, previous);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(ENERGY_TAG)) {
            energyStorage.deserializeNBT(tag.get(ENERGY_TAG));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ENERGY_TAG, energyStorage.serializeNBT());
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }
}
