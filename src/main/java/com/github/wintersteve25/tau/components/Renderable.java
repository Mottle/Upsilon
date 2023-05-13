package com.github.wintersteve25.tau.components;

import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Widget;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.utils.Vector2i;

import java.util.List;

public final class Renderable implements PrimitiveUIComponent {
    
    private final Widget renderable;

    public Renderable(Widget renderable) {
        this.renderable = renderable;
    }

    @Override
    public Vector2i build(Layout layout, Theme theme, List<Widget> renderables, List<Widget> tooltips, List<DynamicUIComponent> dynamicUIComponents, List<GuiEventListener> eventListeners) {
        renderables.add(renderable);
        return layout.getSize();
    }
}
