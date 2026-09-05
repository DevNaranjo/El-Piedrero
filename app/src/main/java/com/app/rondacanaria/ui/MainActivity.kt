package com.app.rondacanaria.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import com.app.rondacanaria.data.history.LocalGamePersistence
import com.app.rondacanaria.service.AppCleanupService
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.app.rondacanaria.ui.screens.HostLobbyScreen
import com.app.rondacanaria.ui.screens.LobbyScreen
import com.app.rondacanaria.ui.screens.ModeSelectionScreen
import com.app.rondacanaria.ui.screens.ScannerScreen
import com.app.rondacanaria.ui.screens.ScoreBoardScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ScoreViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply {
            fontScale = 1.0f
        }
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null) {
            overrideConfiguration.fontScale = 1.0f
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                viewModel.pauseAllAudio()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            registerReceiver(screenOffReceiver, filter)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error registrando screenOffReceiver: ${e.message}")
        }
        try {
            startService(Intent(this, AppCleanupService::class.java))
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error iniciando AppCleanupService: ${e.message}")
        }
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = uiState.fontScale
                )
            ) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {

                        // Interceptar el botón físico o gesto de "Atrás" de Android para no cerrar la app
                        BackHandler(enabled = uiState.currentScreen != AppScreen.MODE_SELECTION && uiState.currentScreen != AppScreen.SCOREBOARD && uiState.currentScreen != AppScreen.HOST_LOBBY) {
                            when (uiState.currentScreen) {
                                AppScreen.HISTORY -> viewModel.goToModeSelection()
                                AppScreen.LOBBY -> viewModel.goToModeSelection()
                                AppScreen.CLIENT_SCANNER -> viewModel.goToNetworkLobby()
                                else -> {}
                            }
                        }

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

    override fun onResume() {
        super.onResume()
        viewModel.resumeAllAudio()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseAllAudio()
    }

    override fun onStop() {
        super.onStop()
        viewModel.pauseAllAudio()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {}
        if (isFinishing) {
            try {
                LocalGamePersistence(applicationContext).clearLocalGame()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error limpiando partida en onDestroy: ${e.message}")
            }
        }
    }
}
