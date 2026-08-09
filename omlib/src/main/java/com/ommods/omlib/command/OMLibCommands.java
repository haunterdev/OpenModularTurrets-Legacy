package com.ommods.omlib.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.ommods.omlib.block.OMBlockEntity;
import com.ommods.omlib.config.OMConfig;
import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.permission.EnumAccessLevel;
import com.ommods.omlib.network.OMLibChannel;
import com.ommods.omlib.network.PacketOpenGlobalTrust;
import com.ommods.omlib.util.permission.GlobalTrustRegistry;
import com.ommods.omlib.util.permission.GlobalTrustSavedData;
import com.ommods.omlib.util.permission.OwnerShareRegistry;
import com.ommods.omlib.util.permission.OwnerShareSavedData;
import com.ommods.omlib.util.permission.TrustedPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * omlib's commands.
 *
 * One /omlib root with subcommands rather than upstream's three flat roots (/omshareowner,
 * /omchangeowner, /omdebug): one namespace, and tab completion makes them discoverable. The spec's
 * "preserve upstream names" convention is about Java API surface so later OMT and OMPD phases still
 * map onto upstream source, and no OMT code calls a command by name.
 *
 * Presentation lives here. OwnerShareRegistry returns a Result and this decides what to say about
 * it, which is why the registry needs no game to test.
 */
public final class OMLibCommands {

    private OMLibCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("omlib")
                        .then(Commands.literal("share")
                                // Permission 0: a player manages their own share list.
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                                .executes(context -> share(context, true))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                                .executes(context -> share(context, false))))
                                .then(Commands.literal("list")
                                        .executes(OMLibCommands::listShares)))
                        .then(Commands.literal("trust")
                                // Permission 0: a player manages their own global trust list. The
                                // owner is always the command sender, never a payload, which is the
                                // whole fix for upstream's unauthenticated global trust packets.
                                .then(Commands.literal("add")
                                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                                .executes(context -> trustAdd(context, EnumAccessLevel.OPEN_GUI))
                                                .then(Commands.argument("level", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(LEVEL_NAMES, b))
                                                        .executes(context -> trustAdd(context, level(context))))))
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                                .executes(OMLibCommands::trustRemove)))
                                .then(Commands.literal("setlevel")
                                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                                .then(Commands.argument("level", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(LEVEL_NAMES, b))
                                                        .executes(OMLibCommands::trustSetLevel))))
                                .then(Commands.literal("list")
                                        .executes(OMLibCommands::trustList))
                                .then(Commands.literal("gui")
                                        .executes(OMLibCommands::trustGui)))
                        .then(Commands.literal("changeowner")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                                        .executes(OMLibCommands::changeOwner)))))
                        .then(Commands.literal("debug")
                                .requires(source -> source.hasPermission(4))
                                .then(Commands.literal("on")
                                        .executes(context -> debug(context, true)))
                                .then(Commands.literal("off")
                                        .executes(context -> debug(context, false)))));
    }

    /**
     * Resolves a name to an OMPlayer.
     *
     * Offline mode decides how the uuid is derived, never whether the command works. Upstream had
     * this inverted: add and remove both required offline mode to be **off**, so turning offline
     * mode support on disabled sharing entirely.
     */
    private static OMPlayer toOMPlayer(GameProfile profile) {
        if (OMConfig.Permission.offlineModeSupport) {
            return new OMPlayer(null, profile.getName());
        }
        return new OMPlayer(profile.getId(), profile.getName());
    }

    private static OMPlayer requireSourcePlayer(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        // A console sender has no uuid, and upstream built new Player(null, "Server") from it and
        // let it reach the save path. Sharing is personal, so demand a player.
        ServerPlayer player = context.getSource().getPlayerOrException();
        return OMConfig.Permission.offlineModeSupport
                ? new OMPlayer(null, player.getGameProfile().getName())
                : new OMPlayer(player.getUUID(), player.getGameProfile().getName());
    }

    private static GameProfile singleProfile(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(context, "player");
        if (profiles.size() != 1) {
            throw new IllegalArgumentException("expected exactly one player");
        }
        return profiles.iterator().next();
    }

    private static int share(CommandContext<CommandSourceStack> context, boolean add)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        OwnerShareSavedData shares = OwnerShareSavedData.get(source.getServer());
        OMPlayer owner = requireSourcePlayer(context);
        OMPlayer target = toOMPlayer(singleProfile(context));

        OwnerShareRegistry.Result result = add
                ? shares.addSharePlayer(owner, target)
                : shares.removeSharePlayer(owner, target);

        String name = target.getName();
        switch (result) {
            case ADDED -> source.sendSuccess(
                    () -> Component.literal("Added " + name + " to your share list."), false);
            case REMOVED -> source.sendSuccess(
                    () -> Component.literal("Removed " + name + " from your share list."), false);
            case ALREADY_PRESENT -> source.sendFailure(
                    Component.literal(name + " is already on your share list."));
            case NOT_PRESENT -> source.sendFailure(
                    Component.literal(name + " is not on your share list."));
            case SELF -> source.sendFailure(
                    Component.literal("You cannot share with yourself."));
        }
        return result == OwnerShareRegistry.Result.ADDED
                || result == OwnerShareRegistry.Result.REMOVED ? 1 : 0;
    }

    private static int listShares(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        OwnerShareSavedData shares = OwnerShareSavedData.get(source.getServer());
        List<OMPlayer> sharePlayers = shares.getSharePlayers(requireSourcePlayer(context));
        if (sharePlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Your share list is empty."), false);
            return 0;
        }
        String names = sharePlayers.stream().map(OMPlayer::getName).collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Players on your share list: " + names), false);
        return sharePlayers.size();
    }

    private static final String[] LEVEL_NAMES = {"none", "open_gui", "change_settings", "admin"};

    /** Parses the level word to an EnumAccessLevel, or throws a plain command failure. */
    private static EnumAccessLevel level(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        String word = StringArgumentType.getString(context, "level");
        for (EnumAccessLevel level : EnumAccessLevel.values()) {
            if (level.getName().equalsIgnoreCase(word)) {
                return level;
            }
        }
        throw new SimpleCommandExceptionType(
                Component.literal("Unknown access level: " + word)).create();
    }

    private static int trustAdd(CommandContext<CommandSourceStack> context, EnumAccessLevel level)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        GlobalTrustSavedData trust = GlobalTrustSavedData.get(source.getServer());
        OMPlayer owner = requireSourcePlayer(context);
        OMPlayer target = toOMPlayer(singleProfile(context));

        GlobalTrustRegistry.Result result = trust.addTrustedPlayer(owner, target, level);
        String name = target.getName();
        switch (result) {
            case ADDED -> source.sendSuccess(
                    () -> Component.literal("Trusted " + name + " globally at " + level.getName() + "."), false);
            case ALREADY_PRESENT -> source.sendFailure(
                    Component.literal(name + " is already on your global trust list."));
            case SELF -> source.sendFailure(
                    Component.literal("You cannot trust yourself."));
            default -> source.sendFailure(Component.literal("Could not trust " + name + "."));
        }
        return result == GlobalTrustRegistry.Result.ADDED ? 1 : 0;
    }

    private static int trustRemove(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        GlobalTrustSavedData trust = GlobalTrustSavedData.get(source.getServer());
        OMPlayer owner = requireSourcePlayer(context);
        OMPlayer target = toOMPlayer(singleProfile(context));

        GlobalTrustRegistry.Result result = trust.removeTrustedPlayer(owner, target);
        String name = target.getName();
        if (result == GlobalTrustRegistry.Result.REMOVED) {
            source.sendSuccess(
                    () -> Component.literal("Removed " + name + " from your global trust list."), false);
            return 1;
        }
        source.sendFailure(Component.literal(name + " is not on your global trust list."));
        return 0;
    }

    private static int trustSetLevel(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        GlobalTrustSavedData trust = GlobalTrustSavedData.get(source.getServer());
        OMPlayer owner = requireSourcePlayer(context);
        OMPlayer target = toOMPlayer(singleProfile(context));
        EnumAccessLevel level = level(context);

        GlobalTrustRegistry.Result result = trust.changePermission(owner, target, level);
        String name = target.getName();
        if (result == GlobalTrustRegistry.Result.LEVEL_CHANGED) {
            source.sendSuccess(
                    () -> Component.literal(name + " is now " + level.getName() + " on your global trust list."), false);
            return 1;
        }
        source.sendFailure(Component.literal(name + " is not on your global trust list."));
        return 0;
    }

    private static int trustList(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        GlobalTrustSavedData trust = GlobalTrustSavedData.get(source.getServer());
        List<TrustedPlayer> trusted = trust.getTrustedPlayers(requireSourcePlayer(context));
        if (trusted.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Your global trust list is empty."), false);
            return 0;
        }
        String names = trusted.stream()
                .map(tp -> tp.getName() + " (" + tp.getAccessLevel().getName() + ")")
                .collect(Collectors.joining(", "));
        source.sendSuccess(() -> Component.literal("Your global trust list: " + names), false);
        return trusted.size();
    }

    private static int trustGui(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        GlobalTrustSavedData trust = GlobalTrustSavedData.get(source.getServer());
        OMPlayer owner = requireSourcePlayer(context);
        // Opening is just the first sync: send the current list, the client opens the screen on it.
        OMLibChannel.sendToPlayer(PacketOpenGlobalTrust.of(trust.getTrustedPlayers(owner)), player);
        return 1;
    }

    private static int changeOwner(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        OMPlayer newOwner = toOMPlayer(singleProfile(context));

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof OMBlockEntity ownedBlock)) {
            source.sendFailure(Component.literal("No ownable block at " + pos.toShortString()));
            return 0;
        }
        ownedBlock.setOwner(newOwner);
        // Upstream set the owner and stopped, so the change never reached disk and was lost on the
        // next reload.
        ownedBlock.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
        source.sendSuccess(
                () -> Component.literal("Owner of " + pos.toShortString() + " is now " + newOwner.getName()),
                true);
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> context, boolean on) {
        // Upstream assigned the config field in memory, so the toggle silently reverted on
        // restart. Under ForgeConfigSpec the field cannot be assigned at all: write through the
        // spec, which persists it, and let the bake update the field.
        OMConfig.setDebugChat(on);
        context.getSource().sendSuccess(
                () -> Component.literal((on ? "Enabled" : "Disabled") + " debug chat."), true);
        return 1;
    }
}
