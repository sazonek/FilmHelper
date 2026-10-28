package com.filmhelper.flash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmhelper.core.Scales
import com.filmhelper.core.apertureEv
import com.filmhelper.core.formatEv
import com.filmhelper.core.formatThirds
import com.filmhelper.core.isoEv
import com.filmhelper.core.trimNumber
import com.filmhelper.ui.NumberField
import com.filmhelper.ui.ResultCard
import com.filmhelper.ui.SectionCard
import com.filmhelper.ui.StepperRow
import kotlin.math.abs
import kotlin.math.pow

@Composable
fun FlashScreen(modifier: Modifier = Modifier, viewModel: FlashViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SolveForRow(state.solveFor, viewModel::solveFor)
        FlashResult(state)

        SectionCard("Flash") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberField(
                    label = "Guide number (ISO 100)",
                    value = state.guideNumber,
                    onValueChange = viewModel::setGuideNumber,
                    suffix = state.unit.label,
                    modifier = Modifier.weight(1f),
                )
                UnitToggle(state.unit, viewModel::setUnit)
            }
            if (state.solveFor != FlashVariable.Power) {
                StepperRow("Power", state.powerScale.nearest(-state.powerStops).label, viewModel::stepPower)
            }
            StepperRow("Diffuser loss","${formatThirds(state.lossThirds)} EV", viewModel::stepLoss)
            StepperRow("Lowest power", "1/${1 shl state.minPowerStops}", { viewModel.stepMinPower(-it) })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("1/3-stop power steps", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = state.thirdPowerSteps, onCheckedChange = viewModel::setThirdPowerSteps)
            }
        }

        SectionCard("Camera") {
            if (state.solveFor != FlashVariable.Iso) {
                StepperRow("Film ISO", Scales.iso.nearest(isoEv(state.iso)).label, viewModel::stepIso)
            }
            if (state.solveFor != FlashVariable.Aperture) {
                StepperRow("Aperture", Scales.aperture.nearest(apertureEv(state.aperture)).label, viewModel::stepAperture)
            }
        }

        if (state.solveFor != FlashVariable.Distance) {
            SectionCard("Subject") {
                NumberField(
                    label = "Flash-to-subject distance",
                    value = state.distance,
                    onValueChange = viewModel::setDistance,
                    suffix = state.unit.label,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolveForRow(selected: FlashVariable, onSelect: (FlashVariable) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Solve for", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val variables = FlashVariable.entries
            variables.forEachIndexed { index, variable ->
                SegmentedButton(
                    selected = variable == selected,
                    onClick = { onSelect(variable) },
                    shape = SegmentedButtonDefaults.itemShape(index, variables.size),
                    label = { Text(variable.label, maxLines = 1) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitToggle(selected: DistanceUnit, onSelect: (DistanceUnit) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        val units = DistanceUnit.entries
        units.forEachIndexed { index, unit ->
            SegmentedButton(
                selected = unit == selected,
                onClick = { onSelect(unit) },
                shape = SegmentedButtonDefaults.itemShape(index, units.size),
                icon = {},
                label = { Text(unit.label) },
            )
        }
    }
}

@Composable
private fun FlashResult(state: FlashUiState) {
    val solution = state.solution
    if (solution == null) {
        ResultCard("Result", "—", details = listOf("Enter a guide number and distance."))
        return
    }
    val unit = state.unit.label
    val warning = when (solution.limit) {
        FlashLimit.NeedsMoreLight -> "Not enough light: move the flash closer, open up or use a faster film."
        FlashLimit.TooMuchLight -> "Too much light: step back, stop down, lower the power or add a diffuser."
        null -> null
    }
    val offBy = solution.errorStops.takeIf { abs(it) >= 0.05 }?.let { "Nearest setting: ${formatEv(it)}" }

    when (solution.variable) {
        FlashVariable.Distance -> {
            val d = solution.exact
            // Distances that stay within ±1/3 stop of a correct exposure.
            val near = d * 2.0.pow(-1.0 / 6)
            val far = d * 2.0.pow(1.0 / 6)
            ResultCard(
                caption = "Place the flash at",
                value = "${trimNumber(d, 1)} $unit",
                details = listOf("Within ±1/3 EV: ${trimNumber(near, 1)}–${trimNumber(far, 1)} $unit"),
            )
        }

        FlashVariable.Aperture -> ResultCard(
            caption = "Set the aperture to",
            value = solution.nearest!!.label,
            details = listOfNotNull("Exact: f/${trimNumber(solution.exact, 1)}", offBy),
            warning = warning,
        )

        FlashVariable.Power -> ResultCard(
            caption = "Set the flash power to",
            value = solution.nearest!!.label,
            details = listOfNotNull("Exact: ${trimNumber(solution.exact, 2)} stops below full", offBy),
            warning = warning,
        )

        FlashVariable.Iso -> ResultCard(
            caption = "Load film rated at",
            value = solution.nearest!!.label,
            details = listOfNotNull("Exact: ISO ${trimNumber(solution.exact, 0)}", offBy),
            warning = warning,
        )
    }
}
