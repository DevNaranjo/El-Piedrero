package com.app.rondacanaria.data.history

import android.content.Context

class AccessibilityPersistence(context: Context) {
    private val prefs = context.getSharedPreferences("ronda_accessibility_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FONT_SCALE = "font_scale_multiplier"
        const val FONT_SCALE_NORMAL = 1.0f
        const val FONT_SCALE_LARGE = 1.12f
        const val FONT_SCALE_EXTRA_LARGE = 1.25f
    }

    fun loadFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SCALE, FONT_SCALE_NORMAL)
    }

    fun saveFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }
}
