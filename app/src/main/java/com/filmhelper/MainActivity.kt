package com.filmhelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.filmhelper.exposure.ExposureScreen
import com.filmhelper.flash.FlashScreen
import com.filmhelper.ui.theme.FilmHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FilmHelperTheme {
                FilmHelperApp()
            }
        }
    }
}

private enum class Destination(val label: String, val icon: Int) {
    Flash("Flash", R.drawable.ic_flash),
    Exposure("Exposure", R.drawable.ic_exposure),
}

@Composable
private fun FilmHelperApp() {
    var destination by rememberSaveable { mutableStateOf(Destination.Flash) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach {
                    NavigationBarItem(
                        selected = it == destination,
                        onClick = { destination = it },
                        icon = { Icon(painterResource(it.icon), contentDescription = null) },
                        label = { Text(it.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        when (destination) {
            Destination.Flash -> FlashScreen(modifier)
            Destination.Exposure -> ExposureScreen(modifier)
        }
    }
}
