package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.PartialCommitUnsafe;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.AbstractWidget;

/**
 * Adapts a vanilla {@link AbstractWidget} into a primitive UI component.
 */
public final class WidgetWrapper implements PrimitiveUIComponent, PartialCommitUnsafe {

    private final AbstractWidget child;

    /**
     * Creates a wrapper for a widget instance.
     */
    public WidgetWrapper(AbstractWidget child) {
        this.child = child;
    }

    /**
     * Applies layout bounds to wrapped widget and registers it.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        child.setWidth(layout.getWidth());
        child.setHeight(layout.getHeight());
        child.setX(layout.getPosition(Axis.HORIZONTAL, child.getWidth()));
        child.setY(layout.getPosition(Axis.VERTICAL, child.getHeight()));

        context.renderables().add(child);
        context.eventListeners().add(child);

        return layout.getSize();
    }
}
