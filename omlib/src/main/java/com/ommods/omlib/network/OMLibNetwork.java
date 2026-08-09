package com.ommods.omlib.network;

import com.ommods.omlib.api.network.IController;
import com.ommods.omlib.api.network.INameController;
import com.ommods.omlib.api.network.INetworkTile;
import com.ommods.omlib.api.network.IPowerExchangeTile;
import com.ommods.omlib.util.OMPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One connected group of network devices.
 *
 * Holds no Level. Upstream stored a World on every network, which is what made it untestable and
 * kept the energy bugs alive; NetworkRegistry owns the level association instead.
 */
public class OMLibNetwork {
    private final Map<BlockPos, INetworkTile> devices = new LinkedHashMap<>();
    private final UUID uuid = UUID.randomUUID();

    /** Never null. Upstream left this null and then NPEd writing it to NBT. */
    private String name = "";

    @Nullable
    private OMPlayer owner;

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    @Nullable
    public OMPlayer getOwner() {
        return owner;
    }

    public void setOwner(@Nullable OMPlayer owner) {
        this.owner = owner;
    }

    /**
     * @return false if the device was refused, which happens when the network already has a
     *         controller and the incoming device is another one
     */
    public boolean addDevice(INetworkTile tile) {
        if (tile instanceof IController && getController(IController.class) != null) {
            return false;
        }
        devices.put(tile.getPosition(), tile);
        if (tile instanceof INameController) {
            setName(((INameController) tile).getNetworkName());
        }
        return true;
    }

    public boolean removeDevice(INetworkTile tile) {
        return devices.remove(tile.getPosition()) != null;
    }

    @Nullable
    public INetworkTile getConnectedDevice(BlockPos pos) {
        return devices.get(pos);
    }

    public Collection<INetworkTile> getAllDevices() {
        return devices.values();
    }

    public Set<BlockPos> getDevicePositions() {
        return devices.keySet();
    }

    /** The network's controller if it is of the given type, otherwise null. */
    @Nullable
    public <T> T getController(Class<T> clazz) {
        for (INetworkTile device : devices.values()) {
            if (device instanceof IController && clazz.isInstance(device)) {
                return clazz.cast(device);
            }
        }
        return null;
    }

    /** Absorbs every device of the other network, repointing each at this one. */
    public void mergeNetwork(OMLibNetwork other) {
        if (other == this) {
            return;
        }
        for (INetworkTile tile : new ArrayList<>(other.getAllDevices())) {
            devices.put(tile.getPosition(), tile);
            tile.setNetwork(this);
        }
        if (name.isEmpty()) {
            setName(other.getName());
        }
        if (owner == null) {
            setOwner(other.getOwner());
        }
        other.devices.clear();
    }

    /**
     * One tick of energy movement.
     *
     * @return the energy actually moved
     */
    public int tick() {
        List<IPowerExchangeTile> powerTiles = new ArrayList<>();
        for (INetworkTile device : devices.values()) {
            if (device instanceof IPowerExchangeTile) {
                powerTiles.add((IPowerExchangeTile) device);
            }
        }
        if (powerTiles.isEmpty()) {
            return 0;
        }
        return PowerDistributor.distribute(powerTiles);
    }

    /**
     * Identity only. Devices are not serialized: the network is derived from cable topology and
     * rebuilt when chunks load, so persisting the device list would be state to keep in sync for
     * no gain.
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        tag.putString("name", name);
        return tag;
    }
}
