package com.ommods.openmodularturrets.client.gui;

import com.ommods.openmodularturrets.content.expander.ExpanderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The inventory expander screen, ported from 1.12 ExpanderInvGui: the expander_inv background and
 * nothing else.
 */
public class ExpanderScreen extends AbstractContainerScreen<ExpanderMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("openmodularturrets", "textures/gui/expander_inv.png");

    public ExpanderScreen(ExpanderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Deliberately empty. expander_inv.png is not a full window: it is only the 60x60 ammo panel
        // at (58,13) and the inventory panel at (1,76), with transparent gaps everywhere else. 1.12's
        // ExpanderInvGui overrode only the background layer, so upstream drew no title, no "Ammo" and
        // no "Inventory" here. Adding them put dark grey text on the see-through gaps, where "Ammo"
        // floated above the panel and "Inventory" landed across the panel's top border.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
