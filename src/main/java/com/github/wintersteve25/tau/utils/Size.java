package com.github.wintersteve25.tau.utils;

import moe.liar.upsilon.Upsilon;

/**
 * Functional size resolver from max available size to concrete component size.
 */
@FunctionalInterface
public interface Size {
    /** Constant size strategy that always resolves to zero. */
    Size ZERO = (s) -> SimpleVec2i.zero();

    /**
     * Resolves component size from available max size.
     */
    SimpleVec2i get(SimpleVec2i maxSize);

    /**
     * Creates a proportional size strategy using one percentage for both axes.
     */
    static Size percentage(float percentage) {
        if (percentage < 0 || percentage > 1) {
            Upsilon.LOGGER.error("Size percentage can not be less than 0 or greater than 1");
            return ZERO;
        }

        return size -> new SimpleVec2i(Math.round(size.x * percentage), Math.round(size.y * percentage));
    }

    /**
     * Creates a proportional size strategy using separate axis percentages.
     */
    static Size percentage(float percentageX, float percentageY) {
        if (percentageX < 0 || percentageX > 1 || percentageY < 0 || percentageY > 1) {
            Upsilon.LOGGER.error("Size percentage can not be less than 0 or greater than 1");
            return ZERO;
        }

        return size -> new SimpleVec2i(Math.round(size.x * percentageX), Math.round(size.y * percentageY));
    }

    /**
     * Creates a fixed size strategy from a vector.
     */
    static Size staticSize(SimpleVec2i size) {
        return s -> new SimpleVec2i(size.x, size.y);
    }

    /**
     * Creates a fixed size strategy from width/height values.
     */
    static Size staticSize(int width, int height) {
        return s -> new SimpleVec2i(width, height);
    }
}
