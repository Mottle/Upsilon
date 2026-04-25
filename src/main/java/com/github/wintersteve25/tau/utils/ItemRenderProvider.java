package com.github.wintersteve25.tau.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * {@link RenderProvider} that renders an item stack at component origin.
 */
public class ItemRenderProvider implements RenderProvider {

    private final ItemStack itemStack;

    /** Creates a renderer from an existing item stack. */
    public ItemRenderProvider(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    /** Creates a renderer from an item instance. */
    public ItemRenderProvider(Item item) {
        this(new ItemStack(item));
    }

    /** Creates a renderer from a block's item form. */
    public ItemRenderProvider(Block block) {
        this(new ItemStack(block.asItem()));
    }

    /**
     * Renders the item stack at the provided top-left position.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, int x, int y, int width, int height) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        graphics.renderItem(itemStack, x, y);
        poseStack.popPose();
    }
}
