package com.app.rondacanaria.data.history

import android.content.Context

class AccessibilityPersistence(context: Context) {
    private val prefs = context.getSharedPreferences("ronda_accessibility_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FONT_SCALE = "font_scale_multiplier"
        const val FONT_SCALE_NORMAL = 1.0f
        const val FONT_SCALE_LARGE = 1.12f
        const val FONT_SCALE_EXTRA_LARGE = 1.25f

        private const val KEY_DEAL_REMINDER_ENABLED = "deal_reminder_enabled"
        private const val KEY_DEAL_REMINDER_SECONDS = "deal_reminder_seconds"
        const val DEFAULT_DEAL_REMINDER_SECONDS = 30
    }

    fun loadFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SCALE, FONT_SCALE_NORMAL)
    }

    fun saveFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    fun loadDealReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_DEAL_REMINDER_ENABLED, true)
    }

    fun saveDealReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEAL_REMINDER_ENABLED, enabled).apply()
    }

    fun loadDealReminderSeconds(): Int {
        return prefs.getInt(KEY_DEAL_REMINDER_SECONDS, DEFAULT_DEAL_REMINDER_SECONDS)
    }

    fun saveDealReminderSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_DEAL_REMINDER_SECONDS, seconds).apply()
    }
}
