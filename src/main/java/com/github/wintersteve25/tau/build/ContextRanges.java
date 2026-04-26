package com.github.wintersteve25.tau.build;

/**
 * Continuous artifact ranges occupied by a mounted subtree.
 */
public record ContextRanges(
        int renderableStart,
        int renderableEnd,
        int tooltipStart,
        int tooltipEnd,
        int dynamicStart,
        int dynamicEnd,
        int listenerStart,
        int listenerEnd,
        int slotStart,
        int slotEnd
) {
    public ContextRanges shiftedBy(int dr, int dt, int dd, int dl, int ds) {
        return new ContextRanges(
                renderableStart + dr, renderableEnd + dr,
                tooltipStart + dt, tooltipEnd + dt,
                dynamicStart + dd, dynamicEnd + dd,
                listenerStart + dl, listenerEnd + dl,
                slotStart + ds, slotEnd + ds
        );
    }
}
