package com.app.rondacanaria.data.history

import android.content.Context
import com.app.rondacanaria.data.model.GameHistoryRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameHistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("ronda_game_history_prefs", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _history = MutableStateFlow<List<GameHistoryRecord>>(emptyList())
    val history: StateFlow<List<GameHistoryRecord>> = _history.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val rawJson = prefs.getString(KEY_HISTORY, null)
        if (!rawJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<GameHistoryRecord>>(rawJson)
                _history.value = list
            } catch (e: Exception) {
                _history.value = emptyList()
            }
        }
    }

    fun saveGame(record: GameHistoryRecord) {
        val currentList = _history.value.toMutableList()
        currentList.removeAll { it.id == record.id }
        currentList.add(0, record)
        val trimmedList = currentList.take(MAX_HISTORY_ITEMS)
        _history.value = trimmedList
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(trimmedList)).apply()
    }

    fun deleteGame(gameId: String) {
        val currentList = _history.value.toMutableList()
        val removed = currentList.removeAll { it.id == gameId }
        if (removed) {
            _history.value = currentList
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(currentList)).apply()
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    companion object {
        private const val KEY_HISTORY = "game_history_list"
        const val MAX_HISTORY_ITEMS = 30
    }
}
