package com.app.rondacanaria.data.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.animation.LinearInterpolator
import com.app.rondacanaria.data.model.SoundType
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

class RondaAudioPlayer(private val context: Context) {

    private val tag = "RondaAudioPlayer"
    private var toneGenerator: ToneGenerator? = null
    private var currentToneVolume = -1
    private var activeMediaPlayer: MediaPlayer? = null
    private var sfxMediaPlayer: MediaPlayer? = null
    private var bgmMediaPlayer: MediaPlayer? = null
    private var bgmPlaylist: List<String> = emptyList()
    private var currentBgmIndex = 0
    private var bgmFadeAnimator: ValueAnimator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val trackEndFadeRunnable = Runnable {
        if (!isMusicEnabled) return@Runnable
        fadeOutBgm(1000L) {
            if (isMusicEnabled) {
                playNextBgmTrack()
            }
        }
    }
    private val voiceAudioQueue = ConcurrentLinkedQueue<SoundType>()
    private var isPlayingVoice = false
    private var isPlayingBuenas = false
    private var isPlayingVictory = false
    private val playerLock = Any()
    private val bgmLock = Any()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseBackgroundMusic()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                updateBgmVolume()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isMusicEnabled) {
                    resumeBackgroundMusic()
                }
            }
        }
    }

    private fun requestSystemAudioFocus(): Boolean {
        val am = audioManager ?: return true
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener, mainHandler)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun abandonSystemAudioFocus() {
        val am = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {}
    }

    private val prefs = context.getSharedPreferences("ronda_audio_preferences", Context.MODE_PRIVATE)

    var isMusicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_MUSIC_ENABLED, value).apply()
            if (!value) {
                mainHandler.removeCallbacks(trackEndFadeRunnable)
                bgmFadeAnimator?.cancel()
                synchronized(bgmLock) {
                    try {
                        bgmMediaPlayer?.apply {
                            setVolume(0f, 0f)
                            if (isPlaying) {
                                pause()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error al pausar BGM", e)
                    }
                }
            } else {
                resumeBackgroundMusic()
            }
        }

    var isSfxEnabled: Boolean
        get() = prefs.getBoolean(KEY_SFX_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_SFX_ENABLED, value).apply()
            if (!value) {
                stopActiveMediaPlayerOnly()
                stopSfxMediaPlayer()
            }
            updateToneGenerator()
        }

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()
        }

    var masterVolume: Float
        get() = prefs.getFloat(KEY_MASTER_VOLUME, 1.0f)
        set(value) {
            prefs.edit().putFloat(KEY_MASTER_VOLUME, value.coerceIn(0f, 1f)).apply()
            updateBgmVolume()
            updateToneGenerator()
        }

    var musicVolume: Float
        get() = prefs.getFloat(KEY_MUSIC_VOLUME, 0.5f)
        set(value) {
            prefs.edit().putFloat(KEY_MUSIC_VOLUME, value.coerceIn(0f, 1f)).apply()
            updateBgmVolume()
        }

    var sfxVolume: Float
        get() = prefs.getFloat(KEY_SFX_VOLUME, 0.9f)
        set(value) {
            prefs.edit().putFloat(KEY_SFX_VOLUME, value.coerceIn(0f, 1f)).apply()
            updateToneGenerator()
        }

    private fun getToneGeneratorVolume(): Int {
        if (!isSfxEnabled) return 0
        // Reducir la ganancia base del tono sintético para que no aturda (escala a máx 60)
        return (masterVolume * sfxVolume * 60f).toInt().coerceIn(0, 100)
    }

    private fun updateToneGenerator() {
        val targetVolume = getToneGeneratorVolume()
        if (currentToneVolume != targetVolume) {
            try {
                toneGenerator?.release()
                toneGenerator = if (targetVolume > 0) ToneGenerator(AudioManager.STREAM_MUSIC, targetVolume) else null
                currentToneVolume = targetVolume
            } catch (e: Exception) {
                Log.e(tag, "Error actualizando ToneGenerator", e)
            }
        }
    }

    fun getEffectiveSfxVolume(nominalVolume: Float = 1.0f): Float {
        if (!isSfxEnabled) return 0f
        return (masterVolume * sfxVolume * nominalVolume).coerceIn(0f, 1f)
    }

    fun getEffectiveBgmVolume(): Float {
        if (!isMusicEnabled) return 0f
        val duck = if (isPlayingVoice) 0.33f else 1.0f
        return (masterVolume * musicVolume * MAX_BGM_GAIN * duck).coerceIn(0f, 1f)
    }

    companion object {
        const val KEY_MASTER_VOLUME = "audio_master_volume"
        const val KEY_MUSIC_VOLUME = "audio_music_volume"
        const val KEY_SFX_VOLUME = "audio_sfx_volume"
        const val KEY_MUSIC_ENABLED = "audio_music_enabled"
        const val KEY_SFX_ENABLED = "audio_sfx_enabled"
        const val KEY_VIBRATION_ENABLED = "audio_vibration_enabled"
        const val MAX_BGM_GAIN = 0.24f // Techo máximo de ganancia BGM para mantenerla siempre en nivel ambiental
    }

    init {
        updateToneGenerator()
        loadBgmPlaylist()
        if (isMusicEnabled) {
            startBackgroundMusic()
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

    private fun stopSfxMediaPlayer() {
        try {
            sfxMediaPlayer?.apply {
                setOnCompletionListener(null)
                setOnErrorListener(null)
                try {
                    if (isPlaying) stop()
                } catch (e: Exception) { }
                release()
            }
        } catch (e: Exception) {
            // ignorar
        } finally {
            sfxMediaPlayer = null
        }
    }

    private fun stopCurrentPlayback() {
        synchronized(playerLock) {
            voiceAudioQueue.clear()
            isPlayingVoice = false
            isPlayingBuenas = false
            isPlayingVictory = false
            stopActiveMediaPlayerOnly()
            updateBgmVolume()
        }
    }

    private fun isStoneSound(type: SoundType): Boolean {
        return type == SoundType.PIEDRA_ADD || type == SoundType.PIEDRA_SUBTRACT || type == SoundType.CARD_PLAYED
    }

    fun playSound(type: SoundType) {
        triggerHapticFeedback(type)
        if (!isSfxEnabled) return

        if (type == SoundType.ENTERED_BUENAS || type == SoundType.GAME_WON) {
            // Si hay un cántico de voz sonando, se encola para sonar inmediatamente al terminar dicho cántico sin cortarlo
            synchronized(playerLock) {
                if (isPlayingVoice) {
                    voiceAudioQueue.offer(type)
                    return
                }
                isPlayingVoice = true
                isPlayingBuenas = (type == SoundType.ENTERED_BUENAS)
                isPlayingVictory = (type == SoundType.GAME_WON)
            }
            playVoiceType(type)
        } else if (isStoneSound(type)) {
            // El sonido de sumar o restar piedras NO debe cortar el audio de cuando estás en buenas o al ganar la partida
            synchronized(playerLock) {
                if (isPlayingBuenas || isPlayingVictory) {
                    // Reproducir el sonido de la piedra en paralelo sin interrumpir el audio de Buenas o Victoria
                    playSfx(type)
                    return
                }
            }
            playSfx(type)
        } else {
            // Los audios de cantos se detendrán si se canta otra jugada
            stopCurrentPlayback()

            if (isVoiceAudio(type)) {
                synchronized(playerLock) {
                    isPlayingVoice = true
                }
                playVoiceType(type)
            } else {
                val volume = getEffectiveSfxVolume(1.0f)
                val played = playAudioFileForType(type, volume = volume, onComplete = null)
                if (!played) {
                    Log.w(tag, "No se encontró archivo de audio para $type. Usando fallback acústico.")
                    playSyntheticFallback(type)
                }
            }
        }
    }

    private fun playSfx(type: SoundType) {
        val candidateNames = when (type) {
            SoundType.CARD_PLAYED,
            SoundType.PIEDRA_ADD -> listOf("piedra", "sumar", "piedra_add", "click", "tock", "card_played")
            SoundType.PIEDRA_SUBTRACT -> listOf("restar", "piedra_sub", "pop", "remove", "untock")
            else -> return
        }

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
                if (playSfxAssetDirect(matchingAsset)) {
                    return
                }
                val cachedFile = extractAssetToCache(matchingAsset)
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    stopSfxMediaPlayer()
                    val attributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()

                    val sfxVol = getEffectiveSfxVolume(1.0f)
                    sfxMediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(cachedFile.absolutePath)
                        setVolume(sfxVol, sfxVol)
                        prepare()
                        start()
                        setOnCompletionListener {
                            it.release()
                            if (sfxMediaPlayer == it) sfxMediaPlayer = null
                        }
                        setOnErrorListener { it, _, _ ->
                            it.release()
                            if (sfxMediaPlayer == it) sfxMediaPlayer = null
                            true
                        }
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error buscando asset para SFX $type", e)
        }

        // Fallback acústico independiente con ToneGenerator (no detiene MediaPlayer)
        playSyntheticFallback(type)
    }

    private fun playSfxAssetDirect(assetName: String): Boolean {
        return try {
            val afd = context.assets.openFd(assetName)
            stopSfxMediaPlayer()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()

            val sfxVol = getEffectiveSfxVolume(1.0f)
            sfxMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setVolume(sfxVol, sfxVol)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    if (sfxMediaPlayer == it) sfxMediaPlayer = null
                }
                setOnErrorListener { it, _, _ ->
                    it.release()
                    if (sfxMediaPlayer == it) sfxMediaPlayer = null
                    true
                }
            }
            true
        } catch (e: Exception) {
            false
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
            SoundType.JUGADA_SOBREMAJO,
            SoundType.JUGADA_REQUETECONTRAMAJO,
            SoundType.ENTERED_BUENAS,
            SoundType.GAME_WON -> true
            else -> false
        }
    }

    private fun playVoiceType(type: SoundType) {
        synchronized(playerLock) {
            isPlayingVoice = true
            isPlayingBuenas = (type == SoundType.ENTERED_BUENAS)
            isPlayingVictory = (type == SoundType.GAME_WON)
        }
        updateBgmVolume()
        val volume = if (type == SoundType.GAME_WON) getEffectiveSfxVolume(0.35f) else getEffectiveSfxVolume(1.0f)
        val played = playAudioFileForType(type, volume = volume, onComplete = {
            synchronized(playerLock) {
                if (type == SoundType.ENTERED_BUENAS) {
                    isPlayingBuenas = false
                }
                if (type == SoundType.GAME_WON) {
                    isPlayingVictory = false
                }
            }
            onVoiceCompleted()
        })
        if (!played) {
            Log.w(tag, "No se encontró archivo de audio para $type. Usando fallback acústico.")
            playSyntheticFallback(type)
            synchronized(playerLock) {
                if (type == SoundType.ENTERED_BUENAS) {
                    isPlayingBuenas = false
                }
                if (type == SoundType.GAME_WON) {
                    isPlayingVictory = false
                }
            }
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
                isPlayingBuenas = false
                isPlayingVictory = false
                updateBgmVolume()
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
            SoundType.JUGADA_SOBREMAJO,
            SoundType.JUGADA_REQUETECONTRAMAJO -> listOf("sobremajo", "sobre_majo", "sobrmajo", "requetecontramajo", "requete_contra_majo", "requetecontra")
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
                if (playAssetDirect(matchingAsset, volume, onComplete)) {
                    Log.i(tag, "Reproduciendo audio directo desde asset: $matchingAsset (volumen: $volume)")
                    return true
                }
                val cachedFile = extractAssetToCache(matchingAsset)
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    playFile(cachedFile, volume, onComplete)
                    Log.i(tag, "Reproduciendo audio desde cache: $matchingAsset (volumen: $volume)")
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

    private fun playAssetDirect(assetName: String, volume: Float = 1.0f, onComplete: (() -> Unit)?): Boolean {
        return try {
            val afd = context.assets.openFd(assetName)
            stopActiveMediaPlayerOnly()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()

            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(attributes)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
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
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractAssetToCache(assetName: String): File? {
        return try {
            val safeName = assetName.replace("/", "_").replace("\\", "_")
            val outFile = File(context.cacheDir, "audio_$safeName")
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
        if (!isSfxEnabled) return
        updateToneGenerator()
        val tg = toneGenerator ?: return
        try {
            when (type) {
                SoundType.CANTO_RONDA -> tg.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                SoundType.CANTO_PARRANDA -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                SoundType.CANTO_CARACOL -> tg.startTone(ToneGenerator.TONE_CDMA_PIP, 400)
                SoundType.CANTO_CARACOLILLO -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 450)
                SoundType.JUGADA_LIMPIAR -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 220)
                SoundType.JUGADA_MAJO -> tg.startTone(ToneGenerator.TONE_CDMA_PIP, 260)
                SoundType.JUGADA_MAJO_Y_LIMPIO -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 360)
                SoundType.JUGADA_CONTRAMAJO -> tg.startTone(ToneGenerator.TONE_CDMA_PIP, 280)
                SoundType.JUGADA_REQUETEMAJO -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 360)
                SoundType.JUGADA_SOBREMAJO,
                SoundType.JUGADA_REQUETECONTRAMAJO -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 420)
                SoundType.ENTERED_BUENAS -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                SoundType.GAME_WON -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                SoundType.CARD_PLAYED,
                SoundType.PIEDRA_ADD -> tg.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
                SoundType.PIEDRA_SUBTRACT -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 70)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error al emitir tono sintético", e)
        }
    }

    private fun triggerHapticFeedback(type: SoundType) {
        if (!isVibrationEnabled) return
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
                        SoundType.JUGADA_SOBREMAJO,
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

    private fun stopBgmMediaPlayerOnly() {
        mainHandler.removeCallbacks(trackEndFadeRunnable)
        bgmFadeAnimator?.cancel()
        try {
            bgmMediaPlayer?.apply {
                setOnCompletionListener(null)
                setOnErrorListener(null)
                try {
                    if (isPlaying) stop()
                } catch (_: Exception) {}
                release()
            }
        } catch (_: Exception) {
        } finally {
            bgmMediaPlayer = null
        }
    }

    private fun loadBgmPlaylist() {
        try {
            val musicDirFiles = context.assets.list("music")?.filter {
                it.endsWith(".mp3", ignoreCase = true) || it.endsWith(".ogg", ignoreCase = true) || it.endsWith(".wav", ignoreCase = true)
            }?.map { "music/$it" } ?: emptyList()

            val rootBgmFiles = context.assets.list("")?.filter {
                it.startsWith("bgm_", ignoreCase = true) && (it.endsWith(".mp3", ignoreCase = true) || it.endsWith(".ogg", ignoreCase = true) || it.endsWith(".wav", ignoreCase = true))
            } ?: emptyList()

            bgmPlaylist = (musicDirFiles + rootBgmFiles).shuffled()
            Log.i(tag, "Playlist BGM cargada con ${bgmPlaylist.size} canciones: $bgmPlaylist")
        } catch (e: Exception) {
            Log.e(tag, "Error cargando playlist de música BGM", e)
            bgmPlaylist = emptyList()
        }
    }

    fun startBackgroundMusic() {
        if (!isMusicEnabled) return
        requestSystemAudioFocus()
        synchronized(bgmLock) {
            if (bgmPlaylist.isEmpty()) {
                loadBgmPlaylist()
            }
            if (bgmPlaylist.isEmpty()) return
            playCurrentBgmTrack()
        }
    }

    private fun fadeInBgm(targetVolume: Float, durationMs: Long = 1000L) {
        mainHandler.post {
            bgmFadeAnimator?.cancel()
            val player = bgmMediaPlayer ?: return@post
            try {
                player.setVolume(0f, 0f)
                if (!player.isPlaying) {
                    player.start()
                }
            } catch (_: Exception) {
                return@post
            }

            bgmFadeAnimator = ValueAnimator.ofFloat(0f, targetVolume).apply {
                duration = durationMs
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val v = animator.animatedValue as Float
                    try {
                        bgmMediaPlayer?.setVolume(v, v)
                    } catch (_: Exception) {}
                }
                start()
            }
        }
    }

    private fun fadeOutBgm(durationMs: Long = 1000L, onComplete: (() -> Unit)? = null) {
        mainHandler.post {
            bgmFadeAnimator?.cancel()
            val player = bgmMediaPlayer
            if (player == null || !player.isPlaying) {
                onComplete?.invoke()
                return@post
            }

            val currentVol = getEffectiveBgmVolume()
            bgmFadeAnimator = ValueAnimator.ofFloat(currentVol, 0f).apply {
                duration = durationMs
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val v = animator.animatedValue as Float
                    try {
                        bgmMediaPlayer?.setVolume(v, v)
                    } catch (_: Exception) {}
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        try {
                            bgmMediaPlayer?.setVolume(0f, 0f)
                        } catch (_: Exception) {}
                        onComplete?.invoke()
                    }
                })
                start()
            }
        }
    }

    private fun playCurrentBgmTrack() {
        if (!isMusicEnabled) return
        synchronized(bgmLock) {
            if (!isMusicEnabled) return
            if (bgmPlaylist.isEmpty()) return
            val trackPath = bgmPlaylist[currentBgmIndex % bgmPlaylist.size]
            stopBgmMediaPlayerOnly()

            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()

            val targetVolume = getEffectiveBgmVolume()

            try {
                var player: MediaPlayer? = null
                try {
                    val afd = context.assets.openFd(trackPath)
                    player = MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    }
                } catch (_: Exception) {
                    val cachedFile = extractAssetToCache(trackPath)
                    if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                        player = MediaPlayer().apply {
                            setAudioAttributes(attributes)
                            setDataSource(cachedFile.absolutePath)
                        }
                    }
                }

                player?.apply {
                    setVolume(0f, 0f)
                    isLooping = false
                    prepare()
                    bgmMediaPlayer = this
                    fadeInBgm(targetVolume, 1000L)

                    // Programar fundido de salida (1s) antes de que termine la canción
                    mainHandler.removeCallbacks(trackEndFadeRunnable)
                    val durationMs = duration
                    if (durationMs > 2500) {
                        mainHandler.postDelayed(trackEndFadeRunnable, (durationMs - 1000L).toLong())
                    }

                    setOnCompletionListener {
                        mainHandler.removeCallbacks(trackEndFadeRunnable)
                        it.release()
                        if (bgmMediaPlayer == it) bgmMediaPlayer = null
                        if (isMusicEnabled) {
                            playNextBgmTrack()
                        }
                    }
                    setOnErrorListener { it, _, _ ->
                        mainHandler.removeCallbacks(trackEndFadeRunnable)
                        it.release()
                        if (bgmMediaPlayer == it) bgmMediaPlayer = null
                        if (isMusicEnabled) {
                            playNextBgmTrack()
                        }
                        true
                    }
                }
                Log.i(tag, "Reproduciendo música ambiental: $trackPath (volumen: $targetVolume con fundido)")
            } catch (e: Exception) {
                Log.e(tag, "Error al reproducir pista BGM: $trackPath", e)
            }
        }
    }

    fun playNextBgmTrack() {
        if (!isMusicEnabled) return
        mainHandler.removeCallbacks(trackEndFadeRunnable)
        fadeOutBgm(1000L) {
            synchronized(bgmLock) {
                if (!isMusicEnabled) return@synchronized
                if (bgmPlaylist.isNotEmpty()) {
                    currentBgmIndex = (currentBgmIndex + 1) % bgmPlaylist.size
                    playCurrentBgmTrack()
                }
            }
        }
    }

    fun pauseBackgroundMusic() {
        mainHandler.removeCallbacks(trackEndFadeRunnable)
        fadeOutBgm(1000L) {
            abandonSystemAudioFocus()
            synchronized(bgmLock) {
                try {
                    bgmMediaPlayer?.let {
                        if (it.isPlaying) {
                            it.pause()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error al pausar BGM", e)
                }
            }
        }
    }

    fun resumeBackgroundMusic() {
        if (!isMusicEnabled) return
        requestSystemAudioFocus()
        synchronized(bgmLock) {
            try {
                bgmMediaPlayer?.let {
                    if (!it.isPlaying) {
                        fadeInBgm(getEffectiveBgmVolume(), 1000L)
                        mainHandler.removeCallbacks(trackEndFadeRunnable)
                        val remainingMs = it.duration - it.currentPosition
                        if (remainingMs > 2000) {
                            mainHandler.postDelayed(trackEndFadeRunnable, (remainingMs - 1000L).toLong())
                        }
                    }
                } ?: run {
                    startBackgroundMusic()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error al reanudar BGM", e)
            }
        }
    }

    private fun updateBgmVolume() {
        synchronized(bgmLock) {
            try {
                if (bgmFadeAnimator?.isRunning != true) {
                    val targetVol = getEffectiveBgmVolume()
                    bgmMediaPlayer?.setVolume(targetVol, targetVol)
                }
            } catch (_: Exception) {}
        }
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        bgmFadeAnimator?.cancel()
        abandonSystemAudioFocus()
        stopCurrentPlayback()
        stopSfxMediaPlayer()
        stopBgmMediaPlayerOnly()
        toneGenerator?.release()
        toneGenerator = null
        try {
            val cacheFiles = context.cacheDir.listFiles { _, name -> name.startsWith("audio_") }
            cacheFiles?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
