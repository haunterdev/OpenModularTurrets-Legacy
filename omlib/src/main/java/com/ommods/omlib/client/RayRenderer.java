package com.ommods.omlib.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ommods.omlib.reference.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * The client side beam list, ported from omlib 1.12 {@code RenderManager} plus its {@code Ray} render
 * object. The server sends a start point, an end point, a colour and a lifetime in ticks; the beam is
 * drawn every frame until the lifetime runs out, then dropped.
 *
 * <p>This is how the laser and railgun turrets are visible in 1.12: a hitscan shot draws no particles
 * at all, it renders a coloured line from the barrel to whatever it struck.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class RayRenderer {

    /** One live beam. {@code ticksLeft} counts down on the client tick, exactly as 1.12's did. */
    private static final class Ray {
        private final Vec3 start;
        private final Vec3 end;
        private final float r;
        private final float g;
        private final float b;
        private final float a;
        private int ticksLeft;

        private Ray(Vec3 start, Vec3 end, float r, float g, float b, float a, int ticks) {
            this.start = start;
            this.end = end;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            this.ticksLeft = ticks;
        }
    }

    private static final List<Ray> RAYS = new ArrayList<>();

    private RayRenderer() {
    }

    /** Queue a beam for {@code ticks} client ticks. Called from the render ray packet handler. */
    public static void add(Vec3 start, Vec3 end, float r, float g, float b, float a, int ticks) {
        RAYS.add(new Ray(start, end, r, g, b, a, Math.max(1, ticks)));
    }

    /** Drop every live beam, so leaving a world does not carry them into the next one. */
    public static void clear() {
        RAYS.clear();
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RAYS.isEmpty()) {
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        RAYS.removeIf(ray -> --ray.ticksLeft <= 0);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // After the particles and translucent terrain, so a beam is not culled by the water surface
        // or drawn under a particle it should sit in front of.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || RAYS.isEmpty()) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(OMRenderTypes.RAY);

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = pose.last().pose();
        Matrix3f normal = pose.last().normal();
        for (Ray ray : RAYS) {
            // RenderType.lines wants a normal per vertex; the segment direction is what vanilla uses.
            Vec3 dir = ray.end.subtract(ray.start);
            if (dir.lengthSqr() < 1.0E-8) {
                continue;
            }
            dir = dir.normalize();
            float nx = (float) dir.x;
            float ny = (float) dir.y;
            float nz = (float) dir.z;
            lines.vertex(matrix, (float) ray.start.x, (float) ray.start.y, (float) ray.start.z)
                    .color(ray.r, ray.g, ray.b, ray.a)
                    .normal(normal, nx, ny, nz).endVertex();
            lines.vertex(matrix, (float) ray.end.x, (float) ray.end.y, (float) ray.end.z)
                    .color(ray.r, ray.g, ray.b, ray.a)
                    .normal(normal, nx, ny, nz).endVertex();
        }
        pose.popPose();
        buffers.endBatch(OMRenderTypes.RAY);
    }
}
