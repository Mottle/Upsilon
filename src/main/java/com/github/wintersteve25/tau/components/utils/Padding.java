package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.utils.Pad;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Applies inset/outset padding around a child component.
 */
public final class Padding implements PrimitiveUIComponent {

    private final Pad pad;
    private final UIComponent child;

    /**
     * Creates a padding wrapper.
     */
    public Padding(Pad pad, UIComponent child) {
        this.pad = pad;
        this.child = child;
    }

    /**
     * Builds child within padding-adjusted layout and returns padded size.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        if (pad == null) {
            return UIBuilder.build(layout, theme, child, context);
        }

        try {
            layout.pushOffset(Axis.HORIZONTAL, pad.left);
            layout.pushSizeMod(Axis.HORIZONTAL, -pad.right - pad.left);

            layout.pushOffset(Axis.VERTICAL, pad.top);
            layout.pushSizeMod(Axis.VERTICAL, -pad.bottom - pad.top);

            return UIBuilder.build(layout, theme, child, context).addNew(pad.getSize());
        } finally {
            layout.popSizeMod(Axis.VERTICAL);
            layout.popOffset(Axis.VERTICAL);
            layout.popSizeMod(Axis.HORIZONTAL);
            layout.popOffset(Axis.HORIZONTAL);
        }
    }
}
