package com.filmhelper.exposure

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class ExposureTest {

    @Test
    fun `ten stop ND turns 1-8 s into 128 s`() {
        assertEquals(128.0, Exposure.withNd(1.0 / 8, 10.0), 1e-9)
    }

    @Test
    fun `exponent reciprocity leaves short times alone`() {
        val hp5 = Reciprocity.Exponent(1.31)
        assertEquals(0.5, hp5.correct(0.5), 1e-9)
        assertEquals(10.0.pow(1.31), hp5.correct(10.0), 1e-9)
    }

    @Test
    fun `acros ii adds half a stop past two minutes`() {
        assertEquals(120.0, Reciprocity.AcrosII.correct(120.0), 1e-9)
        assertEquals(0.5, Exposure.stopsBetween(240.0, Reciprocity.AcrosII.correct(240.0)), 1e-9)
    }

    @Test
    fun `sunny 16 is one over iso at f16`() {
        assertEquals(1.0 / 400, Exposure.sunny16Shutter(Sunny16Scene.Sunny, 400.0, 16.0), 1e-12)
        assertEquals(1.0 / 100, Exposure.sunny16Shutter(Sunny16Scene.Sunny, 400.0, 32.0), 1e-12)
        assertEquals(15.0, Exposure.ev100(16.0, 1.0 / 125, 125.0), 0.4)
    }
}
