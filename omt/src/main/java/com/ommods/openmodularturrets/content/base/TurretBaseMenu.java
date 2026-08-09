package com.ommods.openmodularturrets.content.base;

import com.ommods.omlib.util.MachineMode;
import com.ommods.omlib.util.PlayerUtil;
import com.ommods.openmodularturrets.content.block.TurretBaseBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The turret base inventory menu: a 3x3 ammo grid plus the tier gated addon and upgrade slots, over
 * the player inventory. Slot coordinates match the 1.12 TurretBaseContainer so the ported GUI
 * texture lines up. Addon and upgrade slots appear from tier 2 (and the fourth from tier 5), exactly
 * as upstream.
 */
public class TurretBaseMenu extends AbstractContainerMenu {

    // ContainerData indices: everything numeric the screen shows rides these int slots.
    private static final int DATA_ENERGY = 0;
    private static final int DATA_MAX_ENERGY = 1;
    private static final int DATA_KILLS = 2;
    private static final int DATA_PLAYER_KILLS = 3;
    private static final int DATA_MULTI_TARGETING = 4;
    private static final int DATA_MODE = 5;
    private static final int DATA_ACTIVE = 6;
    private static final int DATA_ATTACKS_MOBS = 7;
    private static final int DATA_ATTACKS_NEUTRALS = 8;
    private static final int DATA_ATTACKS_PLAYERS = 9;
    private static final int DATA_CURRENT_MAX_RANGE = 10;
    private static final int DATA_UPPER_BOUND_RANGE = 11;
    private static final int DATA_VIEWER_ACCESS = 12;
    private static final int DATA_COUNT = 13;

    // Viewer access tiers the screen keys button visibility on, mirroring the 1.12 buttonInit
    // branches (admin sees everything, change-settings a subset).
    public static final int ACCESS_VIEW = 0;
    public static final int ACCESS_CHANGE_SETTINGS = 1;
    public static final int ACCESS_ADMIN = 2;

    // Button ids for clickMenuButton, matching the 1.12 GUI's button ids where one exists.
    public static final int BUTTON_TARGET = 0;
    public static final int BUTTON_RANGE_UP = 1;
    public static final int BUTTON_RANGE_DOWN = 2;
    public static final int BUTTON_DROP_TURRETS = 3;
    public static final int BUTTON_DROP_BASE = 4;
    public static final int BUTTON_CONFIGURE = 5;
    public static final int BUTTON_MODE = 7;

    private final TurretBaseTileEntity base;
    private final Player player;
    private final int baseSlotCount;
    private final int[] synced = new int[DATA_COUNT];

    /** Client side: the base position rides in the buffer. */
    public TurretBaseMenu(MenuType<?> type, int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(type, windowId, playerInventory, resolve(playerInventory, buf));
    }

    /** Server side and the shared build. */
    public TurretBaseMenu(MenuType<?> type, int windowId, Inventory playerInventory, TurretBaseTileEntity base) {
        super(type, windowId);
        this.base = base;
        this.player = playerInventory.player;

        int slots = 0;
        // 3x3 ammo grid, base inventory slots 0-8 ordered column + row * 3.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(base.getInventory(), col + row * 3, 8 + col * 18, 17 + row * 18));
                slots++;
            }
        }
        // Addon and upgrade slots, tier gated. Base inventory slots 9-12.
        if (base.tier().tier() > 1) {
            addSlot(new SlotItemHandler(base.getInventory(), 9, 72, 18));
            addSlot(new SlotItemHandler(base.getInventory(), 10, 92, 18));
            addSlot(new SlotItemHandler(base.getInventory(), 11, 72, 52));
            slots += 3;
        }
        if (base.tier().tier() > 4) {
            addSlot(new SlotItemHandler(base.getInventory(), 12, 92, 52));
            slots++;
        }
        this.baseSlotCount = slots;

        // Player inventory (3x9) and hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + col + row * 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Sync the GUI's numbers to the client each tick while the screen is open.
        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                if (isServer()) {
                    return switch (index) {
                        case DATA_ENERGY -> base.getEnergyStorageImpl().getEnergyStored();
                        case DATA_MAX_ENERGY -> base.getEnergyStorageImpl().getMaxEnergyStored();
                        case DATA_KILLS -> base.getKills();
                        case DATA_PLAYER_KILLS -> base.getPlayerKills();
                        case DATA_MULTI_TARGETING -> base.isMultiTargeting() ? 1 : 0;
                        case DATA_MODE -> base.getMode().ordinal();
                        case DATA_ACTIVE -> base.isActive() ? 1 : 0;
                        case DATA_ATTACKS_MOBS -> base.isAttacksMobs() ? 1 : 0;
                        case DATA_ATTACKS_NEUTRALS -> base.isAttacksNeutrals() ? 1 : 0;
                        case DATA_ATTACKS_PLAYERS -> base.isAttacksPlayers() ? 1 : 0;
                        case DATA_CURRENT_MAX_RANGE -> base.getCurrentMaxRange();
                        case DATA_UPPER_BOUND_RANGE -> base.getUpperBoundMaxRange();
                        default -> viewerAccessOf(player, base);
                    };
                }
                return synced[index];
            }

            @Override
            public void set(int index, int value) {
                synced[index] = value;
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        });
    }

    /** The viewing player's access tier, the server-side truth the client buttons key on. */
    static int viewerAccessOf(Player player, TurretBaseTileEntity base) {
        if (PlayerUtil.isPlayerAdmin(player, base)) {
            return ACCESS_ADMIN;
        }
        if (PlayerUtil.canPlayerChangeSetting(player, base)) {
            return ACCESS_CHANGE_SETTINGS;
        }
        return ACCESS_VIEW;
    }

    private boolean isServer() {
        return base != null && base.getLevel() != null && !base.getLevel().isClientSide();
    }

    public int getEnergy() {
        return isServer() ? base.getEnergyStorageImpl().getEnergyStored() : synced[DATA_ENERGY];
    }

    public int getMaxEnergy() {
        return isServer() ? base.getEnergyStorageImpl().getMaxEnergyStored() : synced[DATA_MAX_ENERGY];
    }

    public int getKills() {
        return isServer() ? base.getKills() : synced[DATA_KILLS];
    }

    public int getPlayerKills() {
        return isServer() ? base.getPlayerKills() : synced[DATA_PLAYER_KILLS];
    }

    public boolean isMultiTargeting() {
        return isServer() ? base.isMultiTargeting() : synced[DATA_MULTI_TARGETING] != 0;
    }

    public MachineMode getMode() {
        return isServer() ? base.getMode() : MachineMode.byOrdinal(synced[DATA_MODE]);
    }

    public boolean isActive() {
        return isServer() ? base.isActive() : synced[DATA_ACTIVE] != 0;
    }

    public boolean attacksMobs() {
        return isServer() ? base.isAttacksMobs() : synced[DATA_ATTACKS_MOBS] != 0;
    }

    public boolean attacksNeutrals() {
        return isServer() ? base.isAttacksNeutrals() : synced[DATA_ATTACKS_NEUTRALS] != 0;
    }

    public boolean attacksPlayers() {
        return isServer() ? base.isAttacksPlayers() : synced[DATA_ATTACKS_PLAYERS] != 0;
    }

    public int getCurrentMaxRange() {
        return isServer() ? base.getCurrentMaxRange() : synced[DATA_CURRENT_MAX_RANGE];
    }

    public int getUpperBoundMaxRange() {
        return isServer() ? base.getUpperBoundMaxRange() : synced[DATA_UPPER_BOUND_RANGE];
    }

    public int viewerAccess() {
        return isServer() ? viewerAccessOf(player, base) : synced[DATA_VIEWER_ACCESS];
    }

    @Nullable
    public TurretBaseTileEntity base() {
        return base;
    }

    /**
     * The base GUI's buttons, with the 1.12 server-side permission gates: the Target toggle needs
     * change-settings access (MessageSetBaseTargetingType); range, mode and the drop buttons need
     * admin (their 1.12 message handlers all checked isPlayerAdmin).
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (base == null) {
            return false;
        }
        boolean admin = PlayerUtil.isPlayerAdmin(player, base);
        boolean changeSettings = PlayerUtil.canPlayerChangeSetting(player, base);
        switch (id) {
            case BUTTON_TARGET -> {
                if (denied(player, changeSettings)) {
                    return true;
                }
                base.setMultiTargeting(!base.isMultiTargeting());
            }
            case BUTTON_RANGE_UP -> {
                if (denied(player, admin)) {
                    return true;
                }
                base.setCurrentMaxRange(base.getCurrentMaxRange() + 1);
            }
            case BUTTON_RANGE_DOWN -> {
                if (denied(player, admin)) {
                    return true;
                }
                base.setCurrentMaxRange(base.getCurrentMaxRange() - 1);
            }
            case BUTTON_DROP_TURRETS -> {
                if (denied(player, admin)) {
                    return true;
                }
                dropTurrets();
            }
            case BUTTON_DROP_BASE -> {
                if (denied(player, admin)) {
                    return true;
                }
                player.closeContainer();
                Level level = base.getLevel();
                if (level != null) {
                    level.destroyBlock(base.getBlockPos(), true);
                }
            }
            case BUTTON_CONFIGURE -> {
                if (denied(player, changeSettings)) {
                    return true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    ConfigureMenu.open(serverPlayer, base);
                }
            }
            case BUTTON_MODE -> {
                if (denied(player, admin)) {
                    return true;
                }
                base.toggleMode();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** The 1.12 "status.ownership" refusal line for a button the player may not press. */
    static boolean denied(Player player, boolean allowed) {
        if (!allowed) {
            player.sendSystemMessage(Component.translatable("status.ownership")
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        return false;
    }

    /** The 1.12 MessageDropTurrets action: pop every attached head off the base as a drop. */
    private void dropTurrets() {
        Level level = base.getLevel();
        if (level == null) {
            return;
        }
        for (Direction facing : Direction.values()) {
            BlockPos headPos = base.getBlockPos().relative(facing);
            if (level.getBlockEntity(headPos) instanceof TurretBaseBlockEntity) {
                level.destroyBlock(headPos, true);
            }
        }
    }

    public BaseTier tier() {
        return base != null ? base.tier() : BaseTier.TIER1;
    }

    private static TurretBaseTileEntity resolve(Inventory playerInventory, FriendlyByteBuf buf) {
        return playerInventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof TurretBaseTileEntity b
                ? b : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return base != null && !base.isRemoved()
                && player.distanceToSqr(base.getBlockPos().getX() + 0.5,
                base.getBlockPos().getY() + 0.5, base.getBlockPos().getZ() + 0.5) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();
        int playerStart = baseSlotCount;
        int playerEnd = slots.size();
        if (index < playerStart) {
            // From the base into the player inventory.
            if (!moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stackInSlot, 0, playerStart, false)) {
            // From the player inventory into the base (ammo/addon slots reject invalid items).
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
