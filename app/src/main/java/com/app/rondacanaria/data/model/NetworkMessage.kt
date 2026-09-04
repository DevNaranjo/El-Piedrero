package com.app.rondacanaria.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class MessageType {
    JOIN_REQUEST,
    JOIN_RESPONSE,
    ROOM_CONFIG_UPDATE,
    SCORE_UPDATE,
    SOUND_TRIGGER,
    END_GAME,
    GAME_STATE_BROADCAST,
    HEARTBEAT_PING,
    HEARTBEAT_PONG,
    SWITCH_TEAM,
    UNDO_LAST_MOVE,
    UPDATE_DEAL
}

@Serializable
enum class Team {
    TEAM_A,
    TEAM_B,
    TEAM_C,
    TEAM_D,
    RESERVE,
    SPECTATOR
}

@Serializable
enum class GameStatus {
    WAITING,
    PLAYING,
    PAUSED,
    FINISHED
}

@Serializable
enum class SoundType {
    ENTERED_BUENAS,
    ONE_STONE_TO_WIN,
    GAME_WON,
    CARD_PLAYED,
    CANTO_RONDA,
    CANTO_PARRANDA,
    CANTO_CARACOL,
    CANTO_CARACOLILLO,
    JUGADA_LIMPIAR,
    JUGADA_MAJO,
    JUGADA_MAJO_Y_LIMPIO,
    JUGADA_CONTRAMAJO,
    JUGADA_REQUETEMAJO,
    JUGADA_SOBREMAJO,
    JUGADA_REQUETECONTRAMAJO,
    PIEDRA_ADD,
    PIEDRA_SUBTRACT
}

@Serializable
enum class CantoType(val defaultPiedras: Int, val displayName: String, val soundType: SoundType) {
    RONDA(1, "Ronda (+1)", SoundType.CANTO_RONDA),
    PARRANDA(3, "Parranda (+3)", SoundType.CANTO_PARRANDA),
    CARACOL(4, "Caracol (+4)", SoundType.CANTO_CARACOL),
    CARACOLILLO(5, "Caracolillo (+5)", SoundType.CANTO_CARACOLILLO),
    LIMPIAR(1, "Limpiar (+1)", SoundType.JUGADA_LIMPIAR),
    MAJO(1, "Majo (+1)", SoundType.JUGADA_MAJO),
    MAJO_Y_LIMPIO(2, "Majo y Limpio (+2)", SoundType.JUGADA_MAJO_Y_LIMPIO),
    CONTRAMAJO(2, "Contramajo (+2)", SoundType.JUGADA_CONTRAMAJO),
    REQUETEMAJO(3, "Requetemajo (+3)", SoundType.JUGADA_REQUETEMAJO),
    SOBREMAJO(4, "Sobremajo (+4)", SoundType.JUGADA_SOBREMAJO),
    REQUETECONTRAMAJO(4, "Sobremajo (+4)", SoundType.JUGADA_SOBREMAJO),
    MANUAL_ADJUST(0, "Ajuste Manual", SoundType.CARD_PLAYED)
}

@Serializable
data class Player(
    val id: String,
    val name: String,
    val team: Team,
    val isHost: Boolean
)

@Serializable
data class TeamScore(
    val totalPiedras: Int = 0,
    val malas: Int = 0,
    val buenas: Int = 0,
    val isInBuenas: Boolean = false
) {
    companion object {
        const val MAX_MALAS = 11
        const val MAX_BUENAS = 10
        const val TOTAL_PIEDRAS_VICTORY = 21

        fun calculate(total: Int): TeamScore {
            val clampedTotal = total.coerceIn(0, TOTAL_PIEDRAS_VICTORY)
            val malas = minOf(clampedTotal, MAX_MALAS)
            val buenas = maxOf(0, clampedTotal - MAX_MALAS)
            val inBuenas = clampedTotal >= MAX_MALAS
            return TeamScore(
                totalPiedras = clampedTotal,
                malas = malas,
                buenas = buenas,
                isInBuenas = inBuenas
            )
        }
    }
}

@Serializable
data class GameMove(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val teamId: Team,
    val deltaPiedras: Int,
    val reason: String,
    val previousTotalPiedras: Int,
    val newTotalPiedras: Int,
    val authorName: String? = null,
    val previousReserveTeams: List<Team> = emptyList(),
    val dealNumber: Int = 1,
    val handNumber: Int = 1,
    val previousDealerId: String? = null
)

@Serializable
data class GameState(
    val gameId: String,
    val nameTeamA: String = "Equipo A",
    val nameTeamB: String = "Equipo B",
    val nameTeamC: String = "Equipo C",
    val nameTeamD: String = "Equipo D",
    val scoreTeamA: TeamScore = TeamScore.calculate(0),
    val scoreTeamB: TeamScore = TeamScore.calculate(0),
    val scoreTeamC: TeamScore = TeamScore.calculate(0),
    val scoreTeamD: TeamScore = TeamScore.calculate(0),
    val winsTeamA: Int = 0,
    val winsTeamB: Int = 0,
    val winsTeamC: Int = 0,
    val winsTeamD: Int = 0,
    val maxPlayers: Int = 4,
    val status: GameStatus = GameStatus.WAITING,
    val winnerTeam: Team? = null,
    val version: Long = 0L,
    val connectedPlayers: List<Player> = emptyList(),
    val moveHistory: List<GameMove> = emptyList(),
    val reserveTeams: List<Team> = emptyList(),
    val currentDeal: Int = 1,
    val currentHand: Int = 1,
    val dealerPlayerId: String? = null
) {
    fun maxDeals(): Int = getMaxDeals(maxPlayers)
}

fun getMaxDeals(maxPlayers: Int): Int = when (maxPlayers) {
    2 -> 6
    3 -> 4
    else -> 3
}

@Serializable
data class JoinRequestPayload(
    val playerName: String,
    val clientVersion: String = "1.0",
    val roomToken: String = ""
)

@Serializable
data class JoinResponsePayload(
    val accepted: Boolean,
    val assignedTeam: Team,
    val errorMessage: String? = null,
    val gameState: GameState? = null
)

@Serializable
data class RoomConfigPayload(
    val teamAName: String? = null,
    val teamBName: String? = null,
    val teamCName: String? = null,
    val teamDName: String? = null,
    val maxPlayers: Int? = null,
    val reserveTeams: List<Team>? = null
)

@Serializable
data class SwitchTeamPayload(
    val playerId: String,
    val targetTeam: Team,
    val playerName: String = ""
)

@Serializable
data class ScoreUpdatePayload(
    val teamId: Team,
    val cantoType: CantoType? = null,
    val piedras: Int,
    val reason: String = ""
)

@Serializable
data class SoundTriggerPayload(
    val soundType: SoundType,
    val teamId: Team? = null
)

@Serializable
data class EndGamePayload(
    val reason: String = "Partida finalizada manualmente"
)

@Serializable
data class UpdateDealPayload(
    val dealNumber: Int
)

@Serializable
data class NetworkEnvelope(
    val type: MessageType,
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sequenceNumber: Long = 0L,
    val senderId: String,
    val joinRequest: JoinRequestPayload? = null,
    val joinResponse: JoinResponsePayload? = null,
    val roomConfigUpdate: RoomConfigPayload? = null,
    val scoreUpdate: ScoreUpdatePayload? = null,
    val soundTrigger: SoundTriggerPayload? = null,
    val endGame: EndGamePayload? = null,
    val switchTeam: SwitchTeamPayload? = null,
    val updateDeal: UpdateDealPayload? = null,
    val gameStateBroadcast: GameState? = null
)
