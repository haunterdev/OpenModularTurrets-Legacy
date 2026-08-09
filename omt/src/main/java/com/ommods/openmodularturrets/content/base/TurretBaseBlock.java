package com.ommods.openmodularturrets.content.base;

import com.ommods.omlib.block.AbstractOMLibBlockWithEntity;
import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.PlayerUtil;
import com.ommods.openmodularturrets.content.item.MemoryCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * A turret base block. Place it, then attach turret heads. Holds the shared energy, ammo, owner,
 * trust and attack toggles in {@link TurretBaseTileEntity}. Its {@link BaseTier} is fixed per block
 * instance; one block class backs all five tiers.
 *
 * Right click opens the base GUI (sneak with a memory card stores settings on the card instead).
 * The attack toggles live on the GUI's configure screen, as in 1.12.
 */
public class TurretBaseBlock extends AbstractOMLibBlockWithEntity {

    private final BaseTier tier;

    public TurretBaseBlock(Properties properties, BaseTier tier) {
        super(properties);
        this.tier = tier;
    }

    public BaseTier tier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return BaseInit.TURRET_BASE_ENTITY.create(pos, state);
    }

    /**
     * The 1.12 canPlaceBlockAt: a base refuses to place if another base sits on a neighbor or on
     * anything touching a neighbor, so no two bases can ever share an attachment cell (a head or
     * expander between them would not know which base it belongs to).
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction facing : Direction.values()) {
            BlockPos neighbor = pos.relative(facing);
            if (level.getBlockEntity(neighbor) instanceof TurretBaseTileEntity) {
                return null;
            }
            for (Direction touching : Direction.values()) {
                if (level.getBlockEntity(neighbor.relative(touching)) instanceof TurretBaseTileEntity) {
                    return null;
                }
            }
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // Server only: the solar and redstone-reactor addons top up the base's energy each tick.
        if (level.isClientSide() || type != BaseInit.TURRET_BASE_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> TurretBaseTileEntity.serverTick(lvl, pos, st, (TurretBaseTileEntity) be);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof TurretBaseTileEntity base) {
            base.setOwner(new OMPlayer(player));
            base.setRedstone(level.hasNeighborSignal(pos));
            base.setChanged();
        }
    }

    // Feed the live redstone input to the machine mode, the 1.20 equivalent of 1.12's
    // block.neighborChanged calling setRedstone on the machine tile.
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TurretBaseTileEntity base) {
            base.setRedstone(level.hasNeighborSignal(pos));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moved) {
        TurretBaseTileEntity base =
                !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TurretBaseTileEntity found
                        ? found
                        : null;
        super.onRemove(state, level, pos, newState, moved);
        if (base != null) {
            base.onRemoved();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof TurretBaseTileEntity base) {
            // Camouflage first, faithful to 1.12 handleCamoActivation: the owner right-clicks with a
            // full opaque block to disguise the base, or sneaks with an empty main hand to reset it.
            if (handleCamo(state, level, pos, player, hand, hit, base)) {
                return InteractionResult.CONSUME;
            }
            // Memory card interactions next, faithful to 1.12 BlockTurretBase: sneak stores
            // this base's settings on the card, plain use applies a loaded card to this base.
            ItemStack held = player.getItemInHand(hand);
            if (held.getItem() instanceof MemoryCardItem) {
                if (player.isShiftKeyDown()) {
                    MemoryCardItem.setData(held, base.writeMemoryCard());
                    player.sendSystemMessage(Component.literal("Base settings stored on card")
                            .withStyle(ChatFormatting.AQUA));
                } else if (MemoryCardItem.hasData(held)) {
                    base.readMemoryCard(MemoryCardItem.getData(held));
                    player.sendSystemMessage(Component.literal("Card settings applied to base")
                            .withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.CONSUME;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                openBaseMenu(serverPlayer, base);
            }
        }
        return InteractionResult.CONSUME;
    }

    /**
     * The 1.12 camo rules, owner only and main hand only: a held full opaque block without a block
     * entity (and not another base) becomes the disguise; sneak with an empty hand resets to the
     * base's own look. Returns true when the click was a camo action (including a refused one).
     */
    private boolean handleCamo(BlockState state, Level level, BlockPos pos, Player player,
                               InteractionHand hand, BlockHitResult hit, TurretBaseTileEntity base) {
        if (hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && held.isEmpty()) {
            if (PlayerUtil.isPlayerOwner(player, base)) {
                base.setCamoState(null);
            } else {
                player.sendSystemMessage(Component.translatable("status.ownership")
                        .withStyle(ChatFormatting.RED));
            }
            return true;
        }
        if (!player.isShiftKeyDown() && held.getItem() instanceof BlockItem blockItem
                && !(blockItem.getBlock() instanceof TurretBaseBlock)
                && !(blockItem.getBlock() instanceof EntityBlock)) {
            BlockState camo = blockItem.getBlock()
                    .getStateForPlacement(new BlockPlaceContext(player, hand, held, hit));
            if (camo == null) {
                camo = blockItem.getBlock().defaultBlockState();
            }
            if (camo.isSolidRender(level, pos)) {
                if (PlayerUtil.isPlayerOwner(player, base)) {
                    base.setCamoState(camo);
                } else {
                    player.sendSystemMessage(Component.translatable("status.ownership")
                            .withStyle(ChatFormatting.RED));
                }
                return true;
            }
        }
        return false;
    }

    /** Open the base inventory GUI; also the Back target from the configure screen. */
    public static void openBaseMenu(ServerPlayer player, TurretBaseTileEntity base) {
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                        (id, inv, p) -> new TurretBaseMenu(BaseInit.TURRET_BASE_MENU.get(), id, inv, base),
                        Component.literal("Turret Base Tier " + base.tier().tier())),
                buf -> buf.writeBlockPos(base.getBlockPos()));
    }
}
