package com.ommods.openmodularturrets.client.turret;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.content.block.LeverBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the hand crank, ported box for box from 1.12 {@code ModelLever}: an axle pointing at the base
 * and a handle that spins with {@link LeverBlockEntity#rotation}. Same anchored-and-flipped transform
 * as the turret renderer, turned so the axle faces the crank's base.
 */
public class LeverRenderer implements BlockEntityRenderer<LeverBlockEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(OpenModularTurrets.MOD_ID, "lever_block"), "main");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(OpenModularTurrets.MOD_ID, "textures/block/lever_block.png");

    private final ModelPart root;

    public LeverRenderer(BlockEntityRendererProvider.Context context) {
        this.root = context.bakeLayer(LAYER);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("axle", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-1.0F, -1.0F, -9.0F, 2, 2, 8), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("handle", CubeListBuilder.create().mirror()
                .texOffs(22, 0).addBox(-1.0F, 0.0F, -3.0F, 2, 6, 2), PartPose.offset(0.0F, 16.0F, 0.0F));
        // 1.12 ModelLever texture size (field_78090_t/u = 32; each box func_78787_b(32,32)).
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(LeverBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        Direction toBase = be.baseDirection();
        if (toBase == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1.0F, -1.0F, -1.0F);
        // The axle model points -Z; turn it so it points at the base (after the Z flip, -Z is south).
        float yaw = switch (toBase) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            default -> 270.0F; // EAST
        };
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        // The crank spin, faithful to 1.12 LeverRenderer: each shape's own zRot is set to
        // rotation / 55, so the handle turns around the axle at the part's pivot (0,16,0). The old
        // code instead spun the whole PoseStack about the origin, and because the parts pivot a block
        // up, the handle swung out in a huge arc across several blocks.
        float spin = (be.rotation + (be.isTurning ? partialTick * 20.0f : 0.0F)) / 55.0F;
        root.getChild("axle").zRot = spin;
        root.getChild("handle").zRot = spin;
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        root.render(pose, vc, light, overlay);
        pose.popPose();
    }
}
