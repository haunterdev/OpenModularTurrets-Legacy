package com.ommods.ompd.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** One flattened registry entry for a metadata tier of the original hardened wall. */
public class DefenseWallBlock extends WallBlock implements EntityBlock, TieredDefenseBlock {
    private final DefenseTier tier;

    public DefenseWallBlock(DefenseTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public DefenseTier tier() {
        return tier;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return OMPDBlocks.OWNED_DEFENSE_ENTITY.create(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        DefenseBlockSupport.setPlacedOwner(level, pos, placer);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return DefenseBlockSupport.use(level, pos, player, hand);
    }
}
