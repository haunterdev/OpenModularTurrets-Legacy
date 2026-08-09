package com.ommods.openmodularturrets.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Shared pieces of the 1.12 shift tooltips: the "Press Shift for details." hint every OMT item
 * printed (omlib's getShiftDetail), and the small formatting helpers. All wording comes from the
 * 1.12 lang files.
 */
public final class ClassicTooltips {

    private ClassicTooltips() {
    }

    /**
     * Adds the shift hint and returns true when the details should stay hidden (shift not held).
     * Only called from appendHoverText, which only runs on the client, so the Screen reference
     * never loads on a dedicated server.
     */
    public static boolean hint(List<Component> tooltip) {
        if (Screen.hasShiftDown()) {
            return false;
        }
        tooltip.add(Component.literal("Press ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("Shift").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" for details.").withStyle(ChatFormatting.GRAY)));
        return true;
    }

    /** A blue category label line ("Turret Upgrade", "Turret Addon", "Turret Ammunition"). */
    public static Component label(String text) {
        return Component.literal(text).withStyle(ChatFormatting.BLUE);
    }

    /** A dark gray flavour line. */
    public static Component flavour(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }
}
