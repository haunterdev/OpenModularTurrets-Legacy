package com.ommods.omlib.network;

import com.ommods.omlib.client.ClientPacketHandler;
import com.ommods.omlib.util.permission.TrustedPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server to client: here is your global trust list, open or refresh the screen with it.
 *
 * Doubles as the sync packet. Every server side mutation answers with a fresh one, so the screen
 * always shows what the server actually stored, never what the client hoped it did.
 */
public class PacketOpenGlobalTrust {

    /** One row: a name and an access level ordinal. */
    public record Entry(String name, int level) {
    }

    public final List<Entry> entries;

    public PacketOpenGlobalTrust(List<Entry> entries) {
        this.entries = entries;
    }

    /** Snapshot the owner's trust list into a packet. */
    public static PacketOpenGlobalTrust of(List<TrustedPlayer> trusted) {
        List<Entry> entries = new ArrayList<>();
        for (TrustedPlayer tp : trusted) {
            entries.add(new Entry(tp.getName(), tp.getAccessLevel().ordinal()));
        }
        return new PacketOpenGlobalTrust(entries);
    }

    public static void encode(PacketOpenGlobalTrust message, FriendlyByteBuf buf) {
        buf.writeInt(message.entries.size());
        for (Entry entry : message.entries) {
            buf.writeUtf(entry.name());
            buf.writeInt(entry.level());
        }
    }

    public static PacketOpenGlobalTrust decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUtf(), buf.readInt()));
        }
        return new PacketOpenGlobalTrust(entries);
    }

    public static void handle(PacketOpenGlobalTrust message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // A dist check, not DistExecutor.safeRunWhenOn: that one's SafeRunnable must capture nothing,
        // and this has to capture the message, which makes Forge log "Detected unsafe referent usage"
        // at FATAL. ClientPacketHandler is still named only inside the lambda body, so the dedicated
        // server never loads it or the screens it touches.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientPacketHandler.openGlobalTrust(message));
        }
        context.setPacketHandled(true);
    }
}
