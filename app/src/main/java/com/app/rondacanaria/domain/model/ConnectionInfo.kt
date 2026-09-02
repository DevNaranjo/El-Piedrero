package com.app.rondacanaria.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ConnectionInfo(
    val ip: String,
    val port: Int,
    val gameId: String,
    val hostName: String,
    val teamAName: String = "Equipo A",
    val teamBName: String = "Equipo B",
    val teamCName: String = "Equipo C",
    val teamDName: String = "Equipo D",
    val maxPlayers: Int = 4,
    val roomToken: String = "",
    val secretKey: String = ""
) {
    fun toJson(): String {
        return Json.encodeToString(this)
    }

    companion object {
        fun fromJson(jsonStr: String): ConnectionInfo? {
            return try {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<ConnectionInfo>(jsonStr.trim())
            } catch (e: Exception) {
                null
            }
        }
    }
}
