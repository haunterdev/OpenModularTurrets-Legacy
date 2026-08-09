package com.ommods.omlib.config;

import com.ommods.omlib.reference.Reference;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Config entry point.
 *
 * The public fields below are "baked": they are plain values copied out of the ConfigValues when a
 * config loads or reloads. Callers read them directly, and tests assign them directly to control
 * behavior without a running game.
 *
 * Upstream 1.19.2 had the same field shape but nothing ever populated the fields. It read them in a
 * static initializer, before any config is loaded, using paths that omitted the category prefix
 * ("offlineModeSupport" rather than "PERMISSION.offlineModeSupport"). The fields therefore stayed
 * null in production and OMPlayer.equals threw NPE unboxing offlineModeSupport. Upstream's tests
 * did not catch it because they assign the fields themselves. The bake below is what actually fills
 * them; the fields are primitives so a missed bake degrades to false instead of throwing.
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OMConfig {
    public static final ForgeConfigSpec serverConfigSpec;
    public static final ForgeConfigSpec commonConfigSpec;

    private static final ServerConfig SERVER;
    private static final CommonConfig COMMON;

    static {
        Pair<ServerConfig, ForgeConfigSpec> serverPair =
                new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        Pair<CommonConfig, ForgeConfigSpec> commonPair =
                new ForgeConfigSpec.Builder().configure(CommonConfig::new);

        SERVER = serverPair.getKey();
        serverConfigSpec = serverPair.getValue();
        COMMON = commonPair.getKey();
        commonConfigSpec = commonPair.getValue();
    }

    public static class General {
        public static String recipes = "auto";
    }

    public static class Permission {
        public static boolean canOPAccessOwnedBlocks = false;
        public static boolean offlineModeSupport = false;
    }

    public static class Debug {
        public static boolean doDebugChat = false;
        public static boolean debugLogging = false;
    }

    /**
     * Writes the debug chat toggle through the spec so it survives a restart, and bakes it.
     *
     * Upstream's /omdebug assigned the field and nothing else, so the toggle silently reverted on
     * restart. Under ForgeConfigSpec the field is not the config, it is a copy of it.
     */
    public static void setDebugChat(boolean value) {
        COMMON.doDebugChat.set(value);
        COMMON.doDebugChat.save();
        Debug.doDebugChat = value;
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event) {
        bake(event.getConfig().getSpec());
    }

    @SubscribeEvent
    public static void onReload(ModConfigEvent.Reloading event) {
        bake(event.getConfig().getSpec());
    }

    private static void bake(Object spec) {
        if (spec == serverConfigSpec) {
            General.recipes = SERVER.recipes.get();
            Permission.canOPAccessOwnedBlocks = SERVER.canOPAccessOwnedBlocks.get();
            Permission.offlineModeSupport = SERVER.offlineModeSupport.get();
        } else if (spec == commonConfigSpec) {
            Debug.doDebugChat = COMMON.doDebugChat.get();
            Debug.debugLogging = COMMON.debugLogging.get();
        }
    }

    private OMConfig() {
    }
}
