package com.app.rondacanaria.data.history

import android.content.Context
import com.app.rondacanaria.data.model.CantoType

val DEFAULT_CANTO_BUTTON_ORDER: List<CantoType> = listOf(
    CantoType.RONDA,
    CantoType.PARRANDA,
    CantoType.CARACOL,
    CantoType.CARACOLILLO,
    CantoType.LIMPIAR,
    CantoType.MAJO_Y_LIMPIO,
    CantoType.MAJO,
    CantoType.CONTRAMAJO,
    CantoType.REQUETEMAJO,
    CantoType.SOBREMAJO
)

class ButtonLayoutPersistence(context: Context) {
    private val prefs = context.getSharedPreferences("ronda_button_layout_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CANTO_ORDER = "canto_buttons_order"
    }

    fun loadButtonOrder(): List<CantoType> {
        val raw = prefs.getString(KEY_CANTO_ORDER, null) ?: return DEFAULT_CANTO_BUTTON_ORDER
        val loaded = raw.split(",").mapNotNull { name ->
            try {
                CantoType.valueOf(name.trim())
            } catch (_: Exception) {
                null
            }
        }.filter { it in DEFAULT_CANTO_BUTTON_ORDER }.distinct().toMutableList()

        DEFAULT_CANTO_BUTTON_ORDER.forEach {
            if (!loaded.contains(it)) loaded.add(it)
        }

        return if (loaded.isNotEmpty()) loaded else DEFAULT_CANTO_BUTTON_ORDER
    }

    fun saveButtonOrder(order: List<CantoType>) {
        val raw = order.joinToString(",") { it.name }
        prefs.edit().putString(KEY_CANTO_ORDER, raw).apply()
    }

    fun resetButtonOrder() {
        prefs.edit().remove(KEY_CANTO_ORDER).apply()
    }
}
