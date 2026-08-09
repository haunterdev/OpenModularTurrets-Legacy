package com.ommods.omlib.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Common config values. See ServerConfig for why the ConfigValues are held as fields.
 */
public class CommonConfig {
    public final ForgeConfigSpec.BooleanValue doDebugChat;
    public final ForgeConfigSpec.BooleanValue debugLogging;

    CommonConfig(ForgeConfigSpec.Builder builder) {
        builder.push("DEBUG");
        doDebugChat = builder
                .comment("Should some blocks write debug messages on interaction?")
                .define("doDebugChat", false);
        debugLogging = builder
                .comment("Should OpenModular Mods print out debug messages in log? (Warning, spammy)")
                .define("debugLogging", false);
        builder.pop();
    }
}
