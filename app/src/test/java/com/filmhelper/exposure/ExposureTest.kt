package com.filmhelper.exposure

import org.junit.Assert.assertEquals
import org.junit.Test

class ExposureTest {

    @Test
    fun `ten stop ND turns 1-8 s into 128 s`() {
        assertEquals(128.0, Exposure.withNd(1.0 / 8, 10.0), 1e-9)
    }

    @Test
    fun `sunny 16 is one over iso at f16`() {
        assertEquals(1.0 / 400, Exposure.sunny16Shutter(Sunny16Scene.Sunny, 400.0, 16.0), 1e-12)
        assertEquals(1.0 / 100, Exposure.sunny16Shutter(Sunny16Scene.Sunny, 400.0, 32.0), 1e-12)
        assertEquals(15.0, Exposure.ev100(16.0, 1.0 / 125, 125.0), 0.4)
    }
}
