package com.github.wintersteve25.tau.components.base;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Low-level component that writes directly into a {@link BuildContext}.
 * <p>
 * Primitive components are responsible for producing renderables/listeners/slots
 * and returning their resulting size.
 */
public interface PrimitiveUIComponent extends UIComponent {
    /**
     * Builds this primitive component directly into the provided context.
     *
     * @param layout  active layout constraints and placement context
     * @param theme   active theme used to draw primitives
     * @param context collector for renderables, listeners, slots and dynamic nodes
     * @return rendered size of this component
     */
    SimpleVec2i build(Layout layout, Theme theme, BuildContext context);

    /**
     * Primitive components terminate the UIComponent chain by default.
     *
     * @param layout current layout context
     * @param theme  active theme
     * @return always {@code null}
     */
    @Override
    default UIComponent build(Layout layout, Theme theme) {
        return null;
    }
}
