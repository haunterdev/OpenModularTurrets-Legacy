package com.ommods.openmodularturrets.content.block;

import com.ommods.omlib.block.AbstractOMLibBlockWithEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * A creative power source block. Generates energy every tick and pushes it to the network; wire it
 * to turrets with cables to charge them. The generation loop lives in {@link EnergizerBlockEntity}.
 */
public class EnergizerBlock extends AbstractOMLibBlockWithEntity {

    public EnergizerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return OMTBlockInit.ENERGIZER_ENTITY.create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != OMTBlockInit.ENERGIZER_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> EnergizerBlockEntity.tick(lvl, pos, st, (EnergizerBlockEntity) be);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moved) {
        EnergizerBlockEntity gen =
                !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof EnergizerBlockEntity found
                        ? found
                        : null;
        super.onRemove(state, level, pos, newState, moved);
        if (gen != null) {
            gen.onRemoved();
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof EnergizerBlockEntity gen) {
            int energy = gen.getEnergyStorage().getEnergyStored();
            player.sendSystemMessage(Component.literal("Energizer [buffer: " + energy + " RF]")
                    .withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.CONSUME;
    }
}
