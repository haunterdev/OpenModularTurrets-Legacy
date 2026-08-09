package com.ommods.omlib.client;

import com.ommods.omlib.network.OMLibChannel;
import com.ommods.omlib.network.PacketGlobalTrustAction.Op;
import com.ommods.omlib.network.PacketOpenGlobalTrust.Entry;
import com.ommods.omlib.network.PacketTileTrustAction;
import com.ommods.omlib.util.permission.EnumAccessLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A single block's trust list, editable in place. The per block twin of {@link GlobalTrustScreen}:
 * same server authoritative loop (every button sends an action, the server answers with the stored
 * list, {@link #setEntries} rebuilds from it), but every action carries the block position so the
 * server edits the right block.
 */
public class TileTrustScreen extends Screen {

    private final BlockPos pos;
    private List<Entry> entries;
    private EditBox nameField;

    public TileTrustScreen(BlockPos pos, List<Entry> entries) {
        super(Component.literal("Block Trust List"));
        this.pos = pos;
        this.entries = entries;
    }

    /** Whether a sync is for the block this screen is showing. */
    public boolean matches(BlockPos other) {
        return pos.equals(other);
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
                send(Op.ADD, name, EnumAccessLevel.OPEN_GUI.ordinal());
            }
        }).bounds(cx + 35, 40, 65, 20).build());

        int y = 70;
        for (Entry entry : entries) {
            String name = entry.name();
            int next = (entry.level() + 1) % EnumAccessLevel.values().length;
            String label = name + ": " + EnumAccessLevel.values()[entry.level()].getName();
            addRenderableWidget(Button.builder(Component.literal("Remove"),
                            b -> send(Op.REMOVE, name, 0))
                    .bounds(cx - 100, y, 60, 20).build());
            // Clicking the row cycles its access level, so no selection state is needed.
            addRenderableWidget(Button.builder(Component.literal(label),
                            b -> send(Op.SET_LEVEL, name, next))
                    .bounds(cx - 35, y, 135, 20).build());
            y += 22;
            if (y > this.height - 40) {
                break;
            }
        }
    }

    private void send(Op op, String name, int level) {
        OMLibChannel.INSTANCE.sendToServer(new PacketTileTrustAction(pos, op, name, level));
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
