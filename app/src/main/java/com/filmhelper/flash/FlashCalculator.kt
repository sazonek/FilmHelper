package com.filmhelper.flash

import com.filmhelper.core.Stop
import com.filmhelper.core.StopScale
import com.filmhelper.core.Scales
import com.filmhelper.core.apertureEv
import com.filmhelper.core.isoEv
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

enum class FlashVariable(val label: String) {
    Distance("Distance"),
    Aperture("Aperture"),
    Power("Power"),
    Iso("ISO"),
}

/**
 * Everything that sets a manual flash exposure.
 *
 * [guideNumber] is rated at ISO 100 and full power, in the same unit as [distance].
 * [powerStops] counts stops below full power (0 = 1/1, 3 = 1/8).
 * [lossStops] is light lost to a diffuser, bounce or gel.
 */
data class FlashSetup(
    val guideNumber: Double,
    val distance: Double,
    val iso: Double,
    val aperture: Double,
    val powerStops: Double,
    val lossStops: Double = 0.0,
)

enum class FlashLimit { NeedsMoreLight, TooMuchLight }

/**
 * The solved value of [variable]: [exact] is the ideal number, [nearest] the closest marked
 * setting (null for distance), [errorStops] the exposure you get with [nearest] (+ = over),
 * and [limit] is set when the ideal value lies beyond the end of the dial.
 */
data class FlashSolution(
    val variable: FlashVariable,
    val exact: Double,
    val nearest: Stop?,
    val errorStops: Double,
    val limit: FlashLimit?,
)

object FlashCalculator {

    /** Guide number after ISO, power and light loss: GN × √(ISO/100) × 2^(−(power+loss)/2). */
    fun effectiveGuideNumber(s: FlashSetup): Double =
        s.guideNumber * sqrt(s.iso / 100) * 2.0.pow(-(s.powerStops + s.lossStops) / 2)

    /** How far [s] is from a correct exposure, in stops (+ = overexposed). */
    fun exposureError(s: FlashSetup): Double =
        2 * log2(effectiveGuideNumber(s) / (s.distance * s.aperture))

    /**
     * Solves [setup] for [solveFor]; the value of that variable in [setup] is ignored.
     * Power is matched against [powerScale], aperture and ISO against the standard third-stop dials.
     */
    fun solve(setup: FlashSetup, solveFor: FlashVariable, powerScale: StopScale): FlashSolution =
        when (solveFor) {
            FlashVariable.Distance -> {
                val distance = effectiveGuideNumber(setup) / setup.aperture
                FlashSolution(solveFor, distance, nearest = null, errorStops = 0.0, limit = null)
            }

            FlashVariable.Aperture -> {
                val exact = effectiveGuideNumber(setup) / setup.distance
                snap(solveFor, exact, apertureEv(exact), Scales.aperture, evAddsLight = false) {
                    setup.copy(aperture = it.value)
                }
            }

            FlashVariable.Power -> {
                val full = effectiveGuideNumber(setup.copy(powerStops = 0.0))
                val exact = 2 * log2(full / (setup.distance * setup.aperture))
                snap(solveFor, exact, -exact, powerScale, evAddsLight = true) {
                    setup.copy(powerStops = it.value)
                }
            }

            FlashVariable.Iso -> {
                val atIso100 = effectiveGuideNumber(setup.copy(iso = 100.0))
                val exact = 100 * (setup.distance * setup.aperture / atIso100).pow(2)
                snap(solveFor, exact, isoEv(exact), Scales.iso, evAddsLight = true) {
                    setup.copy(iso = it.value)
                }
            }
        }

    private fun snap(
        variable: FlashVariable,
        exact: Double,
        exactEv: Double,
        scale: StopScale,
        evAddsLight: Boolean,
        withSetting: (Stop) -> FlashSetup,
    ): FlashSolution {
        val nearest = scale.nearest(exactEv)
        val beyond = when {
            exactEv > scale.last.ev + EPSILON -> if (evAddsLight) FlashLimit.NeedsMoreLight else FlashLimit.TooMuchLight
            exactEv < scale.first.ev - EPSILON -> if (evAddsLight) FlashLimit.TooMuchLight else FlashLimit.NeedsMoreLight
            else -> null
        }
        return FlashSolution(variable, exact, nearest, exposureError(withSetting(nearest)), beyond)
    }

    private const val EPSILON = 0.05
}
