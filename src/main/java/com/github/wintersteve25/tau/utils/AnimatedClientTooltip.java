package com.github.wintersteve25.tau.utils;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

/**
 * Tooltip component that renders text from a live {@link Variable} value.
 */
public class AnimatedClientTooltip implements ClientTooltipComponent {
    
    private Variable<Component> text;

    /** Creates an animated tooltip bound to a reactive text value. */
    public AnimatedClientTooltip(Variable<Component> text) {
        this.text = text;
    }

    /**
     * Draws current tooltip text value.
     */
    @Override
    public void renderText(Font font, int mouseX, int mouseY, Matrix4f matrix, MultiBufferSource.BufferSource bufferSource) {
        font.drawInBatch(this.text.getValue(), (float)mouseX, (float)mouseY, -1, true, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
    }

    /**
     * Returns tooltip row height.
     */
    @Override
    public int getHeight() {
        return 10;
    }

    /**
     * Returns width of current text value.
     */
    @Override
    public int getWidth(Font font) {
        return font.width(this.text.getValue());
    }
}
