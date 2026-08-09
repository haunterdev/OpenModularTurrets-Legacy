package com.ommods.openmodularturrets.content.expander;

import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The Loot Deleter base attachment, faithful to 1.12 {@code BlockBaseAttachment} meta 0: a plain
 * block placed against a turret base. While it touches the base, mobs killed by that base's turrets
 * drop no loot (the base checks for it in {@code dropsLoot()}, and struck mobs are tagged so the drop
 * event cancels). Like the expanders it cannot exist without an adjacent base.
 */
public class LootDeleterBlock extends Block {

    /** Points at the adjacent turret base the pad is mounted on, as the 1.12 attachment facing. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public LootDeleterBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

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

    private static boolean touchesBase(LevelReader level, BlockPos pos) {
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(pos.relative(facing)) instanceof TurretBaseTileEntity) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return touchesBase(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        // Faithful to the 1.12 attachment tick: with the base gone the attachment pops off and drops.
        if (!level.isClientSide() && !touchesBase(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}
