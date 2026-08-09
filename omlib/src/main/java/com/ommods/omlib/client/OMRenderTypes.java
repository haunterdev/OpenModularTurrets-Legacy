package com.ommods.omlib.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * Render types omlib needs that vanilla does not expose.
 *
 * <p>{@link #RAY} is vanilla's {@code RenderType.lines()} with one change: the line width is pinned
 * to 4.0, the value 1.12's {@code Ray.render} passed to {@code GlStateManager.glLineWidth}. Vanilla's
 * own lines type leaves the width empty, which resolves to at least 2.5 and scales with the window,
 * so a laser drawn through it would be thinner than the original and change with the resolution.
 *
 * <p>The class extends {@link RenderType} only to reach its protected {@code create} and the state
 * shard constants; it is never instantiated.
 */
public final class OMRenderTypes extends RenderType {

    /** A 4 pixel wide, translucent, unlit line. The laser and railgun beams draw with this. */
    public static final RenderType RAY = create("omlib_ray",
            DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(4.0)))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    private OMRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                          boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
        throw new UnsupportedOperationException("holder class");
    }
}
