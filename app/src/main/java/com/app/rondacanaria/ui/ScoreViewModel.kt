package com.app.rondacanaria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.rondacanaria.data.audio.RondaAudioPlayer
import com.app.rondacanaria.data.history.GameHistoryRepository
import com.app.rondacanaria.data.model.CantoType
import com.app.rondacanaria.data.model.GameHistoryRecord
import com.app.rondacanaria.data.model.GameState
import com.app.rondacanaria.data.model.Team
import com.app.rondacanaria.data.network.NetworkUtils
import com.app.rondacanaria.domain.model.ConnectionInfo
import com.app.rondacanaria.domain.usecase.ClientGameUseCase
import com.app.rondacanaria.domain.usecase.HostGameUseCase
import com.app.rondacanaria.domain.usecase.SessionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppScreen {
    MODE_SELECTION,
    LOBBY,
    HOST_LOBBY,
    CLIENT_SCANNER,
    SCOREBOARD,
    HISTORY
}

data class ScoreUiState(
    val currentScreen: AppScreen = AppScreen.MODE_SELECTION,
    val playerName: String = "Jugador Canario",
    val teamAName: String = "Equipo A",
    val teamBName: String = "Equipo B",
    val teamCName: String = "Equipo C",
    val maxPlayers: Int = 4,
    val isHost: Boolean = false,
    val isLocalGame: Boolean = false,
    val myTeam: Team = Team.SPECTATOR,
    val gameState: GameState = GameState(gameId = ""),
    val hostConnectionInfo: ConnectionInfo? = null,
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val errorMessage: String? = null
)

class ScoreViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val audioPlayer = RondaAudioPlayer(application.applicationContext)
    private val hostUseCase: HostGameUseCase = HostGameUseCase()
    private val clientUseCase: ClientGameUseCase = ClientGameUseCase()
    private val historyRepository = GameHistoryRepository(application.applicationContext)
    val gameHistory: StateFlow<List<GameHistoryRecord>> = historyRepository.history
    private var lastRecordedGameId: String? = null

    private val _uiState = MutableStateFlow(ScoreUiState())
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    init {
        // Observar estado del Host
        viewModelScope.launch {
            hostUseCase.gameState.collect { state ->
                if (_uiState.value.isHost) {
                    _uiState.update { it.copy(gameState = state) }
                    checkAndRecordVictory(state)
                }
            }
        }

        // Observar avisos sonoros del Host
        viewModelScope.launch {
            hostUseCase.soundEvents.collect { soundPayload ->
                audioPlayer.playSound(soundPayload.soundType)
            }
        }

        // Observar estado del Cliente
        viewModelScope.launch {
            clientUseCase.gameState.collect { state ->
                if (!_uiState.value.isHost && state != null) {
                    _uiState.update { it.copy(gameState = state) }
                    checkAndRecordVictory(state)
                }
            }
        }

        // Observar avisos sonoros del Cliente
        viewModelScope.launch {
            clientUseCase.soundEvents.collect { soundPayload ->
                audioPlayer.playSound(soundPayload.soundType)
            }
        }

        // Observar sesión del Cliente
        viewModelScope.launch {
            clientUseCase.sessionStatus.collect { status ->
                if (!_uiState.value.isHost) {
                    _uiState.update { it.copy(sessionStatus = status) }
                    if (status == SessionStatus.CONNECTED && _uiState.value.currentScreen == AppScreen.CLIENT_SCANNER) {
                        _uiState.update { it.copy(currentScreen = AppScreen.SCOREBOARD) }
                    }
                }
            }
        }

        viewModelScope.launch {
            clientUseCase.myTeam.collect { team ->
                if (!_uiState.value.isHost) {
                    _uiState.update { it.copy(myTeam = team) }
                }
            }
        }
    }

    fun goToModeSelection() {
        _uiState.update { it.copy(currentScreen = AppScreen.MODE_SELECTION) }
    }

    fun goToNetworkLobby() {
        _uiState.update { it.copy(currentScreen = AppScreen.LOBBY) }
    }

    fun startLocalGame(teamA: String, teamB: String, teamC: String = "Equipo C", maxPlayers: Int) {
        val state = _uiState.value
        hostUseCase.startHost(
            hostPlayerName = state.playerName,
            teamAName = teamA,
            teamBName = teamB,
            teamCName = teamC,
            maxPlayers = maxPlayers
        )
        _uiState.update {
            it.copy(
                isHost = true,
                isLocalGame = true,
                teamAName = teamA,
                teamBName = teamB,
                teamCName = teamC,
                maxPlayers = maxPlayers,
                gameState = hostUseCase.gameState.value,
                currentScreen = AppScreen.SCOREBOARD,
                sessionStatus = SessionStatus.CONNECTED
            )
        }
    }

    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
    }

    fun setRoomConfig(teamA: String, teamB: String, teamC: String = "Equipo C", maxPlayers: Int) {
        _uiState.update {
            it.copy(
                teamAName = teamA,
                teamBName = teamB,
                teamCName = teamC,
                maxPlayers = maxPlayers
            )
        }
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.updateRoomConfig(teamA, teamB, teamC, maxPlayers)
            }
        }
    }

    fun startHosting() {
        val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
        val state = _uiState.value
        hostUseCase.startHost(
            hostPlayerName = state.playerName,
            teamAName = state.teamAName,
            teamBName = state.teamBName,
            teamCName = state.teamCName,
            maxPlayers = state.maxPlayers
        )

        val info = ConnectionInfo(
            ip = ip,
            port = NetworkUtils.DEFAULT_PORT,
            gameId = hostUseCase.gameState.value.gameId,
            hostName = state.playerName,
            teamAName = state.teamAName,
            teamBName = state.teamBName,
            teamCName = state.teamCName,
            maxPlayers = state.maxPlayers
        )

        _uiState.update {
            it.copy(
                isHost = true,
                isLocalGame = false,
                myTeam = Team.TEAM_A,
                hostConnectionInfo = info,
                currentScreen = AppScreen.HOST_LOBBY,
                sessionStatus = SessionStatus.CONNECTED
            )
        }
    }

    fun openScanner() {
        _uiState.update {
            it.copy(
                isHost = false,
                isLocalGame = false,
                currentScreen = AppScreen.CLIENT_SCANNER,
                errorMessage = null
            )
        }
    }

    fun onQrScanned(info: ConnectionInfo) {
        val name = _uiState.value.playerName
        clientUseCase.joinGame(
            host = info.ip,
            port = info.port,
            playerName = name
        )
    }

    fun navigateToScoreboard() {
        _uiState.update { it.copy(currentScreen = AppScreen.SCOREBOARD) }
    }

    fun callCanto(teamId: Team, canto: CantoType) {
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                hostUseCase.applyScoreUpdate(teamId, canto, canto.defaultPiedras, canto.displayName)
            } else {
                clientUseCase.requestCanto(teamId, canto)
            }
        }
    }

    fun manualScoreChange(teamId: Team, delta: Int) {
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                hostUseCase.applyScoreUpdate(teamId, CantoType.MANUAL_ADJUST, delta, "Ajuste manual")
            } else {
                clientUseCase.requestManualScoreChange(teamId, delta)
            }
        }
    }

    fun terminateGame() {
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                hostUseCase.endGame("Partida finalizada por el Host")
            } else {
                clientUseCase.requestEndGame("Partida finalizada por jugador")
            }
            exitGame()
        }
    }

    fun resetGame() {
        lastRecordedGameId = null
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.resetGame()
            }
        }
    }

    fun goToHistory() {
        _uiState.update { it.copy(currentScreen = AppScreen.HISTORY) }
    }

    fun clearGameHistory() {
        historyRepository.clearHistory()
    }

    private fun checkAndRecordVictory(state: GameState) {
        val winner = state.winnerTeam
        if (winner != null && state.gameId.isNotBlank() && lastRecordedGameId != state.gameId) {
            val winnerName = when (winner) {
                Team.TEAM_A -> state.nameTeamA
                Team.TEAM_B -> state.nameTeamB
                Team.TEAM_C -> state.nameTeamC
                else -> "Equipo Ganador"
            }
            val record = GameHistoryRecord(
                id = state.gameId,
                timestamp = System.currentTimeMillis(),
                isLocalGame = _uiState.value.isLocalGame,
                winnerTeam = winner,
                winnerName = winnerName,
                totalPiedrasWinner = when (winner) {
                    Team.TEAM_A -> state.scoreTeamA.totalPiedras
                    Team.TEAM_B -> state.scoreTeamB.totalPiedras
                    Team.TEAM_C -> state.scoreTeamC.totalPiedras
                    else -> 21
                },
                teamAName = state.nameTeamA,
                teamAPiedras = state.scoreTeamA.totalPiedras,
                teamBName = state.nameTeamB,
                teamBPiedras = state.scoreTeamB.totalPiedras,
                teamCName = if (state.maxPlayers == 3) state.nameTeamC else null,
                teamCPiedras = if (state.maxPlayers == 3) state.scoreTeamC.totalPiedras else null,
                maxPlayers = state.maxPlayers
            )
            historyRepository.saveGame(record)
            lastRecordedGameId = state.gameId
        }
    }

    fun exitGame() {
        lastRecordedGameId = null
        if (_uiState.value.isHost) {
            hostUseCase.stopHost()
        } else {
            clientUseCase.leaveGame()
        }
        _uiState.update {
            ScoreUiState(
                currentScreen = AppScreen.MODE_SELECTION,
                playerName = it.playerName
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
