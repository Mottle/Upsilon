package com.github.wintersteve25.tau.components.base;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;

/**
 * Stateful component that can request a full renderer rebuild.
 * <p>
 * A dynamic component marks itself dirty via {@link #rebuild()}. Renderers then
 * trigger a full UI rebuild on the next tick.
 */
public abstract class DynamicUIComponent implements UIComponent {

    /** Indicates that the owning renderer should rebuild the UI tree. */
    public boolean dirty;

    /**
     * Per-tick callback before dirty evaluation.
     */
    public void tick() {
    }

    /**
     * Cleanup callback invoked when a renderer discards this component tree.
     */
    public void destroy() {
    }

    /**
     * Marks this component dirty so the owning renderer rebuilds the full UI tree.
     */
    protected void rebuild() {
        dirty = true;
    }

    /**
     * Hook used by {@code UIBuilder} after this dynamic component has been built.
     *
     * @param context active build context
     */
    public final void finalizeDynamic(BuildContext context) {
    }

    /**
     * Hook used by {@code UIBuilder} before this dynamic component is built.
     *
     * @param context active build context
     * @param layout current layout snapshot
     * @param theme active theme
     */
    public final void buildDynamic(BuildContext context, Layout layout, Theme theme) {
    }
}
