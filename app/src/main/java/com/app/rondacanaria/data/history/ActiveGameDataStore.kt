package com.app.rondacanaria.data.history

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.rondacanaria.data.model.GameState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.activeGameDataStore: DataStore<Preferences> by preferencesDataStore(name = "active_game_preferences")

@Serializable
data class LocalSavedGame(
    val gameState: GameState,
    val maxPlayers: Int,
    val teamAName: String,
    val teamBName: String,
    val teamCName: String,
    val teamDName: String,
    val isLocalGame: Boolean = true,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Repositorio moderno de persistencia reactiva basado en Jetpack DataStore Preferences.
 * Guarda y recupera de forma asíncrona y segura el estado completo de partidas activas no finalizadas.
 */
class ActiveGameDataStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Emite reactivamente la partida activa almacenada o null si no hay ninguna.
     */
    val activeGameFlow: Flow<LocalSavedGame?> = context.activeGameDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val rawJson = preferences[KEY_ACTIVE_GAME] ?: return@map null
            try {
                json.decodeFromString<LocalSavedGame>(rawJson)
            } catch (e: Exception) {
                android.util.Log.e("ActiveGameDataStore", "Error al decodificar partida activa: ${e.message}")
                null
            }
        }

    /**
     * Guarda el estado de la partida activa de forma asíncrona.
     */
    suspend fun saveActiveGame(game: LocalSavedGame) {
        try {
            val rawJson = json.encodeToString(game)
            context.activeGameDataStore.edit { preferences ->
                preferences[KEY_ACTIVE_GAME] = rawJson
            }
        } catch (e: Exception) {
            android.util.Log.e("ActiveGameDataStore", "Error al guardar partida activa en DataStore: ${e.message}")
        }
    }

    /**
     * Carga de forma puntual el último estado guardado en DataStore.
     */
    suspend fun loadActiveGame(): LocalSavedGame? {
        return try {
            activeGameFlow.firstOrNull()
        } catch (e: Exception) {
            android.util.Log.e("ActiveGameDataStore", "Error al cargar partida activa: ${e.message}")
            null
        }
    }

    /**
     * Elimina el estado de la partida activa en DataStore.
     */
    suspend fun clearActiveGame() {
        try {
            context.activeGameDataStore.edit { preferences ->
                preferences.remove(KEY_ACTIVE_GAME)
            }
        } catch (e: Exception) {
            android.util.Log.e("ActiveGameDataStore", "Error al limpiar partida activa en DataStore: ${e.message}")
        }
    }

    companion object {
        private val KEY_ACTIVE_GAME = stringPreferencesKey("active_ronda_game_json")
    }
}
