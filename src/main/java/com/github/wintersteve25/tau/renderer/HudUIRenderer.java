package com.github.wintersteve25.tau.renderer;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.build.UIBuilder;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.MinecraftTheme;
import com.github.wintersteve25.tau.theme.Theme;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import java.util.ArrayList;
import java.util.List;

/**
 * HUD renderer for component trees rendered outside a {@link net.minecraft.client.gui.screens.Screen}.
 * <p>
 * Rebuilds when dynamic components request it or when HUD dimensions change.
 */
public class HudUIRenderer {
    private final UIComponent uiComponent;
    private final List<Renderable> components;
    private final List<DynamicUIComponent> dynamicUIComponents;
    private final Theme theme;

    private boolean built;
    private int screenWidth;
    private int screenHeight;

    /**
     * Creates a HUD renderer with an explicit theme.
     */
    public HudUIRenderer(UIComponent uiComponent, Theme theme) {
        this.uiComponent = uiComponent;
        this.components = new ArrayList<>();
        this.dynamicUIComponents = new ArrayList<>();
        this.theme = theme;
    }

    /**
     * Creates a HUD renderer using {@link MinecraftTheme#INSTANCE}.
     */
    public HudUIRenderer(UIComponent uiComponent) {
        this(uiComponent, MinecraftTheme.INSTANCE);
    }

    /**
     * Rebuilds the HUD component tree for current viewport dimensions.
     */
    private void init() {
        Layout layout = new Layout(screenWidth, screenHeight);

        clearDynamicComponents();
        components.clear();
        dynamicUIComponents.clear();
        UIBuilder.build(layout, theme, uiComponent, new BuildContext(components, new ArrayList<>(), dynamicUIComponents, new ArrayList<>(), new ArrayList<>()));

        built = true;
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
     * Ticks dynamic components and rebuilds the HUD tree when required.
     */
    public void tick() {
        if (!built) return;
        if (UIBuilder.tickDynamicUIComponents(dynamicUIComponents)) {
            init();
        }
    }

    /**
     * Renders HUD components and triggers rebuild when scaled window size changes.
     */
    public void render(Window mainWindow, GuiGraphics graphics, float pPartialTicks) {
        int width = mainWindow.getGuiScaledWidth();
        int height = mainWindow.getGuiScaledHeight();

        for (Renderable component : components) {
            component.render(graphics, 0, 0, pPartialTicks);
        }

        if (width != screenWidth || height != screenHeight) {
            screenWidth = width;
            screenHeight = height;
            init();
        }
    }
}
