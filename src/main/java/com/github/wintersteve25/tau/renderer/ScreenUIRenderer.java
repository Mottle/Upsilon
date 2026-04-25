package com.github.wintersteve25.tau.renderer;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.theme.MinecraftTheme;
import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts a {@link UIComponent} tree into a Minecraft {@link Screen}.
 * <p>
 * The renderer rebuilds its full component tree when dynamic components request it.
 */
public class ScreenUIRenderer extends Screen {

    private final UIComponent uiComponent;
    private final List<Renderable> components;
    private final List<Renderable> tooltips;
    private final List<DynamicUIComponent> dynamicUIComponents;
    private final boolean renderBackground;
    private final Theme theme;
    private boolean built;

    /**
     * Creates a screen renderer with explicit background and theme options.
     */
    public ScreenUIRenderer(UIComponent uiComponent, boolean renderBackground, Theme theme) {
        super(Component.empty());
        this.uiComponent = uiComponent;
        this.renderBackground = renderBackground;
        this.theme = theme;
        this.components = new ArrayList<>();
        this.tooltips = new ArrayList<>();
        this.dynamicUIComponents = new ArrayList<>();
    }

    /**
     * Creates a screen renderer using {@link MinecraftTheme#INSTANCE}.
     */
    public ScreenUIRenderer(UIComponent uiComponent, boolean renderBackground) {
        this(uiComponent, renderBackground, MinecraftTheme.INSTANCE);
    }

    /**
     * Creates a screen renderer that also renders the vanilla background.
     */
    public ScreenUIRenderer(UIComponent uiComponent) {
        this(uiComponent, true);
    }

    /**
     * Builds initial UI tree for current screen size.
     */
    @Override
    protected void init() {
        rebuildUi();
        built = true;
    }

    /**
     * Rebuilds renderables/tooltips/event listeners for current screen dimensions.
     */
    private void rebuildUi() {
        Layout layout = new Layout(width, height);

        clearDynamicComponents();
        components.clear();
        tooltips.clear();
        dynamicUIComponents.clear();
        List<GuiEventListener> listeners = new ArrayList<>(children());
        UIBuilder.build(layout, theme, uiComponent, new BuildContext(components, tooltips, dynamicUIComponents, listeners, new ArrayList<>()));
    }

    /**
     * Invokes destroy hooks for currently active dynamic components.
     */
    private void clearDynamicComponents() {
        for (DynamicUIComponent dynamicUIComponent : dynamicUIComponents) {
            dynamicUIComponent.destroy();
        }
    }

    /**
     * Ticks dynamic components and rebuilds tree when marked dirty.
     */
    @Override
    public void tick() {
        if (!built) return;
        if (UIBuilder.tickDynamicUIComponents(dynamicUIComponents)) {
            rebuildUi();
        }
    }

    /**
     * Cleans up dynamic components before closing this screen.
     */
    @Override
    public void onClose() {
        clearDynamicComponents();

        super.onClose();
    }

    /**
     * Renders screen background, component tree, and deferred tooltips.
     */
    @Override
    public void render(GuiGraphics graphics, int pMouseX, int pMouseY, float pPartialTicks) {
        if (renderBackground) {
            this.renderBackground(graphics, pMouseX, pMouseY, pPartialTicks);
        }

        for (Renderable component : components) {
            component.render(graphics, pMouseX, pMouseY, pPartialTicks);
        }

        for (Renderable tooltip : tooltips) {
            tooltip.render(graphics, pMouseX, pMouseY, pPartialTicks);
        }
    }
}
