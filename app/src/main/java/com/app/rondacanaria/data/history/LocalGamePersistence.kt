package com.app.rondacanaria.data.history

import android.content.Context
import com.app.rondacanaria.data.model.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LocalSavedGame(
    val gameState: GameState,
    val maxPlayers: Int,
    val teamAName: String,
    val teamBName: String,
    val teamCName: String,
    val teamDName: String
)

class LocalGamePersistence(context: Context) {
    private val prefs = context.getSharedPreferences("ronda_local_game_prefs", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun saveLocalGame(game: LocalSavedGame) {
        try {
            val raw = json.encodeToString(game)
            prefs.edit().putString(KEY_ACTIVE_GAME, raw).apply()
        } catch (e: Exception) {
            android.util.Log.e("LocalGamePersistence", "Error al guardar partida local: ${e.message}")
        }
    }

    fun loadLocalGame(): LocalSavedGame? {
        val raw = prefs.getString(KEY_ACTIVE_GAME, null) ?: return null
        return try {
            json.decodeFromString<LocalSavedGame>(raw)
        } catch (e: Exception) {
            null
        }
    }

    fun clearLocalGame() {
        prefs.edit().remove(KEY_ACTIVE_GAME).commit()
    }

    companion object {
        private const val KEY_ACTIVE_GAME = "active_local_game"
    }
}
