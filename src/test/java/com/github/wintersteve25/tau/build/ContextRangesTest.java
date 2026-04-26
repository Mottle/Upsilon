package com.github.wintersteve25.tau.build;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextRangesTest {

    @Test
    void shiftedBy_increments_all_fields() {
        ContextRanges original = new ContextRanges(
                1, 5,  2, 6,  3, 7,  4, 8,  5, 9
        );
        ContextRanges shifted = original.shiftedBy(10, 20, 30, 40, 50);

        assertEquals(11, shifted.renderableStart());
        assertEquals(15, shifted.renderableEnd());
        assertEquals(22, shifted.tooltipStart());
        assertEquals(26, shifted.tooltipEnd());
        assertEquals(33, shifted.dynamicStart());
        assertEquals(37, shifted.dynamicEnd());
        assertEquals(44, shifted.listenerStart());
        assertEquals(48, shifted.listenerEnd());
        assertEquals(55, shifted.slotStart());
        assertEquals(59, shifted.slotEnd());
    }

    @Test
    void shiftedBy_zero_is_identity() {
        ContextRanges original = new ContextRanges(10, 20, 30, 40, 50, 60, 70, 80, 90, 100);
        assertEquals(original, original.shiftedBy(0, 0, 0, 0, 0));
    }
}
