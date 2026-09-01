package com.app.rondacanaria.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GameHistoryRecord(
    val id: String,
    val timestamp: Long,
    val isLocalGame: Boolean,
    val winnerTeam: Team,
    val winnerName: String,
    val totalPiedrasWinner: Int,
    val teamAName: String,
    val teamAPiedras: Int,
    val teamBName: String,
    val teamBPiedras: Int,
    val teamCName: String? = null,
    val teamCPiedras: Int? = null,
    val maxPlayers: Int = 4
)
