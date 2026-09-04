package com.app.rondacanaria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.rondacanaria.data.audio.RondaAudioPlayer
import com.app.rondacanaria.data.history.GameHistoryRepository
import com.app.rondacanaria.data.history.LocalGamePersistence
import com.app.rondacanaria.data.history.LocalSavedGame
import com.app.rondacanaria.data.model.*
import com.app.rondacanaria.data.network.NetworkUtils
import com.app.rondacanaria.domain.model.ConnectionInfo
import com.app.rondacanaria.domain.usecase.ClientGameUseCase
import com.app.rondacanaria.domain.usecase.HostGameUseCase
import com.app.rondacanaria.domain.usecase.SessionStatus
import com.app.rondacanaria.domain.usecase.TeamChangeRequest
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
    val playerName: String = "",
    val teamAName: String = "Equipo A",
    val teamBName: String = "Equipo B",
    val teamCName: String = "Equipo C",
    val teamDName: String = "Equipo D",
    val maxPlayers: Int = 4,
    val isHost: Boolean = false,
    val isLocalGame: Boolean = false,
    val myTeam: Team = Team.SPECTATOR,
    val gameState: GameState = GameState(gameId = ""),
    val hostConnectionInfo: ConnectionInfo? = null,
    val sessionStatus: SessionStatus = SessionStatus.IDLE,
    val reconnectCountdown: Int? = null,
    val errorMessage: String? = null,
    val connectingHostName: String? = null,
    val isMusicEnabled: Boolean = true,
    val isSfxEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.5f,
    val sfxVolume: Float = 0.9f,
    val isTvAudioOptimizationEnabled: Boolean = true,
    val isTvCastingActive: Boolean = false
)

class ScoreViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val audioPlayer = RondaAudioPlayer(application.applicationContext)
    private val hostUseCase: HostGameUseCase = HostGameUseCase()
    private val clientUseCase: ClientGameUseCase = ClientGameUseCase()
    private val historyRepository = GameHistoryRepository(application.applicationContext)
    private val localPersistence = LocalGamePersistence(application.applicationContext)
    val gameHistory: StateFlow<List<GameHistoryRecord>> = historyRepository.history
    private var lastRecordedGameId: String? = null

    private val _uiState = MutableStateFlow(
        ScoreUiState(
            isMusicEnabled = audioPlayer.isMusicEnabled,
            isSfxEnabled = audioPlayer.isSfxEnabled,
            isVibrationEnabled = audioPlayer.isVibrationEnabled,
            masterVolume = audioPlayer.masterVolume,
            musicVolume = audioPlayer.musicVolume,
            sfxVolume = audioPlayer.sfxVolume,
            isTvAudioOptimizationEnabled = audioPlayer.isTvAudioOptimizationEnabled,
            isTvCastingActive = audioPlayer.isTvCastingActive
        )
    )
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    val localPlayerId: String get() = clientUseCase.localPlayerId

    init {
        // Observar estado del Host y persistir automáticamente partidas locales
        viewModelScope.launch {
            hostUseCase.gameState.collect { state ->
                if (_uiState.value.isHost) {
                    _uiState.update { it.copy(gameState = state) }
                    checkAndRecordVictory(state)
                    if (_uiState.value.isLocalGame && _uiState.value.currentScreen == AppScreen.SCOREBOARD && state.status != GameStatus.FINISHED) {
                        localPersistence.saveLocalGame(
                            LocalSavedGame(
                                gameState = state,
                                maxPlayers = _uiState.value.maxPlayers,
                                teamAName = _uiState.value.teamAName,
                                teamBName = _uiState.value.teamBName,
                                teamCName = _uiState.value.teamCName,
                                teamDName = _uiState.value.teamDName
                            )
                        )
                    }
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
                    val isTwoPlayers = state.maxPlayers == 2
                    val myPlayer = state.connectedPlayers.find { 
                        it.id == clientUseCase.localPlayerId || (it.name.isNotBlank() && it.name == _uiState.value.playerName && !it.isHost)
                    }
                    val resolvedTeam = when {
                        isTwoPlayers -> Team.TEAM_B
                        myPlayer != null && myPlayer.team != Team.SPECTATOR -> myPlayer.team
                        clientUseCase.myTeam.value != Team.SPECTATOR -> clientUseCase.myTeam.value
                        else -> Team.TEAM_B
                    }

                    _uiState.update { current ->
                        val targetScreen = if (state.status == GameStatus.PLAYING && current.currentScreen == AppScreen.HOST_LOBBY) {
                            AppScreen.SCOREBOARD
                        } else if (state.status == GameStatus.WAITING && current.currentScreen == AppScreen.SCOREBOARD) {
                            AppScreen.HOST_LOBBY
                        } else {
                            current.currentScreen
                        }
                        current.copy(
                            gameState = state,
                            maxPlayers = state.maxPlayers,
                            currentScreen = targetScreen,
                            myTeam = resolvedTeam
                        )
                    }
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
                    val resolvedTeam = clientUseCase.myTeam.value
                    _uiState.update { current ->
                        val targetScreen = if (status == SessionStatus.CONNECTED && current.currentScreen == AppScreen.CLIENT_SCANNER) {
                            if (current.gameState.status == GameStatus.PLAYING) AppScreen.SCOREBOARD else AppScreen.HOST_LOBBY
                        } else {
                            current.currentScreen
                        }
                        current.copy(
                            sessionStatus = status,
                            currentScreen = targetScreen,
                            myTeam = if (resolvedTeam != Team.SPECTATOR) resolvedTeam else current.myTeam
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            clientUseCase.myTeam.collect { team ->
                if (!_uiState.value.isHost && team != Team.SPECTATOR) {
                    _uiState.update { it.copy(myTeam = team) }
                }
            }
        }

        // Observar errores del Cliente
        viewModelScope.launch {
            clientUseCase.errorMessage.collect { err ->
                if (!_uiState.value.isHost) {
                    _uiState.update { it.copy(errorMessage = err) }
                }
            }
        }

        // Observar cuenta atrás de reconexión del Cliente (5 minutos)
        viewModelScope.launch {
            clientUseCase.reconnectCountdown.collect { seconds ->
                _uiState.update { it.copy(reconnectCountdown = seconds) }
            }
        }

        // Restaurar partida local activa si existe y no se había cerrado formalmente
        val savedLocal = localPersistence.loadLocalGame()
        if (savedLocal != null && savedLocal.gameState.status != GameStatus.FINISHED) {
            _uiState.update { current ->
                current.copy(
                    isLocalGame = true,
                    isHost = true,
                    currentScreen = AppScreen.SCOREBOARD,
                    maxPlayers = savedLocal.maxPlayers,
                    teamAName = savedLocal.teamAName,
                    teamBName = savedLocal.teamBName,
                    teamCName = savedLocal.teamCName,
                    teamDName = savedLocal.teamDName,
                    gameState = savedLocal.gameState,
                    sessionStatus = SessionStatus.CONNECTED
                )
            }
            viewModelScope.launch {
                hostUseCase.restoreGameState(savedLocal.gameState)
            }
        }
    }

    fun goToModeSelection() {
        localPersistence.clearLocalGame()
        _uiState.update { it.copy(currentScreen = AppScreen.MODE_SELECTION) }
    }

    fun goToNetworkLobby() {
        if (_uiState.value.isHost) {
            hostUseCase.stopHost()
        } else {
            clientUseCase.leaveGame()
        }
        _uiState.update {
            it.copy(
                currentScreen = AppScreen.LOBBY,
                hostConnectionInfo = null,
                connectingHostName = null,
                errorMessage = null,
                sessionStatus = SessionStatus.IDLE
            )
        }
    }

    fun startLocalGame(
        teamA: String,
        teamB: String,
        teamC: String = "Equipo C",
        teamD: String = "Equipo D",
        maxPlayers: Int,
        reserveTeams: List<Team> = if (maxPlayers == 6) listOf(Team.TEAM_C) else if (maxPlayers == 8) listOf(Team.TEAM_C, Team.TEAM_D) else emptyList(),
        customPlayers: List<Pair<String, Team>>? = null
    ) {
        val effectiveReserves = when {
            maxPlayers == 8 -> if (reserveTeams.size == 2) reserveTeams else listOf(Team.TEAM_C, Team.TEAM_D)
            maxPlayers == 6 -> if (reserveTeams.isNotEmpty()) reserveTeams else listOf(Team.TEAM_C)
            else -> reserveTeams
        }
        val state = _uiState.value
        val localPlayers = if (!customPlayers.isNullOrEmpty()) {
            customPlayers.mapIndexed { index, (name, team) ->
                Player(
                    id = "local_${index + 1}",
                    name = name.ifBlank { "Jugador ${index + 1}" },
                    team = team,
                    isHost = index == 0
                )
            }
        } else {
            when (maxPlayers) {
                2 -> listOf(
                    Player(id = "local_1", name = teamA, team = Team.TEAM_A, isHost = true),
                    Player(id = "local_2", name = teamB, team = Team.TEAM_B, isHost = false)
                )
                3 -> listOf(
                    Player(id = "local_1", name = teamA, team = Team.TEAM_A, isHost = true),
                    Player(id = "local_2", name = teamB, team = Team.TEAM_B, isHost = false),
                    Player(id = "local_3", name = teamC, team = Team.TEAM_C, isHost = false)
                )
                4 -> listOf(
                    Player(id = "local_1", name = "$teamA (J1)", team = Team.TEAM_A, isHost = true),
                    Player(id = "local_2", name = "$teamB (J1)", team = Team.TEAM_B, isHost = false),
                    Player(id = "local_3", name = "$teamA (J2)", team = Team.TEAM_A, isHost = false),
                    Player(id = "local_4", name = "$teamB (J2)", team = Team.TEAM_B, isHost = false)
                )
                6 -> listOf(
                    Player(id = "local_1", name = "$teamA (J1)", team = Team.TEAM_A, isHost = true),
                    Player(id = "local_2", name = "$teamB (J1)", team = Team.TEAM_B, isHost = false),
                    Player(id = "local_3", name = "$teamA (J2)", team = Team.TEAM_A, isHost = false),
                    Player(id = "local_4", name = "$teamB (J2)", team = Team.TEAM_B, isHost = false),
                    Player(id = "local_5", name = "$teamC (J1)", team = Team.TEAM_C, isHost = false),
                    Player(id = "local_6", name = "$teamC (J2)", team = Team.TEAM_C, isHost = false)
                )
                8 -> listOf(
                    Player(id = "local_1", name = "$teamA (J1)", team = Team.TEAM_A, isHost = true),
                    Player(id = "local_2", name = "$teamB (J1)", team = Team.TEAM_B, isHost = false),
                    Player(id = "local_3", name = "$teamA (J2)", team = Team.TEAM_A, isHost = false),
                    Player(id = "local_4", name = "$teamB (J2)", team = Team.TEAM_B, isHost = false),
                    Player(id = "local_5", name = "$teamC (J1)", team = Team.TEAM_C, isHost = false),
                    Player(id = "local_6", name = "$teamC (J2)", team = Team.TEAM_C, isHost = false),
                    Player(id = "local_7", name = "$teamD (J1)", team = Team.TEAM_D, isHost = false),
                    Player(id = "local_8", name = "$teamD (J2)", team = Team.TEAM_D, isHost = false)
                )
                else -> listOf(
                    Player(id = "local_1", name = teamA, team = Team.TEAM_A, isHost = true),
                    Player(id = "local_2", name = teamB, team = Team.TEAM_B, isHost = false)
                )
            }
        }

        hostUseCase.startHost(
            hostPlayerName = state.playerName,
            teamAName = teamA,
            teamBName = teamB,
            teamCName = teamC,
            teamDName = teamD,
            maxPlayers = maxPlayers,
            reserveTeams = effectiveReserves,
            initialPlayers = localPlayers
        )
        _uiState.update {
            it.copy(
                isHost = true,
                isLocalGame = true,
                teamAName = teamA,
                teamBName = teamB,
                teamCName = teamC,
                teamDName = teamD,
                maxPlayers = maxPlayers,
                gameState = hostUseCase.gameState.value,
                currentScreen = AppScreen.SCOREBOARD,
                sessionStatus = SessionStatus.CONNECTED
            )
        }
        localPersistence.saveLocalGame(
            LocalSavedGame(
                gameState = hostUseCase.gameState.value,
                maxPlayers = maxPlayers,
                teamAName = teamA,
                teamBName = teamB,
                teamCName = teamC,
                teamDName = teamD
            )
        )
    }

    fun setDealer(dealerPlayerId: String) {
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.setDealer(dealerPlayerId)
            }
        }
    }

    fun updateReserveTeams(reserveTeams: List<Team>) {
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.setReserveTeams(reserveTeams)
            }
        }
    }

    fun setPlayerName(name: String) {
        _uiState.update { it.copy(playerName = name) }
    }

    fun setRoomConfig(
        teamA: String,
        teamB: String,
        teamC: String = "Equipo C",
        teamD: String = "Equipo D",
        maxPlayers: Int,
        reserveTeams: List<Team>? = null
    ) {
        val calculatedReserves = reserveTeams ?: when (maxPlayers) {
            6 -> listOf(Team.TEAM_C)
            8 -> listOf(Team.TEAM_C, Team.TEAM_D)
            else -> emptyList()
        }
        _uiState.update {
            it.copy(
                teamAName = teamA,
                teamBName = teamB,
                teamCName = teamC,
                teamDName = teamD,
                maxPlayers = maxPlayers,
                gameState = it.gameState.copy(maxPlayers = maxPlayers, reserveTeams = calculatedReserves)
            )
        }
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.updateRoomConfig(teamA, teamB, teamC, teamD, maxPlayers, calculatedReserves)
            }
        }
    }

    fun startHosting(customReserves: List<Team>? = null) {
        val ip = NetworkUtils.getLocalIpAddress() ?: "127.0.0.1"
        val state = _uiState.value
        val initialReserves = customReserves ?: when (state.maxPlayers) {
            6 -> if (state.gameState.reserveTeams.isNotEmpty()) state.gameState.reserveTeams else listOf(Team.TEAM_C)
            8 -> if (state.gameState.reserveTeams.isNotEmpty()) state.gameState.reserveTeams else listOf(Team.TEAM_C, Team.TEAM_D)
            else -> emptyList()
        }
        hostUseCase.startHost(
            hostPlayerName = state.playerName,
            teamAName = state.teamAName,
            teamBName = state.teamBName,
            teamCName = state.teamCName,
            teamDName = state.teamDName,
            maxPlayers = state.maxPlayers,
            reserveTeams = initialReserves
        )

        val currentHostState = hostUseCase.gameState.value

        val info = ConnectionInfo(
            ip = ip,
            port = NetworkUtils.DEFAULT_PORT,
            gameId = currentHostState.gameId,
            hostName = state.playerName,
            teamAName = state.teamAName,
            teamBName = state.teamBName,
            teamCName = state.teamCName,
            teamDName = state.teamDName,
            maxPlayers = state.maxPlayers,
            roomToken = hostUseCase.roomToken,
            secretKey = hostUseCase.encryptionKey
        )

        _uiState.update {
            it.copy(
                isHost = true,
                isLocalGame = false,
                myTeam = Team.TEAM_A,
                maxPlayers = state.maxPlayers,
                gameState = currentHostState,
                hostConnectionInfo = info,
                currentScreen = AppScreen.HOST_LOBBY,
                sessionStatus = SessionStatus.CONNECTED
            )
        }
    }

    fun openScanner() {
        clientUseCase.leaveGame()
        _uiState.update {
            it.copy(
                isHost = false,
                isLocalGame = false,
                currentScreen = AppScreen.CLIENT_SCANNER,
                connectingHostName = null,
                errorMessage = null,
                sessionStatus = SessionStatus.IDLE
            )
        }
    }

    fun resetScannerConnection() {
        clientUseCase.leaveGame()
        _uiState.update {
            it.copy(
                connectingHostName = null,
                errorMessage = null,
                sessionStatus = SessionStatus.IDLE
            )
        }
    }

    fun onQrScanned(info: ConnectionInfo) {
        val name = _uiState.value.playerName
        _uiState.update {
            it.copy(
                hostConnectionInfo = info,
                connectingHostName = info.hostName,
                errorMessage = null,
                maxPlayers = info.maxPlayers,
                myTeam = if (info.maxPlayers in listOf(2, 3)) Team.TEAM_B else it.myTeam
            )
        }
        clientUseCase.joinGame(
            host = info.ip,
            port = info.port,
            playerName = name,
            roomToken = info.roomToken,
            encryptionKey = info.secretKey,
            hostName = info.hostName
        )
    }

    fun navigateToScoreboard() {
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.setGameStatus(GameStatus.PLAYING)
            }
        }
        _uiState.update { it.copy(currentScreen = AppScreen.SCOREBOARD) }
    }

    fun callCanto(teamId: Team, canto: CantoType) {
        val state = _uiState.value
        if (state.gameState.reserveTeams.contains(teamId)) return
        if (!state.isLocalGame) {
            val isTwoPlayers = state.gameState.maxPlayers == 2 || state.maxPlayers == 2
            val effectiveMyTeam = when {
                isTwoPlayers && !state.isHost -> Team.TEAM_B
                isTwoPlayers && state.isHost -> Team.TEAM_A
                state.myTeam != Team.SPECTATOR -> state.myTeam
                else -> state.gameState.connectedPlayers.find { it.id == clientUseCase.localPlayerId }?.team
                    ?: state.gameState.connectedPlayers.find { it.name.isNotBlank() && it.name == state.playerName && !it.isHost }?.team
                    ?: if (!state.isHost) Team.TEAM_B else state.myTeam
            }
            if (state.gameState.reserveTeams.contains(effectiveMyTeam) || effectiveMyTeam == Team.RESERVE || effectiveMyTeam == Team.SPECTATOR) return
            if (effectiveMyTeam != teamId) return // En multijugador no se puede cantar para el equipo rival
        }

        val author = state.playerName
        viewModelScope.launch {
            if (state.isHost) {
                hostUseCase.applyScoreUpdate(teamId, canto, canto.defaultPiedras, canto.displayName, author)
            } else {
                clientUseCase.requestCanto(teamId, canto)
            }
        }
    }

    fun manualScoreChange(teamId: Team, delta: Int, reason: String = "Ajuste manual") {
        val state = _uiState.value
        if (state.gameState.reserveTeams.contains(teamId)) return
        if (!state.isLocalGame) {
            val isTwoPlayers = state.gameState.maxPlayers == 2 || state.maxPlayers == 2
            val effectiveMyTeam = when {
                isTwoPlayers && !state.isHost -> Team.TEAM_B
                isTwoPlayers && state.isHost -> Team.TEAM_A
                state.myTeam != Team.SPECTATOR -> state.myTeam
                else -> state.gameState.connectedPlayers.find { it.id == clientUseCase.localPlayerId }?.team
                    ?: state.gameState.connectedPlayers.find { it.name.isNotBlank() && it.name == state.playerName && !it.isHost }?.team
                    ?: if (!state.isHost) Team.TEAM_B else state.myTeam
            }
            if (state.gameState.reserveTeams.contains(effectiveMyTeam) || effectiveMyTeam == Team.RESERVE || effectiveMyTeam == Team.SPECTATOR) return
            if (effectiveMyTeam != teamId) return // En multijugador no se puede modificar el tanteo del equipo rival
        }

        val author = state.playerName
        viewModelScope.launch {
            if (state.isHost) {
                hostUseCase.applyScoreUpdate(teamId, CantoType.MANUAL_ADJUST, delta, reason, author)
            } else {
                clientUseCase.requestManualScoreChange(teamId, delta, reason)
            }
        }
    }

    fun undoLastMove() {
        val state = _uiState.value
        val isTwoPlayers = state.gameState.maxPlayers == 2 || state.maxPlayers == 2
        val effectiveMyTeam = when {
            isTwoPlayers && !state.isHost -> Team.TEAM_B
            isTwoPlayers && state.isHost -> Team.TEAM_A
            state.myTeam != Team.SPECTATOR -> state.myTeam
            else -> state.gameState.connectedPlayers.find { it.id == clientUseCase.localPlayerId }?.team
                ?: state.gameState.connectedPlayers.find { it.name.isNotBlank() && it.name == state.playerName && !it.isHost }?.team
                ?: if (!state.isHost) Team.TEAM_B else state.myTeam
        }
        if (!state.isLocalGame && (state.gameState.reserveTeams.contains(effectiveMyTeam) || effectiveMyTeam == Team.RESERVE || effectiveMyTeam == Team.SPECTATOR)) return

        viewModelScope.launch {
            if (state.isHost) {
                hostUseCase.undoLastMove()
            } else {
                clientUseCase.requestUndoLastMove()
            }
        }
    }

    fun changeDeal(newDeal: Int) {
        val state = _uiState.value
        if (!state.isHost && !state.isLocalGame && (state.gameState.reserveTeams.contains(state.myTeam) || state.myTeam == Team.RESERVE)) return
        val maxP = if (state.gameState.maxPlayers in listOf(2, 3, 4, 6, 8)) {
            state.gameState.maxPlayers
        } else {
            state.maxPlayers
        }
        val maxDeals = getMaxDeals(maxP)
        val targetDeal = if (newDeal > maxDeals || newDeal < 1) 1 else newDeal
        viewModelScope.launch {
            if (state.isHost || state.isLocalGame) {
                hostUseCase.setCurrentDeal(targetDeal)
            } else {
                clientUseCase.requestUpdateDeal(targetDeal)
            }
        }
    }

    fun applyCardCount(cardCounts: Map<Team, Int>) {
        val state = _uiState.value
        val totalDeckCards = if (state.gameState.maxPlayers == 3) 39 else 40
        val totalSum = cardCounts.values.sum()
        if (totalSum < totalDeckCards) {
            // No permitir sumar piedras si el recuento de cartas es insuficiente
            return
        }
        val threshold = if (state.gameState.maxPlayers == 3) 13 else 20
        viewModelScope.launch {
            if (state.isHost || state.isLocalGame) {
                hostUseCase.applyCardCount(cardCounts, state.playerName)
            } else {
                val effectiveMyTeam = if (state.myTeam != Team.SPECTATOR) {
                    state.myTeam
                } else {
                    state.gameState.connectedPlayers.find { it.id == clientUseCase.localPlayerId }?.team ?: state.myTeam
                }
                val myCount = cardCounts[effectiveMyTeam] ?: 0
                val extra = (myCount - threshold).coerceAtLeast(0)
                if (extra > 0) {
                    clientUseCase.requestManualScoreChange(
                        teamId = effectiveMyTeam,
                        deltaPiedras = extra,
                        reason = "Recuento de cartas: $myCount cartas (+$extra)"
                    )
                }
                clientUseCase.requestUpdateDeal(1)
            }
        }
    }

    fun restartHand() {
        val state = _uiState.value
        if (!state.isHost && !state.isLocalGame && (state.gameState.reserveTeams.contains(state.myTeam) || state.myTeam == Team.RESERVE)) return
        viewModelScope.launch {
            if (state.isHost || state.isLocalGame) {
                hostUseCase.restartHand()
            } else {
                clientUseCase.requestUpdateDeal(1)
            }
        }
    }

    fun returnToHostLobby() {
        if (_uiState.value.isHost && !_uiState.value.isLocalGame) {
            viewModelScope.launch {
                hostUseCase.resetGame(resetWins = false, newStatus = GameStatus.WAITING)
            }
            _uiState.update { it.copy(currentScreen = AppScreen.HOST_LOBBY) }
        }
    }

    fun terminateGame() {
        viewModelScope.launch {
            if (_uiState.value.isHost && !_uiState.value.isLocalGame) {
                // En multijugador, terminar la partida vuelve a la sala (HOST_LOBBY) sin cerrarla
                hostUseCase.resetGame(resetWins = false, newStatus = GameStatus.WAITING)
                _uiState.update { it.copy(currentScreen = AppScreen.HOST_LOBBY) }
            } else if (_uiState.value.isLocalGame) {
                // En partida local, reiniciar piedras a 0 manteniendo la sesión activa en el marcador
                hostUseCase.resetGame(resetWins = false, newStatus = GameStatus.PLAYING)
            } else {
                clientUseCase.leaveGame()
                _uiState.update { it.copy(currentScreen = AppScreen.MODE_SELECTION) }
            }
        }
    }

    fun resetGame(resetWins: Boolean = false) {
        lastRecordedGameId = null
        if (_uiState.value.isHost || _uiState.value.isLocalGame) {
            viewModelScope.launch {
                hostUseCase.resetGame(resetWins, newStatus = GameStatus.PLAYING)
            }
        }
    }

    val pendingTeamChangeRequest: StateFlow<TeamChangeRequest?> = hostUseCase.pendingTeamChangeRequest

    fun approveTeamChange(request: TeamChangeRequest) {
        viewModelScope.launch {
            hostUseCase.approveTeamChange(request)
        }
    }

    fun rejectTeamChange() {
        hostUseCase.rejectTeamChange()
    }

    fun switchPlayerTeam(playerId: String, newTeam: Team) {
        val maxP = if (_uiState.value.gameState.maxPlayers in listOf(2, 3, 4, 6, 8)) _uiState.value.gameState.maxPlayers else _uiState.value.maxPlayers
        if (maxP == 2) return
        // En tríos, solo se puede usar suplente antes de contar cualquier piedra
        if (maxP == 3 && _uiState.value.gameState.moveHistory.isNotEmpty()) return
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                hostUseCase.switchPlayerTeam(playerId, newTeam)
            }
        }
    }

    fun switchMyTeam(newTeam: Team) {
        val maxP = if (_uiState.value.gameState.maxPlayers in listOf(2, 3, 4, 6, 8)) _uiState.value.gameState.maxPlayers else _uiState.value.maxPlayers
        if (maxP == 2) return
        // En tríos, solo se puede usar suplente antes de contar cualquier piedra
        if (maxP == 3 && _uiState.value.gameState.moveHistory.isNotEmpty()) return
        viewModelScope.launch {
            if (_uiState.value.isHost) {
                val hostPlayer = _uiState.value.gameState.connectedPlayers.find { it.isHost }
                if (hostPlayer != null) {
                    val targetTeam = if (maxP == 3 && newTeam != Team.RESERVE) Team.TEAM_A else newTeam
                    hostUseCase.switchPlayerTeam(hostPlayer.id, targetTeam)
                    _uiState.update { it.copy(myTeam = targetTeam) }
                } else {
                    _uiState.update { it.copy(myTeam = newTeam) }
                }
            } else if (_uiState.value.isLocalGame) {
                _uiState.update { it.copy(myTeam = newTeam) }
            } else {
                clientUseCase.requestSwitchTeam(newTeam)
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
                Team.TEAM_D -> state.nameTeamD
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
                    Team.TEAM_D -> state.scoreTeamD.totalPiedras
                    else -> 21
                },
                teamAName = state.nameTeamA,
                teamAPiedras = state.scoreTeamA.totalPiedras,
                teamBName = state.nameTeamB,
                teamBPiedras = state.scoreTeamB.totalPiedras,
                teamCName = if (state.maxPlayers in listOf(3, 6, 8)) state.nameTeamC else null,
                teamCPiedras = if (state.maxPlayers in listOf(3, 6, 8)) state.scoreTeamC.totalPiedras else null,
                teamDName = if (state.maxPlayers == 8) state.nameTeamD else null,
                teamDPiedras = if (state.maxPlayers == 8) state.scoreTeamD.totalPiedras else null,
                maxPlayers = state.maxPlayers
            )
            historyRepository.saveGame(record)
            lastRecordedGameId = state.gameId
            localPersistence.clearLocalGame()
        }
    }

    fun exitGame() {
        localPersistence.clearLocalGame()
        lastRecordedGameId = null
        if (_uiState.value.isHost) {
            hostUseCase.stopHost()
        } else {
            clientUseCase.leaveGame()
        }
        _uiState.update {
            ScoreUiState(
                currentScreen = AppScreen.MODE_SELECTION,
                playerName = it.playerName,
                isMusicEnabled = audioPlayer.isMusicEnabled,
                isSfxEnabled = audioPlayer.isSfxEnabled,
                isVibrationEnabled = audioPlayer.isVibrationEnabled,
                masterVolume = audioPlayer.masterVolume,
                musicVolume = audioPlayer.musicVolume,
                sfxVolume = audioPlayer.sfxVolume,
                isTvAudioOptimizationEnabled = audioPlayer.isTvAudioOptimizationEnabled,
                isTvCastingActive = audioPlayer.isTvCastingActive
            )
        }
    }

    fun pauseBackgroundMusic() {
        audioPlayer.pauseBackgroundMusic()
    }

    fun resumeBackgroundMusic() {
        audioPlayer.resumeBackgroundMusic()
    }

    fun toggleMusic(enabled: Boolean) {
        audioPlayer.isMusicEnabled = enabled
        _uiState.update { it.copy(isMusicEnabled = enabled) }
    }

    fun toggleSfx(enabled: Boolean) {
        audioPlayer.isSfxEnabled = enabled
        _uiState.update { it.copy(isSfxEnabled = enabled) }
    }

    fun toggleVibration(enabled: Boolean) {
        audioPlayer.isVibrationEnabled = enabled
        _uiState.update { it.copy(isVibrationEnabled = enabled) }
    }

    fun setMasterVolume(volume: Float) {
        audioPlayer.masterVolume = volume
        _uiState.update { it.copy(masterVolume = volume) }
    }

    fun setMusicVolume(volume: Float) {
        audioPlayer.musicVolume = volume
        _uiState.update { it.copy(musicVolume = volume) }
    }

    fun setSfxVolume(volume: Float) {
        audioPlayer.sfxVolume = volume
        _uiState.update { it.copy(sfxVolume = volume) }
    }

    fun setTvAudioOptimizationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isTvAudioOptimizationEnabled = true) }
    }

    fun setTvCastingActive(active: Boolean) {
        audioPlayer.isTvCastingActive = active
        _uiState.update { it.copy(isTvCastingActive = active) }
    }

    override fun onCleared() {
        super.onCleared()
        hostUseCase.stopHost()
        clientUseCase.leaveGame()
        audioPlayer.release()
    }
}
