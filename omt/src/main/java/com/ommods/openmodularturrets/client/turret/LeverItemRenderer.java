package com.ommods.openmodularturrets.client.turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ommods.openmodularturrets.OpenModularTurrets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders the crank lever item as the same 3D hand crank drawn in world, so its inventory icon is the
 * model, not a flat sheet. Faithful to the 1.12 {@code LeverRenderer} item path ({@code te == null}):
 * the same anchor, flip and 45 degree tilt the turret icons use, with the crank at its rest angle.
 */
public class LeverItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(OpenModularTurrets.MOD_ID, "textures/block/lever_block.png");

    private ModelPart root;

    public LeverItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    private ModelPart root() {
        if (root == null) {
            root = Minecraft.getInstance().getEntityModels().bakeLayer(LeverRenderer.LAYER);
        }
        return root;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        if (context == ItemDisplayContext.GUI) {
            // The 1.12 LeverRenderer item transform (te == null), identical to the turret icon path.
            pose.translate(0.5, 1.5, 0.5);
            pose.scale(0.7F, -0.7F, -0.7F);
            pose.translate(0.0, 0.4, 0.5);
            pose.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(45.0),
                    new Vector3f(2.5F, -4.5F, -1.0F).normalize()));
        } else {
            // Ground / frame / hand: centre the crank in the item cube so a dropped item rests on its
            // shadow instead of floating at the GUI anchor. Model spans ~15..22px, centre ~18.5px.
            pose.translate(0.5, 0.5, 0.5);
            pose.scale(0.5F, -0.5F, -0.5F);
            pose.translate(0.0, -18.5F / 16.0F, 0.0);
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        root().render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
