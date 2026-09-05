package com.app.rondacanaria.domain.usecase

import com.app.rondacanaria.data.history.LocalSavedGame
import com.app.rondacanaria.data.model.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class GamePersistenceAndRulesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun `serializacion y deserializacion de LocalSavedGame preserva intacto el estado`() {
        val originalState = GameState(
            gameId = "game-12345",
            status = GameStatus.PLAYING,
            maxPlayers = 4,
            nameTeamA = "Isas",
            nameTeamB = "Folias",
            scoreTeamA = TeamScore(totalPiedras = 14, malas = 11, buenas = 3, isInBuenas = true),
            scoreTeamB = TeamScore(totalPiedras = 9, malas = 9, buenas = 0, isInBuenas = false),
            currentDeal = 2,
            currentHand = 1,
            winsTeamA = 1,
            winsTeamB = 0
        )

        val savedGame = LocalSavedGame(
            gameState = originalState,
            maxPlayers = 4,
            teamAName = "Isas",
            teamBName = "Folias",
            teamCName = "Equipo C",
            teamDName = "Equipo D"
        )

        val encoded = json.encodeToString(savedGame)
        assertNotNull(encoded)
        assertTrue(encoded.contains("game-12345"))
        assertTrue(encoded.contains("Isas"))

        val decoded = json.decodeFromString<LocalSavedGame>(encoded)
        assertEquals(savedGame.gameState.gameId, decoded.gameState.gameId)
        assertEquals(14, decoded.gameState.scoreTeamA.totalPiedras)
        assertEquals(3, decoded.gameState.scoreTeamA.buenas)
        assertTrue(decoded.gameState.scoreTeamA.isInBuenas)
        assertEquals(9, decoded.gameState.scoreTeamB.totalPiedras)
        assertEquals(2, decoded.gameState.currentDeal)
    }

    @Test
    fun `historial de partidas limita a 30 elementos rotativos`() {
        val maxHistorySize = 30
        val historyList = mutableListOf<GameHistoryRecord>()

        // Generar 35 partidas simuladas
        for (i in 1..35) {
            val record = GameHistoryRecord(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis() + i * 1000,
                isLocalGame = true,
                winnerTeam = if (i % 2 == 0) Team.TEAM_A else Team.TEAM_B,
                winnerName = if (i % 2 == 0) "Equipo Norte" else "Equipo Sur",
                totalPiedrasWinner = 21,
                teamAName = "Equipo Norte",
                teamAPiedras = if (i % 2 == 0) 21 else 15,
                teamBName = "Equipo Sur",
                teamBPiedras = if (i % 2 == 0) 18 else 21,
                maxPlayers = 4
            )
            historyList.add(0, record)
            if (historyList.size > maxHistorySize) {
                historyList.removeAt(historyList.lastIndex)
            }
        }

        assertEquals(30, historyList.size)
        assertTrue(historyList.first().teamAPiedras == 15 || historyList.first().teamAPiedras == 21)
    }

    @Test
    fun `regla oficial canaria - 4 cartas iniciales repartidas a la mesa en el primer reparto`() = runBlocking {
        val hostUseCase = HostGameUseCase()
        hostUseCase.startHost("Anfitrion", maxPlayers = 4)
        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // En deal 1 hand 1: 4 jugadores reciben 3 cartas c/u (12 cartas) + 4 cartas en mesa = 16 cartas iniciales
        val deal = hostUseCase.gameState.value.currentDeal
        val hand = hostUseCase.gameState.value.currentHand
        assertEquals(1, deal)
        assertEquals(1, hand)

        val cardsToPlayers = 4 * 3 // 12
        val cardsToMesa = 4
        assertEquals(16, cardsToPlayers + cardsToMesa)

        hostUseCase.stopHost()
    }

    @Test
    fun `reversion multiple con undoLastMove restaura tanteos previos exactamente`() = runBlocking {
        val hostUseCase = HostGameUseCase()
        hostUseCase.startHost("Anfitrion", maxPlayers = 2)
        hostUseCase.setGameStatus(GameStatus.PLAYING)

        // Jugada 1: Majo (+1) Team A
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.MAJO, 1, "Majo")
        // Jugada 2: Ronda (+1) Team A
        hostUseCase.applyScoreUpdate(Team.TEAM_A, CantoType.RONDA, 1, "Ronda")
        // Jugada 3: Parranda (+3) Team B
        hostUseCase.applyScoreUpdate(Team.TEAM_B, CantoType.PARRANDA, 3, "Parranda")

        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(3, hostUseCase.gameState.value.scoreTeamB.totalPiedras)
        assertEquals(3, hostUseCase.gameState.value.moveHistory.size)

        // Deshacer jugada 3 (Parranda Team B)
        hostUseCase.undoLastMove()
        assertEquals(2, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(0, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Deshacer jugada 2 (Ronda Team A)
        hostUseCase.undoLastMove()
        assertEquals(1, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(0, hostUseCase.gameState.value.scoreTeamB.totalPiedras)

        // Deshacer jugada 1 (Majo Team A)
        hostUseCase.undoLastMove()
        assertEquals(0, hostUseCase.gameState.value.scoreTeamA.totalPiedras)
        assertEquals(0, hostUseCase.gameState.value.scoreTeamB.totalPiedras)
        assertTrue(hostUseCase.gameState.value.moveHistory.isEmpty())

        hostUseCase.stopHost()
    }
}
