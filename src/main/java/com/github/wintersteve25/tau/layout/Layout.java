package com.github.wintersteve25.tau.layout;

import com.github.wintersteve25.tau.utils.SimpleVec2i;

/**
 * Mutable layout state passed down the UI build tree.
 * <p>
 * Layout uses push/pop stacks for offsets, alignment settings, and size modifications,
 * allowing parent components to scope layout changes for their children.
 */
public class Layout {

    private final int width;
    private final int height;

    private final StackedAxialSettings<Integer> offsets;
    private final StackedAxialSettings<Integer> sizeModification;
    private final StackedAxialSettings<LayoutSetting> layoutSettings;

    /**
     * Creates a layout with the given available size and zero offsets.
     */
    public Layout(int width, int height) {
        this(width, height, 0, 0);
    }

    /**
     * Creates a layout with initial size and offsets.
     */
    public Layout(int width, int height, int xOffset, int yOffset) {
        this.width = width;
        this.height = height;

        this.offsets = new StackedAxialSettings<>();
        this.offsets.push(Axis.HORIZONTAL, xOffset);
        this.offsets.push(Axis.VERTICAL, yOffset);

        this.sizeModification = new StackedAxialSettings<>();
        this.sizeModification.push(Axis.HORIZONTAL, 0);
        this.sizeModification.push(Axis.VERTICAL, 0);

        this.layoutSettings = new StackedAxialSettings<>();
        this.layoutSettings.push(Axis.HORIZONTAL, LayoutSetting.START);
        this.layoutSettings.push(Axis.VERTICAL, LayoutSetting.START);
    }

    private Layout(int width, int height, StackedAxialSettings<Integer> offsets, StackedAxialSettings<Integer> sizeModification, StackedAxialSettings<LayoutSetting> layoutSettings) {
        this.width = width;
        this.height = height;
        this.offsets = offsets;
        this.sizeModification = sizeModification;
        this.layoutSettings = layoutSettings;
    }

    /**
     * Returns current width after size modifications.
     */
    public int getWidth() {
        return width + sizeModification.getLast(Axis.HORIZONTAL);
    }

    /**
     * Returns current height after size modifications.
     */
    public int getHeight() {
        return height + sizeModification.getLast(Axis.VERTICAL);
    }

    /**
     * Returns current available size.
     */
    public SimpleVec2i getSize() {
        return new SimpleVec2i(getWidth(), getHeight());
    }

    /**
     * Returns current alignment strategy for an axis.
     */
    public LayoutSetting getLayoutSetting(Axis axis) {
        return layoutSettings.getLast(axis);
    }

    /**
     * Pushes a new alignment strategy for the given axis.
     */
    public void pushLayoutSetting(Axis axis, LayoutSetting layoutSetting) {
        layoutSettings.push(axis, layoutSetting);
    }

    /**
     * Pops the latest alignment strategy for the given axis.
     */
    public void popLayoutSetting(Axis axis) {
        layoutSettings.pop(axis);
    }

    /**
     * Pushes an offset delta for the given axis.
     */
    public void pushOffset(Axis axis, int amount) {
        offsets.push(axis, amount);
    }

    /**
     * Pops the latest offset delta for the given axis.
     */
    public void popOffset(Axis axis) {
        offsets.pop(axis);
    }

    /**
     * Pushes a size modification delta for the given axis.
     */
    public void pushSizeMod(Axis axis, int amount) {
        sizeModification.push(axis, amount);
    }

    /**
     * Pops the latest size modification delta for the given axis.
     */
    public void popSizeMod(Axis axis) {
        sizeModification.pop(axis);
    }

    /**
     * Resolves final coordinate for a component length on an axis.
     */
    public int getPosition(Axis axis, int length) {
        return getOffset(axis) + getLayoutSetting(axis).place(getMaximumLength(axis), length);
    }

    /**
     * Resolves final position for a 2D size.
     */
    public SimpleVec2i getPosition(SimpleVec2i size) {
        return new SimpleVec2i(getPosition(Axis.HORIZONTAL, size.x), getPosition(Axis.VERTICAL, size.y));
    }

    /**
     * Returns accumulated offset for the given axis.
     */
    public int getOffset(Axis axis) {
        int result = 0;

        for (int offset : offsets.get(axis)) {
            result += offset;
        }

        return result;
    }

    /**
     * Returns maximum available length for the given axis.
     */
    public int getMaximumLength(Axis axis) {
        return axis == Axis.VERTICAL ? getHeight() : getWidth();
    }

    /**
     * Creates a copy preserving current stacked layout state.
     */
    public Layout copy() {
        return new Layout(width, height, this.offsets.copy(), this.sizeModification.copy(), this.layoutSettings.copy());
    }
}
