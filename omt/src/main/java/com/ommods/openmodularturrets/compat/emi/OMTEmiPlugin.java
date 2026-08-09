package com.ommods.openmodularturrets.compat.emi;

import com.ommods.openmodularturrets.client.gui.TurretBaseScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * EMI integration. Every OMT recipe is a vanilla crafting recipe loaded from JSON, so EMI already
 * shows them; the only thing it gets wrong is the layout. The base screens draw outside the usual
 * container rectangle - the side button column and the Back button sit to the right of the GUI, and
 * the base's info panel hangs off its left edge - so EMI's item panel lands on top of them. These
 * exclusion areas tell EMI to keep clear of those regions.
 *
 * <p>EMI is a compile-only dependency; this class is only ever loaded when EMI is installed, because
 * EMI's own annotation scan is what finds it.
 */
@EmiEntrypoint
public class OMTEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // Any widget an OMT screen places outside its own background rectangle. EMI already avoids
        // the container rectangle itself, so reporting the widgets inside it costs nothing, and this
        // stays correct if a screen's button layout changes later.
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (!isOurScreen(screen)) {
                return;
            }
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget widget && widget.visible) {
                    consumer.accept(new Bounds(widget.getX() - 2, widget.getY() - 2,
                            widget.getWidth() + 4, widget.getHeight() + 4));
                }
            }
        });

        // The turret base's 1.12 info panel, drawn as a hovering text box anchored at
        // (leftPos - 128, topPos + 17). Its width grows with the trusted player list, so reserve the
        // whole strip left of the GUI rather than guessing at the text metrics.
        registry.addExclusionArea(TurretBaseScreen.class, (screen, consumer) ->
                consumer.accept(new Bounds(screen.getGuiLeft() - 132, screen.getGuiTop(), 132, 190)));
    }

    private static boolean isOurScreen(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>
                && screen.getClass().getName().startsWith("com.ommods.openmodularturrets.");
    }
}
