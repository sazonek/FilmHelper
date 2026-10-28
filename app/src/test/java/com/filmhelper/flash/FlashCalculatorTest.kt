package com.filmhelper.flash

import com.filmhelper.core.Scales
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlashCalculatorTest {

    private val power = Scales.flashPower(minPowerStops = 7, thirds = true)

    private fun setup(
        gn: Double = 36.0,
        distance: Double = 3.0,
        iso: Double = 100.0,
        aperture: Double = 8.0,
        powerStops: Double = 0.0,
        loss: Double = 0.0,
    ) = FlashSetup(gn, distance, iso, aperture, powerStops, loss)

    @Test
    fun `distance is guide number over aperture at full power`() {
        val s = FlashCalculator.solve(setup(aperture = 4.0), FlashVariable.Distance, power)
        assertEquals(9.0, s.exact, 1e-9)
    }

    @Test
    fun `quarter power halves the guide number`() {
        val s = FlashCalculator.solve(setup(gn = 40.0, distance = 2.5, powerStops = 2.0), FlashVariable.Aperture, power)
        assertEquals(8.0, s.exact, 1e-9)
        assertEquals("f/8", s.nearest!!.label)
        assertEquals(0.0, s.errorStops, 0.01)
        assertNull(s.limit)
    }

    @Test
    fun `power for ISO 400 at f8 and 3 m`() {
        // GN 36 at ISO 400 is 72; 3 m at f/8 needs 24, a factor of 3 = 3.17 stops.
        val s = FlashCalculator.solve(setup(iso = 400.0), FlashVariable.Power, power)
        assertEquals(3.17, s.exact, 0.01)
        assertEquals("1/16 +0.7", s.nearest!!.label)
        assertEquals(0.0, s.errorStops, 1.0 / 6)
    }

    @Test
    fun `power solution matching full power`() {
        val s = FlashCalculator.solve(setup(distance = 4.5), FlashVariable.Power, power)
        assertEquals(0.0, s.exact, 1e-9)
        assertEquals("1/1", s.nearest!!.label)
    }

    @Test
    fun `iso solution`() {
        val s = FlashCalculator.solve(setup(gn = 20.0, distance = 5.0, aperture = 4.0), FlashVariable.Iso, power)
        assertEquals(100.0, s.exact, 1e-9)
        assertEquals("ISO 100", s.nearest!!.label)
    }

    @Test
    fun `diffuser loss counts like lower power`() {
        val withLoss = FlashCalculator.solve(setup(loss = 1.0), FlashVariable.Distance, power)
        val halfPower = FlashCalculator.solve(setup(powerStops = 1.0), FlashVariable.Distance, power)
        assertEquals(halfPower.exact, withLoss.exact, 1e-9)
    }

    @Test
    fun `beyond full power needs more light`() {
        val s = FlashCalculator.solve(setup(distance = 20.0, aperture = 16.0), FlashVariable.Power, power)
        assertEquals(FlashLimit.NeedsMoreLight, s.limit)
        assertEquals("1/1", s.nearest!!.label)
    }

    @Test
    fun `below minimum power is too much light`() {
        val s = FlashCalculator.solve(setup(iso = 3200.0, distance = 0.5, aperture = 16.0), FlashVariable.Power, power)
        assertEquals(FlashLimit.TooMuchLight, s.limit)
        assertEquals("1/128", s.nearest!!.label)
    }

    @Test
    fun `aperture smaller than f64 is too much light`() {
        val s = FlashCalculator.solve(setup(iso = 6400.0, distance = 0.3), FlashVariable.Aperture, power)
        assertEquals(FlashLimit.TooMuchLight, s.limit)
    }
}
