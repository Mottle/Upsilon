package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.menu.MenuSlot;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class BuildContextTest {

    @Test
    void removeRange_clears_correct_elements() {
        List<Renderable> list = new ArrayList<>(List.of(mock(Renderable.class), mock(Renderable.class), mock(Renderable.class)));
        BuildContext.removeRange(list, 1, 2);
        assertEquals(2, list.size());
    }

    @Test
    void insertAll_adds_at_correct_index() {
        List<Renderable> list = new ArrayList<>(List.of(mock(Renderable.class)));
        BuildContext.insertAll(list, 1, List.of(mock(Renderable.class), mock(Renderable.class)));
        assertEquals(3, list.size());
    }

    @Test
    void rangeLength_is_end_minus_start() {
        assertEquals(4, BuildContext.rangeLength(1, 5));
        assertEquals(0, BuildContext.rangeLength(3, 3));
    }

    @Test
    void slice_returns_copy_of_range() {
        Renderable a = mock(Renderable.class);
        Renderable b = mock(Renderable.class);
        Renderable c = mock(Renderable.class);
        List<Renderable> list = new ArrayList<>(List.of(a, b, c));
        List<Renderable> sliced = BuildContext.slice(list, 1, 2);
        assertEquals(1, sliced.size());
        assertSame(b, sliced.get(0));
        assertEquals(3, list.size());
    }

    @Test
    void splice_with_non_empty_replacement() {
        BuildContext target = new BuildContext();
        Renderable old = mock(Renderable.class);
        target.renderables().add(old);

        BuildContext replacement = new BuildContext();
        Renderable newer = mock(Renderable.class);
        replacement.renderables().add(newer);

        ContextRanges oldRanges = new ContextRanges(0, 1, 0, 0, 0, 0, 0, 0, 0, 0);
        ContextRanges replacementRanges = new ContextRanges(0, 1, 0, 0, 0, 0, 0, 0, 0, 0);

        BuildContext.splice(target, oldRanges, replacement, replacementRanges);

        assertEquals(1, target.renderables().size());
        assertSame(newer, target.renderables().get(0));
    }

    @Test
    void addAll_merges_contexts() {
        BuildContext a = new BuildContext();
        Renderable ra = mock(Renderable.class);
        a.renderables().add(ra);

        BuildContext b = new BuildContext();
        Renderable rb = mock(Renderable.class);
        b.renderables().add(rb);

        a.addAll(b);
        assertEquals(2, a.renderables().size());
        assertSame(ra, a.renderables().get(0));
        assertSame(rb, a.renderables().get(1));
    }
}
