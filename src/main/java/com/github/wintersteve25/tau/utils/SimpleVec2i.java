package com.github.wintersteve25.tau.utils;

/**
 * Mutable integer 2D vector used across layout and input math.
 */
public class SimpleVec2i {
    public int x;
    public int y;

    /** Creates a vector from x/y coordinates. */
    public SimpleVec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /** Adds {@code other} into this vector in-place. */
    public void add(SimpleVec2i other) {
        this.x += other.x;
        this.y += other.y;
    }

    /** Returns a new vector equal to this + other. */
    public SimpleVec2i addNew(SimpleVec2i other) {
        return new SimpleVec2i(x + other.x, y + other.y);
    }

    /** Returns true when either coordinate exceeds {@code other}. */
    public boolean outside(SimpleVec2i other) {
        return x > other.x || y > other.y;
    }

    /** Returns a zero vector. */
    public static SimpleVec2i zero() {
        return new SimpleVec2i(0, 0);
    }

    /** Inclusive point-in-rect check using vector position/size. */
    public static boolean within(int mouseX, int mouseY, SimpleVec2i position, SimpleVec2i size) {
        return mouseX >= position.x && mouseX <= position.x + size.x && mouseY >= position.y && mouseY <= position.y + size.y;
    }

    /** Inclusive point-in-rect check using raw integer bounds. */
    public static boolean within(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
