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
import java.util.concurrent.ConcurrentLinkedQueue

class RondaAudioPlayer(private val context: Context) {

    private val tag = "RondaAudioPlayer"
    private var toneGenerator: ToneGenerator? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private val voiceAudioQueue = ConcurrentLinkedQueue<SoundType>()
    private var isPlayingVoice = false
    private val playerLock = Any()

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e(tag, "No se pudo inicializar ToneGenerator", e)
        }
    }

    private fun stopActiveMediaPlayerOnly() {
        try {
            activeMediaPlayer?.apply {
                setOnCompletionListener(null)
                setOnErrorListener(null)
                try {
                    if (isPlaying) {
                        stop()
                    }
                } catch (e: Exception) {
                    // ignorar errores al detener MediaPlayer
                }
                release()
            }
        } catch (e: Exception) {
            // ignorar
        } finally {
            activeMediaPlayer = null
        }
    }

    private fun stopCurrentPlayback() {
        synchronized(playerLock) {
            voiceAudioQueue.clear()
            isPlayingVoice = false
            stopActiveMediaPlayerOnly()
        }
    }

    fun playSound(type: SoundType) {
        triggerHapticFeedback(type)

        if (type == SoundType.ENTERED_BUENAS) {
            // Solo no se debe cortar cuando suene el audio que afirma que estás en buenas:
            // Si hay un cántico de voz sonando, se encola para sonar inmediatamente al terminar dicho cántico
            synchronized(playerLock) {
                if (isPlayingVoice) {
                    voiceAudioQueue.offer(type)
                    return
                }
                isPlayingVoice = true
            }
            playVoiceType(type)
        } else {
            // Los audios se podrán cortar por ejemplo si haces dos clics en ronda.
            // Además si "Buenas" (o cualquier otro) está sonando y se le da a algún botón, se debe cortar.
            stopCurrentPlayback()

            if (isVoiceAudio(type)) {
                synchronized(playerLock) {
                    isPlayingVoice = true
                }
                playVoiceType(type)
            } else {
                // Al ganar (GAME_WON), reproducir a volumen reducido (0.35f)
                val volume = if (type == SoundType.GAME_WON) 0.35f else 1.0f
                val played = playAudioFileForType(type, volume = volume, onComplete = null)
                if (!played) {
                    Log.w(tag, "No se encontró archivo de audio para $type. Usando fallback acústico.")
                    playSyntheticFallback(type)
                }
            }
        }
    }

    private fun isVoiceAudio(type: SoundType): Boolean {
        return when (type) {
            SoundType.CANTO_RONDA,
            SoundType.CANTO_PARRANDA,
            SoundType.CANTO_CARACOL,
            SoundType.CANTO_CARACOLILLO,
            SoundType.JUGADA_LIMPIAR,
            SoundType.JUGADA_MAJO,
            SoundType.JUGADA_MAJO_Y_LIMPIO,
            SoundType.JUGADA_CONTRAMAJO,
            SoundType.JUGADA_REQUETEMAJO,
            SoundType.JUGADA_REQUETECONTRAMAJO,
            SoundType.ENTERED_BUENAS -> true
            else -> false
        }
    }

    private fun playVoiceType(type: SoundType) {
        val played = playAudioFileForType(type, onComplete = {
            onVoiceCompleted()
        })
        if (!played) {
            Log.w(tag, "No se encontró archivo de audio para $type. Usando fallback acústico.")
            playSyntheticFallback(type)
            onVoiceCompleted()
        }
    }

    private fun onVoiceCompleted() {
        synchronized(playerLock) {
            val next = voiceAudioQueue.poll()
            if (next != null) {
                playVoiceType(next)
            } else {
                isPlayingVoice = false
            }
        }
    }

    private fun playAudioFileForType(type: SoundType, volume: Float = 1.0f, onComplete: (() -> Unit)? = null): Boolean {
        val candidateNames = when (type) {
            SoundType.CANTO_RONDA -> listOf("ronda", "canto_ronda")
            SoundType.CANTO_PARRANDA -> listOf("parranda", "canto_parranda")
            SoundType.CANTO_CARACOL -> listOf("caracol", "canto_caracol")
            SoundType.CANTO_CARACOLILLO -> listOf("caracolillo", "canto_caracolillo")
            SoundType.JUGADA_LIMPIAR -> listOf("limpio", "limpiar", "limpia", "mesa_limpia")
            SoundType.JUGADA_MAJO -> listOf("majo", "caida", "jugada_majo")
            SoundType.JUGADA_MAJO_Y_LIMPIO -> listOf("majo-y-limpio", "majo_y_limpio", "majoylimpio", "majo y limpio", "majo_limpio")
            SoundType.JUGADA_CONTRAMAJO -> listOf("contramajo", "contra_majo", "contra")
            SoundType.JUGADA_REQUETEMAJO -> listOf("requetemajo", "requete_majo", "requete")
            SoundType.JUGADA_REQUETECONTRAMAJO -> listOf("requetecontramajo", "requete_contra_majo", "requetecontra")
            SoundType.ENTERED_BUENAS -> listOf("buenas", "buenas_sound", "en_buenas")
            SoundType.GAME_WON -> listOf("victoria", "victoria_sound", "game_won", "win", "ganador")
            SoundType.CARD_PLAYED,
            SoundType.PIEDRA_ADD -> listOf("piedra", "sumar", "piedra_add", "click", "tock", "card_played")
            SoundType.PIEDRA_SUBTRACT -> listOf("restar", "piedra_sub", "pop", "remove", "untock")
        }

        // 1. Buscar en la carpeta assets/ (case-insensitive para admitir "Limpio.mp3", "Majo.mp3", "Victoria.mp3", etc.)
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
                    playFile(cachedFile, volume, onComplete)
                    Log.i(tag, "Reproduciendo audio desde asset: $matchingAsset (volumen: $volume)")
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
                    playRaw(rawId, volume, onComplete)
                    Log.i(tag, "Reproduciendo audio desde raw: $candidate (volumen: $volume)")
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

    private fun playFile(file: File, volume: Float = 1.0f, onComplete: (() -> Unit)?) {
        try {
            stopActiveMediaPlayerOnly()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()

            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(file.absolutePath)
                setVolume(volume, volume)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                    onComplete?.invoke()
                }
                setOnErrorListener { it, _, _ ->
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                    onComplete?.invoke()
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al reproducir MediaPlayer desde archivo: ${file.absolutePath}", e)
            onComplete?.invoke()
        }
    }

    private fun playRaw(rawResId: Int, volume: Float = 1.0f, onComplete: (() -> Unit)?) {
        try {
            stopActiveMediaPlayerOnly()
            activeMediaPlayer = MediaPlayer.create(context, rawResId)?.apply {
                setVolume(volume, volume)
                start()
                setOnCompletionListener {
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                    onComplete?.invoke()
                }
                setOnErrorListener { it, _, _ ->
                    it.release()
                    if (activeMediaPlayer == it) activeMediaPlayer = null
                    onComplete?.invoke()
                    true
                }
            }
            if (activeMediaPlayer == null) {
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al reproducir MediaPlayer desde raw: $rawResId", e)
            onComplete?.invoke()
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
                SoundType.JUGADA_CONTRAMAJO -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 280)
                SoundType.JUGADA_REQUETEMAJO -> toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 360)
                SoundType.JUGADA_REQUETECONTRAMAJO -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 420)
                SoundType.ENTERED_BUENAS -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                SoundType.GAME_WON -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                SoundType.CARD_PLAYED,
                SoundType.PIEDRA_ADD -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                SoundType.PIEDRA_SUBTRACT -> toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 70)
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
                            val pattern = longArrayOf(0, 250, 100, 250, 100, 450)
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
                        SoundType.JUGADA_CONTRAMAJO -> {
                            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 100), -1))
                        }
                        SoundType.JUGADA_REQUETEMAJO -> {
                            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 80, 40, 100), -1))
                        }
                        SoundType.JUGADA_REQUETECONTRAMAJO -> {
                            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 40, 80, 40, 80, 40, 120), -1))
                        }
                        SoundType.CARD_PLAYED,
                        SoundType.PIEDRA_ADD -> {
                            v.vibrate(VibrationEffect.createOneShot(45, 180))
                        }
                        SoundType.PIEDRA_SUBTRACT -> {
                            v.vibrate(VibrationEffect.createOneShot(40, 140))
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (type == SoundType.GAME_WON) {
                        v.vibrate(longArrayOf(0, 250, 100, 250, 100, 450), -1)
                    } else if (type == SoundType.PIEDRA_ADD || type == SoundType.PIEDRA_SUBTRACT || type == SoundType.CARD_PLAYED) {
                        v.vibrate(40)
                    } else {
                        v.vibrate(150)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        stopCurrentPlayback()
        toneGenerator?.release()
        toneGenerator = null
    }
}
