package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.DynamicUIComponent;
import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UIBuilderPartialCommitTest {

    @Test
    void filterTopLevelDirty_keeps_only_topmost_ancestors() {
        ComponentMount grandparent = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount parent = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount child = new ComponentMount(mock(UIComponent.class), null);

        grandparent.addChild(parent);
        parent.addChild(child);

        List<ComponentMount> dirty = List.of(grandparent, parent, child);
        List<ComponentMount> topLevel = UIBuilder.filterTopLevelDirty(dirty);

        assertEquals(1, topLevel.size());
        assertSame(grandparent, topLevel.get(0));
    }

    @Test
    void filterTopLevelDirty_keeps_siblings_when_parent_not_dirty() {
        ComponentMount parent = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount a = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount b = new ComponentMount(mock(UIComponent.class), null);

        parent.addChild(a);
        parent.addChild(b);

        List<ComponentMount> dirty = List.of(a, b);
        List<ComponentMount> topLevel = UIBuilder.filterTopLevelDirty(dirty);

        assertEquals(2, topLevel.size());
        assertTrue(topLevel.contains(a));
        assertTrue(topLevel.contains(b));
    }

    @Test
    void collectOrphanDynamics_finds_only_removed() {
        DynamicUIComponent survivor = mock(DynamicUIComponent.class);
        DynamicUIComponent removed = mock(DynamicUIComponent.class);

        ComponentMount oldSurvivor = new ComponentMount(survivor, survivor);
        ComponentMount oldRemoved = new ComponentMount(removed, removed);

        ComponentMount newSurvivor = new ComponentMount(survivor, survivor);

        List<ComponentMount> oldSubtree = List.of(oldSurvivor, oldRemoved);
        List<ComponentMount> newSubtree = List.of(newSurvivor);

        List<DynamicUIComponent> orphans = UIBuilder.collectOrphanDynamics(oldSubtree, newSubtree);

        assertEquals(1, orphans.size());
        assertSame(removed, orphans.get(0));
    }

    @Test
    void destroyOrphans_calls_destroy_only_on_orphans() {
        DynamicUIComponent survivor = mock(DynamicUIComponent.class);
        DynamicUIComponent removed = mock(DynamicUIComponent.class);

        ComponentMount oldSurvivor = new ComponentMount(survivor, survivor);
        ComponentMount oldRemoved = new ComponentMount(removed, removed);
        ComponentMount newSurvivor = new ComponentMount(survivor, survivor);

        UIBuilder.destroyOrphans(List.of(oldSurvivor, oldRemoved), List.of(newSurvivor));

        verify(removed, times(1)).destroy();
        verify(survivor, never()).destroy();
    }

    @Test
    void shiftSubtreeRanges_shifts_all_mounts() {
        ComponentMount a = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount b = new ComponentMount(mock(UIComponent.class), null);

        a.setRanges(new ContextRanges(0, 1, 0, 1, 0, 1, 0, 1, 0, 1));
        b.setRanges(new ContextRanges(5, 6, 5, 6, 5, 6, 5, 6, 5, 6));

        UIBuilder.shiftSubtreeRanges(List.of(a, b), 10, 0, 0, 0, 0);

        assertEquals(10, a.getRanges().renderableStart());
        assertEquals(11, a.getRanges().renderableEnd());
        assertEquals(0, a.getRanges().tooltipStart());
        assertEquals(15, b.getRanges().renderableStart());
    }

    @Test
    void chooseCommitTarget_returns_first_non_unsafe_ancestor() {
        UIComponent owner = mock(UIComponent.class);
        ComponentMount mount = new ComponentMount(owner, null);

        ComponentMount result = UIBuilder.chooseCommitTarget(mount);
        assertSame(mount, result);
    }

    @Test
    void chooseCommitTarget_returns_null_when_all_unsafe() {
        UIComponent unsafeOwner = mock(UIComponent.class, withSettings().extraInterfaces(PartialCommitUnsafe.class));
        ComponentMount mount = new ComponentMount(unsafeOwner, null);

        ComponentMount result = UIBuilder.chooseCommitTarget(mount);
        assertNull(result);
    }

    @Test
    void containsPartialCommitUnsafe_detects_unsafe_in_subtree() {
        UIComponent unsafeOwner = mock(UIComponent.class, withSettings().extraInterfaces(PartialCommitUnsafe.class));
        UIComponent safeOwner = mock(UIComponent.class);

        ComponentMount root = new ComponentMount(safeOwner, null);
        ComponentMount child = new ComponentMount(unsafeOwner, null);
        root.addChild(child);

        assertTrue(UIBuilder.containsPartialCommitUnsafe(root));
    }

    @Test
    void findReusableMount_finds_matching_owner() {
        UIComponent target = mock(UIComponent.class);
        ComponentMount active = new ComponentMount(target, null);
        ComponentMount candidate = new ComponentMount(target, null);

        ComponentMount found = UIBuilder.findReusableMount(active, candidate);
        assertSame(active, found);
    }

    @Test
    void findReusableMount_walks_up_parents() {
        UIComponent target = mock(UIComponent.class);
        UIComponent other = mock(UIComponent.class);

        ComponentMount activeRoot = new ComponentMount(target, null);
        ComponentMount activeChild = new ComponentMount(other, null);
        activeRoot.addChild(activeChild);

        ComponentMount candidateRoot = new ComponentMount(target, null);

        ComponentMount found = UIBuilder.findReusableMount(activeChild, candidateRoot);
        assertSame(activeRoot, found);
    }

    @Test
    void findReusableMount_returns_null_when_no_match() {
        UIComponent a = mock(UIComponent.class);
        UIComponent b = mock(UIComponent.class);

        ComponentMount active = new ComponentMount(a, null);
        ComponentMount candidate = new ComponentMount(b, null);

        assertNull(UIBuilder.findReusableMount(active, candidate));
    }
}
