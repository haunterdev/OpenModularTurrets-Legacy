package com.ommods.openmodularturrets.content.expander;

import com.ommods.omlib.block.AbstractOMLibBlockWithEntity;
import com.ommods.omlib.util.PlayerUtil;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * An expander block, placed adjacent to a turret base to lend it either extra energy capacity (a
 * power expander) or nine extra ammo slots (an inventory expander). Faithful to 1.12 {@code
 * BlockExpander}: one block class backs all ten variants (two kinds x five tiers), the owner is
 * inherited from the base on placement, and an expander with no base drops itself. Right clicking an
 * inventory expander opens its ammo GUI for anyone who may access the base; a power expander has no
 * GUI.
 */
public class ExpanderBlock extends AbstractOMLibBlockWithEntity {

    /** Points at the adjacent turret base the pad is mounted on, as the 1.12 Expander orientation. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final boolean power;
    private final int tier;

    public ExpanderBlock(Properties properties, boolean power, int tier) {
        super(properties);
        this.power = power;
        this.tier = tier;
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    /**
     * The 1.12 canPlaceBlockAt gate plus setSide: an expander only places against a turret base,
     * facing it. Returning null makes the block item refuse placement.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = AttachmentPad.facingTowardBase(context.getLevel(), context.getClickedPos());
        return facing == null ? null : defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return AttachmentPad.SHAPES.get(state.getValue(FACING));
    }

    public boolean isPower() {
        return power;
    }

    public int tier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ExpanderInit.EXPANDER_ENTITY.create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ExpanderInit.EXPANDER_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> ExpanderBlockEntity.serverTick(lvl, pos, st, (ExpanderBlockEntity) be);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof ExpanderBlockEntity expander)) {
            return;
        }
        TurretBaseTileEntity base = expander.findBase();
        if (base == null) {
            // No base to attach to: drop straight back to an item, matching 1.12.
            level.destroyBlock(pos, true);
            return;
        }
        expander.setOwner(base.getOwner());
        expander.setChanged();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ExpanderBlockEntity expander)) {
            return InteractionResult.CONSUME;
        }
        TurretBaseTileEntity base = expander.findBase();
        if (base == null) {
            level.destroyBlock(pos, true);
            return InteractionResult.CONSUME;
        }
        if (!PlayerUtil.canPlayerAccessBlock(player, base)) {
            player.sendSystemMessage(Component.translatable("status.ownership").withStyle(ChatFormatting.RED));
            return InteractionResult.CONSUME;
        }
        // Only inventory expanders have a GUI; a power expander is inert on right click.
        if (!power && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                            (id, inv, p) -> new ExpanderMenu(ExpanderInit.EXPANDER_MENU.get(), id, inv, expander),
                            Component.literal("Inventory Expander")),
                    buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        // Spill the ammo slots before the block entity is gone, so the player gets their ammo back.
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof ExpanderBlockEntity expander
                && expander.getInventory() != null) {
            var inv = expander.getInventory();
            for (int slot = 0; slot < inv.getSlots(); slot++) {
                Block.popResource(level, pos, inv.getStackInSlot(slot));
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
