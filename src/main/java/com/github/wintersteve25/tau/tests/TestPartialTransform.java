package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.render.Transform;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import com.github.wintersteve25.tau.utils.Transformation;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.joml.Vector3f;

/**
 * Tests partial rebuild inside a Transform: the dynamic component
 * lives inside a translated container. Click toggles text, and the
 * partial commit operates within the Transform's inner artifact context.
 */
public class TestPartialTransform extends DynamicUIComponent implements GuiEventListener {
    private boolean state;

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        state = !state;
        return new Transform(
                new Sized(
                        Size.staticSize(200, 20),
                        new Center(new Text.Builder(state ? "Inside Transform - A" : "Inside Transform - B"))
                ),
                Transformation.translate(new Vector3f(50, 50, 0))
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
