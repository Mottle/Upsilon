package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.*;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Supplier;

/**
 * Mount-state-backed widget wrapper for partial-rebuild-safe widgets.
 */
public final class WidgetFactoryWrapper implements PrimitiveUIComponent, MountStateHost<WidgetFactoryWrapper.WidgetMountState> {
    private final Supplier<AbstractWidget> widgetFactory;
    private WidgetMountState activeMountState;

    public WidgetFactoryWrapper(Supplier<AbstractWidget> widgetFactory) {
        this.widgetFactory = widgetFactory;
    }

    @Override
    public SimpleVec2i build(Layout layout, Theme theme, com.github.wintersteve25.tau.build.BuildContext context) {
        BuildSession session = UIBuilder.currentSession();
        if (session != null && session.getMode() == BuildMode.MOUNTED_COMMITTABLE) {
            WidgetMountState state = session.getOrCreateStagedState(this, WidgetMountState::new);
            if (state.widget == null) {
                state.widget = widgetFactory.get();
            }
            state.widget.setWidth(layout.getWidth());
            state.widget.setHeight(layout.getHeight());
            state.widget.setX(layout.getPosition(Axis.HORIZONTAL, state.widget.getWidth()));
            state.widget.setY(layout.getPosition(Axis.VERTICAL, state.widget.getHeight()));

            session.addRenderable(state.widget);
            session.addListener(state.widget);
        } else if (session != null) {
            AbstractWidget widget = widgetFactory.get();
            widget.setWidth(layout.getWidth());
            widget.setHeight(layout.getHeight());
            widget.setX(layout.getPosition(Axis.HORIZONTAL, widget.getWidth()));
            widget.setY(layout.getPosition(Axis.VERTICAL, widget.getHeight()));
        } else {
            AbstractWidget widget = widgetFactory.get();
            widget.setWidth(layout.getWidth());
            widget.setHeight(layout.getHeight());
            widget.setX(layout.getPosition(Axis.HORIZONTAL, widget.getWidth()));
            widget.setY(layout.getPosition(Axis.VERTICAL, widget.getHeight()));
            context.renderables().add(widget);
            context.eventListeners().add(widget);
        }

        return layout.getSize();
    }

    @Override
    public WidgetMountState getActiveMountState() {
        return activeMountState;
    }

    @Override
    public void setActiveMountState(WidgetMountState state) {
        this.activeMountState = state;
    }

    public static final class WidgetMountState implements MountState {
        AbstractWidget widget;
    }
}
