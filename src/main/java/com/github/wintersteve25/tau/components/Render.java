package com.github.wintersteve25.tau.components;

import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Widget;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.utils.RenderProvider;
import com.github.wintersteve25.tau.utils.Vector2i;

import java.util.List;

public final class Render implements PrimitiveUIComponent {
    private final RenderProvider renderer;

    public Render(RenderProvider renderer) {
        this.renderer = renderer;
    }

    @Override
    public Vector2i build(Layout layout, Theme theme, List<Widget> renderables, List<Widget> tooltips, List<DynamicUIComponent> dynamicUIComponents, List<GuiEventListener> eventListeners) {
        int width = layout.getWidth();
        int height = layout.getHeight();
        int x = layout.getPosition(Axis.HORIZONTAL, width);
        int y = layout.getPosition(Axis.VERTICAL, height);
        renderables.add((pPoseStack, pMouseX, pMouseY, pPartialTicks) -> renderer.render(pPoseStack, pMouseX, pMouseY, pPartialTicks, x, y, width, height));
        return layout.getSize();
    }
}
