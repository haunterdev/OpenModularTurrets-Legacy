package com.ommods.openmodularturrets.client.gui;

import com.ommods.omlib.util.OMPlayer;
import com.ommods.omlib.util.permission.TrustedPlayer;
import com.ommods.openmodularturrets.content.base.TurretBaseMenu;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The turret base inventory screen, ported from 1.12 TurretBaseGui. Draws the tier's GUI background,
 * the energy bar, the ammo/addon/upgrade labels, the adjustable range readout, the side button
 * column (Mode/Configure/Target/Drop base/Drop turrets) gated by the viewer's access, and the 1.12
 * info panel (owner, mode, active, kills, trusted players, attack flags) to the left of the GUI.
 */
public class TurretBaseScreen extends AbstractContainerScreen<TurretBaseMenu> {

    private static final String[] TIER_WORD = {"one", "two", "three", "four", "five"};
    private static final java.util.Random RANDOM = new java.util.Random();

    /**
     * Label colour for every OMT panel. All of the mod's GUI sheets paint their body #4E4E4E, and 1.12
     * drew its labels in black on top of that, about 2.5:1 contrast, which is why the ammo and upgrade
     * headings were barely there. Light grey on the same panel is roughly 7:1 and legible.
     */
    static final int LABEL_COLOR = 0xE0E0E0;

    /** The range readout's normal colour. 1.12 used 40000 (#009C40), too dark to read on the panel. */
    static final int RANGE_COLOR = 0x54E36B;

    private Button targetButton;
    private Button modeButton;
    // The access tier the current button set was built for; a resync rebuilds, as 1.12 did.
    private int builtAccess = -1;

    public TurretBaseScreen(TurretBaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 97 + 4;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        targetButton = null;
        modeButton = null;
        builtAccess = menu.viewerAccess();
        // The 1.12 buttonInit layout: admins get the whole column plus the range +/-;
        // change-settings players get Configure, Target and the range +/-.
        if (builtAccess >= TurretBaseMenu.ACCESS_ADMIN) {
            modeButton = sideButton(0, modeLabel(), TurretBaseMenu.BUTTON_MODE);
            sideButton(25, Component.literal("Configure"), TurretBaseMenu.BUTTON_CONFIGURE);
            targetButton = sideButton(50, targetLabel(), TurretBaseMenu.BUTTON_TARGET);
            sideButton(75, Component.literal("Drop base"), TurretBaseMenu.BUTTON_DROP_BASE);
            sideButton(100, Component.literal("Drop turrets"), TurretBaseMenu.BUTTON_DROP_TURRETS);
            rangeButtons();
        } else if (builtAccess >= TurretBaseMenu.ACCESS_CHANGE_SETTINGS) {
            sideButton(25, Component.literal("Configure"), TurretBaseMenu.BUTTON_CONFIGURE);
            targetButton = sideButton(50, targetLabel(), TurretBaseMenu.BUTTON_TARGET);
            rangeButtons();
        }
    }

    private Button sideButton(int yOffset, Component label, int buttonId) {
        Button button = Button.builder(label, b -> click(buttonId))
                .bounds(leftPos + 180, topPos + yOffset, 80, 20)
                .build();
        addRenderableWidget(button);
        return button;
    }

    private void rangeButtons() {
        addRenderableWidget(Button.builder(Component.literal("+"), b -> click(TurretBaseMenu.BUTTON_RANGE_UP))
                .bounds(leftPos + 120, topPos + 15, 20, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> click(TurretBaseMenu.BUTTON_RANGE_DOWN))
                .bounds(leftPos + 120, topPos + 50, 20, 20)
                .build());
    }

    private void click(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private Component targetLabel() {
        return Component.literal("Target: " + (menu.isMultiTargeting() ? "Multi" : "Single"));
    }

    private Component modeLabel() {
        return Component.literal("Mode: " + menu.getMode().label());
    }

    private ResourceLocation texture() {
        int tierIndex = Math.max(0, Math.min(4, menu.tier().tier() - 1));
        return new ResourceLocation("openmodularturrets", "textures/gui/turret_base_tier_" + TIER_WORD[tierIndex] + ".png");
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = texture();
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Energy bar: empty frame from (178,17), then a fill of height h from one of the three fill
        // columns (196/215/234) picked at random each frame, exactly as drawEnergyBar - the random
        // column is what gives the classic bar its shimmer. 14 wide, 51 tall, at (153,17).
        int max = menu.getMaxEnergy();
        int stored = menu.getEnergy();
        int h = max <= 0 ? 0 : Math.min(51, stored * 51 / max);
        graphics.blit(texture, leftPos + 153, topPos + 17, 178, 17, 14, 51);
        if (h > 0) {
            int column = switch (RANDOM.nextInt(3)) {
                case 0 -> 196;
                case 1 -> 215;
                default -> 234;
            };
            graphics.blit(texture, leftPos + 153, topPos + 17 + 51 - h, column, 68 - h, 14, h);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, "Ammo", 8, 6, LABEL_COLOR, false);
        if (menu.tier().tier() > 1) {
            graphics.drawString(font, "Addons", 71, 6, LABEL_COLOR, false);
            graphics.drawString(font, "Upgrades", 71, 39, LABEL_COLOR, false);
        }
        graphics.drawString(font, this.playerInventoryTitle, 8, this.inventoryLabelY, LABEL_COLOR, false);

        // The 1.12 range readout between the +/- buttons: red once pinned at the heads' upper bound.
        int range = menu.getCurrentMaxRange();
        graphics.drawString(font, "Range", 116, 6, LABEL_COLOR, false);
        int rangeX = String.valueOf(range).length() == 1 ? 127 : 124;
        int rangeColor = range == menu.getUpperBoundMaxRange() ? 0xFF3232 : RANGE_COLOR;
        graphics.drawString(font, String.valueOf(range), rangeX, 39, rangeColor, true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        // The viewer's access arrives with the first data sync, after init; rebuild the button set
        // when it lands or changes, exactly as 1.12 re-ran buttonInit on access changes.
        if (menu.viewerAccess() != builtAccess) {
            rebuildWidgets();
        }
        // The synced flags can change under us (another player, a memory card), so keep labels live.
        if (targetButton != null) {
            targetButton.setMessage(targetLabel());
        }
        if (modeButton != null) {
            modeButton.setMessage(modeLabel());
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        drawInfoPanel(graphics);
        drawHoverTooltips(graphics, mouseX, mouseY);
    }

    /**
     * The 1.12 drawTooltips pass: a line for whichever side button is hovered, the energy readout on
     * the bar, and one for each labelled slot region. The regions are upstream's own pixel boxes.
     */
    private void drawHoverTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        String text = null;
        for (var child : children()) {
            if (child instanceof Button button && button.isHovered()) {
                text = buttonTooltip(button);
            }
        }
        if (text == null && isHovering(153, 17, 14, 51, mouseX, mouseY)) {
            text = menu.getEnergy() + " / " + menu.getMaxEnergy() + " RF";
        }
        boolean hasModifierSlots = menu.tier().tier() > 1;
        if (text == null && hasModifierSlots && isHovering(71, 6, 40, 14, mouseX, mouseY)) {
            text = "Addons (for more functions) for the modular turret.";
        }
        if (text == null && hasModifierSlots && isHovering(71, 39, 40, 14, mouseX, mouseY)) {
            text = "Upgrades for the modular turret.";
        }
        if (text == null && isHovering(10, 6, 38, 14, mouseX, mouseY)) {
            text = "Ammo and Fuel for the modular turret.";
        }
        if (text == null && isHovering(123, 35, 11, 13, mouseX, mouseY)) {
            text = "Max range for turrets, in blocks.";
        }
        if (text != null) {
            graphics.renderTooltip(font, Component.literal(text), mouseX, mouseY);
        }
    }

    private String buttonTooltip(Button button) {
        String label = button.getMessage().getString();
        if (label.equals("+")) {
            return "Increase the max range for the turret(s)";
        }
        if (label.equals("-")) {
            return "Decrease the max range for the turret(s)";
        }
        if (label.equals("Configure")) {
            return "Enter the base's config screen.";
        }
        if (label.startsWith("Target")) {
            return "Change if turrets should focus fire or spread it.";
        }
        if (label.startsWith("Mode")) {
            return "Toggle on/off modes";
        }
        return null;
    }

    // The 1.12 hovering info panel at (-128, 17) left of the GUI: owner, mode, active, the kill
    // counters, the trusted player list, and the attack flags.
    private void drawInfoPanel(GuiGraphics graphics) {
        List<Component> lines = new ArrayList<>();
        TurretBaseTileEntity base = menu.base();
        OMPlayer owner = base != null ? base.getOwner() : null;
        lines.add(Component.literal("Owner: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(owner != null && owner.getName() != null ? owner.getName() : "?")
                        .withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal("Mode: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(menu.getMode().label()).withStyle(ChatFormatting.WHITE)));
        lines.add(Component.literal("Active: ").withStyle(ChatFormatting.GOLD)
                .append(yesNo(menu.isActive())));
        lines.add(Component.empty());
        lines.add(Component.literal("Kills: " + menu.getKills()).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Player kills: " + menu.getPlayerKills()).withStyle(ChatFormatting.GOLD));
        lines.add(Component.empty());
        List<TrustedPlayer> trusted = base != null ? base.getTrustManager().getTrustedPlayers() : List.of();
        if (!trusted.isEmpty()) {
            lines.add(Component.literal("Trusted Players:").withStyle(ChatFormatting.DARK_PURPLE));
            for (TrustedPlayer trustedPlayer : trusted) {
                lines.add(Component.literal(trustedPlayer.getName()).withStyle(ChatFormatting.AQUA));
            }
        } else {
            lines.add(Component.literal("Trusted Players: ").withStyle(ChatFormatting.DARK_PURPLE)
                    .append(yesNo(false)));
        }
        lines.add(Component.empty());
        lines.add(Component.literal("Attack Mobs: ").withStyle(ChatFormatting.GRAY)
                .append(yesNo(menu.attacksMobs())));
        lines.add(Component.literal("Attack Neutrals: ").withStyle(ChatFormatting.GRAY)
                .append(yesNo(menu.attacksNeutrals())));
        lines.add(Component.literal("Attack Players: ").withStyle(ChatFormatting.GRAY)
                .append(yesNo(menu.attacksPlayers())));
        graphics.renderComponentTooltip(font, lines, leftPos - 128, topPos + 17);
    }

    private static Component yesNo(boolean value) {
        return Component.literal(value ? "yes" : "no")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
