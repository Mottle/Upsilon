package com.github.wintersteve25.tau.theme;

import com.github.wintersteve25.tau.utils.Color;
import com.github.wintersteve25.tau.utils.InteractableState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/**
 * Rendering abstraction used by UI primitives.
 * <p>
 * Implementations define the visual style for controls while layout and input remain
 * component-driven.
 */
public interface Theme {
    /**
     * Draws a button frame for the given state.
     */
    void drawButton(GuiGraphics graphics, int x, int y, int width, int height, float partialTicks, int mouseX, int mouseY, InteractableState state);

    /**
     * Draws a container background.
     */
    void drawContainer(GuiGraphics graphics, int x, int y, int width, int height, float partialTicks, int mouseX, int mouseY);

    /**
     * Draws a scrollbar track/thumb.
     */
    void drawScrollbar(GuiGraphics graphics, int x, int y, int width, int height, float partialTicks, int mouseX, int mouseY);

    /**
     * Draws tooltip components at the current mouse position.
     */
    void drawTooltip(GuiGraphics graphics, int mouseX, int mouseY, Font font, List<ClientTooltipComponent> tooltips, Optional<ClientTooltipPositioner> positioner);

    /**
     * Draws a single slot frame.
     */
    void drawSlot(GuiGraphics graphics, int x, int y);

    /**
     * Returns default text color used by text components.
     */
    Color getTextColor();
}
