package com.ommods.openmodularturrets.client.turret;

import com.ommods.openmodularturrets.OpenModularTurrets;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.block.TurretBaseBlockEntity;
import com.ommods.openmodularturrets.content.turret.TurretType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the 3D turret in world. Reproduces the 1.12 TESR transform: the model is anchored at the top
 * of the block (translate 0.5, 1.5, 0.5) and flipped on Y and Z, then the ported ModelPart renders
 * itself (ModelPart divides by 16 internally, so no extra scale). Aiming is per part, faithful to
 * each 1.12 model's setRotationForTarget: the head parts take both pitch and yaw, the gimbal ring
 * swivels with yaw only, and the base plate with its pole never moves. The relativistic and
 * teleporter heads never aim at all, exactly as their 1.12 renderers passed doRotation = false.
 */
public class TurretRenderer implements BlockEntityRenderer<TurretBaseBlockEntity> {

    // The parts each 1.12 model rotated fully toward the target (xRot from pitch, yRot from yaw) in
    // setRotationForTarget. RELATIVISTIC and TELEPORTER had empty implementations, hence no entry.
    private static final Map<TurretType, List<String>> AIM_PARTS = new EnumMap<>(Map.of(
            TurretType.MACHINE_GUN, List.of("cross_bar", "gun_stock", "barrel"),
            TurretType.LASER, List.of("cross_bar", "chamber", "bar_under", "bar_middle", "bar_top", "counter_weight"),
            TurretType.RAILGUN, List.of("barrel_top", "barrel_bot", "barrel_right", "barrel_left",
                    "body_bot", "body_top", "guard_binder", "right_guard", "left_guard"),
            TurretType.ROCKET, List.of("cross_bar", "launcher", "missile1", "missile2"),
            TurretType.GRENADE, List.of("cross_bar", "barrel", "chamber", "sight"),
            TurretType.INCENDIARY, List.of("cross_bar", "barrel1", "barrel2", "tank"),
            TurretType.POTATO_CANNON, List.of("cross_bar", "barrel", "chamber"),
            TurretType.DISPOSABLE, List.of("cross_bar", "cannon")));

    // Models with the shared gimbal: box_under turns with yaw, and the two cheek plates (which are
    // pre-rolled 90 degrees on Z) take yaw on their local X, matching 1.12.
    private static final List<TurretType> GIMBAL_TYPES = List.of(TurretType.MACHINE_GUN, TurretType.LASER,
            TurretType.ROCKET, TurretType.GRENADE, TurretType.INCENDIARY, TurretType.POTATO_CANNON,
            TurretType.DISPOSABLE);

    // 1.12 AbstractTurretRenderer.addonsRendered, a bitmask: 1 damage amp, 2 solar panel, 4 redstone
    // reactor. Every head draws all three except the relativistic (solar only) and the teleporter (none).
    private static final int ADDON_AMP = 1;
    private static final int ADDON_SOLAR = 2;
    private static final int ADDON_REACTOR = 4;

    private static int addonsRendered(TurretType type) {
        return switch (type) {
            case RELATIVISTIC -> ADDON_SOLAR;
            case TELEPORTER -> 0;
            default -> ADDON_AMP | ADDON_SOLAR | ADDON_REACTOR;
        };
    }

    private final Map<TurretType, ModelPart> roots = new EnumMap<>(TurretType.class);
    private final ModelPart damageAmp;
    private final ModelPart solarPanel;
    private final ModelPart redstoneReactor;

    public TurretRenderer(BlockEntityRendererProvider.Context context) {
        for (TurretType type : TurretType.values()) {
            roots.put(type, context.bakeLayer(TurretModels.layerFor(type)));
        }
        damageAmp = context.bakeLayer(AddonModels.DAMAGE_AMP);
        solarPanel = context.bakeLayer(AddonModels.SOLAR_PANEL);
        redstoneReactor = context.bakeLayer(AddonModels.REDSTONE_REACTOR);
    }

    @Override
    public void render(TurretBaseBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        // A concealer addon tucks the idle head away entirely, the port of 1.12's CONCEALED state.
        if (be.isConcealed()) {
            return;
        }
        // Light comes from the head's own position, as the 1.12 TESR did. An earlier fix sampled the
        // block above instead, to dodge models rendering black; the actual cure for that was giving
        // the head block noOcclusion (both landed together in ed8b5a2), and sampling above breaks a
        // head mounted under a base - the block above it is then the solid base, so it read as pitch
        // black.
        render(be.turretType(), be.renderYaw(), be.renderPitch(), be.baseDirection(),
                pose, buffer, light, OverlayTexture.NO_OVERLAY);
        renderAddons(be, pose, buffer, light);
    }

    /**
     * The addon models 1.12's AbstractTurretRenderer draws on the head once its base carries them:
     * the damage amp rack, the solar panel and the redstone reactor. Each swings with the aim, and
     * which of the three a head draws comes from its {@link #addonsRendered} mask.
     */
    private void renderAddons(TurretBaseBlockEntity be, PoseStack pose, MultiBufferSource buffer, int light) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        TurretBaseTileEntity base = be.findBase(level, be.getBlockPos());
        if (base == null) {
            return;
        }
        int mask = addonsRendered(be.turretType());
        float rotY = rotationXZ(be.renderYaw());
        float rotX = rotationXY(be.renderPitch());
        if ((mask & ADDON_AMP) != 0 && base.hasDamageAmpAddon()) {
            renderAddon(damageAmp, AddonModels.DAMAGE_AMP_TEXTURE, rotX, rotY, pose, buffer, light);
        }
        if ((mask & ADDON_SOLAR) != 0 && base.hasSolarPanelAddon()) {
            renderAddon(solarPanel, AddonModels.SOLAR_PANEL_TEXTURE, rotX, rotY, pose, buffer, light);
        }
        if ((mask & ADDON_REACTOR) != 0 && base.hasRedstoneReactorAddon()) {
            renderAddon(redstoneReactor, AddonModels.REDSTONE_REACTOR_TEXTURE, rotX, rotY, pose, buffer, light);
        }
    }

    private void renderAddon(ModelPart root, ResourceLocation texture, float rotX, float rotY,
                             PoseStack pose, MultiBufferSource buffer, int light) {
        // Each addon model's setRotationForTarget drove every one of its parts, so the whole model
        // turns with the head rather than any one piece of it.
        root.getAllParts().forEach(part -> {
            part.xRot = rotX;
            part.yRot = rotY;
        });
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1.0F, -1.0F, -1.0F);
        root.render(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** Shared by the world renderer and the item renderer; yaw/pitch turn the head toward its target. */
    public void render(TurretType type, float yaw, float pitch, @Nullable Direction toBase, PoseStack pose,
                       MultiBufferSource buffer, int light, int overlay) {
        ModelPart root = roots.get(type);
        if (root == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1.0F, -1.0F, -1.0F);
        applyAim(type, root, yaw, pitch);
        // 1.12 called setBaseRotation after setRotationForTarget, so the mount wins on the shared
        // parts. Keep that order.
        applyBaseRotation(root, toBase);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(textureFor(type)));
        root.render(pose, vc, light, overlay);
        pose.popPose();
    }

    // Faithful to omlib MathUtil: getRotationXZFromYawPitch = ((yaw - 90) / 90) * (pi/2) becomes the
    // Y rotation, getRotationXYFromYawPitch = ((pitch + 270) / 90) * (pi/2) the X rotation. Pitch 90
    // (a level target) lands on a full turn, i.e. no visible tilt.
    static float rotationXZ(float yaw) {
        return (float) (((yaw - 90.0) / 90.0) * (Math.PI / 2.0));
    }

    static float rotationXY(float pitch) {
        return (float) (((pitch + 270.0) / 90.0) * (Math.PI / 2.0));
    }

    private void applyAim(TurretType type, ModelPart root, float yaw, float pitch) {
        float rotY = rotationXZ(yaw);
        float rotX = rotationXY(pitch);
        for (String name : AIM_PARTS.getOrDefault(type, List.of())) {
            ModelPart part = root.getChild(name);
            part.xRot = rotX;
            part.yRot = rotY;
        }
        if (GIMBAL_TYPES.contains(type)) {
            root.getChild("box_under").yRot = rotY;
            root.getChild("box_left").xRot = rotY;
            root.getChild("box_right").xRot = rotY;
        }
    }

    /**
     * The 1.12 TurretHead.setSide mount table, applied by ModelAbstractTurret.setBaseRotation. A head
     * lives in the cell next to its base, so without this it renders upright wherever it is put and a
     * side-mounted turret looks like it is standing on the ground instead of hanging off the base.
     * Only a base directly below (the values are both zero) needs no rotation, which is why that one
     * case looked right. The second value is 1.12's baseFitRotationZ, which it fed to rotateAngleY.
     */
    private static final Map<Direction, float[]> MOUNT_ROTATION = Map.of(
            Direction.EAST, new float[]{1.56F, 1.565F},
            Direction.WEST, new float[]{1.56F, 4.705F},
            Direction.SOUTH, new float[]{1.56F, 3.145F},
            Direction.NORTH, new float[]{1.56F, 0.0F},
            Direction.UP, new float[]{3.145F, 0.0F},
            Direction.DOWN, new float[]{0.0F, 0.0F});

    // A head with no base (the inventory item) renders as if mounted downward, matching 1.12, whose
    // item path never called setBaseRotation and so left every mount angle at zero.
    private void applyBaseRotation(ModelPart root, @Nullable Direction toBase) {
        float[] rotation = MOUNT_ROTATION.get(toBase == null ? Direction.DOWN : toBase);
        float rotX = rotation[0];
        float rotY = rotation[1];
        if (root.hasChild("base")) {
            root.getChild("base").xRot = rotX;
            root.getChild("base").yRot = rotY;
        }
        if (root.hasChild("pole")) {
            root.getChild("pole").xRot = rotX;
            root.getChild("pole").yRot = rotY;
        }
        // setBaseRotation gave BoxUnder the X rotation only; its Y stays whatever the aim set.
        if (root.hasChild("box_under")) {
            root.getChild("box_under").xRot = rotX;
        }
    }

    public static ResourceLocation textureFor(TurretType type) {
        return new ResourceLocation(OpenModularTurrets.MOD_ID, "textures/block/" + type.registryName() + ".png");
    }
}
