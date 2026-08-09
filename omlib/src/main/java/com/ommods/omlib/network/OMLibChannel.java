package com.ommods.omlib.network;

import com.ommods.omlib.reference.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * omlib's packet channel. Distinct from {@link NetworkRegistry} (the in world power network); this
 * is the client to server wire.
 *
 * The client to server packets ({@link PacketGlobalTrustAction}, {@link PacketTileTrustAction})
 * never carry who the actor is. The server reads that from the connection ({@code ctx.getSender()}),
 * which is the fix for upstream's global trust packets that trusted an owner field off the wire.
 */
public final class OMLibChannel {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Reference.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private OMLibChannel() {
    }

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, PacketGlobalTrustAction.class,
                PacketGlobalTrustAction::encode, PacketGlobalTrustAction::decode,
                PacketGlobalTrustAction::handle);
        INSTANCE.registerMessage(id++, PacketOpenGlobalTrust.class,
                PacketOpenGlobalTrust::encode, PacketOpenGlobalTrust::decode,
                PacketOpenGlobalTrust::handle);
        INSTANCE.registerMessage(id++, PacketTileTrustAction.class,
                PacketTileTrustAction::encode, PacketTileTrustAction::decode,
                PacketTileTrustAction::handle);
        INSTANCE.registerMessage(id++, PacketOpenTileTrust.class,
                PacketOpenTileTrust::encode, PacketOpenTileTrust::decode,
                PacketOpenTileTrust::handle);
        INSTANCE.registerMessage(id++, PacketRenderRay.class,
                PacketRenderRay::encode, PacketRenderRay::decode,
                PacketRenderRay::handle);
    }

    public static void sendToPlayer(Object message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Send to every player within {@code radius} of a point, the 1.20.1 equivalent of 1.12's
     * {@code sendToAllAround} with a {@code TargetPoint}. Used by the beam packet, which upstream sent
     * with a 120 block radius.
     */
    public static void sendToNear(Object message, ServerLevel level, Vec3 at, double radius) {
        INSTANCE.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(at.x, at.y, at.z, radius, level.dimension())),
                message);
    }
}
