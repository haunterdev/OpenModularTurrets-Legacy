package com.ommods.openmodularturrets.content.block;

import com.ommods.omlib.block.AbstractOMLibBlockWithEntity;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * The Basic Turret Base Crank, faithful to 1.12 {@code LeverBlock}: a hand crank that attaches to a
 * tier 1 turret base on one of its four horizontal sides. Each full turn (started by a right click)
 * feeds the base 50 FE, the early-game way to power a base before real generation. Drawn by
 * {@link com.ommods.openmodularturrets.client.turret.LeverRenderer}.
 */
public class LeverBlock extends AbstractOMLibBlockWithEntity {

    public static final int ENERGY_PER_TURN = 50;

    // 1.12 LeverBlock.baseBB, an 0.64 cube 0.1 deep on the far side and 0.4 deep toward the base,
    // placed by rotateAABB(baseBB, facing.getOpposite()) then offset 0.1 toward the base. In world
    // units that is a 10.24 pixel square core filling the half of the block nearest the base, which is
    // exactly the volume the spinning handle sweeps.
    //
    // Upstream's rotateAABB is written so its two "flip" cases rebuild the box with the min and max of
    // an axis swapped, and AxisAlignedBB re-sorts them, so NORTH came out identical to SOUTH and WEST
    // identical to EAST. That left the box on the wrong side of the block for two of the four
    // attachment directions. The four shapes here are the intended rotation, not that no-op.
    private static final double MIN = 2.88D;   // 0.18 * 16
    private static final double MAX = 13.12D;  // 0.82 * 16
    private static final VoxelShape SHAPE_NORTH = Block.box(MIN, MIN, 0.0D, MAX, MAX, 8.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(MIN, MIN, 8.0D, MAX, MAX, 16.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0D, MIN, MIN, 8.0D, MAX, MAX);
    private static final VoxelShape SHAPE_EAST = Block.box(8.0D, MIN, MIN, 16.0D, MAX, MAX);
    // Used while no base can be seen, including the state cache's EmptyBlockGetter pass. It only has
    // to be smaller than a full cube: that is what makes the block stop blocking skylight, which is
    // where the block-shaped shadow under a crank came from.
    private static final VoxelShape SHAPE_CORE = Block.box(MIN, MIN, MIN, MAX, MAX, MAX);

    public LeverBlock(Properties properties) {
        super(properties);
    }

    private static boolean isTierOneBase(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TurretBaseTileEntity base && base.tier().tier() == 1;
    }

    private static boolean touchesTierOneBase(LevelReader level, BlockPos pos) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (isTierOneBase(level, pos.relative(facing))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (isTierOneBase(level, pos.relative(facing))) {
                return switch (facing) {
                    case NORTH -> SHAPE_NORTH;
                    case SOUTH -> SHAPE_SOUTH;
                    case WEST -> SHAPE_WEST;
                    default -> SHAPE_EAST;
                };
            }
        }
        return SHAPE_CORE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return touchesTierOneBase(level, pos);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return OMTBlockInit.LEVER_ENTITY.create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // Drawn by the block entity renderer so the crank can spin.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // Both sides: the client mirrors the crank spin for the renderer.
        if (type != OMTBlockInit.LEVER_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> LeverBlockEntity.tick(lvl, pos, st, (LeverBlockEntity) be);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LeverBlockEntity lever) {
            TurretBaseTileEntity base = lever.findBase();
            if (base != null && lever.startTurning()) {
                // One full crank = 50 FE, faithful to the 1.12 receiveEnergy(50) per turn.
                base.getEnergyStorageImpl().receiveEnergy(ENERGY_PER_TURN, false);
                level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 0.8F);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide() && !touchesTierOneBase(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}
