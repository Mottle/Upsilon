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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Horizontal layout component that places children left-to-right.
 */
public final class Row implements PrimitiveUIComponent {

    private final List<UIComponent> children;
    private final int spacing;
    private final FlexSizeBehaviour sizeBehaviour;
    private final LayoutSetting alignment;

    /**
     * Creates a row layout.
     *
     * @param spacing       horizontal spacing between children
     * @param sizeBehaviour row width/height sizing behavior
     * @param children      child components in display order
     * @param alignment     vertical alignment applied to children
     */
    public Row(int spacing, FlexSizeBehaviour sizeBehaviour, Iterable<UIComponent> children, LayoutSetting alignment) {
        this.children = copyChildren(children);
        this.spacing = spacing;
        this.sizeBehaviour = sizeBehaviour;
        this.alignment = alignment;
    }

    private static List<UIComponent> copyChildren(Iterable<UIComponent> children) {
        List<UIComponent> copied = new ArrayList<>();
        children.forEach(copied::add);
        return List.copyOf(copied);
    }

    /**
     * Builds row children and returns resolved container size.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {

        SimpleVec2i size;

        if (sizeBehaviour == FlexSizeBehaviour.MIN) {
            size = SimpleVec2i.zero();
            boolean first = true;

            for (UIComponent child : children) {
                SimpleVec2i childSize = UIBuilder.measure(layout.copy(), theme, child);
                if (!first) size.x += spacing;
                first = false;
                size.x += childSize.x;
                size.y = Math.max(size.y, childSize.y);
            }
        } else {
            size = new SimpleVec2i(layout.getWidth(), layout.getHeight());
        }

        Layout childrenLayout = new Layout(size.x, size.y, layout.getPosition(Axis.HORIZONTAL, size.x), layout.getPosition(Axis.VERTICAL, size.y));
        childrenLayout.pushLayoutSetting(Axis.VERTICAL, alignment);

        int pushedOffsets = 0;
        try {
            for (UIComponent child : children) {
                SimpleVec2i childSize = UIBuilder.build(childrenLayout, theme, child, context);
                childrenLayout.pushOffset(Axis.HORIZONTAL, childSize.x + spacing);
                pushedOffsets++;
            }
        } finally {
            for (int i = 0; i < pushedOffsets; i++) {
                childrenLayout.popOffset(Axis.HORIZONTAL);
            }
            childrenLayout.popLayoutSetting(Axis.VERTICAL);
        }

        return size;
    }


    public static final class Builder {
        private int spacing;
        private FlexSizeBehaviour sizeBehaviour;
        private LayoutSetting alignment;

        /**
         * Creates a new row builder.
         */
        public Builder() {
        }

        /**
         * Sets horizontal spacing between children.
         */
        public Builder withSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Sets size behavior used by the row container.
         */
        public Builder withSizeBehaviour(FlexSizeBehaviour horizontalSizeBehaviour) {
            this.sizeBehaviour = horizontalSizeBehaviour;
            return this;
        }

        /**
         * Sets vertical alignment used for children inside the row.
         */
        public Builder withAlignment(LayoutSetting alignment) {
            this.alignment = alignment;
            return this;
        }

        /**
         * Builds a row from vararg children.
         */
        public Row build(UIComponent... children) {
            return build(Arrays.asList(children));
        }

        /**
         * Builds a row from iterable children.
         */
        public Row build(Iterable<UIComponent> children) {
            return new Row(spacing,
                    sizeBehaviour == null
                            ? FlexSizeBehaviour.MIN
                            : sizeBehaviour,
                    children,
                    alignment == null ? LayoutSetting.CENTER : alignment);
        }
    }
}
