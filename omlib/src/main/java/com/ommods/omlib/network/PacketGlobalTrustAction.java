package com.ommods.omlib.network;

import com.mojang.authlib.GameProfile;
import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.permission.EnumAccessLevel;
import com.ommods.omlib.util.permission.GlobalTrustRegistry;
import com.ommods.omlib.util.permission.GlobalTrustSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Client to server: edit my global trust list.
 *
 * The packet names an operation and a target, and nothing else. Who is doing it is the connection's
 * authenticated player ({@code ctx.getSender()}), never a field in the payload. That is the whole
 * difference from upstream, whose handler read the owner off the wire and asked no questions.
 * Because the actor is always the sender, a player can only ever edit their own list.
 */
public class PacketGlobalTrustAction {

    public enum Op {ADD, REMOVE, SET_LEVEL}

    public final Op op;
    public final String targetName;
    public final int level;

    public PacketGlobalTrustAction(Op op, String targetName, int level) {
        this.op = op;
        this.targetName = targetName;
        this.level = level;
    }

    public static void encode(PacketGlobalTrustAction message, FriendlyByteBuf buf) {
        buf.writeEnum(message.op);
        buf.writeUtf(message.targetName);
        buf.writeInt(message.level);
    }

    public static PacketGlobalTrustAction decode(FriendlyByteBuf buf) {
        return new PacketGlobalTrustAction(buf.readEnum(Op.class), buf.readUtf(), buf.readInt());
    }

    public static void handle(PacketGlobalTrustAction message, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                apply(message, sender);
            }
        });
        context.setPacketHandled(true);
    }

    private static void apply(PacketGlobalTrustAction message, ServerPlayer sender) {
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return;
        }
        GlobalTrustSavedData trust = GlobalTrustSavedData.get(server);
        OMPlayer owner = ownerOf(sender);
        OMPlayer target = resolve(server, message.targetName);
        if (target == null) {
            // Silent no-op was the reported bug: the screen just sat there. In online mode a player
            // must be resolvable to a real uuid, so a name the server has never seen cannot be
            // trusted. Say so instead of doing nothing.
            say(sender, "Unknown player: " + message.targetName
                    + ". In online mode only players the server has seen can be trusted; turn on"
                    + " offlineModeSupport to trust by name.");
            return;
        }

        String name = target.getName();
        switch (message.op) {
            case ADD -> {
                GlobalTrustRegistry.Result result =
                        trust.addTrustedPlayer(owner, target, levelOf(message.level));
                switch (result) {
                    case ADDED -> say(sender, "Trusted " + name + " globally.");
                    case ALREADY_PRESENT -> say(sender, name + " is already on your global trust list.");
                    case SELF -> say(sender, "You cannot trust yourself.");
                    default -> { }
                }
            }
            case REMOVE -> say(sender, trust.removeTrustedPlayer(owner, target) == GlobalTrustRegistry.Result.REMOVED
                    ? "Removed " + name + " from your global trust list."
                    : name + " is not on your global trust list.");
            case SET_LEVEL -> say(sender, trust.changePermission(owner, target, levelOf(message.level)) == GlobalTrustRegistry.Result.LEVEL_CHANGED
                    ? name + " is now " + levelOf(message.level).getName() + " on your global trust list."
                    : name + " is not on your global trust list.");
        }

        // Answer with the stored truth so the screen reflects the server, not its own guess.
        OMLibChannel.sendToPlayer(PacketOpenGlobalTrust.of(trust.getTrustedPlayers(owner)), sender);
    }

    private static void say(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    private static OMPlayer ownerOf(ServerPlayer sender) {
        return OMConfig.Permission.offlineModeSupport
                ? new OMPlayer(null, sender.getGameProfile().getName())
                : new OMPlayer(sender.getUUID(), sender.getGameProfile().getName());
    }

    @Nullable
    private static OMPlayer resolve(MinecraftServer server, String name) {
        if (OMConfig.Permission.offlineModeSupport) {
            return new OMPlayer(null, name);
        }
        Optional<GameProfile> profile = server.getProfileCache() == null
                ? Optional.empty()
                : server.getProfileCache().get(name);
        return profile.map(p -> new OMPlayer(p.getId(), p.getName())).orElse(null);
    }

    private static EnumAccessLevel levelOf(int ordinal) {
        EnumAccessLevel[] values = EnumAccessLevel.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : EnumAccessLevel.NONE;
    }
}
