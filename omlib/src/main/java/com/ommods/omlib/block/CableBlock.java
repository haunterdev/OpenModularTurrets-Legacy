package com.ommods.omlib.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * The network cable.
 *
 * Connection state lives on the block entity, not in blockstate properties. The model is a plain
 * cube that does not change shape with its neighbours, so six boolean connection properties would
 * have nothing to drive.
 */
public class CableBlock extends AbstractOMLibBlockWithEntity {

    public CableBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // BlockEntityEntry.create supplies the registered type.
        return BlockInit.NETWORK_CABLE_ENTITY.create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE, which would hide the cube.
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Right click reports which network this cable belongs to and how large it is.
        if (level.getBlockEntity(pos) instanceof BlockEntityCable cable) {
            player.sendSystemMessage(
                    Component.literal(cable.describeNetwork()).withStyle(ChatFormatting.AQUA));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moved) {
        // The block entity has to be resolved before super runs and forgotten after: super is what
        // calls removeBlockEntity, and the rebuild must not see this cable. Rebuilding first walked
        // the topology through the block entity still sitting in the hole, so a severed run still
        // looked connected and both halves joined one network.
        BlockEntityCable cable =
                !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BlockEntityCable found
                        ? found
                        : null;
        super.onRemove(state, level, pos, newState, moved);
        if (cable != null) {
            cable.onRemoved();
        }
    }
}
