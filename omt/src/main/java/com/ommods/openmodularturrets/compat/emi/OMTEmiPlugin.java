package com.ommods.openmodularturrets.compat.emi;

import com.ommods.omlib.api.gui.IBlockingScreen;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;

/**
 * EMI integration. Every OMT recipe is a vanilla crafting recipe loaded from JSON, so EMI already
 * shows them; the only thing it gets wrong is the layout. The base screens draw outside the usual
 * container rectangle - the side button column and the Back button sit to the right of the GUI, and
 * the base's info panel hangs off its left edge - so EMI's item panel lands on top of them. The
 * screens report those regions themselves and this hands them to EMI.
 *
 * <p>EMI is a compile-only dependency; this class is only ever loaded when EMI is installed, because
 * EMI's own annotation scan is what finds it.
 */
@EmiEntrypoint
public class OMTEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // One generic handler covers every blocking screen, so a new one needs no entry here.
        registry.addGenericExclusionArea((screen, consumer) -> {
            if (screen instanceof IBlockingScreen blocking) {
                for (Rect2i area : blocking.getBlockingAreas()) {
                    consumer.accept(new Bounds(area.getX(), area.getY(),
                            area.getWidth(), area.getHeight()));
                }
            }
        });
    }
}
