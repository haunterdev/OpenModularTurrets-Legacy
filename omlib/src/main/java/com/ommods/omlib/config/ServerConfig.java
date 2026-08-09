package com.ommods.omlib.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.Arrays;

/**
 * Server config values.
 *
 * Upstream 1.19.2 assigned each define(...) to a local final that was immediately discarded, so
 * nothing could read the values back. The ConfigValues are held as fields here instead. Category
 * and option names match upstream so the generated TOML keeps the same shape.
 */
public class ServerConfig {
    public final ForgeConfigSpec.BooleanValue canOPAccessOwnedBlocks;
    public final ForgeConfigSpec.BooleanValue offlineModeSupport;
    public final ForgeConfigSpec.ConfigValue<String> recipes;

    ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.push("PERMISSION");
        canOPAccessOwnedBlocks = builder
                .comment("Can OPs access all owned blocks?")
                .define("canOPAccessOwnedBlocks", false);
        offlineModeSupport = builder
                .comment("Enable compat for offline mode servers?")
                .define("offlineModeSupport", false);
        builder.pop();

        builder.push("GENERAL");
        recipes = builder
                .comment("Which recipes to use. Valid values: auto,vanilla")
                .defineInList("recipes", "auto", Arrays.asList("auto", "vanilla"));
        builder.pop();
    }
}
