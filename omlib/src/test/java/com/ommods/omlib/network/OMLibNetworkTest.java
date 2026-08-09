package com.ommods.omlib.network;

import com.ommods.omlib.api.network.*;
import com.ommods.omlib.util.OMPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OMLibNetworkTest {

    static class Cable implements INetworkCable {
        private final BlockPos pos;
        private OMLibNetwork network;

        Cable(int x) {
            this.pos = new BlockPos(x, 0, 0);
        }

        @Override public boolean shouldConnect(net.minecraft.core.Direction side) { return true; }
        @Override public OMLibNetwork getNetwork() { return network; }
        @Override public void setNetwork(OMLibNetwork network) { this.network = network; }
        @Nonnull @Override public String getDeviceName() { return "cable"; }
        @Nonnull @Override public BlockPos getPosition() { return pos; }
    }

    static class Controller implements INameController, INetworkTile {
        private final BlockPos pos;
        private final String name;
        private OMLibNetwork network;

        Controller(int x, String name) {
            this.pos = new BlockPos(x, 0, 0);
            this.name = name;
        }

        @Nonnull @Override public String getNetworkName() { return name; }
        @Override public OMLibNetwork getNetwork() { return network; }
        @Override public void setNetwork(OMLibNetwork network) { this.network = network; }
        @Nonnull @Override public String getDeviceName() { return "controller"; }
        @Nonnull @Override public BlockPos getPosition() { return pos; }
    }

    static class PowerTile implements IPowerExchangeTile {
        private final EnergyStorage storage;
        private final boolean delivers;
        private final BlockPos pos;

        PowerTile(int stored, boolean delivers, int x) {
            this.storage = new EnergyStorage(10000, 10000, 10000, stored);
            this.delivers = delivers;
            this.pos = new BlockPos(x, 0, 0);
        }

        @Override public boolean requiresEnergy() { return !delivers; }
        @Override public boolean deliversEnergy() { return delivers; }
        @Override public IEnergyStorage getEnergyStorage() { return storage; }
        @Override public OMLibNetwork getNetwork() { return null; }
        @Override public void setNetwork(OMLibNetwork network) { }
        @Nonnull @Override public String getDeviceName() { return "power"; }
        @Nonnull @Override public BlockPos getPosition() { return pos; }
    }

    private OMLibNetwork network;

    @BeforeEach
    void setUp() {
        network = new OMLibNetwork();
    }

    @Test
    void aNewNetworkHasAUuidAndAnEmptyName() {
        assertNotNull(network.getUuid());
        assertEquals("", network.getName());
    }

    @Test
    void addsAndFindsADevice() {
        Cable cable = new Cable(0);

        assertTrue(network.addDevice(cable));
        assertEquals(1, network.getAllDevices().size());
        assertSame(cable, network.getConnectedDevice(new BlockPos(0, 0, 0)));
    }

    @Test
    void removesADevice() {
        Cable cable = new Cable(0);
        network.addDevice(cable);

        assertTrue(network.removeDevice(cable));
        assertTrue(network.getAllDevices().isEmpty());
        assertFalse(network.removeDevice(cable));
    }

    @Test
    void aNameControllerNamesTheNetwork() {
        network.addDevice(new Controller(0, "base alpha"));

        assertEquals("base alpha", network.getName());
    }

    @Test
    void aSecondControllerIsRefused() {
        assertTrue(network.addDevice(new Controller(0, "first")));

        // One controller per network. Upstream only blocked a second controller of the same class.
        assertFalse(network.addDevice(new Controller(1, "second")));
        assertEquals("first", network.getName());
        assertEquals(1, network.getAllDevices().size());
    }

    @Test
    void findsItsControllerByType() {
        Controller controller = new Controller(0, "base");
        network.addDevice(controller);
        network.addDevice(new Cable(1));

        assertSame(controller, network.getController(Controller.class));
    }

    @Test
    void reportsNoControllerWhenThereIsNone() {
        network.addDevice(new Cable(0));

        assertNull(network.getController(Controller.class));
    }

    @Test
    void mergeTakesOverTheOtherNetworksDevices() {
        Cable mine = new Cable(0);
        Cable theirs = new Cable(1);
        network.addDevice(mine);
        OMLibNetwork other = new OMLibNetwork();
        other.addDevice(theirs);

        network.mergeNetwork(other);

        assertEquals(2, network.getAllDevices().size());
        assertSame(network, theirs.getNetwork());
    }

    @Test
    void tickMovesEnergyBetweenDevices() {
        PowerTile producer = new PowerTile(1000, true, 0);
        PowerTile consumer = new PowerTile(0, false, 1);
        network.addDevice(producer);
        network.addDevice(consumer);

        int moved = network.tick();

        assertEquals(1000, moved);
        assertEquals(1000, consumer.getEnergyStorage().getEnergyStored());
    }

    @Test
    void tickOnAnEmptyNetworkDoesNothing() {
        assertEquals(0, network.tick());
    }

    @Test
    void savingAnUnnamedNetworkDoesNotThrow() {
        // Upstream's getAsNBTTagCompound called setString with a null name and threw.
        CompoundTag tag = assertDoesNotThrow(() -> network.save());
        assertEquals("", tag.getString("name"));
        assertEquals(network.getUuid(), tag.getUUID("uuid"));
    }

    @Test
    void savingKeepsTheNameAndUuid() {
        network.setName("base alpha");

        CompoundTag tag = network.save();

        assertEquals("base alpha", tag.getString("name"));
        assertEquals(network.getUuid(), tag.getUUID("uuid"));
    }

    @Test
    void ownerRoundTrips() {
        OMPlayer owner = new OMPlayer(UUID.randomUUID(), "owner", "test");
        network.setOwner(owner);

        assertEquals(owner, network.getOwner());
    }
}
