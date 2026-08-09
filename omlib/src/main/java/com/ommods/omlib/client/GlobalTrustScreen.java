package com.ommods.omlib.client;

import com.ommods.omlib.network.OMLibChannel;
import com.ommods.omlib.network.PacketGlobalTrustAction;
import com.ommods.omlib.network.PacketOpenGlobalTrust.Entry;
import com.ommods.omlib.util.permission.EnumAccessLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The player's global trust list, editable in place.
 *
 * Deliberately plain: vanilla widgets, no texture. Every button sends an action to the server and
 * the server answers with the stored list, which {@link #setEntries} rebuilds from. The screen holds
 * no authority and keeps no optimistic state; it shows what the server last sent.
 *
 * This is the block free trust screen (upstream's "global" mode). The per block trust screen is the
 * same idea over a block entity's own list and waits for a block that has a trust manager.
 */
public class GlobalTrustScreen extends Screen {

    private List<Entry> entries;
    private EditBox nameField;

    public GlobalTrustScreen(List<Entry> entries) {
        super(Component.literal("Global Trust List"));
        this.entries = entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
        if (this.minecraft != null) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        nameField = new EditBox(this.font, cx - 100, 40, 130, 20, Component.literal("player name"));
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            String name = nameField.getValue().trim();
            if (!name.isEmpty()) {
                send(PacketGlobalTrustAction.Op.ADD, name, EnumAccessLevel.OPEN_GUI.ordinal());
            }
        }).bounds(cx + 35, 40, 65, 20).build());

        int y = 70;
        for (Entry entry : entries) {
            String name = entry.name();
            int next = (entry.level() + 1) % EnumAccessLevel.values().length;
            String label = name + ": " + EnumAccessLevel.values()[entry.level()].getName();
            addRenderableWidget(Button.builder(Component.literal("Remove"),
                            b -> send(PacketGlobalTrustAction.Op.REMOVE, name, 0))
                    .bounds(cx - 100, y, 60, 20).build());
            // Clicking the row cycles its access level, so no selection state is needed.
            addRenderableWidget(Button.builder(Component.literal(label),
                            b -> send(PacketGlobalTrustAction.Op.SET_LEVEL, name, next))
                    .bounds(cx - 35, y, 135, 20).build());
            y += 22;
            if (y > this.height - 40) {
                break;
            }
        }
    }

    private void send(PacketGlobalTrustAction.Op op, String name, int level) {
        OMLibChannel.INSTANCE.sendToServer(new PacketGlobalTrustAction(op, name, level));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.literal("No trusted players. Type a name and Add."),
                    this.width / 2, 74, 0xAAAAAA);
        }
        super.render(graphics, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
