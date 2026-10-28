package com.filmhelper.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ScalesTest {

    @Test
    fun `dials span the expected ranges`() {
        assertEquals("f/1.0", Scales.aperture.first.label)
        assertEquals(64.0, Scales.aperture.last.value, 1e-9)
        assertEquals("ISO 100", Scales.iso.nearest(isoEv(100.0)).label)
        assertEquals("1/8000", Scales.shutter.first.label)
        assertEquals("30\"", Scales.shutter.last.label)
        assertEquals("1/125", Scales.shutter.nearest(timeEv(1.0 / 125)).label)
    }

    @Test
    fun `flash power labels count thirds above the full stop`() {
        val labels = Scales.flashPower(minPowerStops = 2, thirds = true).stops.map { it.label }
        assertEquals(listOf("1/4", "1/4 +0.3", "1/4 +0.7", "1/2", "1/2 +0.3", "1/2 +0.7", "1/1"), labels)
    }

    @Test
    fun `stepping clamps at the ends of a dial`() {
        assertEquals("f/64", Scales.aperture.step(apertureEv(64.0), 1).label)
        assertEquals("f/5.6", Scales.aperture.step(apertureEv(4.0), 3).label)
    }

    @Test
    fun `durations read like shutter times`() {
        assertEquals("1/250", formatDuration(1.0 / 250))
        assertEquals("2.5 s", formatDuration(2.5))
        assertEquals("4 min 10 s", formatDuration(250.0))
        assertEquals("54 min", formatDuration(3254.0))
        assertEquals("1 h", formatDuration(3590.0))
        assertEquals("1 h 5 min", formatDuration(3900.0))
        assertEquals("1 2/3", formatThirds(5))
    }
}
