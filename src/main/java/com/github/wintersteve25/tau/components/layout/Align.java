package com.github.wintersteve25.tau.components.layout;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Applies temporary horizontal/vertical alignment overrides to a child.
 */
public final class Align implements PrimitiveUIComponent {

    private final UIComponent child;
    private final LayoutSetting horizontal;
    private final LayoutSetting vertical;

    /**
     * Creates an alignment wrapper.
     */
    public Align(UIComponent child, LayoutSetting horizontal, LayoutSetting vertical) {
        this.child = child;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /**
     * Builds child with temporary alignment overrides applied to the layout.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        boolean pushedH = false;
        boolean pushedV = false;

        if (horizontal != null) {
            layout.pushLayoutSetting(Axis.HORIZONTAL, horizontal);
            pushedH = true;
        }

        if (vertical != null) {
            layout.pushLayoutSetting(Axis.VERTICAL, vertical);
            pushedV = true;
        }

        try {
            return UIBuilder.build(layout, theme, child, context);
        } finally {
            if (pushedV) layout.popLayoutSetting(Axis.VERTICAL);
            if (pushedH) layout.popLayoutSetting(Axis.HORIZONTAL);
        }
    }


    public static final class Builder {
        private LayoutSetting horizontal;
        private LayoutSetting vertical;

        /**
         * Creates a new align builder.
         */
        public Builder() {
        }

        /**
         * Sets horizontal alignment override.
         */
        public Builder withHorizontal(LayoutSetting horizontal) {
            this.horizontal = horizontal;
            return this;
        }

        /**
         * Sets vertical alignment override.
         */
        public Builder withVertical(LayoutSetting vertical) {
            this.vertical = vertical;
            return this;
        }

        /**
         * Builds an align wrapper around the child.
         */
        public Align build(UIComponent child) {
            return new Align(child, horizontal, vertical);
        }
    }
}
