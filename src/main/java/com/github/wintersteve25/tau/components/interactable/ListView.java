package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ListView extends DynamicUIComponent implements PrimitiveUIComponent, ContainerEventHandler {

    private static final int scrollSensitivity = 8;

    private final List<UIComponent> children;
    private final LayoutSetting childrenAlignment;
    private final int spacing;

    private int scrollOffset;
    private int maxScroll;

    private SimpleVec2i size;
    private SimpleVec2i position;
    private final List<GuiEventListener> childEventListeners = new ArrayList<>();
    private int measuredContentHeight = -1;
    private SimpleVec2i measuredViewport = SimpleVec2i.zero();

    public ListView(List<UIComponent> children, LayoutSetting childrenAlignment, int spacing) {
        this.children = children;
        this.childrenAlignment = childrenAlignment;
        this.spacing = spacing;
        scrollOffset = 0;
    }

    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        size = layout.getSize();
        position = layout.getPosition(size);

        if (measuredContentHeight == -1 || measuredViewport.x != size.x || measuredViewport.y != size.y) {
            Column.Builder measureColumn = new Column.Builder()
                    .withSpacing(spacing)
                    .withAlignment(childrenAlignment);
            SimpleVec2i childrenSize = UIBuilder.build(layout.copy(), theme, measureColumn.build(children), new BuildContext());
            measuredContentHeight = childrenSize.y;
            measuredViewport = new SimpleVec2i(size.x, size.y);
        }

        maxScroll = Math.max(0, measuredContentHeight - size.y + 1); // 1 for padding
        scrollOffset = clamp(scrollOffset, -maxScroll, 0);

        List<Renderable> childRenderables = new ArrayList<>();
        childEventListeners.clear();

        BuildContext innerContext = new BuildContext(
                childRenderables,
                context.tooltips(),
                context.dynamicUIComponents(),
                childEventListeners,
                context.slots()
        );

        Layout childLayout = layout.copy();
        childLayout.pushLayoutSetting(Axis.HORIZONTAL, childrenAlignment);
        childLayout.pushOffset(Axis.VERTICAL, scrollOffset);

        int pushedOffsets = 0;
        try {
            for (UIComponent child : children) {
                SimpleVec2i childSize = UIBuilder.build(childLayout, theme, child, innerContext);
                childLayout.pushOffset(Axis.VERTICAL, childSize.y + spacing);
                pushedOffsets++;
            }
        } finally {
            for (int i = 0; i < pushedOffsets; i++) {
                childLayout.popOffset(Axis.VERTICAL);
            }
            childLayout.popOffset(Axis.VERTICAL);
            childLayout.popLayoutSetting(Axis.HORIZONTAL);
        }

        Window window = Minecraft.getInstance().getWindow();
        double guiScale = window.getGuiScale();
        int glX = (int) (position.x * guiScale);
        int glY = (int) ((window.getGuiScaledHeight() - (position.y + size.y)) * guiScale);
        int glWidth = (int) (size.x * guiScale);
        int glHeight = (int) (size.y * guiScale);

        context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> renderClipped(graphics, childRenderables, glX, glY, glWidth, glHeight, pMouseX, pMouseY, pPartialTicks));

        return size;
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (!isMouseOver(pMouseX, pMouseY)) {
            return false;
        }

        if (scrollOffset >= maxScroll && pScrollY < 0) {
            return false;
        }

        if (scrollOffset >= 0 && pScrollY > 0) {
            return false;
        }

        scrollOffset += pScrollY > 0 ? scrollSensitivity : -scrollSensitivity;
        scrollOffset = clamp(scrollOffset, -maxScroll, 0);

        return true;
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return SimpleVec2i.within((int) pMouseX, (int) pMouseY, position, size);
    }

    @Override
    public void setFocused(boolean pFocused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return childEventListeners;
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return null;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
        return ContainerEventHandler.super.getChildAt(pMouseX, pMouseY - scrollOffset);
    }

    @Override
    public boolean isDragging() {
        return false;
    }

    @Override
    public void setDragging(boolean pIsDragging) {
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return ContainerEventHandler.super.mouseClicked(pMouseX, pMouseY - scrollOffset, pButton);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return ContainerEventHandler.super.mouseReleased(pMouseX, pMouseY - scrollOffset, pButton);
    }

    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return ContainerEventHandler.super.mouseDragged(pMouseX, pMouseY - scrollOffset, pButton, pDragX, pDragY);
    }

    private void renderClipped(GuiGraphics graphics, List<Renderable> childRenderables, int glX, int glY, int glWidth, int glHeight, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.enableScissor(glX, glY, glWidth, glHeight);

        try {
            for (Renderable renderable : childRenderables) {
                renderable.render(graphics, mouseX, mouseY, partialTicks);
            }
        } finally {
            RenderSystem.disableScissor();
        }
    }

    private int clamp(int x, int min, int max) {
        if (x < min) {
            return min;
        }

        return Math.min(x, max);
    }

    public static final class Builder {
        private int spacing;
        private LayoutSetting childrenAlignment;

        public Builder() {
        }

        public Builder withSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder withAlignment(LayoutSetting alignment) {
            childrenAlignment = alignment;
            return this;
        }

        public ListView build(UIComponent... children) {
            return build(Arrays.asList(children));
        }

        public ListView build(List<UIComponent> children) {
            return new ListView(children, childrenAlignment == null ? LayoutSetting.CENTER : childrenAlignment, spacing);
        }
    }
}
