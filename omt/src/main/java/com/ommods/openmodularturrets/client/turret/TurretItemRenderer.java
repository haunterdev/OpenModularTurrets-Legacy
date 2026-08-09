package com.ommods.openmodularturrets.client.turret;

import com.ommods.openmodularturrets.content.block.TurretBaseBlock;
import com.ommods.openmodularturrets.content.turret.TurretType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Renders a turret head item as the same 3D model shown in world, so the inventory icon matches the
 * placed block instead of the flat texture sheet. Baked lazily from the entity model set the first
 * time each type is drawn, which is safe because items only render after the layers are registered.
 */
public class TurretItemRenderer extends BlockEntityWithoutLevelRenderer {

    // Head model mid height in block units: pivot at 16px, geometry spans ~10..24px, centre ~17px.
    private static final float MODEL_CENTRE = 17.0F / 16.0F;

    private final Map<TurretType, ModelPart> roots = new EnumMap<>(TurretType.class);

    public TurretItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    private ModelPart root(TurretType type) {
        return roots.computeIfAbsent(type,
                t -> Minecraft.getInstance().getEntityModels().bakeLayer(TurretModels.layerFor(t)));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        TurretType type = typeOf(stack);
        if (type == null) {
            return;
        }
        pose.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // Faithful to the 1.12 AbstractTurretRenderer item path (turretHead == null): anchor at
            // the block top, shrink and flip on Y and Z, nudge, then the classic 45 degree tilt about
            // the (2.5, -4.5, -1) axis. Same order the 1.12 TESR applied, so the inventory icon sits
            // at the 1.12 angle.
            pose.translate(0.5, 1.5, 0.5);
            pose.scale(0.7F, -0.7F, -0.7F);
            pose.translate(0.0, 0.4, 0.5);
            pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(45.0),
                    new Vector3f(2.5F, -4.5F, -1.0F).normalize()));
        } else {
            // Every other context (dropped on the ground, item frame, in hand): the 1.12 GUI anchor
            // lifts the model ~1.5 blocks, so a dropped item floats off its shadow. Instead centre the
            // model in the item's unit cube upright, flipping Y/Z like the world renderer. MODEL_CENTRE
            // is the head model's mid height in block units (parts pivot at 16px, spanning ~10..24px).
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(0.5F, -0.5F, -0.5F);
            pose.translate(0.0, -MODEL_CENTRE, 0.0);
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TurretRenderer.textureFor(type)));
        root(type).render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    @Nullable
    private static TurretType typeOf(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof TurretBaseBlock head) {
            return head.turretType();
        }
        return null;
    }
}
