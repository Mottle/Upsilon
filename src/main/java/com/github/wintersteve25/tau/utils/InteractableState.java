package com.github.wintersteve25.tau.utils;

/**
 * Visual/input state for interactable components.
 */
public enum InteractableState {
    /**
     * Default idle state.
     */
    IDLE(1),
    /**
     * Mouse is hovering over the component.
     */
    HOVERED(2),
    /**
     * Component interaction is disabled.
     */
    DISABLED(0);

    private int number;

    private InteractableState(int number) {
        this.number = number;
    }

    /**
     * Returns numeric theme variant index.
     */
    public int getNumber() {
        return number;
    }
}
