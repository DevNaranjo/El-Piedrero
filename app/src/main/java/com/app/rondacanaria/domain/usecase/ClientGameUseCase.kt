package com.app.rondacanaria.domain.usecase

import com.app.rondacanaria.data.model.*
import com.app.rondacanaria.data.network.ClientConnectionState
import com.app.rondacanaria.data.network.NetworkUtils
import com.app.rondacanaria.data.network.SocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class SessionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    ERROR
}

class ClientGameUseCase(
    private val socketClient: SocketClient = SocketClient()
) {
    private var useCaseScope: CoroutineScope? = null
    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectCountdownJob: Job? = null

    private var targetHost: String = ""
    private var targetPort: Int = NetworkUtils.DEFAULT_PORT
    private var localPlayerName: String = "Jugador"
    private var targetRoomToken: String = ""
    private var targetHostName: String = ""
    val localPlayerId: String = UUID.randomUUID().toString()
    private val outgoingSequence = java.util.concurrent.atomic.AtomicLong(0)

    private val _sessionStatus = MutableStateFlow(SessionStatus.IDLE)
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private val _reconnectCountdown = MutableStateFlow<Int?>(null)
    val reconnectCountdown: StateFlow<Int?> = _reconnectCountdown.asStateFlow()

    private val _myTeam = MutableStateFlow(Team.SPECTATOR)
    val myTeam: StateFlow<Team> = _myTeam.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _soundEvents = MutableSharedFlow<SoundTriggerPayload>(extraBufferCapacity = 16)
    val soundEvents: SharedFlow<SoundTriggerPayload> = _soundEvents.asSharedFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun joinGame(
        host: String,
        port: Int = NetworkUtils.DEFAULT_PORT,
        playerName: String,
        roomToken: String = "",
        encryptionKey: String = "",
        hostName: String = ""
    ) {
        this.targetHost = host
        this.targetPort = port
        this.localPlayerName = playerName
        this.targetRoomToken = roomToken
        this.targetHostName = hostName
        this.socketClient.setEncryptionKey(encryptionKey)

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        useCaseScope = scope

        _sessionStatus.value = SessionStatus.CONNECTING
        _errorMessage.value = null

        scope.launch {
            observeClientSocket()
        }

        socketClient.connect(host, port)
    }

    private suspend fun observeClientSocket() {
        val scope = useCaseScope ?: return

        scope.launch {
            socketClient.connectionState.collect { state ->
                when (state) {
                    is ClientConnectionState.Connected -> {
                        reconnectCountdownJob?.cancel()
                        reconnectCountdownJob = null
                        _reconnectCountdown.value = null
                        if (_sessionStatus.value != SessionStatus.RECONNECTING) {
                            _sessionStatus.value = SessionStatus.CONNECTING
                        }
                        reconnectJob?.cancel()
                        startHeartbeat()
                        sendJoinRequest()
                    }
                    is ClientConnectionState.Connecting -> {
                        if (_sessionStatus.value != SessionStatus.RECONNECTING) {
                            _sessionStatus.value = SessionStatus.CONNECTING
                        }
                    }
                    is ClientConnectionState.Disconnected -> {
                        stopHeartbeat()
                        if (_sessionStatus.value == SessionStatus.CONNECTED) {
                            triggerAutoReconnect()
                        }
                    }
                    is ClientConnectionState.Error -> {
                        stopHeartbeat()
                        val hostLabel = if (targetHostName.isNotBlank()) "la mesa de $targetHostName" else "la mesa del anfitrión"
                        _errorMessage.value = "Fallo al conectar con $hostLabel. Comprueba que ambos teléfonos estén en la misma red Wi-Fi o Zona Wi-Fi."
                        if (_sessionStatus.value == SessionStatus.CONNECTED || _sessionStatus.value == SessionStatus.RECONNECTING) {
                            triggerAutoReconnect()
                        } else {
                            _sessionStatus.value = SessionStatus.ERROR
                        }
                    }
                }
            }
        }

        scope.launch {
            socketClient.incomingMessages.collect { envelope ->
                handleIncomingEnvelope(envelope)
            }
        }
    }

    private suspend fun sendJoinRequest() {
        val envelope = NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId,
            joinRequest = JoinRequestPayload(
                playerName = localPlayerName,
                clientVersion = "1.0",
                roomToken = targetRoomToken
            )
        )
        socketClient.sendMessage(envelope)
    }

    internal fun handleIncomingEnvelope(envelope: NetworkEnvelope) {
        when (envelope.type) {
            MessageType.JOIN_RESPONSE -> {
                val response = envelope.joinResponse ?: return
                if (response.accepted) {
                    _myTeam.value = response.assignedTeam
                    response.gameState?.let { state ->
                        _gameState.value = state
                    }
                    _sessionStatus.value = SessionStatus.CONNECTED
                } else {
                    _errorMessage.value = response.errorMessage ?: "Conexión rechazada por el anfitrión."
                    _sessionStatus.value = SessionStatus.ERROR
                    leaveGame()
                }
            }
            MessageType.GAME_STATE_BROADCAST -> {
                val newState = envelope.gameStateBroadcast ?: return
                val current = _gameState.value
                if (current == null || newState.version >= current.version) {
                    _gameState.value = newState
                    val myPlayer = newState.connectedPlayers.find { 
                        it.id == localPlayerId || (it.name.isNotBlank() && it.name == localPlayerName && !it.isHost)
                    }
                    if (myPlayer != null && _myTeam.value != myPlayer.team) {
                        _myTeam.value = myPlayer.team
                    } else if (_myTeam.value == Team.SPECTATOR) {
                        if (newState.maxPlayers == 2 || newState.connectedPlayers.size == 2) {
                            _myTeam.value = Team.TEAM_B
                        }
                    }
                }
            }
            MessageType.SOUND_TRIGGER -> {
                envelope.soundTrigger?.let { sound ->
                    _soundEvents.tryEmit(sound)
                }
            }
            MessageType.END_GAME -> {
                _gameState.value?.let { current ->
                    _gameState.value = current.copy(status = GameStatus.FINISHED)
                }
            }
            MessageType.HEARTBEAT_PONG -> {}
            else -> {}
        }
    }

    suspend fun requestCanto(teamId: Team, canto: CantoType) {
        val envelope = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId,
            scoreUpdate = ScoreUpdatePayload(
                teamId = teamId,
                cantoType = canto,
                piedras = canto.defaultPiedras,
                reason = canto.displayName
            )
        )
        socketClient.sendMessage(envelope)
    }

    suspend fun requestManualScoreChange(teamId: Team, deltaPiedras: Int, reason: String = "Ajuste manual") {
        val envelope = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId,
            scoreUpdate = ScoreUpdatePayload(
                teamId = teamId,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = deltaPiedras,
                reason = reason
            )
        )
        socketClient.sendMessage(envelope)
    }

    suspend fun requestEndGame(reason: String = "Fin de partida solicitado") {
        val envelope = NetworkEnvelope(
            type = MessageType.END_GAME,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId,
            endGame = EndGamePayload(reason = reason)
        )
        socketClient.sendMessage(envelope)
    }

    suspend fun requestSwitchTeam(targetTeam: Team) {
        if (_gameState.value?.maxPlayers == 2) return
        // En tríos, solo se puede usar suplente antes de contar cualquier piedra
        if (_gameState.value?.maxPlayers == 3 && _gameState.value?.moveHistory?.isNotEmpty() == true) return
        val envelope = NetworkEnvelope(
            type = MessageType.SWITCH_TEAM,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId,
            switchTeam = SwitchTeamPayload(
                playerId = localPlayerId,
                targetTeam = targetTeam,
                playerName = localPlayerName
            )
        )
        socketClient.sendMessage(envelope)
    }

    suspend fun requestUndoLastMove() {
        val envelope = NetworkEnvelope(
            type = MessageType.UNDO_LAST_MOVE,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId
        )
        socketClient.sendMessage(envelope)
    }

    suspend fun requestUpdateDeal(newDeal: Int) {
        if (newDeal < 1) return
        val envelope = NetworkEnvelope(
            type = MessageType.UPDATE_DEAL,
            sequenceNumber = outgoingSequence.incrementAndGet(),
            senderId = localPlayerId,
            updateDeal = UpdateDealPayload(dealNumber = newDeal)
        )
        socketClient.sendMessage(envelope)
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = useCaseScope?.launch {
            while (isActive) {
                delay(3000L)
                val ping = NetworkEnvelope(
                    type = MessageType.HEARTBEAT_PING,
                    sequenceNumber = outgoingSequence.incrementAndGet(),
                    senderId = localPlayerId
                )
                socketClient.sendMessage(ping)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun triggerAutoReconnect() {
        if (reconnectJob?.isActive == true) return
        _sessionStatus.value = SessionStatus.RECONNECTING

        // Cuenta atrás regresiva de 5 minutos (300 segundos) para reconexión
        _reconnectCountdown.value = 300
        reconnectCountdownJob?.cancel()
        reconnectCountdownJob = useCaseScope?.launch {
            var seconds = 300
            while (isActive && seconds > 0) {
                delay(1000L)
                seconds--
                _reconnectCountdown.value = seconds
            }
            if (isActive && _sessionStatus.value == SessionStatus.RECONNECTING) {
                _sessionStatus.value = SessionStatus.DISCONNECTED
                _reconnectCountdown.value = null
                val hostLabel = if (targetHostName.isNotBlank()) "la mesa de $targetHostName" else "la mesa del anfitrión"
                _errorMessage.value = "Tiempo de espera agotado (5 minutos). Se cerró la conexión con $hostLabel."
                leaveGame()
            }
        }

        reconnectJob = useCaseScope?.launch {
            var backoffMs = 2000L
            while (isActive && _sessionStatus.value == SessionStatus.RECONNECTING) {
                delay(backoffMs)
                if (socketClient.connectionState.value is ClientConnectionState.Connected) {
                    break
                }
                socketClient.connect(targetHost, targetPort)
                backoffMs = (backoffMs * 1.3).toLong().coerceIn(2000L, 8000L)
            }
        }
    }

    fun leaveGame() {
        stopHeartbeat()
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectCountdownJob?.cancel()
        reconnectCountdownJob = null
        _reconnectCountdown.value = null
        socketClient.setEncryptionKey(null)
        socketClient.disconnect()
        useCaseScope?.cancel()
        useCaseScope = null
        targetRoomToken = ""
        targetHostName = ""
        _sessionStatus.value = SessionStatus.DISCONNECTED
        _gameState.value = null
    }
}
