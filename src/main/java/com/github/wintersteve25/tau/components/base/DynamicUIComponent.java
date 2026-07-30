package com.github.wintersteve25.tau.components.base;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;

/**
 * Stateful component that can request a UI rebuild.
 * <p>
 * A dynamic component marks itself dirty via {@link #rebuild()}. Renderers
 * first attempt a mounted partial commit on the next tick and fall back to a
 * full rebuild when the subtree cannot safely be committed in place.
 */
public abstract class DynamicUIComponent implements UIComponent {

    /**
     * Indicates that the owning renderer should rebuild the UI tree.
     */
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
     * Marks this component dirty so the owning renderer refreshes its UI tree.
     */
    protected void rebuild() {
        dirty = true;
    }

    /**
     * Hook used by {@code UIBuilder} after this dynamic component has been built.
     * Subclasses may override to react to build completion.
     *
     * @param context active build context
     */
    public void finalizeDynamic(BuildContext context) {
    }

    /**
     * Hook used by {@code UIBuilder} before this dynamic component is built.
     * Subclasses may override to react to build start.
     *
     * @param context active build context
     * @param layout  current layout snapshot
     * @param theme   active theme
     */
    public void buildDynamic(BuildContext context, Layout layout, Theme theme) {
    }
}
