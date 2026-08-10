package com.ommods.openmodularturrets.compat.probe;

import com.ommods.omlib.util.MachineMode;
import com.ommods.omlib.util.OMPlayer;
import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
import com.ommods.openmodularturrets.content.block.TurretBaseBlockEntity;
import com.ommods.openmodularturrets.content.turret.TurretType;
import com.ommods.openmodularturrets.content.turret.TurretUpgrades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The lines a HUD overlay shows for a turret base or a turret head, ported from the 1.12 Waila
 * handlers (WailaTurretBaseHandler and WailaTurretHandler). It is kept apart from the Jade plugin so a
 * second overlay can be wired to the same text rather than growing its own copy.
 *
 * <p>The split into a gather step and a format step is 1.12's own: its handlers had getNBTData, which
 * ran on the server and packed the fields a client cannot see, and getWailaBody, which turned that tag
 * into text. An overlay whose provider already runs on the server can use the one step helpers.
 */
public final class TurretProbeInfo {

    private static final String OWNER = "ownerName";
    private static final String ACTIVE = "active";
    private static final String MODE = "mode";
    private static final String AMMO = "ammoLevel";
    private static final String AMP = "ampLevel";
    private static final String ACCURACY = "accuracy";
    private static final String ROF = "rof";

    private TurretProbeInfo() {
    }

    /** Fields for a turret base: mode, active, owner, in the 1.12 handler's order. */
    public static void gatherBase(CompoundTag tag, TurretBaseTileEntity base) {
        tag.putInt(MODE, base.getMode().ordinal());
        tag.putBoolean(ACTIVE, base.isActive());
        tag.putString(OWNER, ownerName(base.getOwner()));
    }

    /**
     * Fields for a turret head. Everything but the head's own type comes off the base it is mounted
     * on, which is where 1.12's handler read them too, so a head with no base under it reports
     * nothing.
     */
    public static void gatherHead(CompoundTag tag, TurretBaseBlockEntity head, Level level, BlockPos pos) {
        TurretBaseTileEntity base = head.findBase(level, pos);
        if (base == null) {
            return;
        }
        TurretType type = head.turretType();
        tag.putBoolean(ACTIVE, base.isActive());
        tag.putString(OWNER, ownerName(base.getOwner()));
        tag.putInt(AMMO, base.ammoCount(type.ammo()));
        // 1.12 printed damageAmpBonus * ampLevel as a percentage, so an amp addon reads as the share
        // of the target's health it adds per shot.
        tag.putString(AMP, twoDecimals(type.damageAmp() * 100.0D * base.ampLevel()));
        tag.putString(ACCURACY, twoDecimals(TurretUpgrades.actualAccuracy(
                type.accuracyDeviation(), base.accuracyUpgrades(), base.scattershot())));
        int interval = TurretUpgrades.effectiveFireInterval(type.fireInterval(), base.fireRateUpgrades());
        tag.putString(ROF, twoDecimals(20.0D / interval));
    }

    /**
     * Turns a gathered tag into the overlay lines. A tag with no owner key came from a head with no
     * base, which draws nothing, exactly as 1.12's handler returned the list untouched.
     */
    public static List<Component> lines(CompoundTag tag) {
        List<Component> lines = new ArrayList<>();
        if (!tag.contains(OWNER)) {
            return lines;
        }
        if (tag.contains(MODE)) {
            lines.add(labelled("Mode", Component.literal(
                    MachineMode.byOrdinal(tag.getInt(MODE)).label()).withStyle(ChatFormatting.GREEN)));
        }
        boolean active = tag.getBoolean(ACTIVE);
        lines.add(labelled("Active", Component.literal(active ? "yes" : "no")
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED)));
        lines.add(labelled("Owner", value(tag.getString(OWNER))));
        if (tag.contains(AMMO)) {
            lines.add(labelled("Ammo/Fuel", value(String.valueOf(tag.getInt(AMMO)))));
            lines.add(labelled("Damage Boost (rel. to HP)", value(tag.getString(AMP) + "%")));
            lines.add(labelled("Accuracy", value(tag.getString(ACCURACY))));
            lines.add(labelled("Rate of fire", value(tag.getString(ROF) + " s/sec")));
        }
        return lines;
    }

    /** Gather and format in one step, for a provider that already runs on the server. */
    public static List<Component> baseLines(TurretBaseTileEntity base) {
        CompoundTag tag = new CompoundTag();
        gatherBase(tag, base);
        return lines(tag);
    }

    /** Gather and format in one step, for a provider that already runs on the server. */
    public static List<Component> headLines(TurretBaseBlockEntity head, Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        gatherHead(tag, head, level, pos);
        return lines(tag);
    }

    /** The lines for whichever of the two blocks this is, or an empty list for anything else. */
    public static List<Component> gather(CompoundTag tag, BlockEntity blockEntity, Level level, BlockPos pos) {
        if (blockEntity instanceof TurretBaseTileEntity base) {
            gatherBase(tag, base);
        } else if (blockEntity instanceof TurretBaseBlockEntity head) {
            gatherHead(tag, head, level, pos);
        }
        return lines(tag);
    }

    private static Component labelled(String label, Component value) {
        return Component.literal(label + ": ").withStyle(ChatFormatting.GOLD).append(value);
    }

    private static Component value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private static String ownerName(OMPlayer owner) {
        return owner == null || owner.getName() == null ? "" : owner.getName();
    }

    private static String twoDecimals(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
