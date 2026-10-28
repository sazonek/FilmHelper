package com.filmhelper.exposure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filmhelper.core.Scales
import com.filmhelper.core.apertureEv
import com.filmhelper.core.formatDuration
import com.filmhelper.core.isoEv
import com.filmhelper.core.timeEv
import com.filmhelper.core.trimNumber
import com.filmhelper.ui.Picker
import com.filmhelper.ui.ResultCard
import com.filmhelper.ui.SectionCard
import com.filmhelper.ui.StepperRow
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ExposureTool(val label: String) {
    Nd("ND filter"),
    Sunny16("Sunny 16"),
}

@Composable
fun ExposureScreen(modifier: Modifier = Modifier) {
    var tool by rememberSaveable { mutableStateOf(ExposureTool.Nd) }

    Column(modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tool.ordinal) {
            ExposureTool.entries.forEach {
                Tab(selected = it == tool, onClick = { tool = it }, text = { Text(it.label) })
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (tool) {
                ExposureTool.Nd -> NdTool()
                ExposureTool.Sunny16 -> Sunny16Tool()
            }
        }
    }
}

@Composable
private fun NdTool() {
    var metered by rememberSaveable { mutableDoubleStateOf(1.0 / 125) }
    var ndStops by rememberSaveable { mutableIntStateOf(10) }

    val withNd = Exposure.withNd(metered, ndStops.toDouble())
    ResultCard("Expose for", formatDuration(withNd), details = listOf("Without filter ${formatDuration(metered)}"))

    SectionCard("Filter") {
        StepperRow("Metered shutter", Scales.shutter.nearest(timeEv(metered)).label, { step ->
            metered = Scales.shutter.step(timeEv(metered), step).value
        })
        StepperRow("ND strength", ndLabel(ndStops), { step ->
            ndStops = (ndStops + step).coerceIn(1, 20)
        })
    }
}

private fun ndLabel(stops: Int): String {
    val factor = if (stops == 10) "ND1000" else "ND${1L shl stops}"
    return "$stops stops · $factor"
}

@Composable
private fun Sunny16Tool() {
    var scene by rememberSaveable { mutableStateOf(Sunny16Scene.Sunny) }
    var iso by rememberSaveable { mutableDoubleStateOf(400.0) }
    var aperture by rememberSaveable { mutableDoubleStateOf(16.0) }

    val exact = Exposure.sunny16Shutter(scene, iso, aperture)
    val nearest = Scales.shutter.nearest(timeEv(exact))
    val ev = Exposure.ev100(aperture, exact, iso)
    // Only spell out the exact time and the base rule when they add something to the marked speed.
    val details = buildList {
        if (abs(timeEv(exact) - nearest.ev) > 0.05) add("Exact ${formatDuration(exact)}")
        if (abs(apertureEv(aperture) - apertureEv(scene.aperture)) > 0.05) {
            add("Base: f/${trimNumber(scene.aperture, 1)} at 1/${iso.roundToInt()} s")
        }
        add("EV ${ev.roundToInt()} at ISO 100")
    }
    ResultCard(caption = "Shutter speed", value = nearest.label, details = details)

    SectionCard("Scene") {
        Picker(
            label = "Light",
            selected = scene,
            options = Sunny16Scene.entries,
            optionLabel = { it.label },
            onSelect = { scene = it },
        )
        StepperRow("Film ISO", Scales.iso.nearest(isoEv(iso)).label, { step ->
            iso = Scales.iso.step(isoEv(iso), step).value
        })
        StepperRow("Aperture", Scales.aperture.nearest(apertureEv(aperture)).label, { step ->
            aperture = Scales.aperture.step(apertureEv(aperture), step).value
        })
    }
}
