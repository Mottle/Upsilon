package com.github.wintersteve25.tau.components.base;

import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.layout.Layout;

/**
 * Base interface for all Upsilon UI components.
 * <p>
 * Components are built as a chain where each call may return the next component
 * to build. Returning {@code null} terminates the current branch.
 */
public interface UIComponent {
    /**
     * Builds this component for the current frame/context.
     *
     * @param layout current layout constraints and positioning context
     * @param theme active theme used for drawing primitives
     * @return next component in the build chain, or {@code null} when the branch ends
     */
    UIComponent build(Layout layout, Theme theme);
}
