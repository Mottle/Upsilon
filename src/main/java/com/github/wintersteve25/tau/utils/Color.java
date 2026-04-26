package com.github.wintersteve25.tau.utils;

/**
 * Immutable ARGB color value helper.
 */
public final class Color {
    /**
     * Opaque white.
     */
    public static final Color WHITE = new Color(0xFFFFFFFF);
    /**
     * Opaque gray.
     */
    public static final Color GRAY = new Color(0xFF787878);
    /**
     * Opaque black.
     */
    public static final Color BLACK = new Color(0xFF000000);
    /**
     * Opaque red.
     */
    public static final Color RED = new Color(0xFFFF0000);
    /**
     * Opaque green.
     */
    public static final Color GREEN = new Color(0xFF00FF00);
    /**
     * Opaque blue.
     */
    public static final Color BLUE = new Color(0xFF0000FF);

    private final int hex;
    private final boolean hasTransparency;

    /**
     * Creates a color from an {@code 0xAARRGGBB} integer.
     */
    public Color(int hex) {
        this.hex = hex;
        this.hasTransparency = ((hex >> 24) & 0xFF) < 255;
    }

    private Color(int hex, boolean hasTransparency) {
        this.hex = hex;
        this.hasTransparency = hasTransparency;
    }

    /**
     * Creates a color from RGBA channels.
     */
    public static Color fromRGBA(int r, int g, int b, int a) {
        return new Color((a << 24) + (r << 16) + (g << 8) + b, a < 255);
    }

    /**
     * Returns whether alpha channel is not fully opaque.
     */
    public boolean hasTransparency() {
        return hasTransparency;
    }

    /**
     * Returns packed {@code 0xAARRGGBB} color value.
     */
    public int getAARRGGBB() {
        return hex;
    }
}
