package com.github.wintersteve25.tau.renderer;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import java.util.List;

/**
 * Renders a group of artifacts inside a GUI-coordinate scissor rectangle.
 * <p>
 * {@link GuiGraphics} owns a scissor stack and intersects nested rectangles.
 * Calling {@code RenderSystem.enableScissor} directly would overwrite that
 * state and let nested {@code Clip} / {@code ListView} renderables escape their
 * parent clipping region.
 */
public final class ScissorRenderer {
    private ScissorRenderer() {
    }

    /**
     * Renders artifacts with a scissor rectangle whose right and bottom bounds
     * are derived from its origin and dimensions.
     */
    public static void render(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            List<Renderable> renderables,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            for (Renderable renderable : renderables) {
                renderable.render(graphics, mouseX, mouseY, partialTicks);
            }
        } finally {
            graphics.disableScissor();
        }
    }
}
