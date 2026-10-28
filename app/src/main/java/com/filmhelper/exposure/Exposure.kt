package com.filmhelper.exposure

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/** How a film's long exposures must be stretched once the metered time passes its limit. */
sealed interface Reciprocity {
    fun correct(meteredSeconds: Double): Double

    /** Ilford-style formula: corrected = metered^p, for metered times over one second. */
    data class Exponent(val p: Double) : Reciprocity {
        override fun correct(meteredSeconds: Double): Double =
            if (meteredSeconds <= 1.0) meteredSeconds else meteredSeconds.pow(p)
    }

    /** Fujifilm Acros II: no correction up to 120 s, then +1/2 stop. */
    data object AcrosII : Reciprocity {
        override fun correct(meteredSeconds: Double): Double =
            if (meteredSeconds <= 120.0) meteredSeconds else meteredSeconds * sqrt(2.0)
    }
}

data class FilmStock(val name: String, val reciprocity: Reciprocity)

/** Reciprocity data from the manufacturers' datasheets. */
object FilmStocks {
    val all = listOf(
        FilmStock("Ilford Pan F Plus 50", Reciprocity.Exponent(1.33)),
        FilmStock("Ilford FP4 Plus 125", Reciprocity.Exponent(1.26)),
        FilmStock("Ilford HP5 Plus 400", Reciprocity.Exponent(1.31)),
        FilmStock("Ilford Delta 100", Reciprocity.Exponent(1.26)),
        FilmStock("Ilford Delta 400", Reciprocity.Exponent(1.41)),
        FilmStock("Ilford Delta 3200", Reciprocity.Exponent(1.33)),
        FilmStock("Ilford XP2 Super 400", Reciprocity.Exponent(1.31)),
        FilmStock("Ilford SFX 200", Reciprocity.Exponent(1.43)),
        FilmStock("Kentmere Pan 100", Reciprocity.Exponent(1.26)),
        FilmStock("Kentmere Pan 400", Reciprocity.Exponent(1.30)),
        FilmStock("Fujifilm Acros II 100", Reciprocity.AcrosII),
    )
}

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

    /** Stops between two times (+ = [corrected] is longer). */
    fun stopsBetween(metered: Double, corrected: Double): Double = log2(corrected / metered)
}
