package com.github.wintersteve25.tau.menu;

import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class UIMenuTest {

    @Test
    void defaultQuickMoveStack_returnsEmptyStack() {
        UIMenu menu = new UIMenu() {
            @Override
            public UIComponent build(Layout layout, Theme theme, TauContainerMenu containerMenu) {
                return null;
            }

            @Override
            public SimpleVec2i getSize() {
                return SimpleVec2i.zero();
            }

            @Override
            public Component getTitle() {
                return Component.empty();
            }
        };

        assertSame(ItemStack.EMPTY, menu.quickMoveStack(null, null, 0));
    }
}
