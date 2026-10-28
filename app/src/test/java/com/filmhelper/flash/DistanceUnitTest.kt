package com.filmhelper.flash

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceUnitTest {

    @Test
    fun `metre steps grow with distance`() {
        assertEquals(1.6, DistanceUnit.Metres.step(1.5, 1), 1e-9)
        assertEquals(2.25, DistanceUnit.Metres.step(2.0, 1), 1e-9)
        assertEquals(1.9, DistanceUnit.Metres.step(2.0, -1), 1e-9)
        assertEquals(6.0, DistanceUnit.Metres.step(5.5, 1), 1e-9)
        assertEquals(12.0, DistanceUnit.Metres.step(11.0, 1), 1e-9)
    }

    @Test
    fun `typed values snap to the step grid`() {
        assertEquals(3.0, DistanceUnit.Metres.step(2.83, 1), 1e-9)
        assertEquals(2.5, DistanceUnit.Metres.step(2.83, -1), 1e-9)
    }

    @Test
    fun `never steps below the smallest step`() {
        assertEquals(0.1, DistanceUnit.Metres.step(0.1, -1), 1e-9)
        assertEquals(0.5, DistanceUnit.Feet.step(0.5, -1), 1e-9)
    }

    @Test
    fun `feet use whole and half feet`() {
        assertEquals(5.5, DistanceUnit.Feet.step(5.0, 1), 1e-9)
        assertEquals(11.0, DistanceUnit.Feet.step(10.0, 1), 1e-9)
    }
}
