package com.app.rondacanaria.domain.usecase

import com.app.rondacanaria.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HostGameUseCaseTest {

    private lateinit var hostUseCase: HostGameUseCase

    @Before
    fun setUp() {
        hostUseCase = HostGameUseCase()
    }

    @Test
    fun `partida inicia en estado PLAYING con el Host asignado al Equipo A`() = runBlocking {
        hostUseCase.startHost("Anfitrion", "Equipo Norte", "Equipo Sur", maxPlayers = 4)
        val state = hostUseCase.gameState.value

        assertEquals(GameStatus.WAITING, state.status)
        hostUseCase.setGameStatus(GameStatus.PLAYING)
        assertEquals(GameStatus.PLAYING, hostUseCase.gameState.value.status)
        assertEquals("Equipo Norte", state.nameTeamA)
        assertEquals("Equipo Sur", state.nameTeamB)
        assertEquals(1, state.connectedPlayers.size)
        assertEquals("Anfitrion", state.connectedPlayers.first().name)
        assertEquals(Team.TEAM_A, state.connectedPlayers.first().team)
        assertTrue(state.connectedPlayers.first().isHost)
        hostUseCase.stopHost()
    }

    @Test
    fun `al sumar puntos se alcanza el paso a buenas en 11 piedras`() = runBlocking {
        hostUseCase.startHost("Anfitrion", maxPlayers = 4)

        // Sumar 10 piedras (permanece en malas)
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MANUAL_ADJUST, 10, "Ajuste")
        assertEquals(10, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertFalse(hostUseCase.gameState.value.scoreTeamA.isInBuenas)

        // Sumar 1 piedra adicional (alcanza 11 y entra en Buenas)
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MAJO, 1, "Majo")
        assertEquals(11, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertTrue(hostUseCase.gameState.value.scoreTeamA.isInBuenas)
        assertEquals(0, hostUseCase.gameState.value.scoreTeamA.buenas)
        hostUseCase.stopHost()
    }

    @Test
    fun `al alcanzar 21 piedras se declara la victoria del equipo`() = runBlocking {
        hostUseCase.startHost("Anfitrion", maxPlayers = 4)

        hostUseCase.applyScoreUpdate(Team.TEAM_B, CantoType.MANUAL_ADJUST, 20, "Casi victoria")
        assertNull(hostUseCase.gameState.value.winnerTeam)

        // Añadir piedra ganadora
        hostUseCase.applyScoreUpdate(Team.TEAM_B, CantoType.RONDA, 1, "Ronda final")
        val state = hostUseCase.gameState.value

        assertEquals(21, state.scoreTeamB.totalPiedras)
        assertEquals(Team.TEAM_B, state.winnerTeam)
        assertEquals(GameStatus.FINISHED, state.status)
        assertEquals(1, state.winsTeamB)
        hostUseCase.stopHost()
    }

    @Test
    fun `deshacer jugada ganadora revierte la victoria y decrementa victorias`() = runBlocking {
        hostUseCase.startHost("Anfitrion", maxPlayers = 4)

        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MANUAL_ADJUST, 21, "Victoria directa")
        assertEquals(Team.TEAM_A, hostUseCase.gameState.value.winnerTeam)
        assertEquals(1, hostUseCase.gameState.value.winsTeamA)

        // Deshacer
        hostUseCase.undoLastMove()
        val state = hostUseCase.gameState.value

        assertNull(state.winnerTeam)
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(0, state.scoreTeamA.totalPiedras)
        assertEquals(0, state.winsTeamA)
        hostUseCase.stopHost()
    }

    @Test
    fun `verificar limites de repartos segun numero de jugadores`() {
        assertEquals(6, getMaxDeals(2))
        assertEquals(4, getMaxDeals(3))
        assertEquals(3, getMaxDeals(4))
        assertEquals(3, getMaxDeals(6))
        assertEquals(3, getMaxDeals(8))
    }

    @Test
    fun `cambio de reparto cicla adecuadamente respetando el tope`() = runBlocking {
        hostUseCase.startHost("Anfitrion", maxPlayers = 4)
        assertEquals(1, hostUseCase.gameState.value.currentDeal)

        hostUseCase.setCurrentDeal(2)
        assertEquals(2, hostUseCase.gameState.value.currentDeal)

        hostUseCase.setCurrentDeal(3)
        assertEquals(3, hostUseCase.gameState.value.currentDeal)

        // Al pasar de 3 vuelve a 1
        hostUseCase.setCurrentDeal(4)
        assertEquals(1, hostUseCase.gameState.value.currentDeal)
        hostUseCase.stopHost()
    }

    @Test
    fun `paco se une a la sala de Iriome y suma piedras y cantos para el Equipo B en sala 1v1`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 2)
        val token = hostUseCase.roomToken

        // Paco envía JOIN_REQUEST
        val pacoId = "paco-uuid-123"
        val joinEnvelope = NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(
                playerName = "Paco",
                roomToken = token
            )
        )
        hostUseCase.handleClientMessage("192.168.1.100:1234", joinEnvelope)

        // Verificar que Paco fue aceptado en TEAM_B
        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(2, players.size)
        val pacoPlayer = players.find { it.id == pacoId }
        assertNotNull(pacoPlayer)
        assertEquals(Team.TEAM_B, pacoPlayer?.team)

        // Iniciar partida
        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // Paco envía SCORE_UPDATE manual (+1 piedra para Equipo B)
        val scoreEnvelope = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1 piedra manual"
            )
        )
        hostUseCase.handleClientMessage("192.168.1.100:1234", scoreEnvelope)
        assertEquals(1, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Paco envía SCORE_UPDATE de canto (RONDA para Equipo B)
        val cantoEnvelope = NetworkEnvelope(
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
        hostUseCase.handleClientMessage("192.168.1.100:1234", cantoEnvelope)
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        hostUseCase.stopHost()
    }

    @Test
    fun `paco se une a la sala de Iriome y suma piedras y cantos en sala de 4 jugadores con 2 conectados`() = runBlocking {
        hostUseCase.startHost("Iriome", maxPlayers = 4)
        val token = hostUseCase.roomToken

        // Paco envía JOIN_REQUEST
        val pacoId = "paco-uuid-456"
        val joinEnvelope = NetworkEnvelope(
            type = MessageType.JOIN_REQUEST,
            senderId = pacoId,
            joinRequest = JoinRequestPayload(
                playerName = "Paco",
                roomToken = token
            )
        )
        hostUseCase.handleClientMessage("192.168.1.100:1234", joinEnvelope)

        // Verificar asignación a TEAM_B
        val players = hostUseCase.gameState.value.connectedPlayers
        assertEquals(2, players.size)
        val pacoPlayer = players.find { it.id == pacoId }
        assertNotNull(pacoPlayer)
        assertEquals(Team.TEAM_B, pacoPlayer?.team)

        // Iniciar partida
        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // Verificar que maxPlayers se adaptó a 2 y los nombres de equipo son los de los jugadores
        assertEquals(2, hostUseCase.gameState.value.maxPlayers)
        assertEquals("Iriome", hostUseCase.gameState.value.nameTeamA)
        assertEquals("Paco", hostUseCase.gameState.value.nameTeamB)

        // Paco envía SCORE_UPDATE manual (+1 piedra para Equipo B)
        val scoreEnvelope = NetworkEnvelope(
            type = MessageType.SCORE_UPDATE,
            senderId = pacoId,
            sequenceNumber = 1L,
            scoreUpdate = ScoreUpdatePayload(
                teamId = Team.TEAM_B,
                cantoType = CantoType.MANUAL_ADJUST,
                piedras = 1,
                reason = "+1 piedra manual"
            )
        )
        hostUseCase.handleClientMessage("192.168.1.100:1234", scoreEnvelope)
        assertEquals(1, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Paco envía SCORE_UPDATE de Canto (RONDA)
        val cantoEnvelope = NetworkEnvelope(
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
        hostUseCase.handleClientMessage("192.168.1.100:1234", cantoEnvelope)
        assertEquals(2, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        hostUseCase.stopHost()
    }
}
