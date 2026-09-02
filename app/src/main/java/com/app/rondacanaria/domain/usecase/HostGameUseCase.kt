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

data class TeamChangeRequest(
    val playerId: String,
    val playerName: String,
    val targetTeam: Team
)

class HostGameUseCase(
    private val socketServer: SocketServer = SocketServer()
) {
    private var useCaseScope: CoroutineScope? = null
    private val stateMutex = Mutex()

    private val _gameState = MutableStateFlow(GameState(gameId = UUID.randomUUID().toString()))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _connectedClients = MutableStateFlow<Map<String, Player>>(emptyMap())
    val connectedClients: StateFlow<Map<String, Player>> = _connectedClients.asStateFlow()

    private val _pendingTeamChangeRequest = MutableStateFlow<TeamChangeRequest?>(null)
    val pendingTeamChangeRequest: StateFlow<TeamChangeRequest?> = _pendingTeamChangeRequest.asStateFlow()

    private val _soundEvents = MutableSharedFlow<SoundTriggerPayload>(extraBufferCapacity = 16)
    val soundEvents: SharedFlow<SoundTriggerPayload> = _soundEvents.asSharedFlow()

    private val _isHostRunning = MutableStateFlow(false)
    val isHostRunning: StateFlow<Boolean> = _isHostRunning.asStateFlow()

    private var currentRoomToken: String = ""
    private var currentEncryptionKey: String = ""

    val roomToken: String get() = currentRoomToken
    val encryptionKey: String get() = currentEncryptionKey

    fun startHost(
        hostPlayerName: String,
        teamAName: String = "Equipo A",
        teamBName: String = "Equipo B",
        teamCName: String = "Equipo C",
        teamDName: String = "Equipo D",
        maxPlayers: Int = 4,
        port: Int = NetworkUtils.DEFAULT_PORT,
        reserveTeams: List<Team> = if (maxPlayers == 6) listOf(Team.TEAM_C) else if (maxPlayers == 8) listOf(Team.TEAM_C, Team.TEAM_D) else emptyList()
    ) {
        if (_isHostRunning.value) {
            stopHost()
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        useCaseScope = scope

        currentRoomToken = UUID.randomUUID().toString().take(8)
        currentEncryptionKey = com.app.rondacanaria.data.network.crypto.RondaCipher.generateKey()
        socketServer.setEncryptionKey(currentEncryptionKey)

        val hostPlayer = Player(
            id = UUID.randomUUID().toString(),
            name = hostPlayerName,
            team = Team.TEAM_A,
            isHost = true
        )

        // Para 2 y 3 jugadores:
        // - Si el nombre de equipo ya viene relleno (local), usarlo tal cual.
        // - Si viene vacío o igual al placeholder (multijugador), el host ocupa el slot A con su alias;
        //   B y C se actualizarán cuando los clientes se conecten.
        val actualTeamAName = when {
            maxPlayers !in listOf(2, 3) -> teamAName
            teamAName.isNotBlank() && teamAName != "Jugador 1" -> teamAName
            else -> hostPlayerName.ifBlank { "Jugador 1" }
        }
        val actualTeamBName = when {
            maxPlayers !in listOf(2, 3) -> teamBName
            teamBName.isNotBlank() && teamBName != "Jugador 2" -> teamBName
            else -> "Jugador 2"
        }
        val actualTeamCName = when {
            maxPlayers != 3 -> teamCName
            teamCName.isNotBlank() && teamCName != "Jugador 3" -> teamCName
            else -> "Jugador 3"
        }

        val actualReserveTeams = when {
            maxPlayers == 8 -> if (reserveTeams.size == 2) reserveTeams else listOf(Team.TEAM_C, Team.TEAM_D)
            maxPlayers == 6 -> if (reserveTeams.isNotEmpty()) reserveTeams else listOf(Team.TEAM_C)
            maxPlayers == 2 -> emptyList()
            else -> reserveTeams
        }

        _gameState.value = GameState(
            gameId = UUID.randomUUID().toString(),
            nameTeamA = actualTeamAName,
            nameTeamB = actualTeamBName,
            nameTeamC = actualTeamCName,
            nameTeamD = teamDName,
            scoreTeamA = TeamScore.calculate(0),
            scoreTeamB = TeamScore.calculate(0),
            scoreTeamC = TeamScore.calculate(0),
            scoreTeamD = TeamScore.calculate(0),
            winsTeamA = 0,
            winsTeamB = 0,
            winsTeamC = 0,
            winsTeamD = 0,
            maxPlayers = maxPlayers,
            status = GameStatus.PLAYING,
            winnerTeam = null,
            version = 1L,
            connectedPlayers = listOf(hostPlayer),
            reserveTeams = actualReserveTeams
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

                // Validación de seguridad: Token de autenticación de sala obligatorio
                if (currentRoomToken.isNotBlank() && joinReq.roomToken != currentRoomToken) {
                    val reject = NetworkEnvelope(
                        type = MessageType.JOIN_RESPONSE,
                        senderId = "HOST",
                        joinResponse = JoinResponsePayload(
                            accepted = false,
                            assignedTeam = Team.SPECTATOR,
                            errorMessage = "Token de seguridad de sala inválido."
                        )
                    )
                    socketServer.sendToClient(clientId, reject)
                    socketServer.disconnectClient(clientId)
                    return
                }

                val joinResult: Pair<Boolean, Player?> = stateMutex.withLock {
                    val currentPlayers = _gameState.value.connectedPlayers
                    val maxCapacity = _gameState.value.maxPlayers
                    val existingPlayer = currentPlayers.find { it.id == envelope.senderId }

                    if (existingPlayer == null && currentPlayers.size >= maxCapacity) {
                        Pair(false, null)
                    } else {
                        val player = existingPlayer?.copy(name = joinReq.playerName) ?: Player(
                            id = envelope.senderId,
                            name = joinReq.playerName,
                            team = assignBalancedTeam(),
                            isHost = false
                        )

                        val updatedClients = _connectedClients.value.toMutableMap()
                        updatedClients[clientId] = player
                        _connectedClients.value = updatedClients

                        val updatedPlayers = if (existingPlayer != null) {
                            currentPlayers.map { if (it.id == player.id) player else it }
                        } else {
                            currentPlayers + player
                        }

                        val updatedTeamAName = if (maxCapacity in listOf(2, 3)) {
                            updatedPlayers.firstOrNull { it.team == Team.TEAM_A || (it.team == Team.RESERVE && updatedPlayers.indexOf(it) == 0) }?.name ?: _gameState.value.nameTeamA
                        } else {
                            _gameState.value.nameTeamA
                        }
                        val updatedTeamBName = if (maxCapacity in listOf(2, 3)) {
                            updatedPlayers.firstOrNull { it.team == Team.TEAM_B || (it.team == Team.RESERVE && updatedPlayers.indexOf(it) == 1) }?.name ?: _gameState.value.nameTeamB
                        } else {
                            _gameState.value.nameTeamB
                        }
                        val updatedTeamCName = if (maxCapacity == 3) {
                            updatedPlayers.firstOrNull { it.team == Team.TEAM_C || (it.team == Team.RESERVE && updatedPlayers.indexOf(it) == 2) }?.name ?: _gameState.value.nameTeamC
                        } else {
                            _gameState.value.nameTeamC
                        }

                        _gameState.value = _gameState.value.copy(
                            version = _gameState.value.version + 1,
                            nameTeamA = updatedTeamAName,
                            nameTeamB = updatedTeamBName,
                            nameTeamC = updatedTeamCName,
                            connectedPlayers = updatedPlayers
                        )

                        Pair(true, player)
                    }
                }

                if (!joinResult.first || joinResult.second == null) {
                    val maxCapacity = _gameState.value.maxPlayers
                    val reject = NetworkEnvelope(
                        type = MessageType.JOIN_RESPONSE,
                        senderId = "HOST",
                        joinResponse = JoinResponsePayload(
                            accepted = false,
                            assignedTeam = Team.SPECTATOR,
                            errorMessage = "La sala está completa ($maxCapacity jugadores máximos)."
                        )
                    )
                    socketServer.sendToClient(clientId, reject)
                    socketServer.disconnectClient(clientId)
                    return
                }

                val newPlayer = joinResult.second!!
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
                // Medida de seguridad: La configuración de la sala sólo puede ser modificada localmente por el Host.
                // Se descarta cualquier paquete recibido por socket remoto para evitar manipulación no autorizada.
                android.util.Log.w("HostGameUseCase", "Intento no autorizado de ROOM_CONFIG_UPDATE desde cliente: $clientId")
                return
            }

            MessageType.SWITCH_TEAM -> {
                val maxP = _gameState.value.maxPlayers
                if (maxP == 2) return
                val switchPayload = envelope.switchTeam ?: return
                val player = _gameState.value.connectedPlayers.find { it.id == switchPayload.playerId }
                if (maxP == 3) {
                    val playerIndex = _gameState.value.connectedPlayers.indexOfFirst { it.id == switchPayload.playerId }
                    val origTeam = when (playerIndex) {
                        0 -> Team.TEAM_A
                        1 -> Team.TEAM_B
                        2 -> Team.TEAM_C
                        else -> Team.TEAM_A
                    }
                    if (switchPayload.targetTeam != Team.RESERVE && switchPayload.targetTeam != origTeam) {
                        return
                    }
                }
                val playerName = switchPayload.playerName.ifBlank { player?.name ?: "Un jugador" }
                _pendingTeamChangeRequest.value = TeamChangeRequest(
                    playerId = switchPayload.playerId,
                    playerName = playerName,
                    targetTeam = switchPayload.targetTeam
                )
            }

            MessageType.SCORE_UPDATE -> {
                val scoreUpdate = envelope.scoreUpdate ?: return
                val senderPlayer = _connectedClients.value[clientId]

                // Regla de seguridad multijugador: Los clientes no pueden modificar el tanteo de equipos contrarios ni los reservas
                if (senderPlayer != null && (senderPlayer.team != scoreUpdate.teamId || _gameState.value.reserveTeams.contains(senderPlayer.team) || senderPlayer.team == Team.RESERVE)) {
                    return
                }

                // Los equipos en reserva no pueden recibir puntuación mientras estén en reserva
                if (_gameState.value.reserveTeams.contains(scoreUpdate.teamId)) {
                    return
                }

                // Regla de seguridad de puntuación: Validar que las piedras correspondan a la jugada oficial o ajuste dentro del rango [-21, 21]
                val validPiedras = when (val canto = scoreUpdate.cantoType) {
                    null, CantoType.MANUAL_ADJUST -> {
                        if (scoreUpdate.piedras in -21..21 && scoreUpdate.piedras != 0) scoreUpdate.piedras else return
                    }
                    else -> canto.defaultPiedras
                }

                applyScoreUpdate(scoreUpdate.teamId, scoreUpdate.cantoType, validPiedras, scoreUpdate.reason, senderPlayer?.name)
            }

            MessageType.UNDO_LAST_MOVE -> {
                val senderPlayer = _connectedClients.value[clientId]
                if (senderPlayer != null && (_gameState.value.reserveTeams.contains(senderPlayer.team) || senderPlayer.team == Team.RESERVE)) {
                    return
                }
                undoLastMove()
            }

            MessageType.END_GAME -> {
                // Medida de seguridad: La finalización de la partida para toda la sala está reservada al Host.
                // Si un cliente envía END_GAME, se procesa como salida individual del cliente sin cerrar la mesa.
                android.util.Log.w("HostGameUseCase", "Cliente $clientId envió END_GAME. Desconectando cliente sin cerrar la sala.")
                socketServer.disconnectClient(clientId)
                return
            }

            MessageType.HEARTBEAT_PING -> {
                val pong = NetworkEnvelope(
                    type = MessageType.HEARTBEAT_PONG,
                    senderId = "HOST"
                )
                socketServer.sendToClient(clientId, pong)
            }

            MessageType.UPDATE_DEAL -> {
                val senderPlayer = _connectedClients.value[clientId]
                if (senderPlayer != null && (_gameState.value.reserveTeams.contains(senderPlayer.team) || senderPlayer.team == Team.RESERVE)) {
                    return
                }
                val newDeal = envelope.updateDeal?.dealNumber ?: return
                setCurrentDeal(newDeal)
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
    suspend fun applyScoreUpdate(teamId: Team, cantoType: CantoType?, piedras: Int, reason: String = "", authorName: String? = null) {
        var soundToTrigger: SoundTriggerPayload? = null

        stateMutex.withLock {
            val current = _gameState.value
            val oldScoreA = current.scoreTeamA
            val oldScoreB = current.scoreTeamB
            val oldScoreC = current.scoreTeamC
            val oldScoreD = current.scoreTeamD

            val newTotalA = if (teamId == Team.TEAM_A) (oldScoreA.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreA.totalPiedras
            val newTotalB = if (teamId == Team.TEAM_B) (oldScoreB.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreB.totalPiedras
            val newTotalC = if (teamId == Team.TEAM_C) (oldScoreC.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreC.totalPiedras
            val newTotalD = if (teamId == Team.TEAM_D) (oldScoreD.totalPiedras + piedras).coerceIn(0, TeamScore.TOTAL_PIEDRAS_VICTORY) else oldScoreD.totalPiedras

            val newScoreA = TeamScore.calculate(newTotalA)
            val newScoreB = TeamScore.calculate(newTotalB)
            val newScoreC = TeamScore.calculate(newTotalC)
            val newScoreD = TeamScore.calculate(newTotalD)

            val oldTotal = when (teamId) {
                Team.TEAM_A -> oldScoreA.totalPiedras
                Team.TEAM_B -> oldScoreB.totalPiedras
                Team.TEAM_C -> oldScoreC.totalPiedras
                Team.TEAM_D -> oldScoreD.totalPiedras
                else -> 0
            }
            val newTotal = when (teamId) {
                Team.TEAM_A -> newTotalA
                Team.TEAM_B -> newTotalB
                Team.TEAM_C -> newTotalC
                Team.TEAM_D -> newTotalD
                else -> 0
            }
            val effectiveDelta = newTotal - oldTotal

            val updatedHistory = if (effectiveDelta != 0) {
                val moveDesc = when {
                    reason.isNotBlank() -> reason
                    cantoType != null -> cantoType.displayName
                    effectiveDelta > 0 -> "+$effectiveDelta piedras"
                    else -> "$effectiveDelta piedras"
                }
                current.moveHistory + GameMove(
                    teamId = teamId,
                    deltaPiedras = effectiveDelta,
                    reason = moveDesc,
                    previousTotalPiedras = oldTotal,
                    newTotalPiedras = newTotal,
                    authorName = authorName,
                    previousReserveTeams = current.reserveTeams,
                    dealNumber = current.currentDeal
                )
            } else {
                current.moveHistory
            }

            // Detección de paso a "Buenas" (cruce del umbral de 11 malas)
            if (teamId == Team.TEAM_A && !oldScoreA.isInBuenas && newScoreA.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_A)
            } else if (teamId == Team.TEAM_B && !oldScoreB.isInBuenas && newScoreB.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_B)
            } else if (teamId == Team.TEAM_C && !oldScoreC.isInBuenas && newScoreC.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_C)
            } else if (teamId == Team.TEAM_D && !oldScoreD.isInBuenas && newScoreD.isInBuenas) {
                soundToTrigger = SoundTriggerPayload(SoundType.ENTERED_BUENAS, Team.TEAM_D)
            }

            val isWinnerA = newScoreA.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY
            val isWinnerB = newScoreB.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY
            val isWinnerC = newScoreC.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY
            val isWinnerD = newScoreD.totalPiedras >= TeamScore.TOTAL_PIEDRAS_VICTORY

            val newStatus = if (isWinnerA || isWinnerB || isWinnerC || isWinnerD) GameStatus.FINISHED else current.status
            val winner = when {
                isWinnerA -> Team.TEAM_A
                isWinnerB -> Team.TEAM_B
                isWinnerC -> Team.TEAM_C
                isWinnerD -> Team.TEAM_D
                else -> null
            }

            if (winner != null) {
                soundToTrigger = SoundTriggerPayload(SoundType.GAME_WON, winner)
            }

            val newWinsA = if (isWinnerA && current.winnerTeam == null) current.winsTeamA + 1 else current.winsTeamA
            val newWinsB = if (isWinnerB && current.winnerTeam == null) current.winsTeamB + 1 else current.winsTeamB
            val newWinsC = if (isWinnerC && current.winnerTeam == null) current.winsTeamC + 1 else current.winsTeamC
            val newWinsD = if (isWinnerD && current.winnerTeam == null) current.winsTeamD + 1 else current.winsTeamD

            _gameState.value = current.copy(
                scoreTeamA = newScoreA,
                scoreTeamB = newScoreB,
                scoreTeamC = newScoreC,
                scoreTeamD = newScoreD,
                winsTeamA = newWinsA,
                winsTeamB = newWinsB,
                winsTeamC = newWinsC,
                winsTeamD = newWinsD,
                status = newStatus,
                winnerTeam = winner,
                version = current.version + 1,
                moveHistory = updatedHistory
            )
        }

        // 1. Emitir aviso sonoro del canto o de ajuste de piedras (+ / -)
        if (cantoType != null && cantoType != CantoType.MANUAL_ADJUST) {
            val cantoPayload = SoundTriggerPayload(cantoType.soundType, teamId)
            _soundEvents.emit(cantoPayload)
            val cantoEnvelope = NetworkEnvelope(
                type = MessageType.SOUND_TRIGGER,
                senderId = "HOST",
                soundTrigger = cantoPayload
            )
            socketServer.broadcast(cantoEnvelope)
        } else if (cantoType == CantoType.MANUAL_ADJUST && piedras != 0) {
            val stoneSound = if (piedras > 0) SoundType.PIEDRA_ADD else SoundType.PIEDRA_SUBTRACT
            val stonePayload = SoundTriggerPayload(stoneSound, teamId)
            _soundEvents.emit(stonePayload)
            val stoneEnvelope = NetworkEnvelope(
                type = MessageType.SOUND_TRIGGER,
                senderId = "HOST",
                soundTrigger = stonePayload
            )
            socketServer.broadcast(stoneEnvelope)
        }

        // 2. Emitir aviso de Buenas o Victoria si ocurrió
        soundToTrigger?.let { soundPayload ->
            if (cantoType != null) {
                delay(450L) // Breve pausa para que se distingan ambos audios
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

    suspend fun updateRoomConfig(
        teamAName: String?,
        teamBName: String?,
        teamCName: String? = null,
        teamDName: String? = null,
        maxPlayers: Int?,
        reserveTeams: List<Team>? = null
    ) {
        stateMutex.withLock {
            val current = _gameState.value
            val newMax = maxPlayers ?: current.maxPlayers
            val newReserves = reserveTeams ?: if (current.reserveTeams.isNotEmpty()) current.reserveTeams else {
                if (newMax == 6) listOf(Team.TEAM_C) else if (newMax == 8) listOf(Team.TEAM_C, Team.TEAM_D) else emptyList()
            }
            _gameState.value = current.copy(
                nameTeamA = teamAName ?: current.nameTeamA,
                nameTeamB = teamBName ?: current.nameTeamB,
                nameTeamC = teamCName ?: current.nameTeamC,
                nameTeamD = teamDName ?: current.nameTeamD,
                maxPlayers = newMax,
                reserveTeams = newReserves,
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

    suspend fun resetGame(resetWins: Boolean = false) {
        stateMutex.withLock {
            val current = _gameState.value
            val winner = current.winnerTeam
            val nextReserves = if (winner != null && current.maxPlayers in listOf(6, 8)) {
                val allTeams = when (current.maxPlayers) {
                    6 -> listOf(Team.TEAM_A, Team.TEAM_B, Team.TEAM_C)
                    8 -> listOf(Team.TEAM_A, Team.TEAM_B, Team.TEAM_C, Team.TEAM_D)
                    else -> emptyList()
                }
                val currentRes = if (current.reserveTeams.isNotEmpty()) current.reserveTeams else {
                    if (current.maxPlayers == 6) listOf(Team.TEAM_C) else listOf(Team.TEAM_C, Team.TEAM_D)
                }
                val activeTeams = allTeams - currentRes.toSet()
                val loser = activeTeams.firstOrNull { it != winner }
                if (loser != null) {
                    if (current.maxPlayers == 6) listOf(loser) else (currentRes.drop(1) + loser)
                } else {
                    current.reserveTeams
                }
            } else {
                current.reserveTeams
            }

            _gameState.value = current.copy(
                gameId = UUID.randomUUID().toString(),
                scoreTeamA = TeamScore.calculate(0),
                scoreTeamB = TeamScore.calculate(0),
                scoreTeamC = TeamScore.calculate(0),
                scoreTeamD = TeamScore.calculate(0),
                winsTeamA = if (resetWins) 0 else current.winsTeamA,
                winsTeamB = if (resetWins) 0 else current.winsTeamB,
                winsTeamC = if (resetWins) 0 else current.winsTeamC,
                winsTeamD = if (resetWins) 0 else current.winsTeamD,
                status = GameStatus.PLAYING,
                winnerTeam = null,
                reserveTeams = nextReserves,
                version = current.version + 1,
                moveHistory = emptyList(),
                currentDeal = 1
            )
        }
        broadcastCurrentState()
    }

    suspend fun undoLastMove() {
        stateMutex.withLock {
            val current = _gameState.value
            val lastMove = current.moveHistory.lastOrNull() ?: return@withLock

            val teamId = lastMove.teamId
            val revertedTotal = lastMove.previousTotalPiedras

            val newScoreA = if (teamId == Team.TEAM_A) TeamScore.calculate(revertedTotal) else current.scoreTeamA
            val newScoreB = if (teamId == Team.TEAM_B) TeamScore.calculate(revertedTotal) else current.scoreTeamB
            val newScoreC = if (teamId == Team.TEAM_C) TeamScore.calculate(revertedTotal) else current.scoreTeamC
            val newScoreD = if (teamId == Team.TEAM_D) TeamScore.calculate(revertedTotal) else current.scoreTeamD

            val hadWon = current.winnerTeam == teamId
            val newWinsA = if (hadWon && teamId == Team.TEAM_A) maxOf(0, current.winsTeamA - 1) else current.winsTeamA
            val newWinsB = if (hadWon && teamId == Team.TEAM_B) maxOf(0, current.winsTeamB - 1) else current.winsTeamB
            val newWinsC = if (hadWon && teamId == Team.TEAM_C) maxOf(0, current.winsTeamC - 1) else current.winsTeamC
            val newWinsD = if (hadWon && teamId == Team.TEAM_D) maxOf(0, current.winsTeamD - 1) else current.winsTeamD

            val newStatus = if (hadWon) GameStatus.PLAYING else current.status
            val newWinner = if (hadWon) null else current.winnerTeam

            val restoredReserveTeams = if (hadWon && lastMove.previousReserveTeams.isNotEmpty()) {
                lastMove.previousReserveTeams
            } else {
                current.reserveTeams
            }

            val updatedHistory = current.moveHistory.dropLast(1)

            _gameState.value = current.copy(
                scoreTeamA = newScoreA,
                scoreTeamB = newScoreB,
                scoreTeamC = newScoreC,
                scoreTeamD = newScoreD,
                winsTeamA = newWinsA,
                winsTeamB = newWinsB,
                winsTeamC = newWinsC,
                winsTeamD = newWinsD,
                status = newStatus,
                winnerTeam = newWinner,
                reserveTeams = restoredReserveTeams,
                version = current.version + 1,
                moveHistory = updatedHistory
            )
        }
        broadcastCurrentState()
    }

    suspend fun switchPlayerTeam(playerId: String, newTeam: Team) {
        val maxP = _gameState.value.maxPlayers
        if (maxP == 2) return
        stateMutex.withLock {
            val currentPlayers = _gameState.value.connectedPlayers
            val playerIndex = currentPlayers.indexOfFirst { it.id == playerId }
            val origTeam = when (playerIndex) {
                0 -> Team.TEAM_A
                1 -> Team.TEAM_B
                2 -> Team.TEAM_C
                else -> Team.TEAM_A
            }
            val resolvedTeam = if (maxP == 3 && newTeam != Team.RESERVE) origTeam else newTeam

            val updated = currentPlayers.map {
                if (it.id == playerId) it.copy(team = resolvedTeam) else it
            }

            // En partidas de 3 jugadores, sincronizar reserveTeams cuando alguien pasa a RESERVE o vuelve
            val updatedReserves = if (maxP == 3) {
                val reservedPlayers = updated.filter { it.team == Team.RESERVE }
                reservedPlayers.map { resPlayer ->
                    val idx = currentPlayers.indexOfFirst { it.id == resPlayer.id }
                    when (idx) {
                        0 -> Team.TEAM_A
                        1 -> Team.TEAM_B
                        2 -> Team.TEAM_C
                        else -> Team.TEAM_C
                    }
                }
            } else {
                _gameState.value.reserveTeams
            }

            _gameState.value = _gameState.value.copy(
                version = _gameState.value.version + 1,
                connectedPlayers = updated,
                reserveTeams = updatedReserves
            )
        }
        broadcastCurrentState()
    }

    suspend fun approveTeamChange(request: TeamChangeRequest) {
        switchPlayerTeam(request.playerId, request.targetTeam)
        _pendingTeamChangeRequest.value = null
    }

    fun rejectTeamChange() {
        _pendingTeamChangeRequest.value = null
    }

    suspend fun setReserveTeams(teams: List<Team>) {
        stateMutex.withLock {
            val current = _gameState.value
            val maxP = current.maxPlayers
            val updatedPlayers = if (maxP == 3) {
                current.connectedPlayers.mapIndexed { index, player ->
                    val origTeam = when (index) {
                        0 -> Team.TEAM_A
                        1 -> Team.TEAM_B
                        2 -> Team.TEAM_C
                        else -> Team.TEAM_A
                    }
                    if (teams.contains(origTeam)) {
                        player.copy(team = Team.RESERVE)
                    } else if (player.team == Team.RESERVE) {
                        player.copy(team = origTeam)
                    } else {
                        player
                    }
                }
            } else {
                current.connectedPlayers
            }

            val effectiveTeams = if (maxP == 8 && teams.size != 2) {
                if (current.reserveTeams.size == 2) current.reserveTeams else listOf(Team.TEAM_C, Team.TEAM_D)
            } else {
                teams
            }

            _gameState.value = current.copy(
                reserveTeams = effectiveTeams,
                connectedPlayers = updatedPlayers,
                version = current.version + 1
            )
        }
        broadcastCurrentState()
    }

    suspend fun setCurrentDeal(newDeal: Int) {
        stateMutex.withLock {
            val current = _gameState.value
            val maxDeals = getMaxDeals(current.maxPlayers)
            val effectiveDeal = if (newDeal > maxDeals || newDeal < 1) 1 else newDeal
            if (current.currentDeal != effectiveDeal) {
                _gameState.value = current.copy(
                    currentDeal = effectiveDeal,
                    version = current.version + 1
                )
            }
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
        val countC = currentPlayers.count { it.team == Team.TEAM_C }
        val countD = currentPlayers.count { it.team == Team.TEAM_D }
        val max = _gameState.value.maxPlayers

        val maxPerTeam = when (max) {
            2, 3 -> 1
            else -> 2 // 4: 2x2, 6: 3x2, 8: 4x2
        }

        return when (max) {
            8 -> when {
                countA < maxPerTeam -> Team.TEAM_A
                countB < maxPerTeam -> Team.TEAM_B
                countC < maxPerTeam -> Team.TEAM_C
                countD < maxPerTeam -> Team.TEAM_D
                else -> Team.RESERVE
            }
            3, 6 -> when {
                countA < maxPerTeam && countA <= countB && countA <= countC -> Team.TEAM_A
                countB < maxPerTeam && countB <= countC -> Team.TEAM_B
                countC < maxPerTeam -> Team.TEAM_C
                else -> Team.RESERVE
            }
            else -> when {
                countA < maxPerTeam && countA <= countB -> Team.TEAM_A
                countB < maxPerTeam -> Team.TEAM_B
                else -> Team.RESERVE
            }
        }
    }

    fun stopHost() {
        _isHostRunning.value = false
        socketServer.stop()
        useCaseScope?.cancel()
        useCaseScope = null
        _connectedClients.value = emptyMap()
        currentRoomToken = ""
        currentEncryptionKey = ""
    }
}
