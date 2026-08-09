package com.ommods.openmodularturrets.content.block;

import com.ommods.openmodularturrets.client.OMTClient;
import com.ommods.openmodularturrets.content.item.ClassicTooltips;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** The crank lever's block item with the 1.12 ItemBlockLever shift tooltip and 3D icon. */
public class LeverItem extends BlockItem {

    public LeverItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return OMTClient.leverItemRenderer();
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (ClassicTooltips.hint(tooltip)) {
            return;
        }
        tooltip.add(Component.literal("A lever to generate energy, place on tier 1 base."));
    }
}
