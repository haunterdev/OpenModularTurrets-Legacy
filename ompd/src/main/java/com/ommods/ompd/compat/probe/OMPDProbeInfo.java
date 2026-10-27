package com.ommods.ompd.compat.probe;

import com.ommods.omlib.util.OMPlayer;
import com.ommods.ompd.content.OwnedDefenseBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;

/** The single owner line the 1.12 WailaTileHandler displayed for tiered defense blocks. */
public final class OMPDProbeInfo {
    private static final String OWNER = "ownerName";

    private OMPDProbeInfo() {
    }

    public static void gather(CompoundTag tag, OwnedDefenseBlockEntity defense) {
        OMPlayer owner = defense.getOwner();
        tag.putString(OWNER, owner == null || owner.getName() == null ? "" : owner.getName());
    }

    public static List<Component> lines(CompoundTag tag) {
        if (!tag.contains(OWNER)) {
            return List.of();
        }
        return List.of(Component.literal("Owner: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(tag.getString(OWNER)).withStyle(ChatFormatting.WHITE)));
    }
}
