package com.app.rondacanaria.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.app.rondacanaria.data.model.SoundType
import java.io.File
import java.io.FileOutputStream

class RondaAudioPlayer(private val context: Context) {

    private val tag = "RondaAudioPlayer"
    private var toneGenerator: ToneGenerator? = null
    private var activeMediaPlayer: MediaPlayer? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e(tag, "No se pudo inicializar ToneGenerator", e)
        }
    }

    fun playSound(type: SoundType) {
        triggerHapticFeedback(type)

        // 1. Intentar reproducir el archivo de audio real
        val played = playAudioFileForType(type)
        if (!played) {
            Log.w(tag, "No se encontró archivo de audio para $type. Usando fallback acústico.")
            playSyntheticFallback(type)
        }
    }

    private fun playAudioFileForType(type: SoundType): Boolean {
        val candidateNames = when (type) {
            SoundType.CANTO_RONDA -> listOf("ronda", "canto_ronda")
            SoundType.CANTO_PARRANDA -> listOf("parranda", "canto_parranda")
            SoundType.CANTO_CARACOL -> listOf("caracol", "canto_caracol")
            SoundType.CANTO_CARACOLILLO -> listOf("caracolillo", "canto_caracolillo")
            SoundType.JUGADA_LIMPIAR -> listOf("limpio", "limpiar", "limpia", "mesa_limpia")
            SoundType.JUGADA_MAJO -> listOf("majo", "caida", "jugada_majo")
            SoundType.JUGADA_MAJO_Y_LIMPIO -> listOf("majo-y-limpio", "majo_y_limpio", "majoylimpio", "majo y limpio", "majo_limpio")
            SoundType.ENTERED_BUENAS -> listOf("buenas", "buenas_sound", "en_buenas")
            SoundType.GAME_WON -> listOf("victoria", "ganador", "game_won")
            SoundType.CARD_PLAYED -> listOf("click", "tock", "card_played")
        }

        // 1. Buscar en la carpeta assets/ (case-insensitive para admitir "Limpio.mp3", "Majo.mp3", "Majo-y-limpio.mp3", etc.)
        try {
            val assetList = context.assets.list("") ?: emptyArray()
            val matchingAsset = assetList.firstOrNull { fileName ->
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val normFile = nameWithoutExt.lowercase().replace("-", "_").replace(" ", "_")
                candidateNames.any { candidate ->
                    val normCandidate = candidate.lowercase().replace("-", "_").replace(" ", "_")
                    nameWithoutExt.equals(candidate, ignoreCase = true) || normFile == normCandidate
                }
            }
            if (matchingAsset != null) {
                val cachedFile = extractAssetToCache(matchingAsset)
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    playFile(cachedFile)
                    Log.i(tag, "Reproduciendo audio desde asset: $matchingAsset")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error buscando en assets", e)
        }

        // 2. Buscar en res/raw/ (recursos en minúsculas)
        try {
            for (candidate in candidateNames) {
                val rawId = context.resources.getIdentifier(candidate.lowercase().replace(" ", "_"), "raw", context.packageName)
                if (rawId != 0) {
                    playRaw(rawId)
                    Log.i(tag, "Reproduciendo audio desde raw: $candidate")
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error buscando en raw", e)
        }

        return false
    }

    private fun extractAssetToCache(assetName: String): File? {
        return try {
            val outFile = File(context.cacheDir, "audio_$assetName")
            if (!outFile.exists() || outFile.length() == 0L) {
                context.assets.open(assetName).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            outFile
        } catch (e: Exception) {
            Log.e(tag, "Error extrayendo asset $assetName a cache", e)
            null
        }
    }

    private fun playFile(file: File) {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()

            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(file.absolutePath)
                setVolume(1.0f, 1.0f)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al reproducir MediaPlayer desde archivo: ${file.absolutePath}", e)
        }
    }

    private fun playRaw(rawResId: Int) {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = MediaPlayer.create(context, rawResId)?.apply {
                setVolume(1.0f, 1.0f)
                start()
                setOnCompletionListener {
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al reproducir MediaPlayer desde raw: $rawResId", e)
        }
    }

    private fun playSyntheticFallback(type: SoundType) {
        try {
            when (type) {
                SoundType.CANTO_RONDA -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                SoundType.CANTO_PARRANDA -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                SoundType.CANTO_CARACOL -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 400)
                SoundType.CANTO_CARACOLILLO -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 450)
                SoundType.JUGADA_LIMPIAR -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
                SoundType.JUGADA_MAJO -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 260)
                SoundType.JUGADA_MAJO_Y_LIMPIO -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 360)
                SoundType.ENTERED_BUENAS -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                SoundType.GAME_WON -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 700)
                SoundType.CARD_PLAYED -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al emitir tono sintético", e)
        }
    }

    private fun triggerHapticFeedback(type: SoundType) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (type) {
                        SoundType.ENTERED_BUENAS -> {
                            val pattern = longArrayOf(0, 150, 100, 250)
                            val amplitudes = intArrayOf(0, 200, 0, 255)
                            v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                        }
                        SoundType.GAME_WON -> {
                            val pattern = longArrayOf(0, 200, 100, 200, 100, 400)
                            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                        }
                        SoundType.CANTO_RONDA,
                        SoundType.CANTO_PARRANDA,
                        SoundType.CANTO_CARACOL,
                        SoundType.CANTO_CARACOLILLO,
                        SoundType.JUGADA_LIMPIAR,
                        SoundType.JUGADA_MAJO -> {
                            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        SoundType.JUGADA_MAJO_Y_LIMPIO -> {
                            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 150), -1))
                        }
                        SoundType.CARD_PLAYED -> {
                            v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(150)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        activeMediaPlayer?.stop()
        activeMediaPlayer?.release()
        activeMediaPlayer = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
