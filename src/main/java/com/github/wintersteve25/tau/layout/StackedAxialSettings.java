package com.github.wintersteve25.tau.layout;

import java.util.ArrayDeque;
import java.util.Deque;

public class StackedAxialSettings<T> {
    private final Deque<T> horizontals;
    private final Deque<T> verticals;

    public StackedAxialSettings() {
        horizontals = new ArrayDeque<>();
        verticals = new ArrayDeque<>();
    }

    private StackedAxialSettings(Deque<T> horizontals, Deque<T> verticals) {
        this.horizontals = horizontals;
        this.verticals = verticals;
    }

    public T getLast(Axis axis) {
        if (axis == Axis.VERTICAL) return verticals.peekLast();
        return horizontals.peekLast();
    }

    public Deque<T> get(Axis axis) {
        return axis == Axis.VERTICAL ? verticals : horizontals;
    }

    public void push(Axis axis, T setting) {
        if (axis == Axis.VERTICAL) {
            verticals.addLast(setting);
            return;
        }

        horizontals.addLast(setting);
    }

    public void pop(Axis axis) {
        if (axis == Axis.VERTICAL) {
            verticals.removeLast();
            return;
        }

        horizontals.removeLast();
    }

    public StackedAxialSettings<T> copy() {
        return new StackedAxialSettings<>(new ArrayDeque<>(horizontals), new ArrayDeque<>(verticals));
    }
}
