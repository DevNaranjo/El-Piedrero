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
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

sealed interface ClientConnectionState {
    data object Disconnected : ClientConnectionState
    data object Connecting : ClientConnectionState
    data object Connected : ClientConnectionState
    data class Error(val message: String, val cause: Throwable? = null) : ClientConnectionState
}

class SocketClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }
) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var clientScope: CoroutineScope? = null
    private var readJob: Job? = null
    private val writeMutex = Mutex()
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

    private val _connectionState = MutableStateFlow<ClientConnectionState>(ClientConnectionState.Disconnected)
    val connectionState: StateFlow<ClientConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<NetworkEnvelope>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<NetworkEnvelope> = _incomingMessages.asSharedFlow()

    fun connect(host: String, port: Int = NetworkUtils.DEFAULT_PORT, timeoutMs: Int = 5000) {
        if (_connectionState.value is ClientConnectionState.Connected || _connectionState.value is ClientConnectionState.Connecting) {
            return
        }

        _connectionState.value = ClientConnectionState.Connecting
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        clientScope = scope

        scope.launch {
            try {
                val newSocket = Socket()
                newSocket.tcpNoDelay = true
                newSocket.connect(InetSocketAddress(host, port), timeoutMs)

                val newReader = BufferedReader(InputStreamReader(newSocket.getInputStream(), StandardCharsets.UTF_8))
                val newWriter = BufferedWriter(OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.UTF_8))

                socket = newSocket
                reader = newReader
                writer = newWriter

                _connectionState.value = ClientConnectionState.Connected

                // Loop de escucha continuo de tramas delimitadas por \n
                readJob = launch {
                    try {
                        while (isActive && !newSocket.isClosed) {
                            val line = readBoundedLine(newReader) ?: break // EOF
                            if (line.isNotBlank()) {
                                try {
                                    val currentKey = secretKey
                                    val plainJson = if (currentKey != null) {
                                        RondaCipher.decrypt(line.trim(), currentKey)
                                    } else {
                                        line
                                    }
                                    val envelope = json.decodeFromString<NetworkEnvelope>(plainJson)
                                    _incomingMessages.emit(envelope)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    if (secretKey != null) {
                                        // Error de descifrado o clave incorrecta
                                        break
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            _connectionState.value = ClientConnectionState.Error("Conexión interrumpida", e)
                        }
                    } finally {
                        disconnectInternal()
                    }
                }
            } catch (e: Exception) {
                _connectionState.value = ClientConnectionState.Error("Fallo al conectar con $host:$port", e)
                disconnectInternal()
            }
        }
    }

    suspend fun sendMessage(message: NetworkEnvelope): Boolean {
        if (_connectionState.value !is ClientConnectionState.Connected) return false

        return try {
            val rawJson = json.encodeToString(message)
            val currentKey = secretKey
            val payloadToSend = if (currentKey != null) {
                RondaCipher.encrypt(rawJson, currentKey) + "\n"
            } else {
                rawJson + "\n"
            }
            writeMutex.withLock {
                val currentWriter = writer ?: return false
                currentWriter.write(payloadToSend)
                currentWriter.flush()
            }
            true
        } catch (e: Exception) {
            _connectionState.value = ClientConnectionState.Error("Error enviando mensaje", e)
            disconnect()
            false
        }
    }

    fun disconnect() {
        disconnectInternal()
    }

    private fun disconnectInternal() {
        readJob?.cancel()
        readJob = null

        try {
            reader?.close()
            writer?.close()
            socket?.close()
        } catch (_: Exception) {}

        reader = null
        writer = null
        socket = null

        clientScope?.cancel()
        clientScope = null

        if (_connectionState.value !is ClientConnectionState.Error) {
            _connectionState.value = ClientConnectionState.Disconnected
        }
    }

    companion object {
        const val MAX_MESSAGE_SIZE = 32 * 1024
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
                    throw SecurityException("Trama de red excede la longitud máxima ($maxLength bytes)")
                }
            }
        }
    }
}
