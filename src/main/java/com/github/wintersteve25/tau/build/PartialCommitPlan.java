package com.github.wintersteve25.tau.build;

import java.util.List;

/**
 * Prepared partial-commit candidate replacing one mounted active subtree.
 */
public record PartialCommitPlan(
        ComponentMount activeTarget,
        BuildResult candidate,
        ComponentMount candidateRoot,
        List<ComponentMount> oldSubtree,
        ContextRanges oldRanges,
        int renderableDelta,
        int tooltipDelta,
        int dynamicDelta,
        int listenerDelta,
        int slotDelta
) {
}
