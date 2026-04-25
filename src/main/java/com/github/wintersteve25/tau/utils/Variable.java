package com.github.wintersteve25.tau.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Mutable observable value container for lightweight reactive UI state.
 *
 * @param <T> value type
 */
public class Variable<T> {
    
    private T value;
    private boolean changedSince;
    private final List<Consumer<T>> listeners;

    /**
     * Creates a variable initialized with {@code initial}.
     */
    public Variable(T initial) {
        this.value = initial;
        listeners = new ArrayList<>();
        changedSince = false;
    }

    /**
     * Returns current value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Sets a new value and notifies listeners if the value changed.
     */
    public void setValue(T value) {
        if (this.value == null && value == null) {
            return;
        }

        if (this.value != null && this.value.equals(value)) {
            return;
        }

        this.value = value;
        changedSince = true;

        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }

    /**
     * Registers a listener invoked after value changes.
     */
    public void addListener(Consumer<T> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     */
    public void removeListener(Consumer<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Returns whether this variable has changed since initialization.
     */
    public boolean hasChangedSinceLastGet() {
        return changedSince;
    }
}
