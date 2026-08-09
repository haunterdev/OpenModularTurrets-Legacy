package com.ommods.omlib.network;

import com.mojang.authlib.GameProfile;
import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.permission.IHasTrustManager;
import com.ommods.omlib.network.PacketGlobalTrustAction.Op;
import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.PlayerUtil;
import com.ommods.omlib.util.permission.EnumAccessLevel;
import com.ommods.omlib.util.permission.TrustManager;
import com.ommods.omlib.util.permission.TrustedPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Client to server: edit a block's trust list.
 *
 * The block is named by position; who is acting is the connection's authenticated player, never a
 * payload field. The server re-resolves the block entity, checks the sender is its owner or admin
 * (upstream's one correct branch, the {@code hasTile} path), then mutates and answers with the
 * stored list. A crafted packet can only ever touch a block the sender already administers.
 */
public class PacketTileTrustAction {

    private final BlockPos pos;
    private final Op op;
    private final String targetName;
    private final int level;

    public PacketTileTrustAction(BlockPos pos, Op op, String targetName, int level) {
        this.pos = pos;
        this.op = op;
        this.targetName = targetName;
        this.level = level;
    }

    public static void encode(PacketTileTrustAction message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeEnum(message.op);
        buf.writeUtf(message.targetName);
        buf.writeInt(message.level);
    }

    public static PacketTileTrustAction decode(FriendlyByteBuf buf) {
        return new PacketTileTrustAction(buf.readBlockPos(), buf.readEnum(Op.class),
                buf.readUtf(), buf.readInt());
    }

    public static void handle(PacketTileTrustAction message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                apply(message, sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void apply(PacketTileTrustAction message, ServerPlayer sender) {
        Level level = sender.level();
        // Cross dimension is impossible (the sender's own level), and an unloaded block yields no
        // block entity, so both are handled by this one lookup returning nothing.
        // Any owned block that carries a trust list, not one particular block: the turret base is the
        // real consumer, and matching a single class here made its trust editor a no-op.
        BlockEntity blockEntity = level.getBlockEntity(message.pos);
        if (!(blockEntity instanceof OMBlockEntity trust) || !(blockEntity instanceof IHasTrustManager holder)) {
            return;
        }
        // The trust boundary: only the owner or an admin trustee may edit. A non admin who somehow
        // sent this (a crafted packet) is told no and nothing changes.
        if (!PlayerUtil.isPlayerAdmin(sender, trust)) {
            say(sender, "You are not allowed to edit this block's trust list.");
            return;
        }

        MinecraftServer server = sender.getServer();
        TrustManager manager = holder.getTrustManager();
        switch (message.op) {
            case ADD -> {
                OMPlayer target = resolve(server, message.targetName);
                if (target == null) {
                    say(sender, "Unknown player: " + message.targetName
                            + ". In online mode only players the server has seen can be trusted;"
                            + " turn on offlineModeSupport to trust by name.");
                    return;
                }
                if (manager.getTrustedPlayer(target.getName()) != null) {
                    say(sender, target.getName() + " is already trusted on this block.");
                } else if (manager.addTrustedPlayer(new TrustedPlayer(target, levelOf(message.level)))) {
                    say(sender, "Trusted " + target.getName() + " on this block.");
                }
            }
            case REMOVE -> say(sender, manager.removeTrustedPlayer(message.targetName)
                    ? "Removed " + message.targetName + " from this block."
                    : message.targetName + " is not trusted on this block.");
            case SET_LEVEL -> say(sender, manager.changePermission(message.targetName, levelOf(message.level))
                    ? message.targetName + " is now " + levelOf(message.level).getName() + " on this block."
                    : message.targetName + " is not trusted on this block.");
        }

        trust.setChanged();
        // Answer with the stored truth so the screen reflects the server, not its own guess.
        OMLibChannel.sendToPlayer(
                PacketOpenTileTrust.of(message.pos, manager.getTrustedPlayers()), sender);
    }

    private static void say(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    @Nullable
    private static OMPlayer resolve(@Nullable MinecraftServer server, String name) {
        if (OMConfig.Permission.offlineModeSupport) {
            return new OMPlayer(null, name);
        }
        if (server == null || server.getProfileCache() == null) {
            return null;
        }
        Optional<GameProfile> profile = server.getProfileCache().get(name);
        return profile.map(p -> new OMPlayer(p.getId(), p.getName())).orElse(null);
    }

    private static EnumAccessLevel levelOf(int ordinal) {
        EnumAccessLevel[] values = EnumAccessLevel.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EnumAccessLevel.NONE;
    }
}
