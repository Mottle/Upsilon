package com.github.wintersteve25.tau.tests;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.components.interactable.ListView;
import com.github.wintersteve25.tau.components.layout.Center;
import com.github.wintersteve25.tau.components.layout.Column;
import com.github.wintersteve25.tau.components.utils.Sized;
import com.github.wintersteve25.tau.components.utils.Text;
import com.github.wintersteve25.tau.layout.Layout;
import com.github.wintersteve25.tau.theme.Theme;
import com.github.wintersteve25.tau.utils.Size;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests partial rebuild of a dynamic item inside a ListView.
 * The list contains several static items plus a counter item that
 * increments on click and marks itself dirty.
 */
public class TestPartialListView implements UIComponent {

    @Override
    public UIComponent build(Layout layout, Theme theme) {
        List<UIComponent> items = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            items.add(new Sized(
                    Size.staticSize(100, 20),
                    new Center(new Text.Builder("Item " + i))
            ));
        }
        items.add(new ClickableCounterItem());

        return new ListView.Builder()
                .withSpacing(2)
                .build(items);
    }

    static class ClickableCounterItem extends DynamicUIComponent implements GuiEventListener {
        final AtomicInteger count = new AtomicInteger(0);

        @Override
        public UIComponent build(Layout layout, Theme theme) {
            return new Sized(
                    Size.staticSize(100, 20),
                    new Center(new Text.Builder("Clicks: " + count.get()))
            );
        }

        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            count.incrementAndGet();
            rebuild();
            return true;
        }

        @Override public boolean isFocused() { return false; }
        @Override public void setFocused(boolean f) {}
    }
}
