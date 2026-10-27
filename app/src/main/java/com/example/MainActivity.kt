package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.components.ResilioBottomNavigation
import com.example.ui.components.ResilioTopAppBar
import com.example.ui.screens.*
import com.example.ui.theme.ResilioNavyDark
import com.example.ui.theme.ResilioTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.ResilioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ResilioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ResilioTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val isResilio by viewModel.isResilioMode.collectAsState()
                val reports by viewModel.reports.collectAsState()

                // BackHandler to return to Digital Twin Map if on a secondary screen
                BackHandler(enabled = currentScreen != AppScreen.DIGITAL_TWIN) {
                    viewModel.setScreen(AppScreen.DIGITAL_TWIN)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        ResilioTopAppBar(
                            currentScreen = currentScreen,
                            isResilioMode = isResilio,
                            onToggleMode = { viewModel.toggleSimulationMode(it) },
                            activeIncidentsCount = reports.size
                        )
                    },
                    bottomBar = {
                        ResilioBottomNavigation(
                            currentScreen = currentScreen,
                            onScreenSelected = { viewModel.setScreen(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ResilioNavyDark)
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DIGITAL_TWIN -> DigitalTwinMapScreen(viewModel = viewModel)
                            AppScreen.CITIZEN_SOS -> CitizenEmergencyScreen(viewModel = viewModel)
                            AppScreen.EDSS_COMMAND -> EDSSDecisionScreen(viewModel = viewModel)
                            AppScreen.SIMULATION_EVAL -> SimulationScreen(viewModel = viewModel)
                            AppScreen.OFFLINE_TOOLKIT -> OfflineToolkitScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
