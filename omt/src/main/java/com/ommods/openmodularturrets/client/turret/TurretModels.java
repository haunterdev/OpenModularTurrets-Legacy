package com.ommods.openmodularturrets.client.turret;

import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.content.turret.TurretType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The turret head models, ported box for box from the 1.12 OMT ModelRenderer classes. 1.20.1
 * ModelPart uses the same pixel coordinate space and texOffs UV mapping as 1.12 ModelRenderer, so
 * each box carries over with its texture offset, mirror flag and rotation. The head sits in its rest
 * pose; the renderer turns the whole model toward the target.
 */
public final class TurretModels {

    private static final float Z90 = 1.570796F;

    private TurretModels() {
    }

    public static ModelLayerLocation layerFor(TurretType type) {
        return new ModelLayerLocation(new ResourceLocation(OpenModularTurrets.MOD_ID, type.registryName()), "main");
    }

    public static LayerDefinition createLayer(TurretType type) {
        return switch (type) {
            case MACHINE_GUN -> createMachineGun();
            case LASER -> createLaser();
            case RAILGUN -> createRailgun();
            case ROCKET -> createRocket();
            case GRENADE -> createGrenade();
            case INCENDIARY -> createIncendiary();
            case POTATO_CANNON -> createPotatoCannon();
            case RELATIVISTIC -> createRelativistic();
            case DISPOSABLE -> createDisposable();
            case TELEPORTER -> createTeleporter();
        };
    }

    // Shared base of most turrets: plate, pole, under box (texOffs 37/28/15 on x 0).
    private static void addSharedBase(PartDefinition root) {
        root.addOrReplaceChild("base", CubeListBuilder.create().mirror()
                .texOffs(0, 37).addBox(-6.0F, 7.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("pole", CubeListBuilder.create().mirror()
                .texOffs(0, 28).addBox(-2.0F, 4.0F, -2.0F, 4, 4, 4), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("box_under", CubeListBuilder.create().mirror()
                .texOffs(0, 15).addBox(-4.0F, 3.0F, -4.0F, 8, 1, 8), PartPose.offset(0.0F, 16.0F, 0.0F));
    }

    // Gimbal cheeks and cross bar (texOffs 0,15) shared by the standard heads.
    private static void addSharedGimbal(PartDefinition root) {
        root.addOrReplaceChild("box_left", CubeListBuilder.create().mirror()
                .texOffs(0, 15).addBox(-4.0F, 4.0F, -4.0F, 8, 1, 8),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, Z90));
        root.addOrReplaceChild("box_right", CubeListBuilder.create().mirror()
                .texOffs(0, 15).addBox(-4.0F, -5.0F, -4.0F, 8, 1, 8),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, Z90));
        root.addOrReplaceChild("cross_bar", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-4.0F, -2.0F, 0.0F, 8, 1, 1), PartPose.offset(0.0F, 16.0F, 0.0F));
    }

    public static LayerDefinition createMachineGun() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSharedBase(root);
        addSharedGimbal(root);
        root.addOrReplaceChild("gun_stock", CubeListBuilder.create().mirror()
                .texOffs(36, 0).addBox(-2.0F, -5.0F, -2.0F, 4, 8, 8), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("barrel", CubeListBuilder.create().mirror()
                .texOffs(32, 21).addBox(-1.0F, -2.0F, -15.0F, 2, 2, 14), PartPose.offset(0.0F, 17.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createLaser() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSharedBase(root);
        addSharedGimbal(root);
        root.addOrReplaceChild("chamber", CubeListBuilder.create().mirror()
                .texOffs(20, 0).addBox(-2.0F, -7.0F, -3.0F, 4, 7, 4), PartPose.offset(0.0F, 16.0F, 0.1F));
        root.addOrReplaceChild("bar_under", CubeListBuilder.create().mirror()
                .texOffs(37, 0).addBox(-1.0F, -2.0F, -12.0F, 2, 1, 10), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("bar_middle", CubeListBuilder.create().mirror()
                .texOffs(39, 26).addBox(-1.0F, -4.0F, -8.0F, 2, 1, 7), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("bar_top", CubeListBuilder.create().mirror()
                .texOffs(37, 16).addBox(-1.0F, -6.0F, -6.0F, 2, 1, 7), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("counter_weight", CubeListBuilder.create().mirror()
                .texOffs(0, 4).addBox(-2.0F, -6.0F, 1.0F, 4, 4, 4), PartPose.offset(0.0F, 17.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createRailgun() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-6.0F, 7.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("barrel_top", CubeListBuilder.create().mirror()
                .texOffs(25, 27).addBox(-1.0F, 2.0F, -16.0F, 2, 1, 17), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("barrel_bot", CubeListBuilder.create().mirror()
                .texOffs(25, 27).addBox(-1.0F, -1.0F, -16.0F, 2, 1, 17), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("barrel_right", CubeListBuilder.create().mirror()
                .texOffs(25, 45).addBox(-2.0F, -1.0F, -16.0F, 1, 2, 17), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("barrel_left", CubeListBuilder.create().mirror()
                .texOffs(25, 45).addBox(1.0F, -1.0F, -16.0F, 1, 2, 17), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("body_bot", CubeListBuilder.create().mirror()
                .texOffs(0, 29).addBox(-3.0F, 2.0F, 0.0F, 6, 2, 6), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("body_top", CubeListBuilder.create().mirror()
                .texOffs(0, 37).addBox(-3.0F, -3.0F, 1.0F, 6, 4, 6), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("binder", CubeListBuilder.create().mirror()
                .texOffs(0, 21).addBox(-1.0F, 1.0F, 3.0F, 2, 1, 1), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("right_guard", CubeListBuilder.create().mirror()
                .texOffs(0, 47).addBox(-6.1F, -5.0F, -3.0F, 1, 8, 8), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("left_guard", CubeListBuilder.create().mirror()
                .texOffs(0, 47).addBox(5.1F, -5.0F, -3.0F, 1, 8, 8), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("guard_binder", CubeListBuilder.create().mirror()
                .texOffs(0, 25).addBox(-6.0F, -0.9F, 0.0F, 12, 1, 1), PartPose.offset(0.0F, 15.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createRocket() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSharedBase(root);
        addSharedGimbal(root);
        root.addOrReplaceChild("launcher", CubeListBuilder.create().mirror()
                .texOffs(36, 0).addBox(-2.0F, -5.0F, -2.0F, 4, 8, 8), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("missile1", CubeListBuilder.create().mirror()
                .texOffs(0, 6).addBox(-1.0F, -5.0F, -4.0F, 2, 2, 2), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("missile2", CubeListBuilder.create().mirror()
                .texOffs(0, 6).addBox(-1.0F, -1.0F, -4.0F, 2, 2, 2), PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createGrenade() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSharedBase(root);
        addSharedGimbal(root);
        root.addOrReplaceChild("barrel", CubeListBuilder.create().mirror()
                .texOffs(32, 0).addBox(-2.0F, -4.0F, -8.0F, 4, 4, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("chamber", CubeListBuilder.create().mirror()
                .texOffs(35, 20).addBox(-3.0F, -5.0F, 3.0F, 6, 6, 6), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("sight", CubeListBuilder.create().mirror()
                .texOffs(0, 5).addBox(-1.0F, -6.0F, -7.0F, 2, 2, 4), PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createPotatoCannon() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        addSharedBase(root);
        addSharedGimbal(root);
        root.addOrReplaceChild("barrel", CubeListBuilder.create().mirror()
                .texOffs(36, 0).addBox(-1.0F, -2.0F, -11.0F, 2, 3, 12), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("chamber", CubeListBuilder.create().mirror()
                .texOffs(0, 4).addBox(-2.0F, -3.0F, 1.0F, 4, 4, 4), PartPose.offset(0.0F, 15.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createIncendiary() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Incendiary's pole sits higher (pose 0,19,0, box from 0) than the shared base, so inline it.
        root.addOrReplaceChild("base", CubeListBuilder.create().mirror()
                .texOffs(0, 37).addBox(-6.0F, 7.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("pole", CubeListBuilder.create().mirror()
                .texOffs(0, 28).addBox(-2.0F, 0.0F, -2.0F, 4, 4, 4), PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("box_under", CubeListBuilder.create().mirror()
                .texOffs(0, 15).addBox(-4.0F, 3.0F, -4.0F, 8, 1, 8), PartPose.offset(0.0F, 16.0F, 0.0F));
        addSharedGimbal(root);
        root.addOrReplaceChild("barrel1", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(1.0F, -1.0F, -10.0F, 2, 2, 11), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("barrel2", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-3.0F, -1.0F, -10.0F, 2, 2, 11), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("tank", CubeListBuilder.create().mirror()
                .texOffs(29, 0).addBox(-3.0F, -3.0F, -6.0F, 6, 4, 10), PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createRelativistic() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().mirror()
                .texOffs(0, 37).addBox(-6.0F, 7.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("spike1", CubeListBuilder.create().mirror()
                .texOffs(24, 0).addBox(-6.0F, 0.0F, -6.0F, 1, 8, 1), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("spike2", CubeListBuilder.create().mirror()
                .texOffs(24, 0).addBox(-6.0F, 0.0F, 5.0F, 1, 8, 1), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("spike3", CubeListBuilder.create().mirror()
                .texOffs(24, 0).addBox(5.0F, 0.0F, -6.0F, 1, 8, 1), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("spike4", CubeListBuilder.create().mirror()
                .texOffs(24, 0).addBox(5.0F, 0.0F, 5.0F, 1, 8, 1), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("base2", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-2.0F, 6.0F, -2.0F, 4, 2, 4), PartPose.offset(0.0F, 15.0F, 0.0F));
        root.addOrReplaceChild("crystal", CubeListBuilder.create().mirror()
                .texOffs(0, 25).addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4),
                PartPose.offsetAndRotation(0.0F, 15.0F, 0.0F, 0.7853982F, 0.7853982F, 0.7853982F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    // Ported box for box from 1.12 ModelTeleporterTurret: a plate, a stand below it, a central pillar
    // and four spinner vanes. Drawn at rest, which IS the 1.12 look: the spinner animation hooks in
    // ModelTeleporterTurret read rotationAnimation, but nothing ever increments it for a teleporter
    // (only AbstractDirectedTurret ticks it, and the teleporter is not one), so it is dead code there.
    public static LayerDefinition createTeleporter() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("base", CubeListBuilder.create().mirror()
                .texOffs(0, 37).addBox(-6.0F, 7.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("base_stand", CubeListBuilder.create().mirror()
                .texOffs(0, 51).addBox(-6.0F, -1.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("pillar", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4, 10, 4), PartPose.offset(0.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("spinner1", CubeListBuilder.create().mirror()
                .texOffs(0, 14).addBox(-5.0F, 0.0F, -2.0F, 1, 8, 4), PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("spinner2", CubeListBuilder.create().mirror()
                .texOffs(0, 26).addBox(-2.0F, 0.0F, 4.0F, 4, 8, 1), PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("spinner3", CubeListBuilder.create().mirror()
                .texOffs(0, 26).addBox(-2.0F, 0.0F, -5.0F, 4, 8, 1), PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("spinner4", CubeListBuilder.create().mirror()
                .texOffs(0, 14).addBox(4.0F, 0.0F, -2.0F, 1, 8, 4), PartPose.offset(0.0F, 14.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createDisposable() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Disposable's gimbal and under box use texOffs y 19 (super 37,28,19), so inline it.
        root.addOrReplaceChild("base", CubeListBuilder.create().mirror()
                .texOffs(0, 37).addBox(-6.0F, 7.0F, -6.0F, 12, 1, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("pole", CubeListBuilder.create().mirror()
                .texOffs(0, 28).addBox(-2.0F, 4.0F, -2.0F, 4, 4, 4), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("box_under", CubeListBuilder.create().mirror()
                .texOffs(0, 19).addBox(-4.0F, 3.0F, -4.0F, 8, 1, 8), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("box_left", CubeListBuilder.create().mirror()
                .texOffs(0, 19).addBox(-4.0F, 4.0F, -4.0F, 8, 1, 8),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, Z90));
        root.addOrReplaceChild("box_right", CubeListBuilder.create().mirror()
                .texOffs(0, 19).addBox(-4.0F, -5.0F, -4.0F, 8, 1, 8),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, 0.0F, 0.0F, Z90));
        root.addOrReplaceChild("cross_bar", CubeListBuilder.create().mirror()
                .texOffs(0, 0).addBox(-4.0F, -2.0F, 0.0F, 8, 1, 1), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("cannon", CubeListBuilder.create().mirror()
                .texOffs(20, 0).addBox(-2.0F, -3.0F, -12.0F, 4, 4, 14), PartPose.offset(0.0F, 16.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
