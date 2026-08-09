package com.ommods.openmodularturrets.client.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ommods.openmodularturrets.content.projectile.OMTProjectile;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

/**
 * Draws a flying turret round as an item, ported from 1.12 {@code ItemProjectileRenderer} and the
 * {@code RenderSnowball} registrations in {@code ProjectileRenderers}. Upstream renders four of its six
 * projectiles this way (bullet, grenade, blazing clay, and the disposable turret's thrown round); only
 * the rocket has a model and only the potato is deliberately invisible.
 *
 * <p>Like upstream, the first two frames are skipped: a shot spawns inside the turret head it was fired
 * from, and drawing it there puts an item briefly inside the muzzle.
 */
public class ItemProjectileRenderer<T extends OMTProjectile> extends EntityRenderer<T> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/atlas/blocks.png");
    private static final int SKIPPED_FRAMES = 1;

    private final ItemRenderer itemRenderer;
    private final Function<T, ItemStack> stackFor;

    public ItemProjectileRenderer(EntityRendererProvider.Context context, Function<T, ItemStack> stackFor) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.stackFor = stackFor;
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        if (entity.tickCount <= SKIPPED_FRAMES) {
            return;
        }
        ItemStack stack = stackFor.apply(entity);
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Billboard the item at the camera, as upstream did with its two GlStateManager rotations.
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, buffers, entity.level(), entity.getId());
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
