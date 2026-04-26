package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Tests simple partial rebuild: click toggles between two text labels.
 * Each click marks dirty, triggering a subtree rebuild that only
 * replaces this component's artifacts without full screen rebuild.
 */
public class TestPartialText extends DynamicUIComponent implements GuiEventListener {
    private boolean state;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        state = !state;
        return new Sized(
                Size.staticSize(300, 20),
                new Center(new Text.Builder(state ? "State A - Partial rebuild works!" : "State B - Still working!"))
        );
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        rebuild();
        return GuiEventListener.super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override public boolean isFocused() { return false; }
    @Override public void setFocused(boolean f) {}
}
