package com.app.rondacanaria.domain.usecase

import com.app.rondacanaria.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MultiplayerStoneModificationTest {

    private lateinit var hostUseCase: HostGameUseCase

    @Before
    fun setUp() {
        hostUseCase = HostGameUseCase()
    }

    @Test
    fun `partida multijugador 2 jugadores (1v1) - host y cliente modifican sus respectivas piedras y cantos`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 2)
        val token = hostUseCase.roomToken

        // Cliente Paco se conecta
        val pacoId = "player-paco-2p"
        val joinReq = NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        )
        hostUseCase.handleClientMessage("client-paco", joinReq)

        // Verificar asignación de equipos
        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(2, players.size)
        val paco = players.find { it.id == pacoId }
        assertNotNull(paco)
        assertEquals(Team.TEAM_B, paco?.team)

        // Iniciar partida
        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // 1. Host (Iriome) modifica piedras de su equipo (Equipo A)
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MANUAL_ADJUST, 1, "+1 manual", "Iriome")
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MAJO, 1, "Majo", "Iriome")
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)

        // 2. Cliente (Paco) modifica piedras de su equipo (Equipo B)
        val pacoManual = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1 manual"
            )
        )
        hostUseCase.handleClientMessage("client-paco", pacoManual)

        val pacoCanto = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.RONDA,
                piedras = 1,
                reason = "Ronda"
            )
        )
        hostUseCase.handleClientMessage("client-paco", pacoCanto)

        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // 3. Seguridad: Paco intenta modificar el Equipo A del host -> Descartado
        val pacoTamperA = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 3L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_A,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 5,
                reason = "Intento no autorizado"
            )
        )
        hostUseCase.handleClientMessage("client-paco", pacoTamperA)
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras) // No cambia

        hostUseCase.stopHost()
    }

    @Test
    fun `partida multijugador 3 jugadores (trio) - host y los 2 clientes modifican sus piedras en equipos A, B y C`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 3)
        val token = hostUseCase.roomToken

        // Cliente 1 (Paco)
        val pacoId = "player-paco-3p"
        hostUseCase.handleClientMessage("client-paco", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        ))

        // Cliente 2 (Maria)
        val mariaId = "player-maria-3p"
        hostUseCase.handleClientMessage("client-maria", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = mariaId,
            joinRequest = JoinRequestPayload(playerName = "Maria", roomToken = token)
        ))

        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(3, players.size)
        assertEquals(Team.TEAM_A, players.find { it.name == "Iriome" }?.team)
        assertEquals(Team.TEAM_B, players.find { it.id == pacoId }?.team)
        assertEquals(Team.TEAM_C, players.find { it.id == mariaId }?.team)

        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // Host modifica Equipo A
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MANUAL_ADJUST, 2, "+2 manual", "Iriome")
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)

        // Paco (Equipo B) modifica Equipo B
        hostUseCase.handleClientMessage("client-paco", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1 manual"
            )
        ))
        hostUseCase.handleClientMessage("client-paco", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MAJO,
                piedras = 1,
                reason = "Majo"
            )
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Maria (Equipo C) modifica Equipo C
        hostUseCase.handleClientMessage("client-maria", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = mariaId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_C,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1 manual"
            )
        ))
        hostUseCase.handleClientMessage("client-maria", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = mariaId,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_C,
                cantoType = CantoType.RONDA,
                piedras = 1,
                reason = "Ronda"
            )
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamC.totalPiedras)

        // Seguridad cruzada: Paco no puede modificar C, Maria no puede modificar B
        hostUseCase.handleClientMessage("client-paco", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 3L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_C,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 5,
                reason = "Fraude"
            )
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamC.totalPiedras)

        hostUseCase.handleClientMessage("client-maria", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = mariaId,
            sequenceNumber = 3L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 5,
                reason = "Fraude"
            )
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        hostUseCase.stopHost()
    }

    @Test
    fun `partida multijugador 4 jugadores (2x2) - todos los jugadores de ambos equipos pueden modificar sus piedras`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 4)
        val token = hostUseCase.roomToken

        // Conectar 3 clientes
        val p2Id = "player-2-4p"
        val p3Id = "player-3-4p"
        val p4Id = "player-4-4p"

        hostUseCase.handleClientMessage("c2", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = p2Id,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        ))
        hostUseCase.handleClientMessage("c3", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = p3Id,
            joinRequest = JoinRequestPayload(playerName = "Maria", roomToken = token)
        ))
        hostUseCase.handleClientMessage("c4", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = p4Id,
            joinRequest = JoinRequestPayload(playerName = "Pepe", roomToken = token)
        ))

        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(4, players.size)

        // Verificar reparto equilibrado 2x2
        val teamAPlayers = players.filter { it.team == Team.TEAM_A }
        val teamBPlayers = players.filter { it.team == Team.TEAM_B }
        assertEquals(2, teamAPlayers.size)
        assertEquals(2, teamBPlayers.size)

        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // 1. Host (Iriome, Team A) suma +1 a Team A
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MANUAL_ADJUST, 1, "+1", "Iriome")

        // 2. Jugador 3 (Maria, Team A) suma +1 a Team A mediante socket
        hostUseCase.handleClientMessage("c3", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = p3Id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_A,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1 de companero"
            )
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)

        // 3. Jugador 2 (Paco, Team B) suma +1 a Team B
        hostUseCase.handleClientMessage("c2", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = p2Id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1"
            )
        ))

        // 4. Jugador 4 (Pepe, Team B) suma +1 a Team B
        hostUseCase.handleClientMessage("c4", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = p4Id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MAJO,
                piedras = 1,
                reason = "Majo"
            )
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Seguridad: Jugadores de Team B no pueden puntuar a Team A y viceversa
        hostUseCase.handleClientMessage("c2", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = p2Id,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_A, cantoType = CantoType.MANUAL_ADJUST, piedras = 5)
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)

        hostUseCase.handleClientMessage("c3", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = p3Id,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_B, cantoType = CantoType.MANUAL_ADJUST, piedras = 5)
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        hostUseCase.stopHost()
    }

    @Test
    fun `partida multijugador 6 jugadores (3x2) - equipos activos modifican sus piedras y reservas rotan correctamente`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 6)
        val token = hostUseCase.roomToken

        // Conectar 5 clientes (total 6 jugadores)
        val clientIds = (2..6).map { "player-$it-6p" }
        clientIds.forEachIndexed { index, id ->
            hostUseCase.handleClientMessage("sock-$id", NetworkEnvelope(
                type = MessageType.JOIN_REQUEST,
                senderId = id,
                joinRequest = JoinRequestPayload(playerName = "Jugador ${index + 2}", roomToken = token)
            ))
        }

        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(6, players.size)

        // En 6 jugadores, Team C inicia en reserva por defecto
        assertTrue(hostUseCase.gameState.value.reserveTeams.contains(Team.TEAM_C))

        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // Jugadores de Team A y Team B (activos) suman piedras
        val playerTeamB = players.first { it.team == Team.TEAM_B }
        hostUseCase.handleClientMessage("sock-${playerTeamB.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerTeamB.id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_B, cantoType = CantoType.MANUAL_ADJUST, piedras = 2)
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Jugador de Team C (en reserva) intenta puntuar -> Rechazado
        val playerTeamC = players.first { it.team == Team.TEAM_C }
        hostUseCase.handleClientMessage("sock-${playerTeamC.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerTeamC.id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_C, cantoType = CantoType.MANUAL_ADJUST, piedras = 2)
        ))
        assertEquals(0, hostUseCase.gameState.value.scoreTeamC.totalPiedras) // Sigue en 0

        // Rotar reserva: Team B pasa a reserva, Team C entra a la mesa
        hostUseCase.setReserveTeams(listOf(Team.TEAM_B))
        assertTrue(hostUseCase.gameState.value.reserveTeams.contains(Team.TEAM_B))
        assertFalse(hostUseCase.gameState.value.reserveTeams.contains(Team.TEAM_C))

        // Ahora el Jugador de Team C sí puede modificar sus piedras
        hostUseCase.handleClientMessage("sock-${playerTeamC.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerTeamC.id,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_C, cantoType = CantoType.MANUAL_ADJUST, piedras = 3)
        ))
        assertEquals(3, hostUseCase.gameState.value.scoreTeamC.totalPiedras)

        hostUseCase.stopHost()
    }

    @Test
    fun `partida multijugador 8 jugadores (4x2) - equipos A, B, C y D pueden modificar sus piedras al estar activos`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 8)
        val token = hostUseCase.roomToken

        // Conectar 7 clientes (total 8 jugadores)
        val clientIds = (2..8).map { "player-$it-8p" }
        clientIds.forEachIndexed { index, id ->
            hostUseCase.handleClientMessage("sock-$id", NetworkEnvelope(
                type = MessageType.JOIN_REQUEST,
                senderId = id,
                joinRequest = JoinRequestPayload(playerName = "Jugador ${index + 2}", roomToken = token)
            ))
        }

        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(8, players.size)

        // Cada uno de los 4 equipos debe tener 2 jugadores
        assertEquals(2, players.count { it.team == Team.TEAM_A })
        assertEquals(2, players.count { it.team == Team.TEAM_B })
        assertEquals(2, players.count { it.team == Team.TEAM_C })
        assertEquals(2, players.count { it.team == Team.TEAM_D })

        // En 8 jugadores, Teams C y D inician en reserva
        assertEquals(listOf(Team.TEAM_C, Team.TEAM_D), hostUseCase.gameState.value.reserveTeams)

        hostUseCase.setGameStatus(GameStatus.PLAYING)

        val playerA = players.first { it.team == Team.TEAM_A && !it.isHost }
        val playerB = players.first { it.team == Team.TEAM_B }
        val playerC = players.first { it.team == Team.TEAM_C }
        val playerD = players.first { it.team == Team.TEAM_D }

        // Team A y B modifican piedras
        hostUseCase.handleClientMessage("sock-${playerA.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerA.id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_A, cantoType = CantoType.MANUAL_ADJUST, piedras = 2)
        ))
        hostUseCase.handleClientMessage("sock-${playerB.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerB.id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_B, cantoType = CantoType.MANUAL_ADJUST, piedras = 3)
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(3, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Team C y D en reserva no pueden modificar
        hostUseCase.handleClientMessage("sock-${playerC.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerC.id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_C, cantoType = CantoType.MANUAL_ADJUST, piedras = 4)
        ))
        assertEquals(0, hostUseCase.gameState.value.scoreTeamC.totalPiedras)

        // Rotar reserva: Activar Teams C y D, pasar Teams A y B a reserva
        hostUseCase.setReserveTeams(listOf(Team.TEAM_A, Team.TEAM_B))

        // Ahora Teams C y D sí pueden modificar sus piedras
        hostUseCase.handleClientMessage("sock-${playerC.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerC.id,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_C, cantoType = CantoType.MANUAL_ADJUST, piedras = 4)
        ))
        hostUseCase.handleClientMessage("sock-${playerD.id}", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = playerD.id,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_D, cantoType = CantoType.MAJO, piedras = 1)
        ))

        assertEquals(4, hostUseCase.gameState.value.scoreTeamC.totalPiedras)
        assertEquals(1, hostUseCase.gameState.value.scoreTeamD.totalPiedras)

        hostUseCase.stopHost()
    }

    @Test
    fun `prueba 1 - reconexion de cliente mantiene asiento y permite seguir modificando piedras`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 4)
        val token = hostUseCase.roomToken

        val pacoId = "player-paco-reconnect"
        // 1. Paco se conecta por primera vez y se le asigna Team B
        hostUseCase.handleClientMessage("sock-paco-1", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        ))

        var pacoPlayer = hostUseCase.gameState.value.connectedPlayers.find { it.id == pacoId }
        assertNotNull(pacoPlayer)
        assertEquals(Team.TEAM_B, pacoPlayer?.team)

        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // 2. Paco suma 1 piedra a Team B
        hostUseCase.handleClientMessage("sock-paco-1", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_B, cantoType = CantoType.MANUAL_ADJUST, piedras = 1)
        ))
        assertEquals(1, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // 3. Paco sufre desconexion temporal (cambio de red / wifi drop)
        hostUseCase.handleClientMessage("sock-paco-1", NetworkEnvelope(
            type = MessageType.HEARTBEAT_PING,
            senderId = pacoId
        ))

        // 4. Paco se reconecta con un nuevo socket pero el mismo playerId
        hostUseCase.handleClientMessage("sock-paco-2", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        ))

        // Su asiento sigue siendo Team B
        pacoPlayer = hostUseCase.gameState.value.connectedPlayers.find { it.id == pacoId }
        assertNotNull(pacoPlayer)
        assertEquals(Team.TEAM_B, pacoPlayer?.team)

        // 5. Paco sigue pudiendo modificar piedras tras la reconexion
        hostUseCase.handleClientMessage("sock-paco-2", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 2L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_B, cantoType = CantoType.MAJO, piedras = 1)
        ))
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        hostUseCase.stopHost()
    }

    @Test
    fun `prueba 2 - deshacer jugada mediante UNDO_LAST_MOVE en multijugador y bloqueo a reservas`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 6)
        val token = hostUseCase.roomToken

        val pacoId = "player-paco-undo"
        val reserveId = "player-reserve-undo"

        // Paco entra a Team B
        hostUseCase.handleClientMessage("sock-paco", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        ))
        // Jugador entra a Team C (reserva en 6 jugadores)
        hostUseCase.handleClientMessage("sock-res", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = reserveId,
            joinRequest = JoinRequestPayload(playerName = "Suplente", roomToken = token)
        ))

        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // 1. Paco (Team B) canta Majo (+1)
        hostUseCase.handleClientMessage("sock-paco", NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(teamId = Team.TEAM_B, cantoType = CantoType.MAJO, piedras = 1)
        ))
        assertEquals(1, hostUseCase.gameState.value.scoreTeamB.totalPiedras)
        assertEquals(1, hostUseCase.gameState.value.moveHistory.size)

        // 2. Jugador en reserva intenta deshacer -> Bloqueado (no se deshace)
        hostUseCase.handleClientMessage("sock-res", NetworkEnvelope(
            type = MessageType.UNDO_LAST_MOVE,
            senderId = reserveId,
            sequenceNumber = 1L
        ))
        assertEquals(1, hostUseCase.gameState.value.scoreTeamB.totalPiedras)
        assertEquals(1, hostUseCase.gameState.value.moveHistory.size)

        // 3. Paco envía UNDO_LAST_MOVE -> Se revierte con exito
        hostUseCase.handleClientMessage("sock-paco", NetworkEnvelope(
            type = MessageType.UNDO_LAST_MOVE,
            senderId = pacoId,
            sequenceNumber = 2L
        ))
        assertEquals(0, hostUseCase.gameState.value.scoreTeamB.totalPiedras)
        assertEquals(0, hostUseCase.gameState.value.moveHistory.size)

        hostUseCase.stopHost()
    }

    @Test
    fun `prueba 3 - recuento de cartas de fin de mano applyCardCount y rotacion automatica de repartidor`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 4)
        val token = hostUseCase.roomToken

        val pacoId = "player-paco-cards"
        hostUseCase.handleClientMessage("sock-paco", NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(playerName = "Paco", roomToken = token)
        ))

        hostUseCase.setGameStatus(GameStatus.PLAYING)
        val initialDealer = hostUseCase.gameState.value.dealerPlayerId
        assertNotNull(initialDealer)

        // Simular que se jugaron los 3 repartos de la mano
        hostUseCase.setCurrentDeal(3)
        assertEquals(3, hostUseCase.gameState.value.currentDeal)
        assertEquals(1, hostUseCase.gameState.value.currentHand)

        // 1. Recuento incompleto (ej. 38 cartas en vez de 40) -> Rechazado por seguridad
        hostUseCase.applyCardCount(mapOf(
            Team.TEAM_A to 20,
            Team.TEAM_B to 18
        ), author = "Iriome")
        // No cambia nada
        assertEquals(0, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(3, hostUseCase.gameState.value.currentDeal)

        // 2. Recuento completo reglamentario (40 cartas): Team A saca 23 cartas (+3 sobre el umbral de 20)
        hostUseCase.applyCardCount(mapOf(
            Team.TEAM_A to 23,
            Team.TEAM_B to 17
        ), author = "Iriome")

        // Team A suma exactamente +3 piedras por las cartas extras
        assertEquals(3, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(0, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // La mano se reinicia al reparto 1 de la siguiente mano (mano 2)
        assertEquals(1, hostUseCase.gameState.value.currentDeal)
        assertEquals(2, hostUseCase.gameState.value.currentHand)

        // El repartidor ha rotado al siguiente jugador de la mesa
        val newDealer = hostUseCase.gameState.value.dealerPlayerId
        assertNotNull(newDealer)
        assertNotEquals(initialDealer, newDealer)

        hostUseCase.stopHost()
    }
}
