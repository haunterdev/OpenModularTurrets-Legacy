package com.ommods.omlib.reference;

import com.tterrag.registrate.Registrate;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Every piece of omlib text a player can read, and the English it ships with.
 *
 * <p>Nothing outside this class writes a sentence. Code asks for a key, this class owns the wording,
 * and {@link #register} hands the whole set to Registrate's language provider, which writes
 * {@code assets/omlib/lang/en_us.json} at datagen time. A translator adds one file beside it and
 * needs to touch no Java.
 *
 * <p>Arguments are always separate: a line reads "Trusted %s globally" rather than being glued
 * together from an English fragment and a name, because word order is the first thing a translation
 * changes.
 */
public final class OMLibLang {

    private OMLibLang() {
    }

    // Trust screens.
    public static final String GUI_GLOBAL_TRUST_TITLE = "gui.omlib.global_trust.title";
    public static final String GUI_TILE_TRUST_TITLE = "gui.omlib.tile_trust.title";
    public static final String GUI_TRUST_NAME_HINT = "gui.omlib.trust.name_hint";
    public static final String GUI_TRUST_ADD = "gui.omlib.trust.add";
    public static final String GUI_TRUST_REMOVE = "gui.omlib.trust.remove";
    public static final String GUI_TRUST_ENTRY = "gui.omlib.trust.entry";
    public static final String GUI_TRUST_EMPTY = "gui.omlib.trust.empty";

    // Access levels, in EnumAccessLevel order.
    public static final String GUI_ACCESS_LEVEL_NONE = "gui.omlib.access_level.none";
    public static final String GUI_ACCESS_LEVEL_OPEN_GUI = "gui.omlib.access_level.open_gui";
    public static final String GUI_ACCESS_LEVEL_CHANGE_SETTINGS = "gui.omlib.access_level.change_settings";
    public static final String GUI_ACCESS_LEVEL_ADMIN = "gui.omlib.access_level.admin";

    // Access types, in EnumPlayerAccessType order.
    public static final String GUI_ACCESS_TYPE_NONE = "gui.omlib.access_type.none";
    public static final String GUI_ACCESS_TYPE_OWNER = "gui.omlib.access_type.owner";
    public static final String GUI_ACCESS_TYPE_TRUSTED = "gui.omlib.access_type.trusted";
    public static final String GUI_ACCESS_TYPE_OP = "gui.omlib.access_type.op";

    // Redstone control modes, in MachineMode order.
    public static final String GUI_MODE_ALWAYS_ON = "gui.omlib.mode.always_on";
    public static final String GUI_MODE_ALWAYS_OFF = "gui.omlib.mode.always_off";
    public static final String GUI_MODE_INVERTED = "gui.omlib.mode.inverted";
    public static final String GUI_MODE_NONINVERTED = "gui.omlib.mode.noninverted";

    public static final String GUI_YES = "gui.omlib.yes";
    public static final String GUI_NO = "gui.omlib.no";

    // /omlib share
    public static final String COMMAND_SHARE_ADDED = "command.omlib.share.added";
    public static final String COMMAND_SHARE_REMOVED = "command.omlib.share.removed";
    public static final String COMMAND_SHARE_ALREADY = "command.omlib.share.already";
    public static final String COMMAND_SHARE_NOT_PRESENT = "command.omlib.share.not_present";
    public static final String COMMAND_SHARE_SELF = "command.omlib.share.self";
    public static final String COMMAND_SHARE_EMPTY = "command.omlib.share.empty";
    public static final String COMMAND_SHARE_LIST = "command.omlib.share.list";

    // /omlib trust
    public static final String COMMAND_TRUST_UNKNOWN_LEVEL = "command.omlib.trust.unknown_level";
    public static final String COMMAND_TRUST_ADDED = "command.omlib.trust.added";
    public static final String COMMAND_TRUST_ALREADY = "command.omlib.trust.already";
    public static final String COMMAND_TRUST_SELF = "command.omlib.trust.self";
    public static final String COMMAND_TRUST_FAILED = "command.omlib.trust.failed";
    public static final String COMMAND_TRUST_REMOVED = "command.omlib.trust.removed";
    public static final String COMMAND_TRUST_NOT_PRESENT = "command.omlib.trust.not_present";
    public static final String COMMAND_TRUST_LEVEL_CHANGED = "command.omlib.trust.level_changed";
    public static final String COMMAND_TRUST_EMPTY = "command.omlib.trust.empty";
    public static final String COMMAND_TRUST_LIST = "command.omlib.trust.list";
    public static final String COMMAND_TRUST_LIST_ENTRY = "command.omlib.trust.list_entry";

    // /omlib changeowner and /omlib debug
    public static final String COMMAND_OWNER_NO_BLOCK = "command.omlib.changeowner.no_block";
    public static final String COMMAND_OWNER_CHANGED = "command.omlib.changeowner.changed";
    public static final String COMMAND_DEBUG_ENABLED = "command.omlib.debug.enabled";
    public static final String COMMAND_DEBUG_DISABLED = "command.omlib.debug.disabled";

    // Answers the trust packets send back to the player who clicked.
    public static final String MESSAGE_UNKNOWN_PLAYER = "message.omlib.trust.unknown_player";
    public static final String MESSAGE_GLOBAL_TRUSTED = "message.omlib.global_trust.trusted";
    public static final String MESSAGE_GLOBAL_ALREADY = "message.omlib.global_trust.already";
    public static final String MESSAGE_GLOBAL_SELF = "message.omlib.global_trust.self";
    public static final String MESSAGE_GLOBAL_REMOVED = "message.omlib.global_trust.removed";
    public static final String MESSAGE_GLOBAL_NOT_PRESENT = "message.omlib.global_trust.not_present";
    public static final String MESSAGE_GLOBAL_LEVEL_CHANGED = "message.omlib.global_trust.level_changed";
    public static final String MESSAGE_TILE_NOT_ALLOWED = "message.omlib.tile_trust.not_allowed";
    public static final String MESSAGE_TILE_TRUSTED = "message.omlib.tile_trust.trusted";
    public static final String MESSAGE_TILE_ALREADY = "message.omlib.tile_trust.already";
    public static final String MESSAGE_TILE_REMOVED = "message.omlib.tile_trust.removed";
    public static final String MESSAGE_TILE_NOT_PRESENT = "message.omlib.tile_trust.not_present";
    public static final String MESSAGE_TILE_LEVEL_CHANGED = "message.omlib.tile_trust.level_changed";

    // The cable's right click readout.
    public static final String MESSAGE_CABLE_NETWORK = "message.omlib.cable.network";
    public static final String MESSAGE_CABLE_NO_NETWORK = "message.omlib.cable.no_network";
    public static final String MESSAGE_CABLE_UNNAMED = "message.omlib.cable.unnamed";

    /** Shorthand for a translated line, so call sites read as one key and its arguments. */
    public static MutableComponent text(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** The yes/no pair every OMT panel and tooltip prints for a boolean. */
    public static MutableComponent yesNo(boolean value) {
        return Component.translatable(value ? GUI_YES : GUI_NO);
    }

    public static void register(Registrate registrate) {
        lang(registrate, GUI_GLOBAL_TRUST_TITLE, "Global Trust List");
        lang(registrate, GUI_TILE_TRUST_TITLE, "Block Trust List");
        lang(registrate, GUI_TRUST_NAME_HINT, "player name");
        lang(registrate, GUI_TRUST_ADD, "Add");
        lang(registrate, GUI_TRUST_REMOVE, "Remove");
        lang(registrate, GUI_TRUST_ENTRY, "%1$s: %2$s");
        lang(registrate, GUI_TRUST_EMPTY, "No trusted players. Type a name and Add.");

        lang(registrate, GUI_ACCESS_LEVEL_NONE, "None");
        lang(registrate, GUI_ACCESS_LEVEL_OPEN_GUI, "Can open GUI");
        lang(registrate, GUI_ACCESS_LEVEL_CHANGE_SETTINGS, "Can change settings");
        lang(registrate, GUI_ACCESS_LEVEL_ADMIN, "Can administer");

        lang(registrate, GUI_ACCESS_TYPE_NONE, "None");
        lang(registrate, GUI_ACCESS_TYPE_OWNER, "Owner");
        lang(registrate, GUI_ACCESS_TYPE_TRUSTED, "Trusted");
        lang(registrate, GUI_ACCESS_TYPE_OP, "Operator");

        lang(registrate, GUI_MODE_ALWAYS_ON, "Always on");
        lang(registrate, GUI_MODE_ALWAYS_OFF, "Always off");
        lang(registrate, GUI_MODE_INVERTED, "Inverted");
        lang(registrate, GUI_MODE_NONINVERTED, "Noninverted");

        lang(registrate, GUI_YES, "yes");
        lang(registrate, GUI_NO, "no");

        lang(registrate, COMMAND_SHARE_ADDED, "Added %1$s to your share list.");
        lang(registrate, COMMAND_SHARE_REMOVED, "Removed %1$s from your share list.");
        lang(registrate, COMMAND_SHARE_ALREADY, "%1$s is already on your share list.");
        lang(registrate, COMMAND_SHARE_NOT_PRESENT, "%1$s is not on your share list.");
        lang(registrate, COMMAND_SHARE_SELF, "You cannot share with yourself.");
        lang(registrate, COMMAND_SHARE_EMPTY, "Your share list is empty.");
        lang(registrate, COMMAND_SHARE_LIST, "Players on your share list: %1$s");

        lang(registrate, COMMAND_TRUST_UNKNOWN_LEVEL, "Unknown access level: %1$s");
        lang(registrate, COMMAND_TRUST_ADDED, "Trusted %1$s globally at %2$s.");
        lang(registrate, COMMAND_TRUST_ALREADY, "%1$s is already on your global trust list.");
        lang(registrate, COMMAND_TRUST_SELF, "You cannot trust yourself.");
        lang(registrate, COMMAND_TRUST_FAILED, "Could not trust %1$s.");
        lang(registrate, COMMAND_TRUST_REMOVED, "Removed %1$s from your global trust list.");
        lang(registrate, COMMAND_TRUST_NOT_PRESENT, "%1$s is not on your global trust list.");
        lang(registrate, COMMAND_TRUST_LEVEL_CHANGED, "%1$s is now %2$s on your global trust list.");
        lang(registrate, COMMAND_TRUST_EMPTY, "Your global trust list is empty.");
        lang(registrate, COMMAND_TRUST_LIST, "Your global trust list: %1$s");
        lang(registrate, COMMAND_TRUST_LIST_ENTRY, "%1$s (%2$s)");

        lang(registrate, COMMAND_OWNER_NO_BLOCK, "No ownable block at %1$s");
        lang(registrate, COMMAND_OWNER_CHANGED, "Owner of %1$s is now %2$s");
        lang(registrate, COMMAND_DEBUG_ENABLED, "Enabled debug chat.");
        lang(registrate, COMMAND_DEBUG_DISABLED, "Disabled debug chat.");

        lang(registrate, MESSAGE_UNKNOWN_PLAYER, "Unknown player: %1$s. In online mode only players"
                + " the server has seen can be trusted; turn on offlineModeSupport to trust by name.");
        lang(registrate, MESSAGE_GLOBAL_TRUSTED, "Trusted %1$s globally.");
        lang(registrate, MESSAGE_GLOBAL_ALREADY, "%1$s is already on your global trust list.");
        lang(registrate, MESSAGE_GLOBAL_SELF, "You cannot trust yourself.");
        lang(registrate, MESSAGE_GLOBAL_REMOVED, "Removed %1$s from your global trust list.");
        lang(registrate, MESSAGE_GLOBAL_NOT_PRESENT, "%1$s is not on your global trust list.");
        lang(registrate, MESSAGE_GLOBAL_LEVEL_CHANGED, "%1$s is now %2$s on your global trust list.");
        lang(registrate, MESSAGE_TILE_NOT_ALLOWED, "You are not allowed to edit this block's trust list.");
        lang(registrate, MESSAGE_TILE_TRUSTED, "Trusted %1$s on this block.");
        lang(registrate, MESSAGE_TILE_ALREADY, "%1$s is already trusted on this block.");
        lang(registrate, MESSAGE_TILE_REMOVED, "Removed %1$s from this block.");
        lang(registrate, MESSAGE_TILE_NOT_PRESENT, "%1$s is not trusted on this block.");
        lang(registrate, MESSAGE_TILE_LEVEL_CHANGED, "%1$s is now %2$s on this block.");

        lang(registrate, MESSAGE_CABLE_NETWORK, "%1$s %2$s (%3$s devices)");
        lang(registrate, MESSAGE_CABLE_NO_NETWORK, "no network");
        lang(registrate, MESSAGE_CABLE_UNNAMED, "unnamed");
    }

    private static void lang(Registrate registrate, String key, String english) {
        registrate.addRawLang(key, english);
    }
}
