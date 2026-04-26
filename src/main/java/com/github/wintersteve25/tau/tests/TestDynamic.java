package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Click toggles between red and blue rectangles, testing full dynamic rebuild.
 * Implements PrimitiveUIComponent to produce its own renderable directly.
 */
public class TestDynamic extends DynamicUIComponent implements PrimitiveUIComponent, GuiEventListener {
    private boolean state;

    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        state = !state;
        if (state) {
            context.renderables().add((graphics, mouseX, mouseY, tick) ->
                    graphics.fill(100, 100, 300, 300, 0xFF_FF_00_00));
        } else {
            context.renderables().add((graphics, mouseX, mouseY, tick) ->
                    graphics.fill(100, 100, 300, 300, 0xFF_00_00_FF));
        }
        return layout.getSize();
    }

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        return null;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        rebuild();
        return true;
    }

    @Override
    public boolean isFocused() { return false; }

    @Override
    public void setFocused(boolean f) {}
}
