package com.ommods.openmodularturrets.client.projectile;

import com.ommods.openmodularturrets.content.projectile.OMTProjectile;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * A renderer that draws nothing. The classic turret projectiles show themselves through the particle
 * trails and effects each entity spawns (crit, smoke, flame), so they need no model, but Forge still
 * requires a renderer to be registered for every entity type or spawning one crashes the client.
 */
public class EmptyProjectileRenderer<T extends OMTProjectile> extends EntityRenderer<T> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    public EmptyProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEXTURE;
    }
}
