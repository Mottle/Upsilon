package com.github.wintersteve25.tau.build;

import java.util.List;

/**
 * Prepared partial-commit candidate replacing one mounted active subtree.
 */
public record PartialCommitPlan(
        ComponentMount activeTarget,
        BuildResult candidate,
        ComponentMount candidateRoot,
        ComponentMount candidateTarget,
        List<ComponentMount> oldSubtree,
        List<ComponentMount> newSubtree,
        BuildContext targetContext,
        BuildContext replacementContext,
        ContextRanges oldRanges,
        ContextRanges replacementRanges,
        int renderableDelta,
        int tooltipDelta,
        int dynamicDelta,
        int listenerDelta,
        int slotDelta
) {
}
