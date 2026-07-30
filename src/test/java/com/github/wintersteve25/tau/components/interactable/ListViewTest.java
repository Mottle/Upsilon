package com.github.wintersteve25.tau.components.interactable;

import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.layout.LayoutSetting;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ListViewTest {

    @Test
    void mouseScroll_changesOffsetAndRequestsRebuildOnlyWhenItMoves() {
        ListView listView = listViewWithViewport(10);

        assertTrue(listView.mouseScrolled(10, 10, 0, -1));
        assertEquals(-8, listView.getScrollOffset());
        assertTrue(listView.dirty);

        listView.dirty = false;
        assertTrue(listView.mouseScrolled(10, 10, 0, -1));
        assertEquals(-10, listView.getScrollOffset());
        assertTrue(listView.dirty);

        listView.dirty = false;
        assertFalse(listView.mouseScrolled(10, 10, 0, -1));
        assertEquals(-10, listView.getScrollOffset());
        assertFalse(listView.dirty);
    }

    @Test
    void scrollBy_clampsAndIgnoresZeroScroll() {
        assertEquals(0, ListView.scrollBy(0, 20, 0));
        assertEquals(-8, ListView.scrollBy(0, 20, -1));
        assertEquals(-20, ListView.scrollBy(-18, 20, -1));
        assertEquals(0, ListView.scrollBy(-4, 20, 1));
    }

    @Test
    void focusAndDragging_areForwardedToCurrentChild() {
        ListView listView = listViewWithViewport(10);
        TrackingListener listener = new TrackingListener();
        ListView.ListViewMountState state = listView.getActiveMountState();
        state.childEventListeners.add(listener);

        listView.setFocused(listener);
        listView.setDragging(true);

        assertSame(listener, listView.getFocused());
        assertTrue(listView.isFocused());
        assertTrue(listView.isDragging());
        assertTrue(listView.keyPressed(1, 2, 3));
        assertTrue(listView.keyReleased(1, 2, 3));
        assertTrue(listView.charTyped('x', 3));
        assertEquals(1, listener.keyPressedCalls);
        assertEquals(1, listener.keyReleasedCalls);
        assertEquals(1, listener.charTypedCalls);
    }

    @Test
    void removedFocusedChild_clearsFocusAndDragging() {
        ListView listView = listViewWithViewport(10);
        TrackingListener listener = new TrackingListener();
        ListView.ListViewMountState state = listView.getActiveMountState();
        state.childEventListeners.add(listener);
        listView.setFocused(listener);
        listView.setDragging(true);

        state.childEventListeners.clear();

        assertNull(listView.getFocused());
        assertFalse(listView.isDragging());
    }

    @Test
    void focusedChild_isNotTransferredToAnUnrelatedReplacement() {
        TrackingListener previous = new TrackingListener();
        TrackingListener replacement = new TrackingListener();

        assertNull(ListView.resolveFocusedChild(List.of(replacement), previous));
        assertSame(previous, ListView.resolveFocusedChild(List.of(previous), previous));
    }

    private static ListView listViewWithViewport(int maxScroll) {
        ListView listView = new ListView(List.<UIComponent>of(), LayoutSetting.CENTER, 0);
        ListView.ListViewMountState state = new ListView.ListViewMountState();
        state.position = SimpleVec2i.zero();
        state.size = new SimpleVec2i(100, 100);
        state.maxScroll = maxScroll;
        listView.setActiveMountState(state);
        return listView;
    }

    private static final class TrackingListener implements GuiEventListener {
        private int keyPressedCalls;
        private int keyReleasedCalls;
        private int charTypedCalls;

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            keyPressedCalls++;
            return true;
        }

        @Override
        public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
            keyReleasedCalls++;
            return true;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            charTypedCalls++;
            return true;
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
        }
    }
}
