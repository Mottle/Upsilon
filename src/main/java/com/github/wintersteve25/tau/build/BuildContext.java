package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.menu.MenuSlot;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates all artifacts produced while building a UI tree.
 *
 * @param renderables visual elements rendered during the main pass
 * @param tooltips tooltip renderables rendered after main content
 * @param dynamicUIComponents stateful components that receive tick/destroy callbacks
 * @param eventListeners GUI event listeners participating in input dispatch
 * @param slots menu slot descriptors used by container UIs
 */
public record BuildContext(
        List<Renderable> renderables,
        List<Renderable> tooltips,
        List<DynamicUIComponent> dynamicUIComponents,
        List<GuiEventListener> eventListeners,
        List<MenuSlot<?>> slots
) {
    /**
     * Creates an empty context backed by mutable lists.
     */
    public BuildContext() {
        this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Appends all artifacts from {@code context} into this context.
     *
     * @param context source context to merge
     */
    public void addAll(BuildContext context) {
        this.renderables.addAll(context.renderables);
        this.tooltips.addAll(context.tooltips);
        this.dynamicUIComponents.addAll(context.dynamicUIComponents);
        this.eventListeners.addAll(context.eventListeners);
        this.slots.addAll(context.slots);
    }
}
