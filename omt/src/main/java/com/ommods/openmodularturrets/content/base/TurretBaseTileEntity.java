package com.ommods.openmodularturrets.content.base;

import com.ommods.omlib.api.network.IPowerExchangeTile;
import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.omlib.network.NetworkRegistry;
import com.ommods.omlib.network.OMLibNetwork;
import com.ommods.omlib.util.MachineMode;
import com.ommods.omlib.util.permission.IHasTrustManager;
import com.ommods.omlib.util.permission.TrustManager;
import com.ommods.openmodularturrets.config.OMTConfig;
import com.ommods.openmodularturrets.content.expander.ExpanderBlockEntity;
import com.ommods.openmodularturrets.content.item.OMTItems;
import com.ommods.openmodularturrets.content.turret.AmmoType;
import com.ommods.openmodularturrets.content.turret.TurretTargeting.Flags;
import com.ommods.openmodularturrets.content.turret.TurretUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The turret base: the block you place first and attach turret heads to. It owns the shared
 * resources a head draws on - energy, the ammo and addon inventory, the owner and trusted players,
 * and the attack toggles - faithful to 1.12 TurretBase. It is a network power consumer so it charges
 * from cables and generators.
 *
 * Slots 0-8 are ammo, 9-12 are addons/upgrades (concealment, solar, redstone reactor, damage amp),
 * matching the 13 slot 1.12 layout. Heads read their firing resources from here; the base itself
 * does not fire.
 */
public class TurretBaseTileEntity extends OMBlockEntity implements IPowerExchangeTile, IHasTrustManager {
    private static final String ENERGY_TAG = "energy";
    private static final String INVENTORY_TAG = "inventory";
    private static final String TRUST_TAG = "trust";
    private static final String ATTACKS_MOBS_TAG = "attacksMobs";
    private static final String ATTACKS_NEUTRALS_TAG = "attacksNeutrals";
    private static final String ATTACKS_PLAYERS_TAG = "attacksPlayers";

    public static final int AMMO_SLOTS = 9;
    public static final int ADDON_SLOTS = 4;

    // Modifier slot indices in the base inventory, faithful to 1.12: 9/10 addons, 11/12 upgrades.
    public static final int ADDON_SLOT_A = 9;
    public static final int ADDON_SLOT_B = 10;
    public static final int UPGRADE_SLOT_A = 11;
    public static final int UPGRADE_SLOT_B = 12;

    // Most modifiers cap at four per slot in 1.12 (UpgradeSlot and AddonSlot both return 4).
    private static final int MODIFIER_STACK_LIMIT = 4;

    // Addon generation per server tick (1.12 config defaults).
    private static final int SOLAR_GEN = 10;
    private static final int REDSTONE_GEN = 1600;

    private final BaseTier tier;
    private final BaseEnergyStorage energyStorage;
    private final ItemStackHandler inventory = new ItemStackHandler(AMMO_SLOTS + ADDON_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        /**
         * Per-slot stack caps, from the 1.12 container slots. {@code UpgradeSlot.getSlotStackLimit}
         * returns 4, so four of an upgrade fit per slot. {@code AddonSlot.getItemStackLimit} returns 4
         * only for addon metas 1 and 7 (damage amp, fake drops, the two that stack in levels) and 1 for
         * every other addon. Without these an addon or upgrade slot took a full stack.
         */
        @Override
        protected int getStackLimit(int slot, @NotNull ItemStack stack) {
            if (slot == UPGRADE_SLOT_A || slot == UPGRADE_SLOT_B) {
                return Math.min(MODIFIER_STACK_LIMIT, stack.getMaxStackSize());
            }
            if (slot == ADDON_SLOT_A || slot == ADDON_SLOT_B) {
                return OMTItems.isLevelledAddon(stack.getItem())
                        ? Math.min(MODIFIER_STACK_LIMIT, stack.getMaxStackSize())
                        : 1;
            }
            return super.getStackLimit(slot, stack);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Slots 0-8 take ammo; 9-10 take addons; 11-12 take upgrades, matching the 1.12 layout.
            if (slot < AMMO_SLOTS) {
                // Ammo, plus redstone dust/block as fuel for the redstone-reactor addon.
                return OMTItems.isAmmoItem(stack.getItem()) || isReactorFuel(stack.getItem());
            }
            if (slot == ADDON_SLOT_A || slot == ADDON_SLOT_B) {
                return OMTItems.isAddonItem(stack.getItem());
            }
            return OMTItems.isUpgradeItem(stack.getItem());
        }
    };
    private final TrustManager trustManager = new TrustManager();

    private final LazyOptional<IEnergyStorage> energyCap;
    // Automation sees the nine ammo slots only, never the addon or upgrade slots, matching 1.12
    // TurretBase.getCapabilityInventory: new RangedWrapper(inventory, 0, 9).
    private final LazyOptional<IItemHandler> itemCap =
            LazyOptional.of(() -> new RangedWrapper(inventory, 0, AMMO_SLOTS));

    // Attack toggles, faithful defaults: mobs on, neutrals and players off.
    private boolean attacksMobs = true;
    private boolean attacksNeutrals = false;
    private boolean attacksPlayers = false;

    // Kill bookkeeping and the target-spread toggle, faithful to the 1.12 TurretBase: kills count
    // every turret kill, playerKills the player subset; multiTargeting on makes sibling heads pick
    // different targets instead of focusing one.
    private int kills = 0;
    private int playerKills = 0;
    private boolean multiTargeting = false;

    // Redstone control, faithful to 1.12 TileEntityMachine: the mode decides whether the base's
    // turrets run given the redstone input; INVERTED (the 1.12 default) means a signal switches
    // them off.
    private MachineMode mode = MachineMode.INVERTED;
    private boolean redstone = false;

    // The player-adjustable range cap, faithful to 1.12 TurretBase: the upper bound is the longest
    // reach of any attached head (range plus upgrades), refreshed every five ticks; the current cap
    // follows it unless the GUI's +/- buttons overrode it.
    private int currentMaxRange = 0;
    private int upperBoundMaxRange = 0;
    private boolean rangeOverridden = false;
    private boolean updateRange = false;
    private int ticks = 0;

    // Camouflage, faithful to 1.12 ICamoSupport: the owner right-clicks the base with a full opaque
    // block to disguise it, sneak plus an empty hand resets. Null means the base's own look; the
    // client baked model reads it through this model data property.
    public static final ModelProperty<BlockState> CAMO_PROPERTY = new ModelProperty<>();
    @Nullable
    private BlockState camoState;

    @Nullable
    private OMLibNetwork network;

    public TurretBaseTileEntity(BlockEntityType<?> beType, BlockPos pos, BlockState state) {
        super(beType, pos, state);
        this.tier = state.getBlock() instanceof TurretBaseBlock tb ? tb.tier() : BaseTier.TIER1;
        // A base receives power but never exports it (maxExtract 0); heads spend it directly. Addons
        // inject power past the transfer cap via BaseEnergyStorage.generate.
        this.energyStorage = new BaseEnergyStorage(tier.capacity(), tier.maxTransfer());
        this.energyCap = LazyOptional.of(() -> energyStorage);
    }

    public BaseTier tier() {
        return tier;
    }

    public BaseEnergyStorage getEnergyStorageImpl() {
        return energyStorage;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    /** The player-adjustable range cap heads must respect, faithful to 1.12 getCurrentMaxRange. */
    public int getCurrentMaxRange() {
        return currentMaxRange;
    }

    /** The longest reach any attached head allows, the +/- buttons' ceiling. */
    public int getUpperBoundMaxRange() {
        return upperBoundMaxRange;
    }

    /**
     * Set the range cap from the GUI's +/- buttons, marking it overridden so the tick loop stops
     * snapping it back to the upper bound. Clamped to [0, upper bound], exactly as 1.12.
     */
    public void setCurrentMaxRange(int value) {
        currentMaxRange = Math.max(0, Math.min(value, upperBoundMaxRange));
        rangeOverridden = true;
        setChanged();
    }

    /**
     * The 1.12 setUpdateRange hook: placing a head that outreaches the current cap raises the cap
     * to the refreshed upper bound on the next tick pass, even over a player override.
     */
    public void setUpdateRange() {
        updateRange = true;
    }

    public MachineMode getMode() {
        return mode;
    }

    /** Cycle to the next redstone mode, the GUI Mode button's action (1.12 toggleMode). */
    public void toggleMode() {
        mode = mode.next();
        setChanged();
    }

    /** Called from the block on neighbor changes so the mode sees the live redstone input. */
    public void setRedstone(boolean value) {
        if (redstone != value) {
            redstone = value;
            setChanged();
        }
    }

    /** Whether the base's turrets run right now, from the mode and the redstone input. */
    public boolean isActive() {
        return mode.active(redstone);
    }

    /** The block this base is disguised as, or null for its own look. */
    @Nullable
    public BlockState getCamoState() {
        return camoState;
    }

    /** Disguise the base as the given block state (null resets), pushing the change to clients. */
    public void setCamoState(@Nullable BlockState state) {
        this.camoState = state;
        setChanged();
        syncToClient();
    }

    /** Hands the camo state to the client baked model. */
    @Override
    public @NotNull ModelData getModelData() {
        return camoState != null
                ? ModelData.builder().with(CAMO_PROPERTY, camoState).build()
                : ModelData.EMPTY;
    }

    /** After a server-pushed update on the client, refresh the camo model and redraw the block. */
    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(net, packet);
        Level level = getLevel();
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 8);
        }
    }

    // --- Upgrade/addon readout, from the modifier slots (9-12). Heads ask the base for these so all
    // the slot bookkeeping stays here; the maths lives in the pure TurretUpgrades.

    @Nullable
    private TurretUpgrades.Slot slot(int index) {
        return OMTItems.slotFor(inventory.getStackInSlot(index));
    }

    private TurretUpgrades.Slot up11() {
        return slot(UPGRADE_SLOT_A);
    }

    private TurretUpgrades.Slot up12() {
        return slot(UPGRADE_SLOT_B);
    }

    private TurretUpgrades.Slot addon9() {
        return slot(ADDON_SLOT_A);
    }

    private TurretUpgrades.Slot addon10() {
        return slot(ADDON_SLOT_B);
    }

    /** Extra range in blocks from range upgrades. */
    public int upgradeRangeBonus() {
        return TurretUpgrades.rangeBonus(tier.tier(), up11(), up12());
    }

    /** Number of extra shots per volley from scatter-shot upgrades. */
    public int scattershot() {
        return TurretUpgrades.scattershot(tier.tier(), up11(), up12());
    }

    public double accuracyUpgrades() {
        return TurretUpgrades.accuracyUpgrades(tier.tier(), up11(), up12());
    }

    public double efficiencyUpgrades() {
        return TurretUpgrades.efficiencyUpgrades(tier.tier(), up11(), up12());
    }

    public double fireRateUpgrades() {
        return TurretUpgrades.fireRateUpgrades(tier.tier(), up11(), up12());
    }

    /** Total damage-amp addon level driving the health-based bonus damage. */
    public int ampLevel() {
        return TurretUpgrades.ampLevel(tier.tier(), addon9(), addon10());
    }

    /**
     * True if this base's turrets retract while idle: a concealer addon rides an addon slot, or the
     * server turned concealment on for everyone (1.12 {@code canTurretsConcealWithoutAddon}, which
     * hasConcealmentAddon ORed in the same way).
     */
    public boolean shouldConcealTurrets() {
        return TurretUpgrades.hasConcealment(tier.tier(), addon9(), addon10())
                || OMTConfig.Turrets.canTurretsConcealWithoutAddon;
    }

    /** True if a damage amp addon rides an addon slot. Read by the renderer to draw its rod rack. */
    public boolean hasDamageAmpAddon() {
        return TurretUpgrades.hasDamageAmp(tier.tier(), addon9(), addon10());
    }

    /** True if a solar panel addon rides an addon slot. Read by the renderer to draw the panel. */
    public boolean hasSolarPanelAddon() {
        return TurretUpgrades.hasSolar(tier.tier(), addon9(), addon10());
    }

    /** True if a redstone reactor addon rides an addon slot. Read by the renderer to draw it. */
    public boolean hasRedstoneReactorAddon() {
        return TurretUpgrades.hasRedstoneReactor(tier.tier(), addon9(), addon10());
    }

    /** True if a recycler addon rides an addon slot, giving each shot a chance to keep its ammo. */
    public boolean hasRecycler() {
        return TurretUpgrades.hasRecycler(tier.tier(), addon9(), addon10());
    }

    /** Fake drops addon level: -1 for none, else the 0-3 looting level for player-credited kills. */
    public int fakeDropsLevel() {
        return TurretUpgrades.fakeDropsLevel(tier.tier(), addon9(), addon10());
    }

    /**
     * False when a Loot Deleter attachment touches this base, faithful to 1.12
     * baseHasNoLootDeleter: mobs killed by this base's turrets then drop nothing.
     */
    public boolean dropsLoot() {
        Level level = getLevel();
        if (level == null) {
            return true;
        }
        for (Direction facing : Direction.values()) {
            if (level.getBlockState(getBlockPos().relative(facing))
                    .getBlock() instanceof com.ommods.openmodularturrets.content.expander.LootDeleterBlock) {
                return false;
            }
        }
        return true;
    }

    /**
     * Takes one unit of the given ammo from the ammo slots. Returns the consumed stack if the head may
     * fire (empty for an energy-only NONE head, which consumes nothing), or null if the required ammo
     * is missing so the head must hold fire. {@link AmmoType#ANY} takes whatever is loaded, matching
     * the disposable turret; the returned stack is what a shot like the disposable's throws.
     */
    @Nullable
    public ItemStack consumeAmmo(AmmoType ammo) {
        if (ammo == AmmoType.NONE) {
            return ItemStack.EMPTY;
        }
        if (ammo == AmmoType.ANY) {
            for (int slot = 0; slot < AMMO_SLOTS; slot++) {
                if (isThrowableJunk(inventory.getStackInSlot(slot))) {
                    return takeOne(inventory, slot);
                }
            }
            return takeFromExpanders(null);
        }
        Item needed = OMTItems.itemFor(ammo);
        if (needed == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < AMMO_SLOTS; slot++) {
            if (inventory.getStackInSlot(slot).getItem() == needed) {
                return takeOne(inventory, slot);
            }
        }
        return takeFromExpanders(needed);
    }

    /**
     * Take one round from the given slot, applying the recycler addon when present. The 1.12 numbers
     * (recyclerNegateChance 0.1, recyclerAddChance 0.05, the same in every TurretSetting): a 10 percent
     * chance the round is not consumed, and a further 5 percent chance a bonus round is added on top.
     * (1.12 rolled nextFloat()+0.5 against these bounds, which could never trigger; this port rolls
     * nextFloat() so the addon does what its config and tooltip say.)
     */
    private ItemStack takeOne(ItemStackHandler handler, int slot) {
        Level level = getLevel();
        if (hasRecycler() && level != null) {
            float roll = level.random.nextFloat();
            ItemStack round = new ItemStack(handler.getStackInSlot(slot).getItem());
            if (roll < 0.10f) {
                return round;
            }
            if (roll < 0.15f) {
                handler.insertItem(slot, round.copy(), false);
                return round;
            }
        }
        return handler.extractItem(slot, 1, false);
    }

    /**
     * Take one matching item from a touching inventory expander's ammo slots, faithful to 1.12's
     * getSpecificItemFromInvExpanders. {@code needed == null} takes whatever ammo is loaded, matching
     * getDisposableAmmoFromInvExpander. Returns the consumed stack, or null if none of the six
     * neighbours holds it.
     */
    @Nullable
    private ItemStack takeFromExpanders(@Nullable Item needed) {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        for (Direction facing : Direction.values()) {
            if (!(level.getBlockEntity(getBlockPos().relative(facing)) instanceof ExpanderBlockEntity exp)) {
                continue;
            }
            ItemStackHandler inv = exp.getInventory();
            if (inv == null) {
                continue;
            }
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                ItemStack stack = inv.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                if (needed == null ? isThrowableJunk(stack) : stack.getItem() == needed) {
                    // The recycler addon applies to expander-fed ammo too, faithful to 1.12's
                    // deductFromInvExpander running the same chance block.
                    return takeOne(inv, slot);
                }
            }
        }
        return null;
    }

    /**
     * The base's attack toggles, each vetoed by its server-wide config switch. 1.12 tested
     * {@code base.isAttacksMobs() && OMTConfig.TURRETS.globalCanTargetMobs} (and the same for the
     * other two) at every targeting site, so a server can switch a whole category off without
     * touching anybody's base. The GUI still shows the player's own setting, as upstream did.
     */
    public Flags attackFlags() {
        return new Flags(attacksMobs && OMTConfig.Turrets.globalCanTargetMobs,
                attacksNeutrals && OMTConfig.Turrets.globalCanTargetNeutrals,
                attacksPlayers && OMTConfig.Turrets.globalCanTargetPlayers);
    }

    /**
     * The settings a Turret Memory Card copies, faithful to 1.12 writeMemoryCardNBT: the attack
     * toggles, the trusted player list, the redstone mode, multi-targeting, and the range cap (the
     * "range" key rides along only when the cap was overridden, exactly as upstream). Camo settings
     * are the one 1.12 payload item with no port equivalent.
     */
    public CompoundTag writeMemoryCard() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(ATTACKS_MOBS_TAG, attacksMobs);
        tag.putBoolean(ATTACKS_NEUTRALS_TAG, attacksNeutrals);
        tag.putBoolean(ATTACKS_PLAYERS_TAG, attacksPlayers);
        tag.putBoolean("multiTargeting", multiTargeting);
        tag.putInt("mode", mode.ordinal());
        tag.putInt("currentMaxRange", currentMaxRange);
        if (rangeOverridden) {
            tag.putInt("range", currentMaxRange);
        }
        if (camoState != null) {
            tag.put("camoSettings", NbtUtils.writeBlockState(camoState));
        }
        tag.put(TRUST_TAG, trustManager.writeToNBT(new CompoundTag()));
        return tag;
    }

    /** Apply a memory card's stored settings to this base, faithful to 1.12 readMemoryCardNBT. */
    public void readMemoryCard(CompoundTag tag) {
        attacksMobs = tag.getBoolean(ATTACKS_MOBS_TAG);
        attacksNeutrals = tag.getBoolean(ATTACKS_NEUTRALS_TAG);
        attacksPlayers = tag.getBoolean(ATTACKS_PLAYERS_TAG);
        multiTargeting = tag.getBoolean("multiTargeting");
        mode = tag.contains("mode") ? MachineMode.byOrdinal(tag.getInt("mode")) : MachineMode.INVERTED;
        if (tag.contains("camoSettings")) {
            camoState = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("camoSettings"));
        }
        if (tag.contains("range")) {
            rangeOverridden = true;
            currentMaxRange = tag.getInt("range");
        } else {
            rangeOverridden = false;
            currentMaxRange = upperBoundMaxRange;
        }
        if (tag.contains(TRUST_TAG)) {
            trustManager.readFromNBT(tag.getCompound(TRUST_TAG));
        }
        setChanged();
        syncToClient();
    }

    public boolean isAttacksMobs() {
        return attacksMobs;
    }

    public boolean isAttacksNeutrals() {
        return attacksNeutrals;
    }

    public boolean isAttacksPlayers() {
        return attacksPlayers;
    }

    public void setAttacksMobs(boolean value) {
        this.attacksMobs = value;
        setChanged();
    }

    public void setAttacksNeutrals(boolean value) {
        this.attacksNeutrals = value;
        setChanged();
    }

    public void setAttacksPlayers(boolean value) {
        this.attacksPlayers = value;
        setChanged();
    }

    public int getKills() {
        return kills;
    }

    public int getPlayerKills() {
        return playerKills;
    }

    public void increaseKillCounter() {
        kills++;
        setChanged();
    }

    public void increasePlayerKillCounter() {
        playerKills++;
        setChanged();
    }

    public boolean isMultiTargeting() {
        return multiTargeting;
    }

    public void setMultiTargeting(boolean value) {
        this.multiTargeting = value;
        setChanged();
    }

    @Override
    public TrustManager getTrustManager() {
        return trustManager;
    }

    // --- IPowerExchangeTile: the base is the network's turret consumer.

    @Override
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    public boolean requiresEnergy() {
        return true;
    }

    @Override
    public boolean deliversEnergy() {
        return false;
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
        return tier.registryName();
    }

    @Override
    public BlockPos getPosition() {
        return getBlockPos();
    }

    // --- Addon power generation, run once per base per server tick from the base block's ticker.

    public static void serverTick(Level level, BlockPos pos, BlockState state, TurretBaseTileEntity be) {
        // Every five ticks, follow 1.12's cadence for the range bookkeeping: recompute the upper
        // bound from the attached heads, clamp the current cap to it, and track it while the player
        // has not overridden the cap from the GUI.
        if (++be.ticks % 5 == 0) {
            be.ticks = 0;
            be.recomputeUpperBoundRange(level, pos);
            if (be.currentMaxRange > be.upperBoundMaxRange) {
                be.currentMaxRange = be.upperBoundMaxRange;
            }
            if (!be.rangeOverridden) {
                be.currentMaxRange = be.upperBoundMaxRange;
            }
            if (be.updateRange) {
                be.setCurrentMaxRange(be.upperBoundMaxRange);
                be.updateRange = false;
            }
        }
        be.recomputeCapacity(level, pos);
        be.generateSolar(level, pos);
        be.generateRedstone();
    }

    /**
     * The 1.12 setBaseUpperBoundRange: the longest reach of any touching head, its type's range plus
     * this base's range upgrades.
     */
    private void recomputeUpperBoundRange(Level level, BlockPos pos) {
        int maxRange = 0;
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(pos.relative(facing))
                    instanceof com.ommods.openmodularturrets.content.block.TurretBaseBlockEntity head) {
                maxRange = Math.max(maxRange, (int) head.turretType().range() + upgradeRangeBonus());
            }
        }
        upperBoundMaxRange = maxRange;
    }

    /**
     * Resize the energy buffer to the tier's base capacity plus every touching power expander,
     * faithful to 1.12's per-tick storage.setCapacity(getMaxEnergyStorageWithExtenders()).
     */
    private void recomputeCapacity(Level level, BlockPos pos) {
        int extra = 0;
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(pos.relative(facing)) instanceof ExpanderBlockEntity exp) {
                extra += exp.powerCapacity();
            }
        }
        energyStorage.setCapacity(tier.capacity() + extra);
    }

    private void generateSolar(Level level, BlockPos pos) {
        if (!TurretUpgrades.hasSolar(tier.tier(), addon9(), addon10())) {
            return;
        }
        // Daytime, clear sky, and nothing overhead: the same visibility check as 1.12 (two blocks up).
        if (level.isDay() && !level.isRaining() && level.canSeeSky(pos.above(2))) {
            energyStorage.generate(SOLAR_GEN);
        }
    }

    private void generateRedstone() {
        if (!TurretUpgrades.hasRedstoneReactor(tier.tier(), addon9(), addon10())) {
            return;
        }
        int headroom = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
        // A redstone block burns for nine dust's worth; fall back to a single dust. Only consume fuel
        // when there is room for the full yield, matching 1.12.
        if (REDSTONE_GEN * 9 < headroom && consumeFuel(Blocks.REDSTONE_BLOCK.asItem())) {
            energyStorage.generate(REDSTONE_GEN * 9);
        } else if (REDSTONE_GEN < headroom && consumeFuel(Items.REDSTONE)) {
            energyStorage.generate(REDSTONE_GEN);
        }
    }

    /**
     * Take one of the given fuel item from the ammo slots, or failing that a touching inventory
     * expander, returning true if one was consumed. Matches 1.12, where the reactor checks the base
     * then falls back to getSpecificItemFromInvExpanders.
     */
    private boolean consumeFuel(Item fuel) {
        for (int slot = 0; slot < AMMO_SLOTS; slot++) {
            if (inventory.getStackInSlot(slot).getItem() == fuel) {
                inventory.extractItem(slot, 1, false);
                return true;
            }
        }
        return takeFromExpanders(fuel) != null;
    }

    /**
     * True if this stack is something the disposable turret may throw. 1.12's
     * {@code getDisposableAmmoFromBase} walks the ammo slots for anything that passes
     * {@code isItemStackValidAmmo} <em>and is not an AmmoMetaItem</em>, so the junk ammo (cobblestone,
     * planks, potatoes, redstone) is fair game and the crafted turret ammo is not.
     *
     * <p>Without that second half a disposable turret sharing a base with a gun turret fires the gun
     * turret's bullets away one at a time.
     */
    private static boolean isThrowableJunk(ItemStack stack) {
        return !stack.isEmpty() && !OMTItems.isCraftedAmmoItem(stack.getItem());
    }

    private static boolean isReactorFuel(Item item) {
        return item == Items.REDSTONE || item == Blocks.REDSTONE_BLOCK.asItem();
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
        if (tag.contains(INVENTORY_TAG)) {
            inventory.deserializeNBT(tag.getCompound(INVENTORY_TAG));
        }
        if (tag.contains(TRUST_TAG)) {
            trustManager.readFromNBT(tag.getCompound(TRUST_TAG));
        }
        attacksMobs = !tag.contains(ATTACKS_MOBS_TAG) || tag.getBoolean(ATTACKS_MOBS_TAG);
        attacksNeutrals = tag.getBoolean(ATTACKS_NEUTRALS_TAG);
        attacksPlayers = tag.getBoolean(ATTACKS_PLAYERS_TAG);
        kills = tag.getInt("kills");
        playerKills = tag.getInt("playerKills");
        multiTargeting = tag.getBoolean("multiTargeting");
        mode = tag.contains("mode") ? MachineMode.byOrdinal(tag.getInt("mode")) : MachineMode.INVERTED;
        redstone = tag.getBoolean("redstone");
        currentMaxRange = tag.getInt("currentMaxRange");
        upperBoundMaxRange = tag.getInt("upperBoundMaxRange");
        rangeOverridden = tag.getBoolean("rangeOverridden");
        camoState = tag.contains("camo")
                ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("camo"))
                : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ENERGY_TAG, energyStorage.serializeNBT());
        tag.put(INVENTORY_TAG, inventory.serializeNBT());
        tag.put(TRUST_TAG, trustManager.writeToNBT(new CompoundTag()));
        tag.putBoolean(ATTACKS_MOBS_TAG, attacksMobs);
        tag.putBoolean(ATTACKS_NEUTRALS_TAG, attacksNeutrals);
        tag.putBoolean(ATTACKS_PLAYERS_TAG, attacksPlayers);
        tag.putInt("kills", kills);
        tag.putInt("playerKills", playerKills);
        tag.putBoolean("multiTargeting", multiTargeting);
        tag.putInt("mode", mode.ordinal());
        tag.putBoolean("redstone", redstone);
        tag.putInt("currentMaxRange", currentMaxRange);
        tag.putInt("upperBoundMaxRange", upperBoundMaxRange);
        tag.putBoolean("rangeOverridden", rangeOverridden);
        if (camoState != null) {
            tag.put("camo", NbtUtils.writeBlockState(camoState));
        }
    }

    /**
     * Client sync for the GUI's info panel: the owner and trusted player names are strings, so they
     * ride the block entity update path instead of the menu's int data slots. Everything numeric the
     * screen shows is synced by {@link TurretBaseMenu} instead.
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (owner != null && owner.getUuid() != null) {
            tag.put("owner", owner.serializeNBT());
        }
        tag.put(TRUST_TAG, trustManager.writeToNBT(new CompoundTag()));
        if (camoState != null) {
            tag.put("camo", NbtUtils.writeBlockState(camoState));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Push the update tag to watching clients, for the rare owner or trust list change. */
    public void syncToClient() {
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
        itemCap.invalidate();
    }
}
