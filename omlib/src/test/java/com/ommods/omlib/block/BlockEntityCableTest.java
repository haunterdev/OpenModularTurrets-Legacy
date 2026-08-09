package com.ommods.omlib.block;

import com.ommods.omlib.network.OMLibNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the parts of the cable that do not need a Level.
 *
 * Everything touching the world (onLoad, onRemoved, refreshConnections) is out of reach here and
 * belongs to the in game gate, or to a GameTest once the server harness is available.
 */
class BlockEntityCableTest {

    /** BlockEntity tolerates a null type and state in unit tests, as PlayerUtilTest already relies on. */
    private static BlockEntityCable cable() {
        return new BlockEntityCable(null, BlockPos.ZERO, null);
    }

    @Test
    void describesNoNetworkBeforeItHasJoinedOne() {
        // network is null between placement and the first join. Upstream's getDebugInfo and
        // getOwner both dereferenced it here and threw.
        assertEquals("no network", cable().describeNetwork());
    }

    @Test
    void describesAnUnnamedNetwork() {
        BlockEntityCable cable = cable();
        OMLibNetwork network = new OMLibNetwork();
        network.addDevice(cable);
        cable.setNetwork(network);

        String description = cable.describeNetwork();

        assertTrue(description.startsWith("unnamed "), description);
        assertTrue(description.contains(network.getUuid().toString()), description);
        assertTrue(description.endsWith("(1 devices)"), description);
    }

    @Test
    void describesANamedNetwork() {
        BlockEntityCable cable = cable();
        OMLibNetwork network = new OMLibNetwork();
        network.setName("base alpha");
        network.addDevice(cable);
        cable.setNetwork(network);

        assertTrue(cable.describeNetwork().startsWith("base alpha "), cable.describeNetwork());
    }

    @Test
    void aFreshCableConnectsNowhere() {
        BlockEntityCable cable = cable();

        for (Direction direction : Direction.values()) {
            assertFalse(cable.shouldConnect(direction), direction.getName());
        }
    }

    @Test
    void reportsItsOwnPositionAsItsDevicePosition() {
        BlockEntityCable cable = new BlockEntityCable(null, new BlockPos(3, 4, 5), null);

        assertEquals(new BlockPos(3, 4, 5), cable.getPosition());
        assertEquals("network_cable", cable.getDeviceName());
    }

    @Test
    void aCableWithNoNetworkSurvivesRemoval() {
        // onRemoved must not throw when the cable never joined a network and has no level.
        assertDoesNotThrow(() -> cable().onRemoved());
    }
}
