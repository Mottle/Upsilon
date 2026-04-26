package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.UIComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class UIBuilderAncestorRangeTest {

    @Test
    void growAncestorRangeEnds_adjusts_parent_in_same_context() {
        BuildContext sharedCtx = new BuildContext();
        ComponentMount parent = new ComponentMount(mock(UIComponent.class), null);
        parent.setArtifactContext(sharedCtx);
        parent.setRanges(new ContextRanges(0, 10, 0, 20, 0, 30, 0, 40, 0, 50));

        ComponentMount child = new ComponentMount(mock(UIComponent.class), null);
        child.setArtifactContext(sharedCtx);
        parent.addChild(child);

        UIBuilder.growAncestorRangeEnds(child, sharedCtx, 2, 3, 4, 5, 6);

        ContextRanges updated = parent.getRanges();
        assertEquals(0, updated.renderableStart());
        assertEquals(12, updated.renderableEnd());
        assertEquals(23, updated.tooltipEnd());
        assertEquals(34, updated.dynamicEnd());
        assertEquals(45, updated.listenerEnd());
        assertEquals(56, updated.slotEnd());
    }

    @Test
    void growAncestorRangeEnds_skips_different_context() {
        BuildContext ctxA = new BuildContext();
        BuildContext ctxB = new BuildContext();

        ComponentMount parent = new ComponentMount(mock(UIComponent.class), null);
        parent.setArtifactContext(ctxA);
        parent.setRanges(new ContextRanges(0, 10, 0, 20, 0, 30, 0, 40, 0, 50));

        ComponentMount child = new ComponentMount(mock(UIComponent.class), null);
        child.setArtifactContext(ctxB);
        parent.addChild(child);

        UIBuilder.growAncestorRangeEnds(child, ctxB, 2, 3, 4, 5, 6);

        ContextRanges unchanged = parent.getRanges();
        assertEquals(10, unchanged.renderableEnd());
    }

    @Test
    void growAncestorRangeEnds_traverses_multiple_ancestors() {
        BuildContext sharedCtx = new BuildContext();
        ComponentMount root = new ComponentMount(mock(UIComponent.class), null);
        root.setArtifactContext(sharedCtx);
        root.setRanges(new ContextRanges(0, 100, 0, 0, 0, 0, 0, 0, 0, 0));

        ComponentMount mid = new ComponentMount(mock(UIComponent.class), null);
        mid.setArtifactContext(sharedCtx);
        mid.setRanges(new ContextRanges(10, 50, 0, 0, 0, 0, 0, 0, 0, 0));

        ComponentMount leaf = new ComponentMount(mock(UIComponent.class), null);
        leaf.setArtifactContext(sharedCtx);

        root.addChild(mid);
        mid.addChild(leaf);

        UIBuilder.growAncestorRangeEnds(leaf, sharedCtx, 5, 0, 0, 0, 0);

        assertEquals(55, mid.getRanges().renderableEnd());
        assertEquals(105, root.getRanges().renderableEnd());
    }
}
