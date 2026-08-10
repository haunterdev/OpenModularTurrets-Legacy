package com.ommods.openmodularturrets.content.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A turret damage source that remembers which base fired the shot, the port of 1.12's
 * AbstractOMTDamageSource carrying its TurretBase. The death event handler resolves the position
 * back to the base to bump its kill counters.
 */
public class OMTDamageSource extends DamageSource {

    @Nullable
    private final BlockPos basePos;
    private final String messageId;

    public OMTDamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity attacker,
                           @Nullable BlockPos basePos, String messageId) {
        super(type, directEntity, attacker);
        this.basePos = basePos;
        this.messageId = messageId;
    }

    @Nullable
    public BlockPos basePos() {
        return basePos;
    }

    /**
     * The classic per turret death lines. 1.12 gave the damage source a name per projectile and let
     * vanilla build {@code death.attack.<name>} from it; here the name travels on the source instead,
     * because the damage type is registry data and splitting it per round would mean seven registry
     * entries for one string.
     *
     * <p>Always the one argument form. Upstream shipped no {@code .player} variant of any of these
     * lines, so 1.12's "use the .player key when an attacker exists" branch never fired, and a kill
     * credited to the fake drops fake player still read as the plain line.
     */
    @Override
    public Component getLocalizedDeathMessage(LivingEntity entity) {
        return Component.translatable("death.attack." + messageId, entity.getDisplayName());
    }
}
