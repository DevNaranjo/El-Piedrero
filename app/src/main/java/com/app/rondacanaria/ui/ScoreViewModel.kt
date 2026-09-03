package com.app.rondacanaria.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.rondacanaria.data.audio.RondaAudioPlayer
import com.app.rondacanaria.data.history.GameHistoryRepository
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
    val errorMessage: String? = null,
    val isMusicEnabled: Boolean = true,
    val isSfxEnabled: Boolean = true,
    val masterVolume: Float = 1.0f,
    val musicVolume: Float = 0.5f,
    val sfxVolume: Float = 1.0f
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

    private val _uiState = MutableStateFlow(
        ScoreUiState(
            isMusicEnabled = audioPlayer.isMusicEnabled,
            isSfxEnabled = audioPlayer.isSfxEnabled,
            masterVolume = audioPlayer.masterVolume,
            musicVolume = audioPlayer.musicVolume,
            sfxVolume = audioPlayer.sfxVolume
        )
    )
    val uiState: StateFlow<ScoreUiState> = _uiState.asStateFlow()

    init {
        // Observar estado del Host
        viewModelScope.launch {
            hostUseCase.gameState.collect { state ->
                if (_uiState.value.isHost) {
                    _uiState.update { it.copy(gameState = state, maxPlayers = state.maxPlayers) }
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

        // Observar errores del Cliente
        viewModelScope.launch {
            clientUseCase.errorMessage.collect { err ->
                if (!_uiState.value.isHost) {
                    _uiState.update { it.copy(errorMessage = err) }
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

    fun startLocalGame(
        teamA: String,
        teamB: String,
        teamC: String = "Equipo C",
        teamD: String = "Equipo D",
        maxPlayers: Int,
        reserveTeams: List<Team> = if (maxPlayers == 6) listOf(Team.TEAM_C) else if (maxPlayers == 8) listOf(Team.TEAM_C, Team.TEAM_D) else emptyList()
    ) {
        val effectiveReserves = when {
            maxPlayers == 8 -> if (reserveTeams.size == 2) reserveTeams else listOf(Team.TEAM_C, Team.TEAM_D)
            maxPlayers == 6 -> if (reserveTeams.isNotEmpty()) reserveTeams else listOf(Team.TEAM_C)
            else -> reserveTeams
        }
        val state = _uiState.value
        hostUseCase.startHost(
            hostPlayerName = state.playerName,
            teamAName = teamA,
            teamBName = teamB,
            teamCName = teamC,
            teamDName = teamD,
            maxPlayers = maxPlayers,
            reserveTeams = effectiveReserves
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
            playerName = name,
            roomToken = info.roomToken,
            encryptionKey = info.secretKey
        )
    }

    fun navigateToScoreboard() {
        _uiState.update { it.copy(currentScreen = AppScreen.SCOREBOARD) }
    }

    fun callCanto(teamId: Team, canto: CantoType) {
        val state = _uiState.value
        if (state.gameState.reserveTeams.contains(teamId)) return
        if (!state.isLocalGame) {
            if (state.gameState.reserveTeams.contains(state.myTeam) || state.myTeam == Team.RESERVE || state.myTeam == Team.SPECTATOR) return
            if (state.myTeam != teamId) return // En multijugador no se puede cantar para el equipo rival
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
            if (state.gameState.reserveTeams.contains(state.myTeam) || state.myTeam == Team.RESERVE || state.myTeam == Team.SPECTATOR) return
            if (state.myTeam != teamId) return // En multijugador no se puede modificar el tanteo del equipo rival
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
        if (!state.isLocalGame && (state.gameState.reserveTeams.contains(state.myTeam) || state.myTeam == Team.RESERVE || state.myTeam == Team.SPECTATOR)) return

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

    fun terminateGame() {
        viewModelScope.launch {
            if (_uiState.value.isHost || _uiState.value.isLocalGame) {
                hostUseCase.endGame("Partida finalizada por el Host")
            } else {
                clientUseCase.leaveGame()
            }
            exitGame()
        }
    }

    fun resetGame(resetWins: Boolean = false) {
        lastRecordedGameId = null
        if (_uiState.value.isHost) {
            viewModelScope.launch {
                hostUseCase.resetGame(resetWins)
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
                playerName = it.playerName,
                isMusicEnabled = audioPlayer.isMusicEnabled,
                isSfxEnabled = audioPlayer.isSfxEnabled,
                masterVolume = audioPlayer.masterVolume,
                musicVolume = audioPlayer.musicVolume,
                sfxVolume = audioPlayer.sfxVolume
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

    override fun onCleared() {
        super.onCleared()
        hostUseCase.stopHost()
        clientUseCase.leaveGame()
        audioPlayer.release()
    }
}
