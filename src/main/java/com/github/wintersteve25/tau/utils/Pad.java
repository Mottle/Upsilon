package com.github.wintersteve25.tau.utils;

/**
 * Immutable padding values for top/bottom/left/right edges.
 */
public class Pad {
    public final int top;
    public final int bottom;
    public final int left;
    public final int right;

    /** Creates a padding instance with explicit edge sizes. */
    public Pad(int top, int bottom, int left, int right) {
        this.top = top;
        this.bottom = bottom;
        this.left = left;
        this.right = right;
    }

    /** Returns total horizontal/vertical padding size as a vector. */
    public SimpleVec2i getSize() {
        return new SimpleVec2i(left + right, top + bottom);
    }

    /** Fluent builder for {@link Pad}. */
    public static final class Builder {
        private int top;
        private int bottom;
        private int left;
        private int right;

        public Builder() {
        }

        /** Sets top padding. */
        public Builder top(int top) {
            this.top = top;
            return this;
        }

        /** Sets bottom padding. */
        public Builder bottom(int bottom) {
            this.bottom = bottom;
            return this;
        }

        /** Sets left padding. */
        public Builder left(int left) {
            this.left = left;
            return this;
        }

        /** Sets right padding. */
        public Builder right(int right) {
            this.right = right;
            return this;
        }

        /** Sets all edges to the same padding amount. */
        public Builder all(int amount) {
            this.top = amount;
            this.bottom = amount;
            this.left = amount;
            this.right = amount;
            return this;
        }

        /** Builds an immutable {@link Pad}. */
        public Pad build() {
            return new Pad(top, bottom, left, right);
        }
    }
}
