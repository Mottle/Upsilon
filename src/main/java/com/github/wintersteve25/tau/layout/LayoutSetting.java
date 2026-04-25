package com.github.wintersteve25.tau.layout;

/**
 * Strategy for placing a component of length {@code componentLength}
 * inside an available length {@code maxLength}.
 */
@FunctionalInterface
public interface LayoutSetting {
    /** Places component at the start edge. */
    LayoutSetting START = (maxLength, componentLength) -> 0;
    /** Places component centered in the available length. */
    LayoutSetting CENTER = (maxLength, componentLength) -> (maxLength - componentLength) / 2;
    /** Places component at the end edge. */
    LayoutSetting END = (maxLength, componentLength) -> maxLength - componentLength;

    /**
     * Creates a proportional placement between start and end.
     *
     * @param percent placement percentage in [0, 1] where 0=start and 1=end
     * @return layout setting that computes proportional placement
     */
    static LayoutSetting percentage(float percent) {
        return ((maxLength, componentLength) -> (int) ((maxLength - componentLength) * percent));
    }

    /**
     * Computes placement offset.
     *
     * @param maxLength available parent length
     * @param componentLength component length
     * @return placement offset from parent start edge
     */
    int place(int maxLength, int componentLength);
}
