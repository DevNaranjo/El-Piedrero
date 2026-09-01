package com.app.rondacanaria.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.app.rondacanaria.ui.screens.HostLobbyScreen
import com.app.rondacanaria.ui.screens.LobbyScreen
import com.app.rondacanaria.ui.screens.ModeSelectionScreen
import com.app.rondacanaria.ui.screens.ScannerScreen
import com.app.rondacanaria.ui.screens.ScoreBoardScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()

                    Crossfade(targetState = uiState.currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            AppScreen.MODE_SELECTION -> ModeSelectionScreen(uiState = uiState, viewModel = viewModel)
                            AppScreen.LOBBY -> LobbyScreen(uiState = uiState, viewModel = viewModel)
                            AppScreen.HOST_LOBBY -> HostLobbyScreen(uiState = uiState, viewModel = viewModel)
                            AppScreen.CLIENT_SCANNER -> ScannerScreen(uiState = uiState, viewModel = viewModel)
                            AppScreen.SCOREBOARD -> ScoreBoardScreen(uiState = uiState, viewModel = viewModel)
                            AppScreen.HISTORY -> com.app.rondacanaria.ui.screens.HistoryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
