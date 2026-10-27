package com.ommods.ompd.client;

import com.ommods.ompd.content.CamoTrapBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/** Delegates every render query to the trap's selected camouflage block model. */
public class CamoTrapBakedModel extends BakedModelWrapper<BakedModel> {
    public CamoTrapBakedModel(BakedModel original) {
        super(original);
    }

    @Nullable
    private static BakedModel camoModel(ModelData data) {
        BlockState camo = data.get(CamoTrapBlockEntity.CAMO_PROPERTY);
        return camo == null || camo.isAir()
                ? null
                : Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);
    }

    @Override
    public @NotNull List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
            @Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
            @NotNull ModelData data, @Nullable RenderType renderType) {
        BlockState camo = data.get(CamoTrapBlockEntity.CAMO_PROPERTY);
        BakedModel model = camoModel(data);
        return model == null
                ? super.getQuads(state, side, rand, data, renderType)
                : model.getQuads(camo, side, rand, ModelData.EMPTY, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
                                             @NotNull ModelData data) {
        BlockState camo = data.get(CamoTrapBlockEntity.CAMO_PROPERTY);
        BakedModel model = camoModel(data);
        return model == null
                ? super.getRenderTypes(state, rand, data)
                : model.getRenderTypes(camo, rand, ModelData.EMPTY);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BakedModel model = camoModel(data);
        return model == null ? super.getParticleIcon(data) : model.getParticleIcon(ModelData.EMPTY);
    }
}
