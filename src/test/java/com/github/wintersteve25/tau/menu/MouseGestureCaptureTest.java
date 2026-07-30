package com.github.wintersteve25.tau.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MouseGestureCaptureTest {

    @Test
    void uiConsumedPress_blocksVanillaForTheEntireGesture() {
        MouseGestureCapture capture = new MouseGestureCapture();

        assertTrue(capture.onPressed(0, true));
        assertTrue(capture.isCaptured(0));
        assertTrue(capture.onDragged(0, false), "a captured drag must not reach vanilla slots");
        assertTrue(capture.onReleased(0, false), "a captured release must not reach vanilla slots");
        assertFalse(capture.isCaptured(0));
        assertFalse(capture.onReleased(0, false), "later unrelated releases may use vanilla input");
    }

    @Test
    void unconsumedPress_remainsAvailableToVanillaInput() {
        MouseGestureCapture capture = new MouseGestureCapture();

        assertFalse(capture.onPressed(0, false));
        assertFalse(capture.onDragged(0, false));
        assertFalse(capture.onReleased(0, false));
    }

    @Test
    void capture_tracksMouseButtonsIndependently() {
        MouseGestureCapture capture = new MouseGestureCapture();

        capture.onPressed(0, true);
        capture.onPressed(1, true);

        assertTrue(capture.onReleased(0, false));
        assertTrue(capture.isCaptured(1));
        assertTrue(capture.onReleased(1, false));
        assertFalse(capture.isCaptured(1));
    }
}
