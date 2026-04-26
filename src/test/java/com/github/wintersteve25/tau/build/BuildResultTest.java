package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.components.base.UIComponent;
import com.github.wintersteve25.tau.utils.SimpleVec2i;
import org.junit.jupiter.api.Test;

import java.util.IdentityHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BuildResultTest {

    @Test
    void canPartialCommit_true_when_no_unsafe_components() {
        BuildResult result = new BuildResult(
                SimpleVec2i.zero(),
                new BuildContext(),
                List.of(),
                List.of(new ComponentMount(mock(UIComponent.class), null)),
                List.of(),
                new IdentityHashMap<>()
        );
        assertTrue(result.canPartialCommit());
    }

    @Test
    void canPartialCommit_false_when_unsafe_component_present() {
        UIComponent unsafe = mock(UIComponent.class, withSettings().extraInterfaces(PartialCommitUnsafe.class));
        BuildResult result = new BuildResult(
                SimpleVec2i.zero(),
                new BuildContext(),
                List.of(),
                List.of(new ComponentMount(unsafe, null)),
                List.of(),
                new IdentityHashMap<>()
        );
        assertFalse(result.canPartialCommit());
    }
}

class PartialCommitPlanTest {

    @Test
    void all_fields_preserved() {
        ComponentMount activeTarget = new ComponentMount(mock(UIComponent.class), null);
        BuildResult candidate = mock(BuildResult.class);
        ComponentMount candidateRoot = new ComponentMount(mock(UIComponent.class), null);
        ComponentMount candidateTarget = new ComponentMount(mock(UIComponent.class), null);
        List<ComponentMount> oldSubtree = List.of();
        List<ComponentMount> newSubtree = List.of();
        BuildContext targetContext = new BuildContext();
        BuildContext replacementContext = new BuildContext();
        ContextRanges oldRanges = new ContextRanges(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ContextRanges replacementRanges = new ContextRanges(11, 12, 13, 14, 15, 16, 17, 18, 19, 20);

        PartialCommitPlan plan = new PartialCommitPlan(
                activeTarget, candidate, candidateRoot, candidateTarget,
                oldSubtree, newSubtree, targetContext, replacementContext,
                oldRanges, replacementRanges, 1, 2, 3, 4, 5
        );

        assertSame(activeTarget, plan.activeTarget());
        assertSame(candidate, plan.candidate());
        assertSame(candidateRoot, plan.candidateRoot());
        assertSame(candidateTarget, plan.candidateTarget());
        assertSame(oldSubtree, plan.oldSubtree());
        assertSame(newSubtree, plan.newSubtree());
        assertSame(targetContext, plan.targetContext());
        assertSame(replacementContext, plan.replacementContext());
        assertSame(oldRanges, plan.oldRanges());
        assertSame(replacementRanges, plan.replacementRanges());
        assertEquals(1, plan.renderableDelta());
        assertEquals(2, plan.tooltipDelta());
        assertEquals(3, plan.dynamicDelta());
        assertEquals(4, plan.listenerDelta());
        assertEquals(5, plan.slotDelta());
    }
}
