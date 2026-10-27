package com.ommods.ompd.content;

import com.ommods.omlib.util.PlayerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** The original thin pane fence: every non-owner, non-item contact takes five cactus damage. */
public class DefenseFenceBlock extends IronBarsBlock implements EntityBlock, TieredDefenseBlock {
    private final DefenseTier tier;

    public DefenseFenceBlock(DefenseTier tier, Properties properties) {
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
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        boolean playerEntity = entity instanceof Player;
        boolean owner = playerEntity
                && level.getBlockEntity(pos) instanceof OwnedDefenseBlockEntity defense
                && PlayerUtil.isPlayerOwner((Player) entity, defense);
        if (DefensePolicies.fenceDamages(entity instanceof ItemEntity, playerEntity, owner)) {
            entity.hurt(level.damageSources().cactus(), 5.0F);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        return DefenseBlockSupport.use(level, pos, player, hand);
    }
}
