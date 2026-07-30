package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.build.*;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.renderer.ScissorRenderer;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
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
 * The component caches measured content height per viewport size. A scroll
 * change requests a partial rebuild so child render positions stay synchronized
 * with hit testing.
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
        ListViewMountState previousState = activeMountState;
        GuiEventListener previousFocused = previousState == null ? null : previousState.focused;
        boolean previousDragging = previousState != null && previousState.dragging;
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

        if (session != null) {
            session.addRenderable((graphics, pMouseX, pMouseY, pPartialTicks) -> ScissorRenderer.render(graphics, position.x, position.y, size.x, size.y, childRenderables, pMouseX, pMouseY, pPartialTicks));
        } else {
            context.renderables().add((graphics, pMouseX, pMouseY, pPartialTicks) -> ScissorRenderer.render(graphics, position.x, position.y, size.x, size.y, childRenderables, pMouseX, pMouseY, pPartialTicks));
        }

        if (state != null) {
            state.size = size;
            state.position = position;
            state.maxScroll = maxScroll;
            state.measuredContentHeight = measuredContentHeight;
            state.measuredViewport = measuredViewport;
            state.childRenderables = childRenderables;
            state.focused = resolveFocusedChild(childEventListeners, previousFocused);
            state.dragging = state.focused != null && previousDragging;
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

        int nextScrollOffset = scrollBy(scrollOffset, state.maxScroll, pScrollY);
        if (nextScrollOffset == scrollOffset) {
            return false;
        }

        scrollOffset = nextScrollOffset;
        rebuild();

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
     * Returns whether a child inside this list currently owns focus.
     */
    @Override
    public boolean isFocused() {
        return getFocused() != null;
    }

    /**
     * Returns the event listeners built for the current child subtree.
     */
    @Override
    public List<? extends GuiEventListener> children() {
        clearFocusedIfMissing();
        return activeMountState == null ? List.of() : activeMountState.childEventListeners;
    }

    /**
     * Returns the focused child, if it remains in the active listener subtree.
     */
    @Nullable
    @Override
    public GuiEventListener getFocused() {
        clearFocusedIfMissing();
        return activeMountState == null ? null : activeMountState.focused;
    }

    /**
     * Clears child focus when the parent dispatcher loses focus.
     */
    @Override
    public void setFocused(boolean pFocused) {
        if (!pFocused && activeMountState != null) {
            activeMountState.focused = null;
            activeMountState.dragging = false;
        }
    }

    /**
     * Records the focused child selected by the container event handler.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
        if (activeMountState != null) {
            activeMountState.focused = pFocused;
        }
    }

    /**
     * Resolves child under mouse after compensating for scroll offset.
     */
    @Override
    public Optional<GuiEventListener> getChildAt(double pMouseX, double pMouseY) {
        clearFocusedIfMissing();
        if (!isMouseOver(pMouseX, pMouseY)) {
            return Optional.empty();
        }
        return ContainerEventHandler.super.getChildAt(pMouseX, pMouseY - scrollOffset);
    }

    /**
     * Returns whether the focused child is currently being dragged.
     */
    @Override
    public boolean isDragging() {
        return activeMountState != null && activeMountState.dragging;
    }

    /**
     * Updates drag state set by the container event handler.
     */
    @Override
    public void setDragging(boolean pIsDragging) {
        if (activeMountState != null) {
            activeMountState.dragging = pIsDragging;
        }
    }

    /**
     * Forwards clicks to children after applying scroll offset.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        clearFocusedIfMissing();
        if (!isMouseOver(pMouseX, pMouseY)) {
            return false;
        }
        return ContainerEventHandler.super.mouseClicked(pMouseX, pMouseY - scrollOffset, pButton);
    }

    /**
     * Forwards releases to children after applying scroll offset.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        clearFocusedIfMissing();
        return ContainerEventHandler.super.mouseReleased(pMouseX, pMouseY - scrollOffset, pButton);
    }

    /**
     * Forwards drags to children after applying scroll offset.
     */
    @Override
    public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
        clearFocusedIfMissing();
        return ContainerEventHandler.super.mouseDragged(pMouseX, pMouseY - scrollOffset, pButton, pDragX, pDragY);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        GuiEventListener focused = getFocused();
        return focused != null && focused.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean keyReleased(int pKeyCode, int pScanCode, int pModifiers) {
        GuiEventListener focused = getFocused();
        return focused != null && focused.keyReleased(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        GuiEventListener focused = getFocused();
        return focused != null && focused.charTyped(pCodePoint, pModifiers);
    }

    /**
     * Clamps an integer value to an inclusive range.
     */
    static int scrollBy(int currentScrollOffset, int maxScroll, double scrollAmount) {
        if (scrollAmount == 0) {
            return currentScrollOffset;
        }
        int delta = scrollAmount > 0 ? scrollSensitivity : -scrollSensitivity;
        return clamp(currentScrollOffset + delta, -maxScroll, 0);
    }

    private static int clamp(int x, int min, int max) {
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

    int getScrollOffset() {
        return scrollOffset;
    }

    private void clearFocusedIfMissing() {
        if (activeMountState != null
                && activeMountState.focused != null
                && !activeMountState.childEventListeners.contains(activeMountState.focused)) {
            activeMountState.focused = null;
            activeMountState.dragging = false;
        }
    }

    static GuiEventListener resolveFocusedChild(List<GuiEventListener> children, GuiEventListener previousFocused) {
        if (children.contains(previousFocused)) {
            return previousFocused;
        }
        return children.stream().filter(GuiEventListener::isFocused).findFirst().orElse(null);
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
        boolean dragging;
        GuiEventListener focused;
    }
}
