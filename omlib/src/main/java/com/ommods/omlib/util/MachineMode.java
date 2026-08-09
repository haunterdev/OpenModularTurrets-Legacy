package com.ommods.omlib.util;

/**
 * The redstone control mode of an owned machine, a faithful port of the 1.12 omlib EnumMachineMode.
 * Declaration order matters: modes are saved by ordinal and the GUI's Mode button cycles in this
 * order, exactly as upstream's toggleMode did. The 1.12 default was {@link #INVERTED}: a machine
 * runs until a redstone signal switches it off, so a plain lever acts as an off switch.
 */
public enum MachineMode {
    ALWAYS_ON("Always on"),
    ALWAYS_OFF("Always off"),
    INVERTED("Inverted"),
    NONINVERTED("Noninverted");

    private final String label;

    MachineMode(String label) {
        this.label = label;
    }

    /** The next mode in the 1.12 toggle cycle, wrapping to the first. */
    public MachineMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** Whether a machine in this mode runs given its redstone input, per 1.12 refreshActive. */
    public boolean active(boolean redstone) {
        return switch (this) {
            case ALWAYS_ON -> true;
            case ALWAYS_OFF -> false;
            case INVERTED -> !redstone;
            case NONINVERTED -> redstone;
        };
    }

    /** The English GUI label, word for word from the 1.12 omlib lang file. */
    public String label() {
        return label;
    }

    /** The mode saved at the given ordinal, tolerating out-of-range values from old saves. */
    public static MachineMode byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : INVERTED;
    }
}
