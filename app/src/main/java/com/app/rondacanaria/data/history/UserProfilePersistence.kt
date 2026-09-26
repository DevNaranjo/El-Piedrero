package com.app.rondacanaria.data.history

import android.content.Context
import com.app.rondacanaria.data.model.AvatarCatalog

class UserProfilePersistence(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePlayerName(name: String) {
        prefs.edit().putString(KEY_PLAYER_NAME, name.trim()).apply()
    }

    fun loadPlayerName(): String {
        return prefs.getString(KEY_PLAYER_NAME, "") ?: ""
    }

    fun saveAvatarId(avatarId: String) {
        prefs.edit().putString(KEY_AVATAR_ID, avatarId.trim()).apply()
    }

    fun loadAvatarId(): String {
        return prefs.getString(KEY_AVATAR_ID, AvatarCatalog.DEFAULT_AVATAR_ID) ?: AvatarCatalog.DEFAULT_AVATAR_ID
    }

    companion object {
        private const val PREFS_NAME = "ronda_user_profile_prefs"
        private const val KEY_PLAYER_NAME = "user_player_name"
        private const val KEY_AVATAR_ID = "user_avatar_id"
    }
}
