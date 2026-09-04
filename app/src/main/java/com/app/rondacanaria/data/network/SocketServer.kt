package com.app.rondacanaria.data.network

import com.app.rondacanaria.data.model.NetworkEnvelope
import com.app.rondacanaria.data.network.crypto.RondaCipher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

sealed interface ServerEvent {
    data class ClientConnected(val clientId: String, val remoteAddress: String) : ServerEvent
    data class ClientDisconnected(val clientId: String) : ServerEvent
    data class MessageReceived(val clientId: String, val message: NetworkEnvelope) : ServerEvent
    data class Error(val throwable: Throwable) : ServerEvent
}

class SocketServer(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
) {
    private var serverSocket: ServerSocket? = null
    private var serverScope: CoroutineScope? = null
    private var acceptJob: Job? = null
    private var secretKey: SecretKey? = null

    fun setEncryptionKey(base64Key: String?) {
        secretKey = if (!base64Key.isNullOrBlank()) {
            try {
                RondaCipher.parseKey(base64Key)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    private val _serverEvents = MutableSharedFlow<ServerEvent>(extraBufferCapacity = 64)
    val serverEvents: SharedFlow<ServerEvent> = _serverEvents.asSharedFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val activeClients = ConcurrentHashMap<String, ClientSession>()

    private class ClientSession(
        val id: String,
        val socket: Socket,
        val reader: BufferedReader,
        val writer: BufferedWriter,
        val writeMutex: Mutex = Mutex()
    )

    fun start(port: Int = NetworkUtils.DEFAULT_PORT) {
        if (_isRunning.value) return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serverScope = scope

        try {
            val server = ServerSocket(port).apply {
                reuseAddress = true
            }
            serverSocket = server
            _isRunning.value = true

            acceptJob = scope.launch {
                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        handleNewConnection(clientSocket)
                    } catch (e: Exception) {
                        if (isActive && !server.isClosed) {
                            _serverEvents.emit(ServerEvent.Error(e))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _serverEvents.tryEmit(ServerEvent.Error(e))
            stop()
        }
    }

    private fun handleNewConnection(socket: Socket) {
        val scope = serverScope ?: return
        val clientId = "${socket.inetAddress.hostAddress}:${socket.port}"

        scope.launch {
            try {
                socket.tcpNoDelay = true
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))

                val session = ClientSession(clientId, socket, reader, writer)
                activeClients[clientId] = session
                _connectedClientsCount.value = activeClients.size
                _serverEvents.emit(ServerEvent.ClientConnected(clientId, socket.inetAddress.hostAddress ?: ""))

                // Loop de lectura seguro de tramas NDJSON delimitadas por salto de línea (\n)
                while (isActive && !socket.isClosed) {
                    val line = readBoundedLine(reader) ?: break // EOF (cliente desconectado)
                    if (line.isNotBlank()) {
                        try {
                            val currentKey = secretKey
                            val plainJson = if (currentKey != null) {
                                RondaCipher.decrypt(line.trim(), currentKey)
                            } else {
                                line
                            }
                            val envelope = json.decodeFromString<NetworkEnvelope>(plainJson)
                            _serverEvents.emit(ServerEvent.MessageReceived(clientId, envelope))
                        } catch (parseError: Exception) {
                            _serverEvents.emit(ServerEvent.Error(parseError))
                            android.util.Log.e("SocketServer", "Error al procesar trama de $clientId: ${parseError.message}")
                            // Mantener la sesión activa; no romper el bucle ante anomalías aisladas
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    _serverEvents.emit(ServerEvent.Error(e))
                }
            } finally {
                disconnectClient(clientId)
            }
        }
    }

    suspend fun broadcast(message: NetworkEnvelope) = withContext(Dispatchers.IO) {
        val rawJson = json.encodeToString(message)
        val currentKey = secretKey
        val payloadToSend = if (currentKey != null) {
            RondaCipher.encrypt(rawJson, currentKey) + "\n"
        } else {
            rawJson + "\n"
        }
        activeClients.values.forEach { session ->
            try {
                session.writeMutex.withLock {
                    session.writer.write(payloadToSend)
                    session.writer.flush()
                }
            } catch (e: Exception) {
                android.util.Log.e("SocketServer", "Error enviando broadcast a ${session.id}: ${e.message}")
                disconnectClient(session.id)
            }
        }
    }

    suspend fun sendToClient(clientId: String, message: NetworkEnvelope): Boolean {
        val session = activeClients[clientId] ?: return false
        val rawJson = json.encodeToString(message)
        val currentKey = secretKey
        val payloadToSend = if (currentKey != null) {
            RondaCipher.encrypt(rawJson, currentKey) + "\n"
        } else {
            rawJson + "\n"
        }
        return try {
            session.writeMutex.withLock {
                session.writer.write(payloadToSend)
                session.writer.flush()
            }
            true
        } catch (e: Exception) {
            disconnectClient(clientId)
            false
        }
    }

    fun disconnectClient(clientId: String) {
        val session = activeClients.remove(clientId)
        if (session != null) {
            try {
                session.reader.close()
                session.writer.close()
                session.socket.close()
            } catch (_: Exception) {}

            _connectedClientsCount.value = activeClients.size
            _serverEvents.tryEmit(ServerEvent.ClientDisconnected(clientId))
        }
    }

    fun stop() {
        _isRunning.value = false
        acceptJob?.cancel()

        activeClients.keys.toList().forEach { disconnectClient(it) }
        activeClients.clear()

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        serverScope?.cancel()
        serverScope = null
        _connectedClientsCount.value = 0
    }

    companion object {
        const val MAX_MESSAGE_SIZE = 32 * 1024 // Límite de 32 KB para prevenir ataques DoS por desbordamiento de memoria
    }

    private fun readBoundedLine(reader: BufferedReader, maxLength: Int = MAX_MESSAGE_SIZE): String? {
        val sb = StringBuilder(256)
        while (true) {
            val charCode = reader.read()
            if (charCode == -1) {
                return if (sb.isNotEmpty()) sb.toString() else null
            }
            val ch = charCode.toChar()
            if (ch == '\n') {
                return sb.toString()
            }
            if (ch != '\r') {
                sb.append(ch)
                if (sb.length > maxLength) {
                    throw SecurityException("Trama de red excede la longitud máxima permitida ($maxLength bytes)")
                }
            }
        }
    }
}
