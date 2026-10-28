package com.filmhelper.flash

import androidx.lifecycle.ViewModel
import com.filmhelper.core.Scales
import com.filmhelper.core.StopScale
import com.filmhelper.core.apertureEv
import com.filmhelper.core.isoEv
import com.filmhelper.core.parsePositive
import com.filmhelper.core.trimNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.round

enum class DistanceUnit(val label: String, val metres: Double, private val steps: List<Pair<Double, Double>>) {
    // (distance below which it applies, step), so steps stay roughly 5% of the distance.
    Metres("m", 1.0, listOf(2.0 to 0.1, 5.0 to 0.25, 10.0 to 0.5, Double.MAX_VALUE to 1.0)),
    Feet("ft", 0.3048, listOf(6.0 to 0.5, 15.0 to 1.0, 30.0 to 2.0, Double.MAX_VALUE to 5.0));

    /** The distance one −/+ tap away from [distance], snapped to that step size. */
    fun step(distance: Double, direction: Int): Double {
        // Going down from a boundary uses the finer step below it, so 2 m − 1 = 1.9 m.
        val probe = if (direction < 0) distance - 1e-9 else distance
        val size = steps.first { probe < it.first }.second
        val next = round((distance + direction * size) / size) * size
        return next.coerceAtLeast(steps.first().second)
    }
}

/** Text fields are kept as typed; the dial settings are stored as real values. */
data class FlashUiState(
    val solveFor: FlashVariable = FlashVariable.Power,
    val guideNumber: String = "14",
    val distance: String = "3",
    val unit: DistanceUnit = DistanceUnit.Metres,
    val iso: Double = 400.0,
    val aperture: Double = Scales.aperture.nearest(apertureEv(5.6)).value,
    val powerStops: Double = 0.0,
    val lossThirds: Int = 0,
    val thirdPowerSteps: Boolean = false,
    val minPowerStops: Int = 7,
) {
    val powerScale: StopScale = Scales.flashPower(minPowerStops, thirdPowerSteps)

    val setup: FlashSetup? = run {
        val gn = parsePositive(guideNumber) ?: return@run null
        val d = parsePositive(distance)
        if (d == null && solveFor != FlashVariable.Distance) return@run null
        FlashSetup(gn, d ?: 1.0, iso, aperture, powerStops, lossThirds / 3.0)
    }

    val solution: FlashSolution? = setup?.let { FlashCalculator.solve(it, solveFor, powerScale) }
}

class FlashViewModel : ViewModel() {

    private val _state = MutableStateFlow(FlashUiState())
    val state: StateFlow<FlashUiState> = _state.asStateFlow()

    /** Switching target keeps the last answer, so the value you were solving for becomes an input. */
    fun solveFor(variable: FlashVariable) = _state.update { s ->
        if (s.solveFor == variable) return@update s
        val adopted = s.solution?.let { adopt(s, it) } ?: s
        adopted.copy(solveFor = variable)
    }

    private fun adopt(s: FlashUiState, solution: FlashSolution): FlashUiState {
        val nearest = solution.nearest
        return when (solution.variable) {
            FlashVariable.Distance -> s.copy(distance = trimNumber(solution.exact, 1))
            FlashVariable.Aperture -> s.copy(aperture = nearest!!.value)
            FlashVariable.Power -> s.copy(powerStops = nearest!!.value)
            FlashVariable.Iso -> s.copy(iso = nearest!!.value)
        }
    }

    fun setGuideNumber(text: String) = _state.update { it.copy(guideNumber = text) }

    fun setDistance(text: String) = _state.update { it.copy(distance = text) }

    fun stepDistance(direction: Int) = _state.update { s ->
        val d = parsePositive(s.distance) ?: return@update s
        s.copy(distance = trimNumber(s.unit.step(d, direction), 2))
    }

    /** Converts the guide number and distance so the physical setup stays the same. */
    fun setUnit(unit: DistanceUnit) = _state.update { s ->
        if (s.unit == unit) return@update s
        val factor = s.unit.metres / unit.metres
        fun convert(text: String) = parsePositive(text)?.let { trimNumber(it * factor, 1) } ?: text
        s.copy(unit = unit, guideNumber = convert(s.guideNumber), distance = convert(s.distance))
    }

    fun stepIso(steps: Int) = _state.update {
        it.copy(iso = Scales.iso.step(isoEv(it.iso), steps).value)
    }

    fun stepAperture(steps: Int) = _state.update {
        it.copy(aperture = Scales.aperture.step(apertureEv(it.aperture), steps).value)
    }

    /** Positive [steps] mean more power. */
    fun stepPower(steps: Int) = _state.update {
        it.copy(powerStops = it.powerScale.step(-it.powerStops, steps).value)
    }

    fun stepLoss(steps: Int) = _state.update {
        it.copy(lossThirds = (it.lossThirds + steps).coerceIn(0, MAX_LOSS_THIRDS))
    }

    fun setThirdPowerSteps(thirds: Boolean) = _state.update {
        it.copy(thirdPowerSteps = thirds).snapPower()
    }

    fun stepMinPower(steps: Int) = _state.update {
        it.copy(minPowerStops = (it.minPowerStops + steps).coerceIn(4, 8)).snapPower()
    }

    private fun FlashUiState.snapPower() = copy(powerStops = powerScale.nearest(-powerStops).value)

    private companion object {
        const val MAX_LOSS_THIRDS = 18
    }
}
