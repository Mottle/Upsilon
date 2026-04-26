package com.github.wintersteve25.tau.menu;

import com.github.wintersteve25.tau.build.*;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.renderer.RootInputDispatcher;
import com.github.wintersteve25.tau.theme.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Container screen that renders a {@link UIMenu} through the component build pipeline.
 */
public class TauContainerScreen extends AbstractContainerScreen<TauContainerMenu> implements MenuAccess<TauContainerMenu> {

    private final UIMenu uiMenu;

    private final boolean renderBackground;
    private final Theme theme;
    private final RootInputDispatcher dispatcher;
    private BuildContext mainContext;
    private BuildResult activeBuild;
    private List<DynamicUIComponent> dynamicUIComponents;
    private List<Object> activeSlotStructureKeys;
    private int activeSlotStructureVersion;
    private boolean stale;

    private boolean built;

    /**
     * Creates a container screen wrapper for a {@link UIMenu}.
     */
    public TauContainerScreen(TauContainerMenu pMenu, Inventory pPlayerInventory, UIMenu uiMenu, boolean renderBackground, Theme theme, Component title) {
        super(pMenu, pPlayerInventory, title);
        this.uiMenu = uiMenu;
        this.renderBackground = renderBackground;
        this.theme = theme;
        this.mainContext = new BuildContext();
        this.activeBuild = null;
        this.dynamicUIComponents = new ArrayList<>();
        this.dispatcher = new RootInputDispatcher(() -> mainContext.eventListeners());
        this.activeSlotStructureKeys = List.of();
    }

    @Override
    protected void init() {
        rebuildUi();
        built = true;
    }

    /**
     * Rebuilds renderables/tooltips/dynamic component lists for the current menu state.
     */
    private void rebuildUi() {
        Layout layout = new Layout(uiMenu.getSize().x, uiMenu.getSize().y);
        leftPos = uiMenu.getLeftPos(layout, width, height);
        topPos = uiMenu.getTopPos(layout, width, height);

        layout.pushOffset(Axis.HORIZONTAL, leftPos);
        layout.pushOffset(Axis.VERTICAL, topPos);

        BuildResult result = UIBuilder.buildTree(layout, theme, uiMenu.build(layout, theme, getMenu()));
        commitFullBuild(result);

        layout.popOffset(Axis.HORIZONTAL);
        layout.popOffset(Axis.VERTICAL);
    }

    /**
     * Invokes destroy hooks on all currently tracked dynamic components.
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
        activeSlotStructureKeys = currentSlotStructureKeys(mainContext);
        activeSlotStructureVersion = menu.getSyncedSlotStructureVersion();
        applySlotVisualState(mainContext);
        stale = false;
    }

    private List<Object> currentSlotStructureKeys(BuildContext context) {
        return context.slots().stream().map(slot -> slot.handler().getStructureKey()).toList();
    }

    private boolean slotStructureChanged(BuildResult candidate) {
        return menu.getSyncedSlotStructureVersion() != activeSlotStructureVersion
                || !currentSlotStructureKeys(candidate.context()).equals(activeSlotStructureKeys);
    }

    private boolean tryPartialCommit(ComponentMount dirtyMount) {
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount);
        if (plan == null) {
            return false;
        }

        if (slotStructureChanged(plan.candidate())) {
            stale = true;
            return false;
        }

        activeBuild = UIBuilder.applyPartialCommit(activeBuild, plan, mainContext);
        dynamicUIComponents = new ArrayList<>(mainContext.dynamicUIComponents());
        applySlotVisualState(mainContext);
        activeSlotStructureKeys = currentSlotStructureKeys(mainContext);
        activeSlotStructureVersion = menu.getSyncedSlotStructureVersion();
        return true;
    }

    private void applySlotVisualState(BuildContext context) {
        if (context.slots().size() != menu.slots.size()) {
            stale = true;
            return;
        }

        for (int i = 0; i < context.slots().size(); i++) {
            MenuSlot<?> descriptor = context.slots().get(i);
            Slot slot = menu.slots.get(i);
            slot.x = descriptor.pos().x + 1;
            slot.y = descriptor.pos().y + 1;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float pPartialTick, int pMouseX, int pMouseY) {
        if (renderBackground) {
            renderTransparentBackground(graphics);
        }

        for (Renderable component : mainContext.renderables()) {
            component.render(graphics, pMouseX, pMouseY, pPartialTick);
        }
    }

    /**
     * Renders vanilla container layer then Upsilon tooltip renderables.
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);

        for (Renderable tooltip : mainContext.tooltips()) {
            tooltip.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    /**
     * Ticks dynamic UI state and rebuilds when requested.
     */
    @Override
    public void containerTick() {
        if (!built) return;
        if (stale) {
            rebuildUi();
            uiMenu.tick(menu);
            return;
        }

        List<ComponentMount> dirtyMounts = activeBuild == null ? List.of() : UIBuilder.filterTopLevelDirty(UIBuilder.collectDirtyDynamicMounts(activeBuild));
        if (!dirtyMounts.isEmpty()) {
            for (ComponentMount dirtyMount : dirtyMounts) {
                if (!tryPartialCommit(dirtyMount)) {
                    rebuildUi();
                    break;
                }
            }
        }
        uiMenu.tick(menu);
    }

    /**
     * Destroys tracked dynamic components before closing.
     */
    @Override
    public void onClose() {
        clearDynamicComponents();

        super.onClose();
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
