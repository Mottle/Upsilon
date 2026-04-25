package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.FlexSizeBehaviour;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Optional background container around a child component.
 */
public final class Container implements PrimitiveUIComponent {

    private final UIComponent child;
    private final FlexSizeBehaviour sizeBehaviour;
    private final boolean drawBackground;

    /**
     * Creates a container component.
     *
     * @param child wrapped child component, may be null
     * @param drawBackground whether theme container background is drawn
     * @param sizeBehaviour size policy when parent offers flexible space
     */
    public Container(UIComponent child, boolean drawBackground, FlexSizeBehaviour sizeBehaviour) {
        this.child = child;
        this.drawBackground = drawBackground;
        this.sizeBehaviour = sizeBehaviour;
    }

    /**
     * Builds optional child, optional background, and returns resolved size.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        if (child == null && !drawBackground) {
            return layout.getSize();
        }

        SimpleVec2i size = layout.getSize();
        BuildContext innerContext = new BuildContext();

        if (child != null) {
            if (sizeBehaviour == FlexSizeBehaviour.MIN) {
                size = UIBuilder.build(layout, theme, child, innerContext);
            } else {
                UIBuilder.build(layout, theme, child, innerContext);
            }
        }

        if (drawBackground) {
            int width = size.x;
            int height = size.y;
            int x = layout.getPosition(Axis.HORIZONTAL, width);
            int y = layout.getPosition(Axis.VERTICAL, height);
            context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> theme.drawContainer(graphics, x, y, width, height, pPartialTicks, pMouseX, pMouseY));
        }

        context.addAll(innerContext);
        return size;
    }


    public static final class Builder implements UIComponent {
        private UIComponent child;
        private boolean drawBackground = true;
        private FlexSizeBehaviour sizeBehaviour;

        /** Creates a new container builder. */
        public Builder() {
        }

        /** Sets child component displayed inside container. */
        public Builder withChild(UIComponent child) {
            this.child = child;
            return this;
        }

        /** Disables themed background drawing. */
        public Builder noBackground() {
            this.drawBackground = false;
            return this;
        }

        /** Sets size behavior used for flexible layouts. */
        public Builder withSizeBehaviour(FlexSizeBehaviour sizeBehaviour) {
            this.sizeBehaviour = sizeBehaviour;
            return this;
        }

        /** Builds a container with defaults for unset values. */
        public Container build() {
            return new Container(child, drawBackground, sizeBehaviour == null ? FlexSizeBehaviour.MAX : sizeBehaviour);
        }

        /**
         * Returns a built container component for this builder configuration.
         */
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return build();
        }
    }
}
