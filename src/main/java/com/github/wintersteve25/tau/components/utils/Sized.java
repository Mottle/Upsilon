package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.Tau;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.utils.Size;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

public final class Sized implements PrimitiveUIComponent {

    private final Size size;
    private final UIComponent child;

    public Sized(Size size, UIComponent child) {
        this.size = size;
        this.child = child;
    }

    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        SimpleVec2i componentSize = size.get(layout.getSize());

        if (componentSize.outside(layout.getSize())) {
            Tau.LOGGER.error("Sized UIComponent has a size greater than the amount of size available");
            return layout.getSize();
        }

        Layout childLayout = new Layout(
                componentSize.x,
                componentSize.y,
                layout.getPosition(Axis.HORIZONTAL, componentSize.x),
                layout.getPosition(Axis.VERTICAL, componentSize.y));

        UIBuilder.build(childLayout, theme, child, context);
        return componentSize;
    }
}
