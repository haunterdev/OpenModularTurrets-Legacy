package com.ommods.openmodularturrets.content.expander;

import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * The 1.12 base attachment pad, shared by the expanders and the loot deleter: a 12x12x6 slab that
 * hugs the face toward its turret base. Matches the 1.12 {@code openmodularturrets:block/expander}
 * model geometry and {@code AbstractBaseAttachment}'s facing-rotated bounding box.
 */
final class AttachmentPad {

    static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Map.of(
            Direction.NORTH, Block.box(2, 2, 0, 14, 14, 6),
            Direction.SOUTH, Block.box(2, 2, 10, 14, 14, 16),
            Direction.WEST, Block.box(0, 2, 2, 6, 14, 14),
            Direction.EAST, Block.box(10, 2, 2, 16, 14, 14),
            Direction.DOWN, Block.box(2, 0, 2, 14, 6, 14),
            Direction.UP, Block.box(2, 10, 2, 14, 16, 14)));

    /**
     * The 1.12 TurretHeadUtil.getTurretBaseFacing: the first direction (in Direction order, as
     * upstream) whose neighbor holds a turret base, or null when none does. The base adjacency rule
     * guarantees at most one base touches any attachment cell, so first is also only.
     */
    @Nullable
    static Direction facingTowardBase(LevelReader level, BlockPos pos) {
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(pos.relative(facing)) instanceof TurretBaseTileEntity) {
                return facing;
            }
        }
        return null;
    }

    private AttachmentPad() {
    }
}
