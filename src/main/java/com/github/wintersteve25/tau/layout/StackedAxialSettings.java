package com.github.wintersteve25.tau.layout;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Axis-indexed stack storage used by {@link Layout} for push/pop based state.
 *
 * @param <T> stored setting type
 */
public class StackedAxialSettings<T> {
    private final Deque<T> horizontals;
    private final Deque<T> verticals;

    /**
     * Creates empty stacks for both axes.
     */
    public StackedAxialSettings() {
        horizontals = new ArrayDeque<>();
        verticals = new ArrayDeque<>();
    }

    private StackedAxialSettings(Deque<T> horizontals, Deque<T> verticals) {
        this.horizontals = horizontals;
        this.verticals = verticals;
    }

    /**
     * Returns the top value for the requested axis.
     *
     * @param axis target axis
     * @return most recently pushed value
     */
    public T getLast(Axis axis) {
        if (axis == Axis.VERTICAL) return verticals.peekLast();
        return horizontals.peekLast();
    }

    /**
     * Returns the mutable deque for the requested axis.
     *
     * @param axis target axis
     * @return underlying deque
     */
    public Deque<T> get(Axis axis) {
        return axis == Axis.VERTICAL ? verticals : horizontals;
    }

    /**
     * Pushes a value onto the requested axis stack.
     *
     * @param axis    target axis
     * @param setting value to push
     */
    public void push(Axis axis, T setting) {
        if (axis == Axis.VERTICAL) {
            verticals.addLast(setting);
            return;
        }

        horizontals.addLast(setting);
    }

    /**
     * Pops the top value from the requested axis stack.
     *
     * @param axis target axis
     */
    public void pop(Axis axis) {
        if (axis == Axis.VERTICAL) {
            verticals.removeLast();
            return;
        }

        horizontals.removeLast();
    }

    /**
     * Creates a shallow copy with cloned axis deques.
     *
     * @return copied stack container
     */
    public StackedAxialSettings<T> copy() {
        return new StackedAxialSettings<>(new ArrayDeque<>(horizontals), new ArrayDeque<>(verticals));
    }
}
