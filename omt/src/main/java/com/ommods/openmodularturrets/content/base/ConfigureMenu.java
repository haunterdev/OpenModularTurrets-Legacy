package com.ommods.openmodularturrets.content.base;

import com.ommods.omlib.network.OMLibChannel;
import com.ommods.omlib.network.PacketOpenTileTrust;
import com.ommods.omlib.util.PlayerUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The base's configure screen backing menu, the port of 1.12 ConfigureGui: the three attack
 * toggles, the trusted players editor, and a Back button to the base GUI. Slotless; it only carries
 * the toggles and the viewer's access tier. Reached from the base GUI's Configure button and
 * returns there through Back, mirroring 1.12's server-mediated GUI switch.
 */
public class ConfigureMenu extends AbstractContainerMenu {

    public static final int BUTTON_ATTACK_MOBS = 0;
    public static final int BUTTON_ATTACK_NEUTRALS = 1;
    public static final int BUTTON_ATTACK_PLAYERS = 2;
    public static final int BUTTON_TRUSTED_PLAYERS = 3;
    public static final int BUTTON_BACK = 6;

    private final TurretBaseTileEntity base;
    private final Player player;
    private final int[] synced = new int[4];

    /** Client side: the base position rides in the buffer. */
    public ConfigureMenu(MenuType<?> type, int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(type, windowId, playerInventory, resolve(playerInventory, buf));
    }

    public ConfigureMenu(MenuType<?> type, int windowId, Inventory playerInventory, TurretBaseTileEntity base) {
        super(type, windowId);
        this.base = base;
        this.player = playerInventory.player;
        addDataSlots(new ContainerData() {
            @Override
            public int get(int index) {
                if (isServer()) {
                    return switch (index) {
                        case 0 -> base.isAttacksMobs() ? 1 : 0;
                        case 1 -> base.isAttacksNeutrals() ? 1 : 0;
                        case 2 -> base.isAttacksPlayers() ? 1 : 0;
                        default -> TurretBaseMenu.viewerAccessOf(player, base);
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
                return 4;
            }
        });
    }

    /** Swap the player's open GUI to the configure screen, from the base GUI's Configure button. */
    public static void open(ServerPlayer player, TurretBaseTileEntity base) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new ConfigureMenu(BaseInit.CONFIGURE_MENU.get(), id, inv, base),
                        Component.literal("Configure")),
                buf -> buf.writeBlockPos(base.getBlockPos()));
    }

    private boolean isServer() {
        return base != null && base.getLevel() != null && !base.getLevel().isClientSide();
    }

    public boolean attacksMobs() {
        return isServer() ? base.isAttacksMobs() : synced[0] != 0;
    }

    public boolean attacksNeutrals() {
        return isServer() ? base.isAttacksNeutrals() : synced[1] != 0;
    }

    public boolean attacksPlayers() {
        return isServer() ? base.isAttacksPlayers() : synced[2] != 0;
    }

    public int viewerAccess() {
        return isServer() ? TurretBaseMenu.viewerAccessOf(player, base) : synced[3];
    }

    /**
     * The configure buttons with the 1.12 gates: the attack toggles need change-settings access
     * (their message handlers checked canPlayerChangeSetting), the trusted players editor needs
     * admin (1.12 opened omlib's trusted players GUI for admins only).
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (base == null) {
            return false;
        }
        boolean changeSettings = PlayerUtil.canPlayerChangeSetting(player, base);
        switch (id) {
            case BUTTON_ATTACK_MOBS -> {
                if (TurretBaseMenu.denied(player, changeSettings)) {
                    return true;
                }
                base.setAttacksMobs(!base.isAttacksMobs());
            }
            case BUTTON_ATTACK_NEUTRALS -> {
                if (TurretBaseMenu.denied(player, changeSettings)) {
                    return true;
                }
                base.setAttacksNeutrals(!base.isAttacksNeutrals());
            }
            case BUTTON_ATTACK_PLAYERS -> {
                if (TurretBaseMenu.denied(player, changeSettings)) {
                    return true;
                }
                base.setAttacksPlayers(!base.isAttacksPlayers());
            }
            case BUTTON_TRUSTED_PLAYERS -> {
                if (TurretBaseMenu.denied(player, PlayerUtil.isPlayerAdmin(player, base))) {
                    return true;
                }
                if (player instanceof ServerPlayer serverPlayer) {
                    // The trust editor is not a container screen, so close this menu cleanly first.
                    serverPlayer.closeContainer();
                    OMLibChannel.sendToPlayer(
                            PacketOpenTileTrust.of(base.getBlockPos(),
                                    base.getTrustManager().getTrustedPlayers()),
                            serverPlayer);
                }
            }
            case BUTTON_BACK -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    TurretBaseBlock.openBaseMenu(serverPlayer, base);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Nullable
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
        return ItemStack.EMPTY;
    }
}
