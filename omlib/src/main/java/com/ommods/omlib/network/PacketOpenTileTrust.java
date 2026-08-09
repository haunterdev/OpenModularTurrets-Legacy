package com.ommods.omlib.network;

import com.ommods.omlib.client.ClientPacketHandler;
import com.ommods.omlib.network.PacketOpenGlobalTrust.Entry;
import com.ommods.omlib.util.permission.TrustedPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server to client: here is a block's trust list, open or refresh its screen with it.
 *
 * The block position tags the payload so the client can tell a stale sync (for a screen it already
 * closed, or a different block) from a fresh one. Same role as {@link PacketOpenGlobalTrust} but for
 * a per block list rather than the player wide one, and it reuses that packet's {@link Entry} row.
 */
public class PacketOpenTileTrust {

    private final BlockPos pos;
    private final List<Entry> entries;

    public PacketOpenTileTrust(BlockPos pos, List<Entry> entries) {
        this.pos = pos;
        this.entries = entries;
    }

    public BlockPos pos() {
        return pos;
    }

    public List<Entry> entries() {
        return entries;
    }

    /** Snapshot a block's trust list into a packet. */
    public static PacketOpenTileTrust of(BlockPos pos, List<TrustedPlayer> trusted) {
        List<Entry> entries = new ArrayList<>();
        for (TrustedPlayer tp : trusted) {
            entries.add(new Entry(tp.getName(), tp.getAccessLevel().ordinal()));
        }
        return new PacketOpenTileTrust(pos, entries);
    }

    public static void encode(PacketOpenTileTrust message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeInt(message.entries.size());
        for (Entry entry : message.entries) {
            buf.writeUtf(entry.name());
            buf.writeInt(entry.level());
        }
    }

    public static PacketOpenTileTrust decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readInt();
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readUtf(), buf.readInt()));
        }
        return new PacketOpenTileTrust(pos, entries);
    }

    public static void handle(PacketOpenTileTrust message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        // Same reason as PacketOpenGlobalTrust: safeRunWhenOn forbids the capture this needs.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> ClientPacketHandler.openTileTrust(message));
        }
        context.setPacketHandled(true);
    }
}
