package com.ommods.omlib.client;

import com.ommods.omlib.network.PacketOpenGlobalTrust;
import com.ommods.omlib.network.PacketOpenTileTrust;
import net.minecraft.client.Minecraft;

/**
 * Client side landing spot for packets. Referenced only through DistExecutor on the client, so the
 * dedicated server never loads it or the screens it touches.
 */
public final class ClientPacketHandler {

    private ClientPacketHandler() {
    }

    public static void openGlobalTrust(PacketOpenGlobalTrust message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GlobalTrustScreen screen) {
            screen.setEntries(message.entries);
        } else {
            minecraft.setScreen(new GlobalTrustScreen(message.entries));
        }
    }

    public static void openTileTrust(PacketOpenTileTrust message) {
        Minecraft minecraft = Minecraft.getInstance();
        // Only refresh a screen already showing this block; a sync for another block opens fresh.
        if (minecraft.screen instanceof TileTrustScreen screen && screen.matches(message.pos())) {
            screen.setEntries(message.entries());
        } else {
            minecraft.setScreen(new TileTrustScreen(message.pos(), message.entries()));
        }
    }
}
