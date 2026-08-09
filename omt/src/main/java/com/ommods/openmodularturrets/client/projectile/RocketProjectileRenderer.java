package com.ommods.openmodularturrets.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ommods.openmodularturrets.content.projectile.RocketProjectileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws the rocket, ported from 1.12 {@code RocketRenderer}. Upstream builds the round out of the same
 * geometry vanilla uses for an arrow, on its own {@code rocket.png}: a pair of back-facing quads for the
 * body and four fins swept around the axis, all at the arrow's 0.05625 scale, rotated onto the flight
 * path and rolled 45 degrees. The first two frames are skipped so the round is not drawn inside the
 * muzzle it just left.
 */
public class RocketProjectileRenderer extends EntityRenderer<RocketProjectileEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("openmodularturrets", "textures/block/rocket.png");
    private static final float SCALE = 0.05625F;

    public RocketProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RocketProjectileEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        if (entity.tickCount <= 1) {
            return;
        }
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, entity.xRotO, entity.getXRot())));
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-4.0F, 0.0F, 0.0F);

        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutout(TEXTURE));
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        Matrix3f normal = last.normal();

        // Body: the same quad drawn from both sides, so the round is solid from any angle.
        vertex(buffer, matrix, normal, packedLight, -7, -2, -2, 0.0F, 0.15625F);
        vertex(buffer, matrix, normal, packedLight, -7, -2, 2, 0.15625F, 0.15625F);
        vertex(buffer, matrix, normal, packedLight, -7, 2, 2, 0.15625F, 0.3125F);
        vertex(buffer, matrix, normal, packedLight, -7, 2, -2, 0.0F, 0.3125F);

        vertex(buffer, matrix, normal, packedLight, -7, 2, -2, 0.0F, 0.15625F);
        vertex(buffer, matrix, normal, packedLight, -7, 2, 2, 0.15625F, 0.15625F);
        vertex(buffer, matrix, normal, packedLight, -7, -2, 2, 0.15625F, 0.3125F);
        vertex(buffer, matrix, normal, packedLight, -7, -2, -2, 0.0F, 0.3125F);

        // Four fins, each a quarter turn further around the flight axis.
        for (int fin = 0; fin < 4; ++fin) {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            last = pose.last();
            matrix = last.pose();
            normal = last.normal();
            vertex(buffer, matrix, normal, packedLight, -8, -2, 0, 0.0F, 0.0F);
            vertex(buffer, matrix, normal, packedLight, 8, -2, 0, 0.5F, 0.0F);
            vertex(buffer, matrix, normal, packedLight, 8, 2, 0, 0.5F, 0.15625F);
            vertex(buffer, matrix, normal, packedLight, -8, 2, 0, 0.0F, 0.15625F);
        }

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normal, int light,
                               int x, int y, int z, float u, float v) {
        buffer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(RocketProjectileEntity entity) {
        return TEXTURE;
    }
}
