package com.ommods.openmodularturrets.client.gui;

import com.ommods.omlib.api.gui.IBlockingScreen;
import com.ommods.openmodularturrets.content.base.ConfigureMenu;
import com.ommods.openmodularturrets.content.base.TurretBaseMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The base's configure screen, ported from 1.12 ConfigureGui: the three attack toggles at the 1.12
 * coordinates over the classic configure.png, the admins' Trusted Players editor, and a Back button
 * to the base GUI. (1.12's tier-4 camo light sliders are not ported; the base has no camo system.)
 */
public class ConfigureScreen extends AbstractContainerScreen<ConfigureMenu> implements IBlockingScreen {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("openmodularturrets", "textures/gui/configure.png");

    private Button mobsButton;
    private Button neutralsButton;
    private Button playersButton;
    // The access tier the current button set was built for; a resync rebuilds, as 1.12 did.
    private int builtAccess = -1;

    public ConfigureScreen(ConfigureMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 205;
    }

    @Override
    protected void init() {
        super.init();
        mobsButton = null;
        neutralsButton = null;
        playersButton = null;
        builtAccess = menu.viewerAccess();
        boolean changeSettings = menu.viewerAccess() >= TurretBaseMenu.ACCESS_CHANGE_SETTINGS;
        if (changeSettings) {
            mobsButton = toggleButton(0, ConfigureMenu.BUTTON_ATTACK_MOBS);
            neutralsButton = toggleButton(1, ConfigureMenu.BUTTON_ATTACK_NEUTRALS);
            playersButton = toggleButton(2, ConfigureMenu.BUTTON_ATTACK_PLAYERS);
        }
        if (menu.viewerAccess() >= TurretBaseMenu.ACCESS_ADMIN) {
            addRenderableWidget(Button.builder(Component.literal("Trusted Players"),
                            b -> click(ConfigureMenu.BUTTON_TRUSTED_PLAYERS))
                    .bounds(leftPos + 10, topPos + 95, 155, 20)
                    .build());
        }
        addRenderableWidget(Button.builder(Component.literal("Back"),
                        b -> click(ConfigureMenu.BUTTON_BACK))
                .bounds(leftPos + 185, topPos + 20, 80, 20)
                .build());
    }

    // The 1.12 toggle rows: x+10, y+20/40/60, 155x20.
    private Button toggleButton(int row, int buttonId) {
        Button button = Button.builder(toggleLabel(buttonId), b -> click(buttonId))
                .bounds(leftPos + 10, topPos + 20 + row * 20, 155, 20)
                .build();
        addRenderableWidget(button);
        return button;
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    /** The Back button sits at leftPos + 185, clear of the 176 wide background. */
    @Override
    public List<Rect2i> getBlockingAreas() {
        return IBlockingScreen.widgetAreas(this);
    }

    private Component toggleLabel(int buttonId) {
        return switch (buttonId) {
            case ConfigureMenu.BUTTON_ATTACK_MOBS -> yesNoLabel("Attack Mobs", menu.attacksMobs());
            case ConfigureMenu.BUTTON_ATTACK_NEUTRALS -> yesNoLabel("Attack Neutrals", menu.attacksNeutrals());
            default -> yesNoLabel("Attack Players", menu.attacksPlayers());
        };
    }

    private static Component yesNoLabel(String name, boolean value) {
        return Component.literal(name + ": ")
                .append(Component.literal(value ? "yes" : "no")
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "Targeting Options: ", 10, 9, TurretBaseScreen.LABEL_COLOR, false);
        if (menu.viewerAccess() >= TurretBaseMenu.ACCESS_ADMIN) {
            graphics.drawString(font, "Trusted Players: ", 10, 84, TurretBaseScreen.LABEL_COLOR, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        // The viewer's access arrives with the first data sync, after init; rebuild the button set
        // when it lands or changes.
        if (menu.viewerAccess() != builtAccess) {
            rebuildWidgets();
        }
        // The toggles can change under us (another player, a memory card), so keep the labels live.
        if (mobsButton != null) {
            mobsButton.setMessage(toggleLabel(ConfigureMenu.BUTTON_ATTACK_MOBS));
            neutralsButton.setMessage(toggleLabel(ConfigureMenu.BUTTON_ATTACK_NEUTRALS));
            playersButton.setMessage(toggleLabel(ConfigureMenu.BUTTON_ATTACK_PLAYERS));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
