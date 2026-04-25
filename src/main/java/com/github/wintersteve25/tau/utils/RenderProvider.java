package com.github.wintersteve25.tau.utils;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Callback contract for custom render logic used by {@code Render} components.
 */
@FunctionalInterface
public interface RenderProvider {
    /**
     * Renders using both mouse context and resolved component bounds.
     */
    void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, int x, int y, int width, int height);
}
