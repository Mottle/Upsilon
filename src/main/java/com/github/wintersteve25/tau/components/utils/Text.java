package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.render.Render;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.RenderProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.utils.Color;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.network.chat.FormattedText;

/**
 * Text render component with configurable overflow behavior.
 */
public final class Text implements PrimitiveUIComponent, RenderProvider {

    private static final String ellipsisText = "...";

    private final FormattedText text;
    private final OverflowBehaviour overflowBehaviour;
    private Color color;

    /**
     * Creates a text component.
     *
     * @param text formatted text content
     * @param color explicit text color, or null to use theme color
     * @param overflowBehaviour overflow mode
     */
    public Text(FormattedText text, Color color, OverflowBehaviour overflowBehaviour) {
        this.text = text;
        this.color = color;
        this.overflowBehaviour = overflowBehaviour;
    }

    /**
     * Resolves text bounds and registers render operations.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        if (color == null) {
            color = theme.getTextColor();
        }

        Font fontRenderer = Minecraft.getInstance().font;
        int width = fontRenderer.width(text);
        int ellipsisWidth = fontRenderer.width(ellipsisText);

        boolean willOverflow = width > layout.getWidth();
        if (willOverflow) {
            width = layout.getWidth();
        }

        int height = willOverflow && overflowBehaviour == OverflowBehaviour.WRAP ?
                fontRenderer.wordWrapHeight(text.getString(), width) :
                8; // constant for line height in minecraft

        int x = layout.getPosition(Axis.HORIZONTAL, width);
        int y = layout.getPosition(Axis.VERTICAL, height);

        int finalWidth = width;
        Color finalColor = color;
        if (overflowBehaviour != OverflowBehaviour.CLIP) {
            context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> render(graphics, pMouseX, pMouseY, pPartialTicks, x, y, finalWidth, height, finalColor, ellipsisWidth));
        } else {
            UIBuilder.build(
                    new Layout(width, height, x, y),
                    theme,
                    new Clip.Builder().build(new Render(this)),
                    context
            );
        }

        return new SimpleVec2i(width, height);
    }

    /**
     * Renders text using current overflow mode and color.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, int x, int y, int width, int height) {
        render(graphics, mouseX, mouseY, partialTicks, x, y, width, height, color, Minecraft.getInstance().font.width(ellipsisText));
    }

    private void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, int x, int y, int width, int height, Color renderColor, int ellipsisWidth) {
        Font font = Minecraft.getInstance().font;

        switch (overflowBehaviour) {
            case WRAP -> graphics.drawWordWrap(font, text, x, y, width, renderColor.getAARRGGBB());
            case ELLIPSIS -> graphics.drawString(font, font.substrByWidth(text, width - ellipsisWidth).getString() + ellipsisText, x, y, renderColor.getAARRGGBB(), true);
            default -> graphics.drawString(font, text.getString(), x, y, renderColor.getAARRGGBB(), true);
        }
    }

    public static final class Builder implements UIComponent {
        private final Component text;
        private Color color;
        private OverflowBehaviour overflowBehaviour;

        /** Creates a builder from translatable/literal component text. */
        public Builder(Component text) {
            this.text = text;
        }

        /** Creates a builder from literal string text. */
        public Builder(String text) {
            this.text = Component.literal(text);
        }

        /** Sets explicit text color. */
        public Builder withColor(Color color) {
            this.color = color;
            return this;
        }

        /** Sets overflow behavior for rendering. */
        public Builder withOverflowBehaviour(OverflowBehaviour overflowBehaviour) {
            this.overflowBehaviour = overflowBehaviour;
            return this;
        }

        /** Builds text component with default overflow mode when unset. */
        public Text build() {
            return new Text(
                    text,
                    color,
                    overflowBehaviour == null ? OverflowBehaviour.OVERFLOW : overflowBehaviour
            );
        }

        /**
         * Builds component instance with theme fallback for unset color.
         */
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return new Text(
                    text,
                    color == null ? theme.getTextColor() : color,
                    overflowBehaviour == null ? OverflowBehaviour.OVERFLOW : overflowBehaviour
            );
        }
    }

    /**
     * Overflow handling mode for text rendering.
     */
    public enum OverflowBehaviour {
        OVERFLOW,
        WRAP,
        CLIP,
        ELLIPSIS
    }
}
