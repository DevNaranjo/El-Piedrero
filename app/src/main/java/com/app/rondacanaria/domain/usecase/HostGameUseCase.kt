package com.app.rondacanaria.domain.usecase

import com.app.rondacanaria.data.model.*
import com.app.rondacanaria.data.network.NetworkUtils
import com.app.rondacanaria.data.network.ServerEvent
import com.app.rondacanaria.data.network.SocketServer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class HostGameUseCase(
    private val socketServer: SocketServer = SocketServer()
) {
    private var useCaseScope: CoroutineScope? = null
    private val stateMutex = Mutex()

    private val _gameState = MutableStateFlow(GameState(gameId = UUID.randomUUID().toString()))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _connectedClients = MutableStateFlow<Map<String, Player>>(emptyMap())
    val connectedClients: StateFlow<Map<String, Player>> = _connectedClients.asStateFlow()

    private val _soundEvents = MutableSharedFlow<SoundTriggerPayload>(extraBufferCapacity = 16)
    val soundEvents: SharedFlow<SoundTriggerPayload> = _soundEvents.asSharedFlow()

    private val _isHostRunning = MutableStateFlow(false)
    val isHostRunning: StateFlow<Boolean> = _isHostRunning.asStateFlow()

    fun startHost(
        hostPlayerName: String,
        teamAName: String = "Equipo A",
        teamBName: String = "Equipo B",
        teamCName: String = "Equipo C",
        maxPlayers: Int = 4,
        port: Int = NetworkUtils.DEFAULT_PORT
    ) {
        if (_isHostRunning.value) {
            stopHost()
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        useCaseScope = scope

        val hostPlayer = Player(
            id = UUID.randomUUID().toString(),
            name = hostPlayerName,
            team = Team.TEAM_A,
            isHost = true
        )

        _gameState.value = GameState(
            gameId = UUID.randomUUID().toString(),
            nameTeamA = teamAName,
            nameTeamB = teamBName,
            nameTeamC = teamCName,
            scoreTeamA = TeamScore.calculate(0),
            scoreTeamB = TeamScore.calculate(0),
            scoreTeamC = TeamScore.calculate(0),
            maxPlayers = maxPlayers,
            status = GameStatus.PLAYING,
            winnerTeam = null,
            version = 1L,
            connectedPlayers = listOf(hostPlayer)
        )

        socketServer.start(port)
        _isHostRunning.value = true

        scope.launch {
            socketServer.serverEvents.collect { event ->
                handleServerEvent(event)
            }
        }
    }

    private suspend fun handleServerEvent(event: ServerEvent) {
        when (event) {
            is ServerEvent.ClientConnected -> {}
            is ServerEvent.ClientDisconnected -> {
                handleClientDisconnected(event.clientId)
            }
            is ServerEvent.MessageReceived -> {
                handleClientMessage(event.clientId, event.message)
            }
            is ServerEvent.Error -> {}
        }
    }

    private suspend fun handleClientMessage(clientId: String, envelope: NetworkEnvelope) {
        when (envelope.type) {
            MessageType.JOIN_REQUEST -> {
                val joinReq = envelope.joinRequest ?: return
                val currentPlayers = _gameState.value.connectedPlayers

                // Validar cupo máximo de jugadores (2 a 4)
                if (currentPlayers.size >= _gameState.value.maxPlayers) {
                    val reject = NetworkEnvelope(
                        type = MessageType.JOIN_RESPONSE,
                        senderId = "HOST",
                        joinResponse = JoinResponsePayload(
                            accepted = false,
                            assignedTeam = Team.SPECTATOR,
                            errorMessage = "La sala está completa (${_gameState.value.maxPlayers} jugadores)."
                        )
                    )
                    socketServer.sendToClient(clientId, reject)
                    return
                }

                val newPlayer = Player(
                    id = envelope.senderId,
                    name = joinReq.playerName,
                    team = assignBalancedTeam(),
                    isHost = false
                )

                stateMutex.withLock {
                    val updatedClients = _connectedClients.value.toMutableMap()
                    updatedClients[clientId] = newPlayer
                    _connectedClients.value = updatedClients

                    val updatedPlayers = (currentPlayers.filter { it.id != newPlayer.id } + newPlayer)
                    _gameState.value = _gameState.value.copy(
                        version = _gameState.value.version + 1,
                        connectedPlayers = updatedPlayers
                    )
                }

                val response = NetworkEnvelope(
                    type = MessageType.JOIN_RESPONSE,
                    senderId = "HOST",
                    joinResponse = JoinResponsePayload(
                        accepted = true,
                        assignedTeam = newPlayer.team,
                        gameState = _gameState.value
                    )
                )
                socketServer.sendToClient(clientId, response)
                broadcastCurrentState()
            }

            MessageType.ROOM_CONFIG_UPDATE -> {
                val config = envelope.roomConfigUpdate ?: return
                updateRoomConfig(config.teamAName, config.teamBName, config.teamCName, config.maxPlayers)
            }

            MessageType.SCORE_UPDATE -> {
                val scoreUpdate = envelope.scoreUpdate ?: return
                val senderPlayer = _connectedClients.value[clientId]

                // Regla de seguridad multijugador: Los clientes no pueden modificar el tanteo de equipos contrarios
                if (senderPlayer != null && senderPlayer.team != scoreUpdate.teamId) {
                    return
                }

                // Regla de seguridad de puntuación: Validar que las piedras correspondan a la jugada oficial
                val validPiedras = when (val canto = scoreUpdate.cantoType) {
                    null, CantoType.MANUAL_ADJUST -> {
                        if (scoreUpdate.piedras in listOf(-1, 1)) scoreUpdate.piedras else return
                    }
                    else -> canto.defaultPiedras
                }

                applyScoreUpdate(scoreUpdate.teamId, scoreUpdate.cantoType, validPiedras, scoreUpdate.reason)
            }

            MessageType.END_GAME -> {
                val reason = envelope.endGame?.reason ?: "Partida finalizada"
                endGame(reason)
            }

            MessageType.HEARTBEAT_PING -> {
                val pong = NetworkEnvelope(
                    type = MessageType.HEARTBEAT_PONG,
                    senderId = "HOST"
                )
                socketServer.sendToClient(clientId, pong)
            }

            else -> {}
        }
    }

    private suspend fun handleClientDisconnected(clientId: String) {
        stateMutex.withLock {
            val disconnectedPlayer = _connectedClients.value[clientId] ?: return@withLock
            val updatedClients = _connectedClients.value.toMutableMap()
            updatedClients.remove(clientId)
            _connectedClients.value = updatedClients

            val updatedPlayers = _gameState.value.connectedPlayers.filter { it.id != disconnectedPlayer.id }
            _gameState.value = _gameState.value.copy(
                version = _gameState.value.version + 1,
                connectedPlayers = updatedPlayers
            )
        }
        broadcastCurrentState()
    }

    /**
     * Autoridad del Host: Aplica jugadas de cantos o ajustes manuales,
     * evalúa el paso a Buenas y dispara los avisos sonoros correspondientes.
     */
    suspend fun applyScoreUpdate(teamId: Team, cantoType: CantoType?, piedras: Int, reason: String = "") {
        var soundToTrigger: SoundTriggerPayload? = null

        stateMutex.withLock {
            val current = _gameState.value
            val oldScoreA = current.scoreTeamA
            val oldScoreB = current.scoreTeamB
            val oldScoreC = current.scoreTeamC

            val newTotalA = if (teamId == Team.TEAM_A) (oldScoreA.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreA.totalPiedras
            val newTotalB = if (teamId == Team.TEAM_B) (oldScoreB.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreB.totalPiedras
            val newTotalC = if (teamId == Team.TEAM_C) (oldScoreC.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreC.totalPiedras

            val newScoreA = TeamScore.calculate(newTotalA)
            val newScoreB = TeamScore.calculate(newTotalB)
            val newScoreC = TeamScore.calculate(newTotalC)

            // Detección de paso a "Buenas" (cruce del umbral de 11 malas)
            if (teamId == Team.TEAM_A && !oldScoreA.isInBuenas && newScoreA.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_A)
            } else if (teamId == Team.TEAM_B && !oldScoreB.isInBuenas && newScoreB.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_B)
            } else if (teamId == Team.TEAM_C && !oldScoreC.isInBuenas && newScoreC.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_C)
            }

            val isWinnerA = newScoreA.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY
            val isWinnerB = newScoreB.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY
            val isWinnerC = newScoreC.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY

            val newStatus = if (isWinnerA || isWinnerB || isWinnerC) GameStatus.FINISHED else current.status
            val winner = when {
                isWinnerA -> Team.TEAM_A
                isWinnerB -> Team.TEAM_B
                isWinnerC -> Team.TEAM_C
                else -> null
            }

            if (winner != null) {
                soundToTrigger = SoundTriggerPayload(SoundType.GAME_WON, winner)
            }

            _gameState.value = current.copy(
                scoreTeamA = newScoreA,
                scoreTeamB = newScoreB,
                scoreTeamC = newScoreC,
                status = newStatus,
                winnerTeam = winner,
                version = current.version + 1
            )
        }

        // 1. Emitir aviso sonoro del canto si corresponde
        if (cantoType != null && cantoType != CantoType.MANUAL_ADJUST) {
            val cantoPayload = SoundTriggerPayload(cantoType.soundType, teamId)
            _soundEvents.emit(cantoPayload)
            val cantoEnvelope = NetworkEnvelope(
                type = MessageType.SOUND_TRIGGER,
                senderId = "HOST",
                soundTrigger = cantoPayload
            )
            socketServer.broadcast(cantoEnvelope)
        }

        // 2. Emitir aviso de Buenas o Victoria si ocurrió
        soundToTrigger?.let { soundPayload ->
            if (cantoType != null && cantoType != CantoType.MANUAL_ADJUST) {
                delay(600L) // Breve pausa para que se distingan ambos audios
            }
            _soundEvents.emit(soundPayload)
            val soundEnvelope = NetworkEnvelope(
                type = MessageType.SOUND_TRIGGER,
                senderId = "HOST",
                soundTrigger = soundPayload
            )
            socketServer.broadcast(soundEnvelope)
        }

        broadcastCurrentState()
    }

    suspend fun updateRoomConfig(teamAName: String?, teamBName: String?, teamCName: String? = null, maxPlayers: Int?) {
        stateMutex.withLock {
            val current = _gameState.value
            _gameState.value = current.copy(
                nameTeamA = teamAName ?: current.nameTeamA,
                nameTeamB = teamBName ?: current.nameTeamB,
                nameTeamC = teamCName ?: current.nameTeamC,
                maxPlayers = maxPlayers ?: current.maxPlayers,
                version = current.version + 1
            )
        }
        broadcastCurrentState()
    }

    suspend fun endGame(reason: String = "Partida finalizada manualmente") {
        stateMutex.withLock {
            val current = _gameState.value
            _gameState.value = current.copy(
                status = GameStatus.FINISHED,
                version = current.version + 1
            )
        }
        val endEnvelope = NetworkEnvelope(
            type = MessageType.END_GAME,
            senderId = "HOST",
            endGame = EndGamePayload(reason = reason)
        )
        socketServer.broadcast(endEnvelope)
        broadcastCurrentState()
    }

    suspend fun resetGame() {
        stateMutex.withLock {
            val current = _gameState.value
            _gameState.value = current.copy(
                scoreTeamA = TeamScore.calculate(0),
                scoreTeamB = TeamScore.calculate(0),
                scoreTeamC = TeamScore.calculate(0),
                status = GameStatus.PLAYING,
                winnerTeam = null,
                version = current.version + 1
            )
        }
        broadcastCurrentState()
    }

    private suspend fun broadcastCurrentState() {
        val envelope = NetworkEnvelope(
            type = MessageType.GAME_STATE_BROADCAST,
            senderId = "HOST",
            gameStateBroadcast = _gameState.value
        )
        socketServer.broadcast(envelope)
    }

    private fun assignBalancedTeam(): Team {
        val currentPlayers = _gameState.value.connectedPlayers
        val countA = currentPlayers.count { it.team == Team.TEAM_A }
        val countB = currentPlayers.count { it.team == Team.TEAM_B }

        if (_gameState.value.maxPlayers == 3) {
            val countC = currentPlayers.count { it.team == Team.TEAM_C }
            return when {
                countA <= countB && countA <= countC -> Team.TEAM_A
                countB <= countA && countB <= countC -> Team.TEAM_B
                else -> Team.TEAM_C
            }
        }

        return if (countA <= countB) Team.TEAM_A else Team.TEAM_B
    }

    fun stopHost() {
        _isHostRunning.value = false
        socketServer.stop()
        useCaseScope?.cancel()
        useCaseScope = null
        _connectedClients.value = emptyMap()
    }
}
