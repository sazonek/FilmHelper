package com.filmhelper.core

import kotlin.math.roundToInt

/**
 * A shutter time as a photographer would write it: "1/250", "2.5 s", "4 min 10 s", "54 min", "1 h 5 min".
 * From 10 minutes on, seconds are dropped as they no longer matter for the exposure.
 */
fun formatDuration(seconds: Double): String = when {
    seconds < 0.3 -> "1/${(1 / seconds).roundToInt()}"
    seconds < 10 -> "${trimNumber(seconds, 1)} s"
    seconds < 60 -> "${seconds.roundToInt()} s"
    seconds < 600 -> {
        val total = seconds.roundToInt()
        val s = total % 60
        if (s == 0) "${total / 60} min" else "${total / 60} min $s s"
    }
    else -> {
        val minutes = (seconds / 60).roundToInt()
        val m = minutes % 60
        when {
            minutes < 60 -> "$minutes min"
            m == 0 -> "${minutes / 60} h"
            else -> "${minutes / 60} h $m min"
        }
    }
}

/** Thirds of a stop as "0", "1/3", "1", "1 2/3". */
fun formatThirds(thirds: Int): String {
    val whole = thirds / 3
    val part = when (thirds % 3) {
        1 -> "1/3"
        2 -> "2/3"
        else -> ""
    }
    return when {
        whole == 0 && part.isEmpty() -> "0"
        whole == 0 -> part
        part.isEmpty() -> "$whole"
        else -> "$whole $part"
    }
}

/** Signed stops for display, e.g. "+0.3 EV". */
fun formatEv(stops: Double): String = "%+.1f EV".format(stops)

fun trimNumber(value: Double, decimals: Int): String =
    "%.${decimals}f".format(value).trimEnd('0').trimEnd('.', ',')

/** Parses user input, accepting both "2.8" and "2,8". */
fun parsePositive(text: String): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 && it.isFinite() }
