package com.ommods.openmodularturrets.content.block;

import com.ommods.omlib.block.AbstractOMLibBlockWithEntity;
import com.ommods.openmodularturrets.content.turret.TurretType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.joml.Vector3f;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A turret head block. Its {@link TurretType} selects the faithful stats and visuals; the firing
 * loop lives in {@link TurretBaseBlockEntity}. One block class backs every turret type; the type is
 * fixed per block instance so machine gun, laser and railgun are distinct blocks sharing one entity
 * type. Right click does nothing, as in 1.12: everything is controlled from the base's GUI.
 */
public class TurretBaseBlock extends AbstractOMLibBlockWithEntity {

    private final TurretType turretType;

    public TurretBaseBlock(Properties properties, TurretType turretType) {
        super(properties);
        this.turretType = turretType;
    }

    public TurretType turretType() {
        return turretType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return OMTBlockInit.TURRET_ENTITY.create(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The turret is drawn in world by TurretRenderer (a BlockEntityRenderer), not by a static
        // block model, so the 3D head and barrel show. The item still uses the block model.
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        // Server only: the firing loop mutates the world and must not run on the client.
        if (level.isClientSide() || type != OMTBlockInit.TURRET_ENTITY.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> TurretBaseBlockEntity.tick(lvl, pos, st, (TurretBaseBlockEntity) be);
    }

    /**
     * The 1.12 canPlaceBlockAt gate: a head only places against a base with free turret slots,
     * counting both the tier's total head budget (1/1/2/3/4) and this type's maxSimultaneous cap.
     * Returning null makes the block item refuse placement.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return remainingTurretSlots(context.getLevel(), context.getClickedPos()) > 0
                ? super.getStateForPlacement(context)
                : null;
    }

    // The 1.12 OMTUtil.getRemainingTurretSlots: min of the type's remaining simultaneous budget and
    // the base tier's remaining head budget, over the first adjacent base.
    private int remainingTurretSlots(Level level, BlockPos pos) {
        for (Direction facing : Direction.values()) {
            if (!(level.getBlockEntity(pos.relative(facing))
                    instanceof com.ommods.openmodularturrets.content.base.TurretBaseTileEntity base)) {
                continue;
            }
            int total = 0;
            int sameType = 0;
            for (Direction baseSide : Direction.values()) {
                if (level.getBlockEntity(base.getBlockPos().relative(baseSide))
                        instanceof TurretBaseBlockEntity head) {
                    total++;
                    if (head.turretType() == turretType) {
                        sameType++;
                    }
                }
            }
            return Math.max(0, Math.min(turretType.maxSimultaneous() - sameType,
                    base.tier().maxTurrets() - total));
        }
        return 0;
    }

    // The 1.12 onBlockPlacedBy: a freshly placed head that outreaches the base's current range cap
    // asks the base to raise the cap to the new upper bound.
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof TurretBaseBlockEntity head) {
            var base = head.findBase(level, pos);
            if (base != null && turretType.range() > base.getCurrentMaxRange()) {
                base.setUpdateRange();
            }
        }
    }

    /**
     * The idle head particles, ported from 1.12's {@code randomDisplayTick} overrides. Only two heads
     * have any: the railgun hums cyan dust at its base, and the relativistic turret hums white dust at
     * its centre. Six per random tick each, scattered a gaussian tenth of a block across the two
     * horizontal axes. Every other head is visually silent when idle, so this method draws nothing for
     * them, and a concealed railgun draws nothing either, exactly as upstream gated it on CONCEALED.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        DustParticleOptions dust;
        double y;
        if (turretType == TurretType.RAILGUN) {
            if (level.getBlockEntity(pos) instanceof TurretBaseBlockEntity head && head.isConcealed()) {
                return;
            }
            dust = RAILGUN_DUST;
            y = pos.getY();
        } else if (turretType == TurretType.RELATIVISTIC) {
            dust = RELATIVISTIC_DUST;
            y = pos.getY() + 0.5;
        } else {
            return;
        }
        for (int i = 0; i <= 5; i++) {
            level.addParticle(dust,
                    pos.getX() + random.nextGaussian() / 10.0 + 0.5, y,
                    pos.getZ() + random.nextGaussian() / 10.0 + 0.5, 0.0, 0.0, 0.0);
        }
    }

    // 1.12 passed its redstone dust colour as an RGB triple that the particle clamps to 0..1:
    // (0, 50, 200) is cyan for the railgun and (200, 200, 200) is white for the relativistic turret.
    private static final DustParticleOptions RAILGUN_DUST =
            new DustParticleOptions(new Vector3f(0.0F, 1.0F, 1.0F), 1.0F);
    private static final DustParticleOptions RELATIVISTIC_DUST =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);

    // 1.12 getBoundingBox: a 0.2..0.8 cube, and no hitbox at all while the concealer addon has the
    // head tucked away (a concealed turret can't be seen, selected or collided with).
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof TurretBaseBlockEntity head && head.isConcealed()) {
            return Shapes.empty();
        }
        return HEAD_SHAPE;
    }

    private static final VoxelShape HEAD_SHAPE = Block.box(3.2, 3.2, 3.2, 12.8, 12.8, 12.8);
}
