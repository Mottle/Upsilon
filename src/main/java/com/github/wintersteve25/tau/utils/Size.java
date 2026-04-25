package com.github.wintersteve25.tau.utils;

import moe.liar.upsilon.Upsilon;

@FunctionalInterface
public interface Size {
    Size ZERO = (s) -> SimpleVec2i.zero();

    SimpleVec2i get(SimpleVec2i maxSize);

    static Size percentage(float percentage) {
        if (percentage < 0 || percentage > 1) {
            Upsilon.LOGGER.error("Size percentage can not be less than 0 or greater than 1");
            return ZERO;
        }

        return size -> new SimpleVec2i(Math.round(size.x * percentage), Math.round(size.y * percentage));
    }

    static Size percentage(float percentageX, float percentageY) {
        if (percentageX < 0 || percentageX > 1 || percentageY < 0 || percentageY > 1) {
            Upsilon.LOGGER.error("Size percentage can not be less than 0 or greater than 1");
            return ZERO;
        }

        return size -> new SimpleVec2i(Math.round(size.x * percentageX), Math.round(size.y * percentageY));
    }

    static Size staticSize(SimpleVec2i size) {
        return s -> new SimpleVec2i(size.x, size.y);
    }

    static Size staticSize(int width, int height) {
        return s -> new SimpleVec2i(width, height);
    }
}
