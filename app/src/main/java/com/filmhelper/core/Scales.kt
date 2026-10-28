package com.filmhelper.core

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow

/**
 * One marked setting on a camera or flash dial.
 *
 * [value] is the real-world number (f-number, ISO, seconds, stops below full power) and
 * [ev] is its position in stops, so settings of any kind can be compared and stepped.
 */
data class Stop(val label: String, val value: Double, val ev: Double)

/** An ordered dial of marked settings, from lowest [Stop.ev] to highest. */
class StopScale(val stops: List<Stop>) {
    val lastIndex: Int get() = stops.lastIndex
    val first: Stop get() = stops.first()
    val last: Stop get() = stops.last()

    operator fun get(index: Int): Stop = stops[index.coerceIn(0, lastIndex)]

    fun nearestIndex(ev: Double): Int = stops.indices.minBy { abs(stops[it].ev - ev) }

    fun nearest(ev: Double): Stop = stops[nearestIndex(ev)]

    /** The setting [steps] notches away from the one nearest to [ev], clamped to the dial. */
    fun step(ev: Double, steps: Int): Stop = get(nearestIndex(ev) + steps)
}

fun apertureEv(fNumber: Double): Double = 2 * log2(fNumber)
fun isoEv(iso: Double): Double = log2(iso / 100)
fun timeEv(seconds: Double): Double = log2(seconds)

object Scales {

    /** f/1 to f/64 in third stops. */
    val aperture = StopScale(
        listOf(
            "1.0", "1.1", "1.2", "1.4", "1.6", "1.8", "2", "2.2", "2.5", "2.8", "3.2", "3.5",
            "4", "4.5", "5", "5.6", "6.3", "7.1", "8", "9", "10", "11", "13", "14",
            "16", "18", "20", "22", "25", "29", "32", "36", "40", "45", "51", "57", "64",
        ).mapIndexed { i, label -> Stop("f/$label", 2.0.pow(i / 6.0), i / 3.0) }
    )

    /** ISO 6 to 6400 in third stops. */
    val iso = StopScale(
        listOf(
            6, 8, 10, 12, 16, 20, 25, 32, 40, 50, 64, 80, 100, 125, 160, 200, 250, 320,
            400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400,
        ).mapIndexed { i, label ->
            val ev = (i - 12) / 3.0
            Stop("ISO $label", 100 * 2.0.pow(ev), ev)
        }
    )

    private val shutterLabels = listOf(
        "1/8000", "1/6400", "1/5000", "1/4000", "1/3200", "1/2500", "1/2000", "1/1600",
        "1/1250", "1/1000", "1/800", "1/640", "1/500", "1/400", "1/320", "1/250", "1/200",
        "1/160", "1/125", "1/100", "1/80", "1/60", "1/50", "1/40", "1/30", "1/25", "1/20",
        "1/15", "1/13", "1/10", "1/8", "1/6", "1/5", "1/4", "1/3", "0.4\"", "1/2", "0.6\"",
        "0.8\"", "1\"", "1.3\"", "1.6\"", "2\"", "2.5\"", "3\"", "4\"", "5\"", "6\"", "8\"",
        "10\"", "13\"", "15\"", "20\"", "25\"", "30\"",
    )

    /** 1/8000 s to 30 s in third stops, as found on shutter dials. */
    val shutter = StopScale(
        shutterLabels.mapIndexed { i, label ->
            val ev = (i - 39) / 3.0
            Stop(label, 2.0.pow(ev), ev)
        }
    )

    /**
     * Manual flash power from [minPowerStops] below full (7 = 1/128) up to 1/1.
     * [Stop.value] is the number of stops below full power.
     */
    fun flashPower(minPowerStops: Int, thirds: Boolean): StopScale {
        val perStop = if (thirds) 3 else 1
        val stops = (minPowerStops * perStop downTo 0).map { step ->
            val stopsBelowFull = step.toDouble() / perStop
            Stop(flashPowerLabel(step, perStop), stopsBelowFull, -stopsBelowFull)
        }
        return StopScale(stops)
    }

    /** Labels in the "1/8 +0.7" style used by most manual flashes. */
    private fun flashPowerLabel(step: Int, perStop: Int): String {
        val fullStop = (step + perStop - 1) / perStop
        val thirdsAbove = fullStop * perStop - step
        val fraction = "1/${1 shl fullStop}"
        return when (thirdsAbove) {
            1 -> "$fraction +0.3"
            2 -> "$fraction +0.7"
            else -> fraction
        }
    }
}
