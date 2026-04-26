package com.github.wintersteve25.tau.renderer;

import com.github.wintersteve25.tau.build.*;
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
    private final Theme theme;
    private BuildContext mainContext;
    private BuildResult activeBuild;
    private List<DynamicUIComponent> dynamicUIComponents;

    private boolean built;
    private int screenWidth;
    private int screenHeight;

    /**
     * Creates a HUD renderer with an explicit theme.
     */
    public HudUIRenderer(UIComponent uiComponent, Theme theme) {
        this.uiComponent = uiComponent;
        this.theme = theme;
        this.mainContext = new BuildContext();
        this.activeBuild = null;
        this.dynamicUIComponents = new ArrayList<>();
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
        BuildResult result = UIBuilder.buildTree(layout, theme, uiComponent);
        commitFullBuild(result);

        built = true;
    }

    /**
     * Invokes destroy hooks for currently active dynamic components.
     */
    private void clearDynamicComponents() {
        for (DynamicUIComponent dynamicUIComponent : dynamicUIComponents) {
            dynamicUIComponent.destroy();
        }
        dynamicUIComponents.clear();
    }

    private void commitFullBuild(BuildResult result) {
        if (activeBuild != null) {
            UIBuilder.destroyOrphans(activeBuild.preorderMounts(), result.preorderMounts());
        }
        UIBuilder.promoteStagedStates(result);
        activeBuild = result;
        mainContext = result.context();
        dynamicUIComponents = new ArrayList<>(mainContext.dynamicUIComponents());
    }

    /**
     * Ticks dynamic components and rebuilds the HUD tree when required.
     */
    public void tick() {
        if (!built) return;
        if (activeBuild == null) {
            return;
        }

        List<ComponentMount> dirtyMounts = UIBuilder.filterTopLevelDirty(UIBuilder.collectDirtyDynamicMounts(activeBuild));
        if (dirtyMounts.isEmpty()) {
            return;
        }

        for (ComponentMount dirtyMount : dirtyMounts) {
            PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount);
            if (plan == null) {
                init();
                return;
            }
            activeBuild = UIBuilder.applyPartialCommit(activeBuild, plan, mainContext);
            dynamicUIComponents = new ArrayList<>(mainContext.dynamicUIComponents());
        }
    }

    /**
     * Renders HUD components and triggers rebuild when scaled window size changes.
     */
    public void render(Window mainWindow, GuiGraphics graphics, float pPartialTicks) {
        int width = mainWindow.getGuiScaledWidth();
        int height = mainWindow.getGuiScaledHeight();

        for (Renderable component : mainContext.renderables()) {
            component.render(graphics, 0, 0, pPartialTicks);
        }

        if (width != screenWidth || height != screenHeight) {
            screenWidth = width;
            screenHeight = height;
            init();
        }
    }
}
