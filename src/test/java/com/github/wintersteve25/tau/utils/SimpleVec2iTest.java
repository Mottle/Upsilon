package com.github.wintersteve25.tau.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleVec2iTest {

    @Test
    void zero_returns_0_0() {
        SimpleVec2i v = SimpleVec2i.zero();
        assertEquals(0, v.x);
        assertEquals(0, v.y);
    }

    @Test
    void add_modifies_in_place() {
        SimpleVec2i a = new SimpleVec2i(3, 5);
        a.add(new SimpleVec2i(2, 4));
        assertEquals(5, a.x);
        assertEquals(9, a.y);
    }

    @Test
    void addNew_does_not_mutate_original() {
        SimpleVec2i a = new SimpleVec2i(3, 5);
        SimpleVec2i b = a.addNew(new SimpleVec2i(2, 4));
        assertEquals(3, a.x);
        assertEquals(5, a.y);
        assertEquals(5, b.x);
        assertEquals(9, b.y);
    }

    @Test
    void within_vector_should_contain_point() {
        assertTrue(SimpleVec2i.within(5, 5, new SimpleVec2i(2, 2), new SimpleVec2i(10, 10)));
        assertFalse(SimpleVec2i.within(1, 5, new SimpleVec2i(2, 2), new SimpleVec2i(10, 10)));
    }

    @Test
    void within_raw_should_contain_point() {
        assertTrue(SimpleVec2i.within(5, 5, 2, 2, 10, 10));
        assertFalse(SimpleVec2i.within(1, 1, 2, 2, 10, 10));
    }

    @Test
    void outside_when_coordinate_exceeds() {
        assertTrue(new SimpleVec2i(10, 5).outside(new SimpleVec2i(5, 5)));
        assertFalse(new SimpleVec2i(3, 3).outside(new SimpleVec2i(5, 5)));
    }
}
