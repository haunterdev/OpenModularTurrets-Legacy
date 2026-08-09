package com.ommods.openmodularturrets.client;

import com.ommods.openmodularturrets.content.base.TurretBaseTileEntity;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The turret base's camouflage model, the 1.20 equivalent of 1.12 omlib's CamoBakedModel: when the
 * base block entity carries a camo state, every render query delegates to that block's model, so
 * the base looks exactly like the disguise block. Without one it falls through to the base's own
 * model.
 */
public class CamoBakedModel extends BakedModelWrapper<BakedModel> {

    public CamoBakedModel(BakedModel original) {
        super(original);
    }

    @Nullable
    private static BakedModel camoModel(ModelData data) {
        BlockState camo = data.get(TurretBaseTileEntity.CAMO_PROPERTY);
        if (camo == null || camo.isAir()) {
            return null;
        }
        return Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);
    }

    @Override
    public @NotNull List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
            @Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand,
            @NotNull ModelData data, @Nullable RenderType renderType) {
        BlockState camo = data.get(TurretBaseTileEntity.CAMO_PROPERTY);
        BakedModel model = camoModel(data);
        if (model != null) {
            return model.getQuads(camo, side, rand, ModelData.EMPTY, renderType);
        }
        return super.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
                                             @NotNull ModelData data) {
        BlockState camo = data.get(TurretBaseTileEntity.CAMO_PROPERTY);
        BakedModel model = camoModel(data);
        if (model != null) {
            return model.getRenderTypes(camo, rand, ModelData.EMPTY);
        }
        return super.getRenderTypes(state, rand, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BakedModel model = camoModel(data);
        if (model != null) {
            return model.getParticleIcon(ModelData.EMPTY);
        }
        return super.getParticleIcon(data);
    }
}
