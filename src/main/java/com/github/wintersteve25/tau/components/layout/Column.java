package com.github.wintersteve25.tau.components.layout;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.FlexSizeBehaviour;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

import java.util.Arrays;

/**
 * Vertical layout component that places children top-to-bottom.
 */
public final class Column implements PrimitiveUIComponent {

    private final Iterable<UIComponent> children;
    private final int spacing;
    private final FlexSizeBehaviour sizeBehaviour;
    private final LayoutSetting alignment;

    /**
     * Creates a column layout.
     *
     * @param spacing       vertical spacing between children
     * @param sizeBehaviour column sizing behavior
     * @param children      child components in display order
     * @param alignment     horizontal alignment applied to children
     */
    public Column(int spacing, FlexSizeBehaviour sizeBehaviour, Iterable<UIComponent> children, LayoutSetting alignment) {
        this.children = children;
        this.spacing = spacing;
        this.sizeBehaviour = sizeBehaviour;
        this.alignment = alignment;
    }

    /**
     * Builds column children and returns resolved container size.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {

        SimpleVec2i size;

        if (sizeBehaviour == FlexSizeBehaviour.MIN) {
            size = SimpleVec2i.zero();
            boolean first = true;

            for (UIComponent child : children) {
                SimpleVec2i childSize = UIBuilder.measure(layout.copy(), theme, child);
                if (!first) size.y += spacing;
                first = false;
                size.y += childSize.y;
                size.x = Math.max(size.x, childSize.x);
            }
        } else {
            size = new SimpleVec2i(layout.getWidth(), layout.getHeight());
        }

        Layout childrenLayout = new Layout(size.x, size.y, layout.getPosition(Axis.HORIZONTAL, size.x), layout.getPosition(Axis.VERTICAL, size.y));
        childrenLayout.pushLayoutSetting(Axis.HORIZONTAL, alignment);

        int pushedOffsets = 0;
        try {
            for (UIComponent child : children) {
                SimpleVec2i childSize = UIBuilder.build(childrenLayout, theme, child, context);
                childrenLayout.pushOffset(Axis.VERTICAL, childSize.y + spacing);
                pushedOffsets++;
            }
        } finally {
            for (int i = 0; i < pushedOffsets; i++) {
                childrenLayout.popOffset(Axis.VERTICAL);
            }
            childrenLayout.popLayoutSetting(Axis.HORIZONTAL);
        }

        return size;
    }


    public static final class Builder {
        private int spacing;
        private FlexSizeBehaviour sizeBehaviour;
        private LayoutSetting alignment;

        /**
         * Creates a new column builder.
         */
        public Builder() {
        }

        /**
         * Sets vertical spacing between children.
         */
        public Builder withSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Sets size behavior used by the column container.
         */
        public Builder withSizeBehaviour(FlexSizeBehaviour sizeBehaviour) {
            this.sizeBehaviour = sizeBehaviour;
            return this;
        }

        /**
         * Sets horizontal alignment used for children inside the column.
         */
        public Builder withAlignment(LayoutSetting alignment) {
            this.alignment = alignment;
            return this;
        }

        /**
         * Builds a column from vararg children.
         */
        public Column build(UIComponent... children) {
            return build(Arrays.asList(children));
        }

        /**
         * Builds a column from iterable children.
         */
        public Column build(Iterable<UIComponent> children) {
            return new Column(spacing,
                    sizeBehaviour == null
                            ? FlexSizeBehaviour.MIN
                            : sizeBehaviour,
                    children,
                    alignment == null ? LayoutSetting.CENTER : alignment);
        }
    }
}
