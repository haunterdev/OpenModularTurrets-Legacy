package com.ommods.omlib.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 1.12 omlib EnumMachineMode contract: the redstone control mode of an owned machine. Order and
 * semantics must match upstream exactly, because the mode is saved by ordinal and the GUI's Mode
 * button cycles in declaration order.
 */
class MachineModeTest {

    @Test
    void declarationOrderMatchesThe112Enum() {
        assertEquals(0, MachineMode.ALWAYS_ON.ordinal());
        assertEquals(1, MachineMode.ALWAYS_OFF.ordinal());
        assertEquals(2, MachineMode.INVERTED.ordinal());
        assertEquals(3, MachineMode.NONINVERTED.ordinal());
    }

    @Test
    void toggleCyclesInOrderAndWraps() {
        assertEquals(MachineMode.ALWAYS_OFF, MachineMode.ALWAYS_ON.next());
        assertEquals(MachineMode.INVERTED, MachineMode.ALWAYS_OFF.next());
        assertEquals(MachineMode.NONINVERTED, MachineMode.INVERTED.next());
        assertEquals(MachineMode.ALWAYS_ON, MachineMode.NONINVERTED.next());
    }

    @Test
    void activeFollowsThe112RefreshActiveTable() {
        assertTrue(MachineMode.ALWAYS_ON.active(false));
        assertTrue(MachineMode.ALWAYS_ON.active(true));
        assertFalse(MachineMode.ALWAYS_OFF.active(false));
        assertFalse(MachineMode.ALWAYS_OFF.active(true));
        // Inverted (the 1.12 default): on until a redstone signal switches the machine off.
        assertTrue(MachineMode.INVERTED.active(false));
        assertFalse(MachineMode.INVERTED.active(true));
        assertFalse(MachineMode.NONINVERTED.active(false));
        assertTrue(MachineMode.NONINVERTED.active(true));
    }

    @Test
    void labelsMatchThe112LangFile() {
        assertEquals("Always on", MachineMode.ALWAYS_ON.label());
        assertEquals("Always off", MachineMode.ALWAYS_OFF.label());
        assertEquals("Inverted", MachineMode.INVERTED.label());
        assertEquals("Noninverted", MachineMode.NONINVERTED.label());
    }
}
