package com.ommods.openmodularturrets.content.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * A turret damage source that remembers which base fired the shot, the port of 1.12's
 * AbstractOMTDamageSource carrying its TurretBase. The death event handler resolves the position
 * back to the base to bump its kill counters.
 */
public class OMTDamageSource extends DamageSource {

    @Nullable
    private final BlockPos basePos;

    public OMTDamageSource(Holder<DamageType> type, @Nullable Entity directEntity, @Nullable Entity attacker,
                           @Nullable BlockPos basePos) {
        super(type, directEntity, attacker);
        this.basePos = basePos;
    }

    @Nullable
    public BlockPos basePos() {
        return basePos;
    }
}
