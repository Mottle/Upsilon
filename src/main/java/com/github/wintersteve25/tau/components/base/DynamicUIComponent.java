package com.github.wintersteve25.tau.components.base;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;

/**
 * A UI component that can be rebuilt on demand
 */
public abstract class DynamicUIComponent implements UIComponent {

    public boolean dirty;

    public void tick() {
    }

    public void destroy() {
    }

    protected void rebuild() {
        dirty = true;
    }

    public final void finalizeDynamic(BuildContext context) {
    }

    public final void buildDynamic(BuildContext context, Layout layout, Theme theme) {
    }
}
