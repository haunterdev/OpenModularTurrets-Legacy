package com.ommods.openmodularturrets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.block.TurretHeadItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The placement preview outline, ported from omlib {@code OMLibEventHandler.drawBlockOutline} and OMT
 * {@code IDrawOutlineBase}. While the player holds a turret head and looks at a block whose adjacent
 * cell touches a turret base, a coloured box shows where and how the head would attach: cyan when the
 * cell is free to place into, red when it is blocked. Faithful to 1.12, which drew this on the old
 * {@code DrawBlockHighlightEvent}; 1.20 exposes the same hook as {@link RenderHighlightEvent.Block}.
 */
@Mod.EventBusSubscriber(modid = OpenModularTurrets.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class OMTOutlineHandler {

    private OMTOutlineHandler() {
    }

    @SubscribeEvent
    public static void onDrawHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof TurretHeadItem)) {
            return;
        }
        BlockHitResult target = event.getTarget();
        Level level = player.level();
        // The cell the head would drop into: the block the player looks at, offset by the hit face.
        BlockPos placePos = target.getBlockPos().relative(target.getDirection());
        // IDrawOutlineBase only draws when that cell actually touches a base; the facing toward the
        // base also orients the box so its long axis points at the base, as in 1.12
        // (TurretHeadUtil.getTurretBaseFacing: first neighbour that is a base).
        Direction toBase = facingTowardBase(level, placePos);
        if (toBase == null) {
            return;
        }
        // The 1.12 head getBoundingBoxFromFacing: a box longer on the axis pointing at the base
        // (the rotated -0.3..0.3 by -0.5..0.5 box), centred in the cell.
        double hx = toBase.getAxis() == Direction.Axis.X ? 0.5 : 0.3;
        double hy = toBase.getAxis() == Direction.Axis.Y ? 0.5 : 0.3;
        double hz = toBase.getAxis() == Direction.Axis.Z ? 0.5 : 0.3;
        AABB box = new AABB(0.5 - hx, 0.5 - hy, 0.5 - hz, 0.5 + hx, 0.5 + hy, 0.5 + hz)
                .move(placePos.getX(), placePos.getY(), placePos.getZ());

        // Cyan when the cell is empty and the head can place, red when something is in the way.
        boolean placeable = level.isEmptyBlock(placePos);
        float r = placeable ? 0.0F : 1.0F;
        float g = placeable ? 1.0F : 0.0F;
        float b = placeable ? 1.0F : 0.0F;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        // Alpha 0.5, the value omlib passed to RenderUtil.drawHighlightBox. (1.12 also forced a line
        // width of 2.0; RenderType.lines() picks its own width, so the line is still a touch thicker.)
        LevelRenderer.renderLineBox(pose, lines, box.move(-cam.x, -cam.y, -cam.z),
                r, g, b, 0.5F);
    }

    // 1.12 TurretHeadUtil.getTurretBaseFacing: first neighbour direction that holds a base, else null.
    private static Direction facingTowardBase(Level level, BlockPos pos) {
        for (Direction facing : Direction.values()) {
            if (level.getBlockEntity(pos.relative(facing)) instanceof TurretBaseTileEntity) {
                return facing;
            }
        }
        return null;
    }
}
