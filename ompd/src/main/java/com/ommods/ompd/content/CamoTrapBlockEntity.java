package com.ommods.ompd.content;

import com.ommods.omlib.block.OMBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/** Persisted disguise and owner for the camouflage trap. Null disguise means its own texture. */
public class CamoTrapBlockEntity extends OMBlockEntity {
    public static final ModelProperty<BlockState> CAMO_PROPERTY = new ModelProperty<>();

    @Nullable
    private BlockState camoState;

    public CamoTrapBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public BlockState getCamoState() {
        return camoState;
    }

    public void setCamoState(@Nullable BlockState state) {
        camoState = state;
        setChanged();
        syncToClient();
    }

    @Override
    public @NotNull ModelData getModelData() {
        return camoState == null
                ? ModelData.EMPTY
                : ModelData.builder().with(CAMO_PROPERTY, camoState).build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        camoState = tag.contains("camo")
                ? NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound("camo"))
                : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (camoState != null) {
            tag.put("camo", NbtUtils.writeBlockState(camoState));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (owner != null && owner.getUuid() != null) {
            tag.put("owner", owner.serializeNBT());
        }
        if (camoState != null) {
            tag.put("camo", NbtUtils.writeBlockState(camoState));
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        super.onDataPacket(net, packet);
        Level level = getLevel();
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 8);
        }
    }

    private void syncToClient() {
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2);
        }
    }
}
