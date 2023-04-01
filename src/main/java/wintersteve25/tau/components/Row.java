package wintersteve25.tau.components;

import net.minecraft.client.gui.IRenderable;
import wintersteve25.tau.components.base.PrimitiveUIComponent;
import wintersteve25.tau.components.base.UIComponent;
import wintersteve25.tau.layout.Axis;
import wintersteve25.tau.layout.Layout;
import wintersteve25.tau.utils.FlexSizeBehaviour;
import wintersteve25.tau.utils.UIBuilder;
import wintersteve25.tau.utils.Vector2i;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Row implements PrimitiveUIComponent {
    
    private final List<UIComponent> children;
    private final int spacing;
    private final FlexSizeBehaviour sizeBehaviour;
    
    public Row(int spacing, FlexSizeBehaviour sizeBehaviour, List<UIComponent> children) {
        this.children = children;
        this.spacing = spacing;
        this.sizeBehaviour = sizeBehaviour;
    }
    
    @Override
    public Vector2i build(Layout layout, List<IRenderable> renderables) {
        
        Vector2i size;
        
        if (sizeBehaviour == FlexSizeBehaviour.MIN) {
            size = Vector2i.zero();

            for (UIComponent child : children) {
                Vector2i childSize = UIBuilder.build(new Layout(0, 0), child, new ArrayList<>());
                size.x += childSize.x + spacing;
                size.y = Math.max(size.y, childSize.y);
            }
        } else {
            size = new Vector2i(layout.getWidth(), layout.getHeight());
        }

        Layout childrenLayout = new Layout(size.x, size.y, layout.getPosition(Axis.HORIZONTAL, size.x), layout.getPosition(Axis.VERTICAL, size.y));

        for (UIComponent child : children) {
            Vector2i childSize = UIBuilder.build(childrenLayout, child, renderables);
            childrenLayout.pushOffset(Axis.HORIZONTAL, childSize.x + spacing);
        }

        return size;
    }


    public static final class Builder {
        private int spacing;
        private FlexSizeBehaviour horizontalSizeBehaviour;

        public Builder() {
        }
        
        public Builder withSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder withSizeBehaviour(FlexSizeBehaviour horizontalSizeBehaviour) {
            this.horizontalSizeBehaviour = horizontalSizeBehaviour;
            return this;
        }

        public Row build(UIComponent... children) {
            return new Row(spacing, 
                    horizontalSizeBehaviour == null 
                            ? FlexSizeBehaviour.MIN
                            : horizontalSizeBehaviour, 
                    Arrays.stream(children).collect(Collectors.toList())
            );
        }
    }
}
