package com.github.wintersteve25.tau.components.layout;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

public final class Center implements PrimitiveUIComponent {

    private final UIComponent child;

    public Center(UIComponent child) {
        this.child = child;
    }

    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        layout.pushLayoutSetting(Axis.HORIZONTAL, LayoutSetting.CENTER);
        layout.pushLayoutSetting(Axis.VERTICAL, LayoutSetting.CENTER);

        try {
            return UIBuilder.build(layout, theme, child, context);
        } finally {
            layout.popLayoutSetting(Axis.VERTICAL);
            layout.popLayoutSetting(Axis.HORIZONTAL);
        }
    }
}
