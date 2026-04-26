package com.github.wintersteve25.tau.components.utils;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.BuilderShell;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.resources.ResourceLocation;

/**
 * Primitive textured quad component.
 */
public final class Texture implements PrimitiveUIComponent {

    private final ResourceLocation textureLocation;
    private final SimpleVec2i textureSize;
    private final SimpleVec2i uv;
    private final SimpleVec2i uvSize;
    private final SimpleVec2i size;

    /**
     * Creates a texture component.
     *
     * @param textureLocation texture atlas/resource location
     * @param textureSize     full texture dimensions in pixels
     * @param uv              top-left UV origin in texture pixels
     * @param uvSize          sampled UV size in texture pixels
     * @param size            rendered quad size in screen pixels
     */
    public Texture(ResourceLocation textureLocation, SimpleVec2i textureSize, SimpleVec2i uv, SimpleVec2i uvSize, SimpleVec2i size) {
        this.textureLocation = textureLocation;
        this.textureSize = textureSize;
        this.uv = uv;
        this.uvSize = uvSize;
        this.size = size;
    }

    /**
     * Registers texture blit rendering at resolved layout position.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {

        SimpleVec2i position = layout.getPosition(size);

        context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> {
            graphics.blit(textureLocation, position.x, position.y, size.x, size.y, uv.x, uv.y, uvSize.x, uvSize.y,
                    textureSize.x,
                    textureSize.y);
        });

        return size;
    }

    public static final class Builder implements UIComponent, BuilderShell {

        private final ResourceLocation textureLocation;
        private SimpleVec2i textureSize;
        private SimpleVec2i uv;
        private SimpleVec2i uvSize;
        private SimpleVec2i size;

        /**
         * Creates a new builder for the given texture resource.
         */
        public Builder(ResourceLocation textureLocation) {
            this.textureLocation = textureLocation;
        }

        /**
         * Sets full texture dimensions in pixels.
         */
        public Builder withTextureSize(SimpleVec2i textureSize) {
            this.textureSize = textureSize;
            return this;
        }

        /**
         * Sets top-left UV origin in texture pixels.
         */
        public Builder withUv(SimpleVec2i uv) {
            this.uv = uv;
            return this;
        }

        /**
         * Sets sampled UV size in texture pixels.
         */
        public Builder withUvSize(SimpleVec2i uvSize) {
            this.uvSize = uvSize;
            return this;
        }

        /**
         * Sets rendered size in screen pixels.
         */
        public Builder withSize(SimpleVec2i size) {
            this.size = size;
            return this;
        }

        /**
         * Builds texture component with defaults for unset values.
         */
        public Texture build() {
            textureSize = textureSize == null ? new SimpleVec2i(256, 256) : textureSize;
            uvSize = uvSize == null ? textureSize : uvSize;
            return new Texture(textureLocation, textureSize, uv == null ? SimpleVec2i.zero() : uv, uvSize, size == null ? uvSize : size);
        }

        /**
         * Returns a built texture component for this builder configuration.
         */
        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return build();
        }
    }
}
