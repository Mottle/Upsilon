package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.build.*;
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
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Scrollable vertical list container with clipping and input forwarding.
 * <p>
 * The component caches measured content height per viewport size and updates
 * scroll offset without requesting a full dynamic rebuild on each wheel event.
 */
public final class ListView extends DynamicUIComponent implements PrimitiveUIComponent, ContainerEventHandler, MountStateHost<ListView.ListViewMountState> {

    private static final int scrollSensitivity = 8;

    private final List<UIComponent> children;
    private final LayoutSetting childrenAlignment;
    private final int spacing;

    private int scrollOffset;
    private ListViewMountState activeMountState;

    /**
     * Creates a list view.
     *
     * @param children          list entries rendered top-to-bottom
     * @param childrenAlignment horizontal alignment for each child row
     * @param spacing           vertical spacing between entries
     */
    public ListView(List<UIComponent> children, LayoutSetting childrenAlignment, int spacing) {
        this.children = children;
        this.childrenAlignment = childrenAlignment;
        this.spacing = spacing;
        scrollOffset = 0;
    }

    /**
     * Builds the list view, measures content height, and wires clipped rendering.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        BuildSession session = UIBuilder.currentSession();
        ListViewMountState state = null;
        if (session != null && session.getMode() == BuildMode.MOUNTED_COMMITTABLE) {
            state = session.getOrCreateStagedState(this, ListViewMountState::new);
        }

        SimpleVec2i size = layout.getSize();
        SimpleVec2i position = layout.getPosition(size);

        int measuredContentHeight = state != null ? state.measuredContentHeight : -1;
        SimpleVec2i measuredViewport = state != null ? state.measuredViewport : SimpleVec2i.zero();

        if (measuredContentHeight == -1 || measuredViewport.x != size.x || measuredViewport.y != size.y) {
            Column.Builder measureColumn = new Column.Builder()
                    .withSpacing(spacing)
                    .withAlignment(childrenAlignment);
            SimpleVec2i childrenSize = UIBuilder.measure(layout.copy(), theme, measureColumn.build(children));
            measuredContentHeight = childrenSize.y;
            measuredViewport = new SimpleVec2i(size.x, size.y);
        }

        int maxScroll = Math.max(0, measuredContentHeight - size.y + 1); // 1 for padding
        scrollOffset = clamp(scrollOffset, -maxScroll, 0);

        List<Renderable> childRenderables = new ArrayList<>();
        List<GuiEventListener> childEventListeners = new ArrayList<>();
        if (state != null) {
            state.childEventListeners.clear();
            childEventListeners = state.childEventListeners;
        }

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
            if (session != null) {
                session.pushContext(innerContext);
            }
            for (UIComponent child : children) {
                SimpleVec2i childSize = UIBuilder.build(childLayout, theme, child, innerContext);
                childLayout.pushOffset(Axis.VERTICAL, childSize.y + spacing);
                pushedOffsets++;
            }
        } finally {
            if (session != null) {
                session.popContext();
            }
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

        if (session != null) {
            session.addRenderable((graphics, pMouseX, pMouseY, pPartialTicks) -> renderClipped(graphics, childRenderables, glX, glY, glWidth, glHeight, pMouseX, pMouseY, pPartialTicks));
        } else {
            context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> renderClipped(graphics, childRenderables, glX, glY, glWidth, glHeight, pMouseX, pMouseY, pPartialTicks));
        }

        if (state != null) {
            state.size = size;
            state.position = position;
            state.maxScroll = maxScroll;
            state.measuredContentHeight = measuredContentHeight;
            state.measuredViewport = measuredViewport;
            state.childRenderables = childRenderables;
        }

        return size;
    }

    /**
     * Applies wheel scrolling when cursor is inside the viewport.
     */
    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
        if (!isMouseOver(pMouseX, pMouseY)) {
            return false;
        }

        ListViewMountState state = activeMountState;
        if (state == null) {
            return false;
        }

        if (scrollOffset >= state.maxScroll && pScrollY < 0) {
            return false;
        }

        if (scrollOffset >= 0 && pScrollY > 0) {
            return false;
        }

        scrollOffset += pScrollY > 0 ? scrollSensitivity : -scrollSensitivity;
        scrollOffset = clamp(scrollOffset, -state.maxScroll, 0);

        return true;
    }

    /**
     * Returns whether a position lies inside the list viewport.
     */
    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        ListViewMountState state = activeMountState;
        return state != null && SimpleVec2i.within((int) pMouseX, (int) pMouseY, state.position, state.size);
    }

    /**
     * Returns false because focus is delegated to children.
     */
    @Override
    public boolean isFocused() {
        return false;
    }

    /**
     * Returns the event listeners built for the current child subtree.
     */
    @Override
    public List<? extends GuiEventListener> children() {
        return activeMountState == null ? List.of() : activeMountState.childEventListeners;
    }

    /**
     * List view does not keep a direct focused child reference.
     */
    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return null;
    }

    /**
     * List view does not track a focused state directly.
     */
    @Override
    public void setFocused(boolean pFocused) {
    }

    /**
     * List view does not keep its own focused child reference.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
    }

    /**
     * Resolves child under mouse after compensating for scroll offset.
     */
    @Override
    public Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
        return ContainerEventHandler.super.getChildAt(pMouseX, pMouseY - scrollOffset);
    }

    /**
     * Drag state is not tracked by this container.
     */
    @Override
    public boolean isDragging() {
        return false;
    }

    /**
     * Drag state setter is a no-op for this container.
     */
    @Override
    public void setDragging(boolean pIsDragging) {
    }

    /**
     * Forwards clicks to children after applying scroll offset.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        return ContainerEventHandler.super.mouseClicked(pMouseX, pMouseY - scrollOffset, pButton);
    }

    /**
     * Forwards releases to children after applying scroll offset.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return ContainerEventHandler.super.mouseReleased(pMouseX, pMouseY - scrollOffset, pButton);
    }

    /**
     * Forwards drags to children after applying scroll offset.
     */
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        return ContainerEventHandler.super.mouseDragged(pMouseX, pMouseY - scrollOffset, pButton, pDragX, pDragY);
    }

    /**
     * Renders child content inside a scissor region.
     */
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

    /**
     * Clamps an integer value to an inclusive range.
     */
    private int clamp(int x, int min, int max) {
        if (x < min) {
            return min;
        }

        return Math.min(x, max);
    }

    @Override
    public ListViewMountState getActiveMountState() {
        return activeMountState;
    }

    @Override
    public void setActiveMountState(ListViewMountState state) {
        this.activeMountState = state;
    }

    public static final class Builder {
        private int spacing;
        private LayoutSetting childrenAlignment;

        /**
         * Creates a new list view builder.
         */
        public Builder() {
        }

        /**
         * Sets vertical spacing between list children.
         */
        public Builder withSpacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Sets horizontal alignment applied to list children.
         */
        public Builder withAlignment(LayoutSetting alignment) {
            childrenAlignment = alignment;
            return this;
        }

        /**
         * Builds a list view from vararg children.
         */
        public ListView build(UIComponent... children) {
            return build(Arrays.asList(children));
        }

        /**
         * Builds a list view from iterable children.
         */
        public ListView build(List<UIComponent> children) {
            return new ListView(children, childrenAlignment == null ? LayoutSetting.CENTER : childrenAlignment, spacing);
        }
    }

    public static final class ListViewMountState implements MountState {
        SimpleVec2i size = SimpleVec2i.zero();
        SimpleVec2i position = SimpleVec2i.zero();
        int maxScroll;
        int measuredContentHeight = -1;
        SimpleVec2i measuredViewport = SimpleVec2i.zero();
        List<GuiEventListener> childEventListeners = new ArrayList<>();
        List<Renderable> childRenderables = new ArrayList<>();
    }
}
