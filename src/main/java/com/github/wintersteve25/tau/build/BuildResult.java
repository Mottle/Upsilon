package com.github.wintersteve25.tau.build;

import com.github.wintersteve25.tau.utils.SimpleVec2i;

import java.util.IdentityHashMap;
import java.util.List;

/**
 * Full build output including artifact context and mount tree caches.
 */
public record BuildResult(
        SimpleVec2i size,
        BuildContext context,
        List<ComponentMount> rootMounts,
        List<ComponentMount> preorderMounts,
        List<ComponentMount> dynamicMounts,
        IdentityHashMap<Object, MountState> stagedStates
) {
    public boolean canPartialCommit() {
        for (ComponentMount mount : preorderMounts) {
            if (mount.getOwner() instanceof PartialCommitUnsafe) {
                return false;
            }
        }
        return true;
    }
}
