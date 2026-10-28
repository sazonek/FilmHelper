package com.filmhelper.exposure

import kotlin.math.log2
import kotlin.math.pow

/** Sunny 16 lighting conditions and the aperture that goes with a 1/ISO shutter speed. */
enum class Sunny16Scene(val label: String, val aperture: Double) {
    SnowSand("Snow or sand", 22.0),
    Sunny("Bright sun, hard shadows", 16.0),
    SlightOvercast("Slight overcast, soft shadows", 11.0),
    Overcast("Overcast, barely any shadows", 8.0),
    HeavyOvercast("Heavy overcast", 5.6),
    OpenShade("Open shade or sunset", 4.0),
}

object Exposure {

    /** Shutter time behind an ND filter of [ndStops]. */
    fun withNd(meteredSeconds: Double, ndStops: Double): Double = meteredSeconds * 2.0.pow(ndStops)

    /** Sunny 16 shutter time for [scene] at [iso] when shooting at [aperture]. */
    fun sunny16Shutter(scene: Sunny16Scene, iso: Double, aperture: Double): Double =
        (1 / iso) * (aperture / scene.aperture).pow(2)

    /** Exposure value at ISO 100 for an aperture and shutter time chosen at [iso]. */
    fun ev100(aperture: Double, seconds: Double, iso: Double): Double =
        log2(aperture * aperture / seconds) - log2(iso / 100)
}
