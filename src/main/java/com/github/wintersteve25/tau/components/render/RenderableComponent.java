package com.github.wintersteve25.tau.components.render;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.gui.components.Renderable;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Wraps a prebuilt Minecraft {@link Renderable} as a primitive component.
 */
public final class RenderableComponent implements PrimitiveUIComponent {

    private final Renderable renderable;

    /**
     * Creates a wrapper around an existing renderable.
     */
    public RenderableComponent(Renderable renderable) {
        this.renderable = renderable;
    }

    /**
     * Adds wrapped renderable to build context.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        context.renderables().add(renderable);
        return layout.getSize();
    }
}
