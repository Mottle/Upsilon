package com.github.wintersteve25.tau.menu;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks mouse buttons whose press was consumed by the Upsilon UI tree.
 * <p>
 * A container must route every event in one press/drag/release gesture to the
 * same owner. Otherwise a Upsilon control can consume a press while vanilla
 * inventory code receives the release and mutates a slot unexpectedly.
 */
final class MouseGestureCapture {
    private final Set<Integer> capturedButtons = new HashSet<>();

    /**
     * Records a press when the Upsilon tree consumed it.
     *
     * @return whether the event is consumed and must not reach vanilla input
     */
    boolean onPressed(int button, boolean uiConsumed) {
        if (uiConsumed) {
            capturedButtons.add(button);
        }
        return uiConsumed;
    }

    /**
     * Returns whether a drag belongs to a Upsilon-owned gesture.
     */
    boolean onDragged(int button, boolean uiConsumed) {
        return capturedButtons.contains(button) || uiConsumed;
    }

    /**
     * Releases a captured gesture and returns whether vanilla input is blocked.
     */
    boolean onReleased(int button, boolean uiConsumed) {
        return capturedButtons.remove(button) || uiConsumed;
    }

    /**
     * Clears all captured buttons when the screen closes.
     */
    void clear() {
        capturedButtons.clear();
    }

    boolean isCaptured(int button) {
        return capturedButtons.contains(button);
    }
}
