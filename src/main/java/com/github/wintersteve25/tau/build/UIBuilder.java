package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds composed {@link UIComponent} trees into concrete render/runtime artifacts.
 */
public class UIBuilder {
    /**
     * Builds a UI component tree and collects renderables/listeners/slots into a context.
     *
     * @param layout layout constraints and positioning context for the root
     * @param theme active theme used by primitive components
     * @param uiComponent root component to build
     * @param context build artifact collector
     * @return accumulated size of the built branch
     */
    public static SimpleVec2i build(Layout layout, Theme theme, UIComponent uiComponent, BuildContext context) {
        return build(layout, theme, uiComponent, context, SimpleVec2i.zero());
    }

    // param size is the accumulated size of this component branch
    private static SimpleVec2i build(Layout layout, Theme theme, UIComponent uiComponent, BuildContext context, SimpleVec2i size) {
        if (uiComponent instanceof DynamicUIComponent dynamicUIComponent) {
            dynamicUIComponent.buildDynamic(context, layout.copy(), theme);
            context.dynamicUIComponents().add(dynamicUIComponent);
        }

        if (uiComponent instanceof GuiEventListener) {
            context.eventListeners().add((GuiEventListener) uiComponent);
        }

        if (uiComponent instanceof PrimitiveUIComponent primitiveUIComponent) {
            size.add(primitiveUIComponent.build(layout, theme, context));
        }

        UIComponent next = uiComponent.build(layout, theme);

        if (next == null) {
            finishDynamicUIComponent(uiComponent, context);
            return size;
        }

        SimpleVec2i resultSize = build(layout, theme, next, context, size);
        finishDynamicUIComponent(uiComponent, context);
        return resultSize;
    }

    private static void finishDynamicUIComponent(UIComponent uiComponent, BuildContext context) {
        if (uiComponent instanceof DynamicUIComponent dynamicUIComponent) {
            dynamicUIComponent.finalizeDynamic(context);
        }
    }

    /**
     * Ticks dynamic components and reports whether the owning renderer should rebuild the full UI tree.
     *
     * @param dynamicUIComponents dynamic components currently registered in the renderer
     * @return {@code true} if any component requested rebuild via {@code DynamicUIComponent#rebuild()}
     */
    public static boolean tickDynamicUIComponents(List<DynamicUIComponent> dynamicUIComponents) {
        boolean needsRebuild = false;

        for (DynamicUIComponent dynamicUIComponent : dynamicUIComponents) {
            dynamicUIComponent.tick();
            needsRebuild |= dynamicUIComponent.dirty;
        }

        if (needsRebuild) {
            for (DynamicUIComponent component : new ArrayList<>(dynamicUIComponents)) {
                component.dirty = false;
            }
        }

        return needsRebuild;
    }
}
