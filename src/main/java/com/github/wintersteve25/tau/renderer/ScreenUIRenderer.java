package com.github.wintersteve25.tau.renderer;

import com.github.wintersteve25.tau.build.*;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.MinecraftTheme;
import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapts a {@link UIComponent} tree into a Minecraft {@link Screen}.
 * <p>
 * The renderer rebuilds its full component tree when dynamic components request it.
 */
public class ScreenUIRenderer extends Screen {

    private final UIComponent uiComponent;
    private final boolean renderBackground;
    private final Theme theme;
    private final RootInputDispatcher dispatcher;
    private BuildContext mainContext;
    private BuildResult activeBuild;
    private List<DynamicUIComponent> dynamicUIComponents;
    private boolean built;

    /**
     * Creates a screen renderer with explicit background and theme options.
     */
    public ScreenUIRenderer(UIComponent uiComponent, boolean renderBackground, Theme theme) {
        super(Component.empty());
        this.uiComponent = uiComponent;
        this.renderBackground = renderBackground;
        this.theme = theme;
        this.mainContext = new BuildContext();
        this.activeBuild = null;
        this.dynamicUIComponents = new ArrayList<>();
        this.dispatcher = new RootInputDispatcher(() -> mainContext.eventListeners());
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
        BuildResult result = UIBuilder.buildTree(layout, theme, uiComponent);
        commitFullBuild(result);
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
        dynamicUIComponents = new ArrayList<>(result.context().dynamicUIComponents());
        dispatcher.clearFocusedIfMissing();
    }

    private boolean tryPartialCommit(ComponentMount dirtyMount) {
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, mainContext);
        if (plan == null) {
            return false;
        }
        activeBuild = UIBuilder.applyPartialCommit(activeBuild, plan, mainContext);
        dynamicUIComponents = new ArrayList<>(mainContext.dynamicUIComponents());
        dispatcher.clearFocusedIfMissing();
        return true;
    }

    /**
     * Ticks dynamic components and rebuilds tree when marked dirty.
     */
    @Override
    public void tick() {
        if (!built) return;
        List<ComponentMount> dirtyMounts = UIBuilder.filterTopLevelDirty(UIBuilder.collectDirtyDynamicMounts(activeBuild));
        if (dirtyMounts.isEmpty()) {
            return;
        }

        for (ComponentMount dirtyMount : dirtyMounts) {
            if (!tryPartialCommit(dirtyMount)) {
                rebuildUi();
                return;
            }
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

        for (Renderable component : mainContext.renderables()) {
            component.render(graphics, pMouseX, pMouseY, pPartialTicks);
        }

        for (Renderable tooltip : mainContext.tooltips()) {
            tooltip.render(graphics, pMouseX, pMouseY, pPartialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return dispatcher.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return dispatcher.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return dispatcher.mouseDragged(mouseX, mouseY, button, dragX, dragY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return dispatcher.mouseScrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return dispatcher.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return dispatcher.keyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return dispatcher.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return dispatcher.children();
    }

    @Override
    public GuiEventListener getFocused() {
        return dispatcher.getFocused();
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        dispatcher.setFocused(listener);
        super.setFocused(listener);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        dispatcher.children().forEach(listener -> listener.mouseMoved(mouseX, mouseY));
        super.mouseMoved(mouseX, mouseY);
    }
}
