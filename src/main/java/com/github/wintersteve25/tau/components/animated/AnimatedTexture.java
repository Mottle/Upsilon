package com.github.wintersteve25.tau.components.animated;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.BuilderShell;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.github.wintersteve25.tau.utils.Variable;
import net.minecraft.resources.ResourceLocation;

/**
 * Texture component with mutable UV coordinates driven by {@link Variable} values.
 */
public class AnimatedTexture implements PrimitiveUIComponent {

    private final ResourceLocation textureLocation;
    private final SimpleVec2i textureSize;
    private final SimpleVec2i size;
    private final Variable<SimpleVec2i> uvSize;
    private final Variable<SimpleVec2i> uvPos;
    private final boolean stretchToSize;

    /**
     * Creates an animated texture component.
     */
    public AnimatedTexture(ResourceLocation textureLocation, SimpleVec2i textureSize, SimpleVec2i size, Variable<SimpleVec2i> uvSize, Variable<SimpleVec2i> uvPos, boolean stretchToSize) {
        this.textureLocation = textureLocation;
        this.textureSize = textureSize;
        this.size = size;
        this.uvSize = uvSize;
        this.uvPos = uvPos;
        this.stretchToSize = stretchToSize;
    }

    /**
     * Registers rendering that samples UVs from reactive variables each frame.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        SimpleVec2i position = layout.getPosition(size);

        context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> {
            int uvw = uvSize.getValue().x;
            int uvh = uvSize.getValue().y;
            if (uvw <= 0 || uvh <= 0) return;
            if (stretchToSize) {
                graphics.blit(textureLocation, position.x, position.y, size.x, size.y, uvPos.getValue().x, uvPos.getValue().y, uvw, uvh,
                        textureSize.x,
                        textureSize.y);
            } else {
                graphics.blit(textureLocation, position.x, position.y, uvw, uvh, uvPos.getValue().x, uvPos.getValue().y, uvw, uvh,
                        textureSize.x,
                        textureSize.y);
            }
        });

        return size;
    }

    public static final class Builder implements UIComponent, BuilderShell {

        private final ResourceLocation textureLocation;
        private final Variable<SimpleVec2i> uvSize;
        private final Variable<SimpleVec2i> uvPos;
        private final SimpleVec2i size;
        private SimpleVec2i textureSize;
        private boolean stretchToSize;

        /**
         * Creates a builder for an animated texture.
         */
        public Builder(ResourceLocation textureLocation, SimpleVec2i size, Variable<SimpleVec2i> uvSize, Variable<SimpleVec2i> uvPos) {
            this.textureLocation = textureLocation;
            this.uvSize = uvSize;
            this.uvPos = uvPos;
            this.size = size;
        }

        /**
         * Overrides full texture atlas size.
         */
        public Builder withTextureSize(SimpleVec2i textureSize) {
            this.textureSize = textureSize;
            return this;
        }

        /**
         * Sets whether the UV region should be stretched to component size.
         */
        public Builder shouldStretchToSize(boolean shouldStretchToSize) {
            this.stretchToSize = shouldStretchToSize;
            return this;
        }

        /**
         * Builds the animated texture component.
         */
        public UIComponent build() {
            textureSize = textureSize == null ? new SimpleVec2i(256, 256) : textureSize;
            return new AnimatedTexture(textureLocation, textureSize, size, uvSize, uvPos, stretchToSize);
        }

        /**
         * Returns a built animated texture for this builder state.
         */
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return build();
        }
    }
}
