package com.ommods.openmodularturrets.client.turret;

import com.ommods.openmodularturrets.OpenModularTurrets;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The three addon models 1.12's {@code AbstractTurretRenderer} draws on top of a turret head when the
 * base carries that addon: the damage amp's rod rack, the solar panel, and the redstone reactor.
 * Ported box for box from {@code ModelDamageAmp}, {@code ModelSolarPanelAddon} and
 * {@code ModelRedstoneReactor}, each on its own 64x64 texture.
 */
public final class AddonModels {

    public static final ModelLayerLocation DAMAGE_AMP = layer("addon_damage_amp");
    public static final ModelLayerLocation SOLAR_PANEL = layer("addon_solar_panel");
    public static final ModelLayerLocation REDSTONE_REACTOR = layer("addon_redstone_reactor");

    public static final ResourceLocation DAMAGE_AMP_TEXTURE = texture("addon_damage_amp");
    public static final ResourceLocation SOLAR_PANEL_TEXTURE = texture("addon_solar_panel");
    public static final ResourceLocation REDSTONE_REACTOR_TEXTURE = texture("addon_redstone_reactor");

    private AddonModels() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(new ResourceLocation(OpenModularTurrets.MOD_ID, name), "main");
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(OpenModularTurrets.MOD_ID, "textures/block/" + name + ".png");
    }

    /** ModelDamageAmp: a bottom bar with six rods standing on it, all pivoting with the head. */
    public static LayerDefinition createDamageAmp() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bottom_bar", CubeListBuilder.create().mirror()
                .texOffs(4, 15).addBox(-1.5F, 1.0F, -13.0F, 3, 1, 11), PartPose.offset(0.0F, 16.0F, 0.0F));
        // Six rods in two rows of three, offset by a thousandth so they do not z-fight the bar.
        rod(root, "rod1", 1.001F, -13.0F);
        rod(root, "rod2", 1.001F, -11.0F);
        rod(root, "rod3", -2.001F, -9.0F);
        rod(root, "rod4", -2.001F, -13.0F);
        rod(root, "rod5", -2.001F, -11.0F);
        rod(root, "rod6", 1.001F, -9.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void rod(PartDefinition root, String name, float x, float z) {
        root.addOrReplaceChild(name, CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(x, -5.0F, z, 1, 6, 1), PartPose.offset(0.0F, 16.0F, 0.0F));
    }

    /** ModelSolarPanelAddon: the panel plate and the two struts that hold it, each pre-tilted. */
    public static LayerDefinition createSolarPanel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("strut_front", CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-1.0F, -8.0F, -1.0F, 2, 6, 1),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.2321598F, 0.0F, 0.0F));
        root.addOrReplaceChild("panel", CubeListBuilder.create().mirror()
                .texOffs(26, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 1, 8), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("strut_back", CubeListBuilder.create().mirror()
                        .texOffs(0, 0).addBox(-1.0F, -8.0F, 0.0F, 2, 6, 1),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, -0.2565324F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** ModelRedstoneReactor: two rails out to a block with a pair of fins, pivoted at y 15. */
    public static LayerDefinition createRedstoneReactor() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartPose pivot = PartPose.offset(0.0F, 15.0F, 0.0F);
        root.addOrReplaceChild("rail_low", CubeListBuilder.create().mirror()
                .texOffs(5, 15).addBox(-1.0F, 0.0F, 0.0F, 2, 1, 11), pivot);
        root.addOrReplaceChild("rail_high", CubeListBuilder.create().mirror()
                .texOffs(5, 15).addBox(-1.0F, -3.0F, 0.0F, 2, 1, 11), pivot);
        root.addOrReplaceChild("reactor", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-3.0F, -4.0F, 9.0F, 6, 6, 6), pivot);
        root.addOrReplaceChild("fin_near", CubeListBuilder.create().mirror()
                .texOffs(29, 0).addBox(-1.0F, -6.0F, 11.0F, 2, 3, 1), pivot);
        root.addOrReplaceChild("fin_far", CubeListBuilder.create().mirror()
                .texOffs(29, 0).addBox(-1.0F, -6.0F, 13.0F, 2, 3, 1), pivot);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
