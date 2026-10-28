package com.filmhelper.exposure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.filmhelper.core.formatEv
import com.filmhelper.core.isoEv
import com.filmhelper.core.parsePositive
import com.filmhelper.core.timeEv
import com.filmhelper.core.trimNumber
import com.filmhelper.ui.NumberField
import com.filmhelper.ui.Picker
import com.filmhelper.ui.ResultCard
import com.filmhelper.ui.SectionCard
import com.filmhelper.ui.StepperRow
import kotlin.math.roundToInt

private enum class ExposureTool(val label: String) {
    Reciprocity("Reciprocity"),
    Nd("ND filter"),
    Sunny16("Sunny 16"),
}

@Composable
fun ExposureScreen(modifier: Modifier = Modifier) {
    var tool by rememberSaveable { mutableStateOf(ExposureTool.Reciprocity) }

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
                ExposureTool.Reciprocity -> ReciprocityTool()
                ExposureTool.Nd -> NdTool()
                ExposureTool.Sunny16 -> Sunny16Tool()
            }
        }
    }
}

/** Index into [FilmStocks.all], or [CUSTOM_FILM] for a user-entered exponent. */
private const val CUSTOM_FILM = -1

private fun filmLabel(index: Int) = if (index == CUSTOM_FILM) "Custom exponent" else FilmStocks.all[index].name

@Composable
private fun ReciprocityTool() {
    var filmIndex by rememberSaveable { mutableIntStateOf(2) }
    var exponent by rememberSaveable { mutableStateOf("1.30") }
    var metered by rememberSaveable { mutableDoubleStateOf(8.0) }

    val reciprocity = if (filmIndex == CUSTOM_FILM) {
        parsePositive(exponent)?.let { Reciprocity.Exponent(it) }
    } else {
        FilmStocks.all[filmIndex].reciprocity
    }

    if (reciprocity == null) {
        ResultCard("Expose for", "—", details = listOf("Enter a Schwarzschild exponent, e.g. 1.31."))
    } else {
        val corrected = reciprocity.correct(metered)
        ResultCard(
            caption = "Expose for",
            value = formatDuration(corrected),
            details = listOf(
                "Metered ${formatDuration(metered)} · ${formatEv(Exposure.stopsBetween(metered, corrected))}",
            ),
        )
    }

    SectionCard("Film") {
        Picker(
            label = "Film stock",
            selected = filmIndex,
            options = FilmStocks.all.indices.toList() + CUSTOM_FILM,
            optionLabel = ::filmLabel,
            onSelect = { filmIndex = it },
        )
        if (filmIndex == CUSTOM_FILM) {
            NumberField(
                label = "Exponent p (corrected = metered^p)",
                value = exponent,
                onValueChange = { exponent = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        StepperRow("Metered time", formatDuration(metered), { step ->
            metered = Scales.longShutter.step(timeEv(metered), step).value
        })
    }

    if (reciprocity != null) {
        SectionCard("Quick reference") {
            listOf(1.0, 2.0, 4.0, 8.0, 15.0, 30.0, 60.0, 120.0, 240.0, 480.0).forEach { t ->
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(t), Modifier.weight(1f))
                    Text("→ ${formatDuration(reciprocity.correct(t))}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    Text(
        "Reciprocity data from manufacturer datasheets; bracket critical exposures.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Index into [FilmStocks.all], or [NO_FILM] to skip reciprocity. */
private const val NO_FILM = -1

@Composable
private fun NdTool() {
    var metered by rememberSaveable { mutableDoubleStateOf(1.0 / 125) }
    var ndStops by rememberSaveable { mutableIntStateOf(10) }
    var filmIndex by rememberSaveable { mutableIntStateOf(NO_FILM) }

    val withNd = Exposure.withNd(metered, ndStops.toDouble())
    val film = FilmStocks.all.getOrNull(filmIndex)
    val details = buildList {
        add("Without filter ${formatDuration(metered)}")
        if (film != null) {
            val corrected = film.reciprocity.correct(withNd)
            add("With ${film.name} reciprocity: ${formatDuration(corrected)}")
        }
    }
    ResultCard("Expose for", formatDuration(withNd), details = details)

    SectionCard("Filter") {
        StepperRow("Metered shutter", Scales.shutter.nearest(timeEv(metered)).label, { step ->
            metered = Scales.shutter.step(timeEv(metered), step).value
        })
        StepperRow("ND strength", ndLabel(ndStops), { step ->
            ndStops = (ndStops + step).coerceIn(1, 20)
        })
        Picker(
            label = "Correct reciprocity for",
            selected = filmIndex,
            options = listOf(NO_FILM) + FilmStocks.all.indices,
            optionLabel = { if (it == NO_FILM) "No correction" else FilmStocks.all[it].name },
            onSelect = { filmIndex = it },
        )
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
    ResultCard(
        caption = "Shutter speed",
        value = nearest.label,
        details = listOf(
            "Exact ${formatDuration(exact)} · EV ${ev.roundToInt()} at ISO 100",
            "Base: f/${trimNumber(scene.aperture, 1)} at 1/${iso.roundToInt()} s",
        ),
    )

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
