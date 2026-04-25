package com.github.wintersteve25.tau.components.inventory;

import com.github.wintersteve25.tau.build.BuildContext;
import com.github.wintersteve25.tau.components.base.PrimitiveUIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.menu.MenuSlot;
import com.github.wintersteve25.tau.menu.handlers.ItemSlotHandler;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Primitive component that renders a single item slot frame and emits a slot descriptor.
 */
public class ItemSlot implements PrimitiveUIComponent {

    // hard coded texture length for a slot in minecraft
    private static final SimpleVec2i SIZE = new SimpleVec2i(18, 18);
    private final ItemSlotHandler slotHandler;

    /**
     * Creates an item slot component using the provided slot handler.
     */
    public ItemSlot(ItemSlotHandler slotHandler) {
        this.slotHandler = slotHandler;
    }

    /**
     * Draws slot frame and registers slot binding metadata.
     */
    @Override
    public SimpleVec2i build(Layout layout, Theme theme, BuildContext context) {
        SimpleVec2i pos = layout.getPosition(SIZE);

        context.renderables().add((pGuiGraphics, pMouseX, pMouseY, pPartialTick) -> theme.drawSlot(pGuiGraphics, pos.x, pos.y));
        context.slots().add(new MenuSlot<>(pos, slotHandler));

        return SIZE;
    }
}
