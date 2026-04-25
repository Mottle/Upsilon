package com.github.wintersteve25.tau.components.layout;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Fixed-size empty component used to create layout gaps.
 */
public final class Spacer implements PrimitiveUIComponent {

    private final SimpleVec2i size;

    /**
     * Creates a spacer with explicit size.
     */
    public Spacer(SimpleVec2i size) {
        this.size = size;
    }

    /**
     * Returns fixed spacer size without adding renderables.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        return size;
    }
}
