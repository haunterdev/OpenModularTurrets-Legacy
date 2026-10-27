package com.ommods.ompd.content;

import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.PlayerUtil;
import com.ommods.omlib.block.OMBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Modern equivalents of the two placement/use helpers in the 1.12 BlockHelper. */
public final class DefenseBlockSupport {
    private DefenseBlockSupport() {
    }

    public static void setPlacedOwner(Level level, BlockPos pos, @Nullable LivingEntity placer) {
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof OMBlockEntity owned) {
            owned.setOwner(new OMPlayer(player));
            owned.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 2);
        }
    }

    public static InteractionResult use(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.isShiftKeyDown()
                || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof OwnedDefenseBlockEntity defense
                && PlayerUtil.isPlayerOwner(player, defense)) {
            level.destroyBlock(pos, true, player);
        } else {
            player.sendSystemMessage(Component.translatable("status.ownership")
                    .withStyle(ChatFormatting.RED));
        }
        return InteractionResult.CONSUME;
    }
}
