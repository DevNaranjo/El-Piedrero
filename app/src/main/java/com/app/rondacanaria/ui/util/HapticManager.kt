package com.app.rondacanaria.ui.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.app.rondacanaria.data.model.CantoType

/**
 * Gestor de feedback háptico táctil para "El Piedrero".
 * Proporciona patrones de vibración diferenciados según el impacto de la jugada,
 * cumpliendo con las directrices de Clean Architecture y compatibilidad de Android.
 */
class HapticManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Dispara vibración calibrada según el incremento/decremento de piedras.
     * - +1 / +2: Vibración estándar (pulso táctil ligero).
     * - +3: Pulso doble medio.
     * - +4 / +5: Impacto pesado y profundo.
     */
    fun performScoreHaptic(delta: Int, isVibrationEnabled: Boolean = true) {
        if (!isVibrationEnabled || vibrator?.hasVibrator() != true) return

        try {
            val absDelta = kotlin.math.abs(delta)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when {
                    absDelta >= 4 -> {
                        // Impacto pesado para jugadas de alta puntuación (+4, +5)
                        val timings = longArrayOf(0, 70, 40, 120)
                        val amplitudes = intArrayOf(0, 220, 0, 255)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    absDelta == 3 -> {
                        // Pulso rítmico doble para Parranda (+3)
                        val timings = longArrayOf(0, 50, 40, 80)
                        val amplitudes = intArrayOf(0, 180, 0, 210)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    absDelta == 2 -> {
                        // Pulso dual ligero
                        val timings = longArrayOf(0, 40, 30, 50)
                        val amplitudes = intArrayOf(0, 160, 0, 180)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    else -> {
                        // Pulso estándar para +1 / -1
                        vibrator?.vibrate(VibrationEffect.createOneShot(45, 170))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when {
                    absDelta >= 4 -> vibrator?.vibrate(longArrayOf(0, 70, 40, 120), -1)
                    absDelta == 3 -> vibrator?.vibrate(longArrayOf(0, 50, 40, 80), -1)
                    else -> vibrator?.vibrate(45)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HapticManager", "Error ejecutando vibración de puntuación: ${e.message}")
        }
    }

    /**
     * Feedback háptico específico según el tipo tradicional de canto.
     */
    fun performCantoHaptic(cantoType: CantoType, isVibrationEnabled: Boolean = true) {
        if (!isVibrationEnabled || vibrator?.hasVibrator() != true) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (cantoType) {
                    CantoType.CARACOLILLO -> {
                        // Canto supremo (+5): triple impacto progresivo
                        val timings = longArrayOf(0, 60, 40, 80, 40, 150)
                        val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    CantoType.REQUETEMAJO, CantoType.SOBREMAJO, CantoType.REQUETECONTRAMAJO -> {
                        // Jugadas maestras mayores (+4, +5): doble impacto pesado
                        val timings = longArrayOf(0, 70, 40, 130)
                        val amplitudes = intArrayOf(0, 210, 0, 255)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    CantoType.CARACOL -> {
                        // Caracol (+4)
                        val timings = longArrayOf(0, 60, 40, 100)
                        val amplitudes = intArrayOf(0, 190, 0, 230)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    CantoType.PARRANDA, CantoType.MAJO_Y_LIMPIO, CantoType.DE_BUFOS -> {
                        // Jugadas intermedias (+3) y De bufos
                        val timings = longArrayOf(0, 50, 30, 70)
                        val amplitudes = intArrayOf(0, 170, 0, 200)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    CantoType.CONTRAMAJO, CantoType.LIMPIAR -> {
                        // Respuestas de 2 piedras
                        val timings = longArrayOf(0, 40, 30, 50)
                        val amplitudes = intArrayOf(0, 150, 0, 180)
                        vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                    }
                    else -> {
                        // Ronda, Majo (+1)
                        vibrator?.vibrate(VibrationEffect.createOneShot(45, 170))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                when (cantoType) {
                    CantoType.CARACOLILLO, CantoType.REQUETEMAJO, CantoType.SOBREMAJO -> {
                        vibrator?.vibrate(longArrayOf(0, 60, 40, 80, 40, 150), -1)
                    }
                    CantoType.CARACOL, CantoType.PARRANDA, CantoType.MAJO_Y_LIMPIO, CantoType.DE_BUFOS -> {
                        vibrator?.vibrate(longArrayOf(0, 50, 30, 70), -1)
                    }
                    else -> vibrator?.vibrate(45)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HapticManager", "Error ejecutando vibración de canto: ${e.message}")
        }
    }

    /**
     * Secuencia festiva y triunfal cuando un equipo gana la partida (21 piedras).
     */
    fun performVictoryHaptic(isVibrationEnabled: Boolean = true) {
        if (!isVibrationEnabled || vibrator?.hasVibrator() != true) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 120, 70, 160, 70, 220, 80, 400)
                val amplitudes = intArrayOf(0, 180, 0, 210, 0, 235, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 120, 70, 160, 70, 220, 80, 400), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("HapticManager", "Error ejecutando vibración de victoria: ${e.message}")
        }
    }

    /**
     * Pulsación ligera para clics de botones y confirmaciones táctiles.
     */
    fun performClickHaptic(isVibrationEnabled: Boolean = true) {
        if (!isVibrationEnabled || vibrator?.hasVibrator() != true) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }
}
