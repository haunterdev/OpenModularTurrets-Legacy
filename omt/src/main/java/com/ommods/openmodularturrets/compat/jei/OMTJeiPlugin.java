package com.ommods.openmodularturrets.compat.jei;

import com.ommods.omlib.api.gui.IBlockingScreen;
import com.ommods.openmodularturrets.client.gui.ConfigureScreen;
import com.ommods.openmodularturrets.client.gui.TurretBaseScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * JEI integration, ported from 1.12 JEIPluginOMT and AdvancedGuiHandlerOMT. Every OMT recipe is a
 * vanilla crafting recipe loaded from JSON, so JEI already lists them; what it gets wrong is the
 * layout. The base screens draw outside the usual container rectangle, so JEI's item list lands on
 * top of the side buttons and the info panel. The screens report those regions themselves and this
 * hands them to JEI.
 *
 * <p>1.12 registered one IAdvancedGuiHandler against BlockingAbstractGuiContainer and let every
 * screen extending it be covered. IGuiContainerHandler is registered per screen class instead, so
 * each blocking screen is named here.
 *
 * <p>JEI is a compile-only dependency; this class is only ever loaded when JEI is installed, because
 * JEI's own annotation scan is what finds it.
 */
@JeiPlugin
public class OMTJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            new ResourceLocation("openmodularturrets", "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(TurretBaseScreen.class, new BlockingAreaHandler<>());
        registration.addGuiContainerHandler(ConfigureScreen.class, new BlockingAreaHandler<>());
    }

    /**
     * Reads the areas back off the screen. IGuiContainerHandler gives every method a default body,
     * so it is not a functional interface and this cannot be a lambda.
     */
    private static class BlockingAreaHandler<T extends AbstractContainerScreen<?>>
            implements IGuiContainerHandler<T> {

        @Override
        public List<Rect2i> getGuiExtraAreas(T screen) {
            return screen instanceof IBlockingScreen blocking ? blocking.getBlockingAreas() : List.of();
        }
    }
}
