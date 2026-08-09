package com.ommods.omlib.network;

import com.ommods.omlib.client.RayRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: draw a beam from {@code start} to {@code end} for {@code duration} ticks.
 *
 * <p>Ported from omlib 1.12 {@code MessageRenderRay}, minus its {@code bloom} flag, which upstream
 * carried on the wire and never read (its {@code Ray.render} has a {@code //TODO: bloom} where the
 * effect would go). Purely visual, so there is nothing here to authorise; the client cannot use it to
 * change any state.
 */
public class PacketRenderRay {

    private final Vec3 start;
    private final Vec3 end;
    private final float r;
    private final float g;
    private final float b;
    private final float a;
    private final int duration;

    public PacketRenderRay(Vec3 start, Vec3 end, float r, float g, float b, float a, int duration) {
        this.start = start;
        this.end = end;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        this.duration = duration;
    }

    public static void encode(PacketRenderRay message, FriendlyByteBuf buf) {
        buf.writeDouble(message.start.x);
        buf.writeDouble(message.start.y);
        buf.writeDouble(message.start.z);
        buf.writeDouble(message.end.x);
        buf.writeDouble(message.end.y);
        buf.writeDouble(message.end.z);
        buf.writeFloat(message.r);
        buf.writeFloat(message.g);
        buf.writeFloat(message.b);
        buf.writeFloat(message.a);
        buf.writeInt(message.duration);
    }

    public static PacketRenderRay decode(FriendlyByteBuf buf) {
        Vec3 start = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 end = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        return new PacketRenderRay(start, end, buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readInt());
    }

    public static void handle(PacketRenderRay message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // Not DistExecutor.safeRunWhenOn. Its SafeRunnable contract is that the lambda captures
        // nothing, and this one has to capture the message to have anything to draw, so Forge logged
        // "Detected unsafe referent usage" at FATAL for every beam a laser or railgun fired. A plain
        // dist check does the same job: RayRenderer is named only inside the lambda body, so a
        // dedicated server never resolves it.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> RayRenderer.add(message.start, message.end,
                    message.r, message.g, message.b, message.a, message.duration));
        }
        context.setPacketHandled(true);
    }
}
