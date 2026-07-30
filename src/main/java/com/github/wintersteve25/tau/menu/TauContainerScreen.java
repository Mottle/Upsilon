package com.github.wintersteve25.tau.menu;

import com.github.wintersteve25.tau.build.*;
import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.layout.Axis;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.renderer.RootInputDispatcher;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import moe.liar.upsilon.Upsilon;
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
    private final MouseGestureCapture mouseGestureCapture;
    private BuildContext mainContext;
    private BuildResult activeBuild;
    private List<DynamicUIComponent> dynamicUIComponents;
    private int activeSlotStructureVersion;
    private boolean stale;
    private boolean slotMappingInvalid;

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
        this.mouseGestureCapture = new MouseGestureCapture();
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
        activeSlotStructureVersion = menu.getSyncedSlotStructureVersion();
        stale = false;
        applySlotVisualState(mainContext);
        dispatcher.clearFocusedIfMissing();
    }

    private boolean slotStructureChanged() {
        return slotStructureChanged(menu.getSyncedSlotStructureVersion(), activeSlotStructureVersion);
    }

    static boolean slotStructureChanged(int syncedVersion, int activeVersion) {
        return syncedVersion != activeVersion;
    }

    private boolean slotSubtreeChanged(PartialCommitPlan plan) {
        if (menu.getSyncedSlotStructureVersion() != activeSlotStructureVersion) {
            return true;
        }
        ContextRanges oldRanges = plan.oldRanges();
        ContextRanges replacementRanges = plan.replacementRanges();
        List<Object> oldKeys = BuildContext.slice(plan.targetContext().slots(), oldRanges.slotStart(), oldRanges.slotEnd()).stream().map(slot -> slot.handler().getStructureKey()).toList();
        List<Object> newKeys = BuildContext.slice(plan.replacementContext().slots(), replacementRanges.slotStart(), replacementRanges.slotEnd()).stream().map(slot -> slot.handler().getStructureKey()).toList();
        return !newKeys.equals(oldKeys);
    }

    private boolean tryPartialCommit(ComponentMount dirtyMount) {
        PartialCommitPlan plan = UIBuilder.planPartialCommit(dirtyMount, mainContext);
        if (plan == null) {
            return false;
        }

        if (slotSubtreeChanged(plan)) {
            stale = true;
            return false;
        }

        activeBuild = UIBuilder.applyPartialCommit(activeBuild, plan, mainContext);
        dynamicUIComponents = new ArrayList<>(mainContext.dynamicUIComponents());
        if (!applySlotVisualState(mainContext)) {
            stale = true;
            return false;
        }
        activeSlotStructureVersion = menu.getSyncedSlotStructureVersion();
        dispatcher.clearFocusedIfMissing();
        return true;
    }

    private boolean applySlotVisualState(BuildContext context) {
        boolean applied = applySlotVisualState(context.slots(), menu.slots);
        if (applied) {
            slotMappingInvalid = false;
            return true;
        }

        if (!slotMappingInvalid) {
            Upsilon.LOGGER.error(
                    "Unable to align menu slots: UI descriptors materialize {} slots but the menu contains {} slots",
                    materializedSlotCount(context.slots()),
                    menu.slots.size()
            );
        }
        slotMappingInvalid = true;
        return false;
    }

    static int materializedSlotCount(List<? extends MenuSlot<?>> descriptors) {
        int count = 0;
        for (MenuSlot<?> descriptor : descriptors) {
            int handlerSlotCount = descriptor.handler().getSlotCount();
            if (handlerSlotCount < 0) {
                throw new IllegalArgumentException("Slot handler returned a negative slot count: " + descriptor.handler().getClass().getName());
            }
            count += handlerSlotCount;
        }
        return count;
    }

    static boolean applySlotVisualState(List<? extends MenuSlot<?>> descriptors, List<Slot> slots) {
        if (materializedSlotCount(descriptors) != slots.size()) {
            return false;
        }

        int materializedIndex = 0;
        for (MenuSlot<?> descriptor : descriptors) {
            for (int slotIndex = 0; slotIndex < descriptor.handler().getSlotCount(); slotIndex++) {
                SimpleVec2i offset = descriptor.handler().getSlotOffset(slotIndex);
                Slot slot = slots.get(materializedIndex++);
                slot.x = descriptor.pos().x + offset.x + 1;
                slot.y = descriptor.pos().y + offset.y + 1;
            }
        }
        return true;
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
        if (stale || slotStructureChanged()) {
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
        mouseGestureCapture.clear();

        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseGestureCapture.onPressed(button, dispatcher.mouseClicked(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (mouseGestureCapture.onReleased(button, dispatcher.mouseReleased(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (mouseGestureCapture.onDragged(button, dispatcher.mouseDragged(mouseX, mouseY, button, dragX, dragY))) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        dispatcher.children().forEach(listener -> listener.mouseMoved(mouseX, mouseY));
    }
}
