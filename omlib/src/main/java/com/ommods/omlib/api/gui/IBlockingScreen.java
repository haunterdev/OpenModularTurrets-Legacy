package com.ommods.omlib.api.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

/**
 * A screen that draws outside its own background rectangle, ported from 1.12
 * BlockingAbstractGuiContainer. A recipe viewer lays its item list over whatever the open screen does
 * not claim, so a screen with a side button column or a hovering panel has to report those regions or
 * the list covers them.
 *
 * <p>1.12 declared this as an abstract GuiContainer subclass with one abstract getBlockingAreas.
 * A menu screen here already extends AbstractContainerScreen&lt;T&gt;, so it is an interface instead
 * and each screen keeps its own supertype. The viewer plugins in omt read it; nothing else does.
 */
public interface IBlockingScreen {

    /** Screen-space rectangles a recipe viewer must keep its own widgets out of. */
    List<Rect2i> getBlockingAreas();

    /**
     * Every visible widget the screen owns, padded by two pixels. Reporting the widgets that sit
     * inside the background rectangle as well costs nothing, since a viewer already avoids that
     * rectangle, and it keeps the result right when a button moves.
     */
    static List<Rect2i> widgetAreas(Screen screen) {
        List<Rect2i> areas = new ArrayList<>();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.visible) {
                areas.add(new Rect2i(widget.getX() - 2, widget.getY() - 2,
                        widget.getWidth() + 4, widget.getHeight() + 4));
            }
        }
        return areas;
    }
}
