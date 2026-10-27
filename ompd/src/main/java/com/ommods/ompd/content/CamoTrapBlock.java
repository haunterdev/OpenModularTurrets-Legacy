package com.ommods.ompd.content;

import com.ommods.omlib.util.PlayerUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** A full-looking disguise that only its owner can stand on; everyone else falls through. */
public class CamoTrapBlock extends BaseEntityBlock {
    public CamoTrapBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return OMPDBlocks.CAMO_TRAP_ENTITY.create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof CamoTrapBlockEntity trap
                && trap.getCamoState() != null) {
            return new ItemStack(trap.getCamoState().getBlock());
        }
        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        DefenseBlockSupport.setPlacedOwner(level, pos, placer);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        Entity entity = context instanceof EntityCollisionContext entityContext
                ? entityContext.getEntity()
                : null;
        boolean playerEntity = entity instanceof Player;
        boolean owner = playerEntity
                && level.getBlockEntity(pos) instanceof CamoTrapBlockEntity trap
                && PlayerUtil.isPlayerOwner((Player) entity, trap);
        return DefensePolicies.trapIsSolid(playerEntity, owner) ? Shapes.block() : Shapes.empty();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND
                || !(level.getBlockEntity(pos) instanceof CamoTrapBlockEntity trap)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && held.isEmpty()) {
            return changeCamo(level, player, trap, null);
        }
        if (!player.isShiftKeyDown() && held.getItem() instanceof BlockItem blockItem
                && !(blockItem.getBlock() instanceof EntityBlock)) {
            BlockState camo = blockItem.getBlock()
                    .getStateForPlacement(new BlockPlaceContext(player, hand, held, hit));
            if (camo == null) {
                camo = blockItem.getBlock().defaultBlockState();
            }
            if (camo.isSolidRender(level, pos)) {
                return changeCamo(level, player, trap, camo);
            }
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult changeCamo(Level level, Player player, CamoTrapBlockEntity trap,
                                                @Nullable BlockState camo) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (PlayerUtil.isPlayerOwner(player, trap)) {
            trap.setCamoState(camo);
        } else {
            player.sendSystemMessage(Component.translatable("status.ownership")
                    .withStyle(ChatFormatting.RED));
        }
        return InteractionResult.CONSUME;
    }
}
