package com.app.rondacanaria.data.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.animation.LinearInterpolator
import android.media.MediaDataSource
import android.os.Process
import com.app.rondacanaria.data.model.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class RondaAudioPlayer(private val context: Context) {

    private val tag = "RondaAudioPlayer"

    // Hilo dedicado de audio con prioridad de tiempo real (THREAD_PRIORITY_AUDIO)
    // para evitar que el codificador de vídeo de proyección/Smart TV le robe ciclos de CPU al reproductor
    private val audioThreadFactory = ThreadFactory { runnable ->
        Thread({
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (e: Exception) {
                Log.w(tag, "No se pudo asignar THREAD_PRIORITY_AUDIO al hilo de audio", e)
            }
            runnable.run()
        }, "RondaAudioPlaybackThread")
    }
    private val audioExecutor = Executors.newSingleThreadExecutor(audioThreadFactory)
    private val audioDispatcher = audioExecutor.asCoroutineDispatcher()
    private val audioScope = CoroutineScope(SupervisorJob() + audioDispatcher)

    // Búfer en memoria RAM para pistas de música ambiental: pre-carga y reproducción directa
    // desde memoria para eliminar latencias de I/O y evitar que micro-cortes vacíen el búfer
    private val bgmRamCache = ConcurrentHashMap<String, ByteArray>()

    private var soundPool: SoundPool? = null
    private var soundAddId = 0
    private var soundSubId = 0
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
    private var isPlayingOneStoneToWin = false
    private val playerLock = Any()
    private val bgmLock = Any()

    @Volatile
    var isForeground: Boolean = true

    fun isScreenInteractive(): Boolean {
        return try {
            val pm = context.getSystemService(PowerManager::class.java)
            val isInteractive = pm?.isInteractive ?: true
            val km = context.getSystemService(KeyguardManager::class.java)
            val isLocked = km?.isKeyguardLocked ?: false
            isInteractive && !isLocked
        } catch (_: Exception) {
            true
        }
    }

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
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
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
                am.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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
        // Nivel de volumen para tonos y clics de botones (sumar y restar)
        return (masterVolume * sfxVolume * 85f).toInt().coerceIn(0, 100)
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

    val isTvAudioOptimizationEnabled: Boolean = true

    var isTvCastingActive: Boolean = false
        set(value) {
            field = value
            updateBgmVolume()
        }

    private fun isExternalDisplayConnected(): Boolean {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager ?: return false
        return dm.displays.size > 1
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
        const val KEY_TV_AUDIO_OPTIMIZATION = "audio_tv_optimization_enabled"
        const val MAX_BGM_GAIN = 0.24f // Techo máximo de ganancia BGM para mantenerla siempre en nivel ambiental
    }

    init {
        initSoundPool()
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

    fun stopCurrentPlayback() {
        synchronized(playerLock) {
            voiceAudioQueue.clear()
            isPlayingVoice = false
            isPlayingBuenas = false
            isPlayingVictory = false
            isPlayingOneStoneToWin = false
            stopActiveMediaPlayerOnly()
            updateBgmVolume()
        }
    }

    fun pauseAllAudio() {
        isForeground = false
        mainHandler.removeCallbacks(trackEndFadeRunnable)
        pauseBackgroundMusic()
        stopCurrentPlayback()
        stopSfxMediaPlayer()
        try {
            soundPool?.autoPause()
        } catch (_: Exception) {}
    }

    fun resumeAllAudio() {
        isForeground = true
        try {
            soundPool?.autoResume()
        } catch (_: Exception) {}
        if (isMusicEnabled && isScreenInteractive()) {
            resumeBackgroundMusic()
        }
    }

    private fun isStoneSound(type: SoundType): Boolean {
        return type == SoundType.PIEDRA_ADD || type == SoundType.PIEDRA_SUBTRACT || type == SoundType.CARD_PLAYED
    }

    fun playSound(type: SoundType) {
        if (!isForeground || !isScreenInteractive()) return
        triggerHapticFeedback(type)
        if (!isSfxEnabled) return

        if (type == SoundType.ENTERED_BUENAS || type == SoundType.ONE_STONE_TO_WIN || type == SoundType.GAME_WON) {
            // Si hay un cántico de voz sonando, se encola para sonar inmediatamente al terminar dicho cántico sin cortarlo
            synchronized(playerLock) {
                if (isPlayingVoice) {
                    voiceAudioQueue.offer(type)
                    return
                }
                isPlayingVoice = true
                isPlayingBuenas = (type == SoundType.ENTERED_BUENAS)
                isPlayingVictory = (type == SoundType.GAME_WON)
                isPlayingOneStoneToWin = (type == SoundType.ONE_STONE_TO_WIN)
            }
            playVoiceType(type)
        } else if (isStoneSound(type)) {
            // El sonido de sumar o restar piedras NO debe cortar el audio de cuando estás en buenas o al ganar la partida
            synchronized(playerLock) {
                if (isPlayingBuenas || isPlayingVictory || isPlayingOneStoneToWin) {
                    // Reproducir el sonido de la piedra en paralelo sin interrumpir el audio de Buenas, Queda Una o Victoria
                    playSfx(type)
                    return
                }
            }
            playSfx(type)
        } else {
            // Si está sonando el audio de Buenas (o Queda Una o Victoria),
            // NO cortar dicho audio si se canta algo: encolar el cántico para que se reproduzca
            // al terminar, tal y como ocurre a la inversa para no cortar los cánticos.
            synchronized(playerLock) {
                if (isPlayingBuenas || isPlayingVictory || isPlayingOneStoneToWin) {
                    if (isVoiceAudio(type)) {
                        voiceAudioQueue.offer(type)
                    }
                    return
                }
            }

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

    private fun initSoundPool() {
        try {
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attributes)
                .build()

            val assetList = context.assets.list("") ?: emptyArray()
            val addAsset = assetList.firstOrNull { it.startsWith("piedra", true) || it.startsWith("sumar", true) || it.startsWith("click", true) }
            val subAsset = assetList.firstOrNull { it.startsWith("restar", true) || it.startsWith("pop", true) }

            if (addAsset != null) {
                val afd = context.assets.openFd(addAsset)
                soundAddId = soundPool?.load(afd.fileDescriptor, afd.startOffset, afd.length, 1) ?: 0
                afd.close()
            } else {
                val addWav = generateCrispClickWav(isAdd = true)
                if (addWav != null && addWav.exists()) {
                    soundAddId = soundPool?.load(addWav.absolutePath, 1) ?: 0
                }
            }

            if (subAsset != null) {
                val afd = context.assets.openFd(subAsset)
                soundSubId = soundPool?.load(afd.fileDescriptor, afd.startOffset, afd.length, 1) ?: 0
                afd.close()
            } else {
                val subWav = generateCrispClickWav(isAdd = false)
                if (subWav != null && subWav.exists()) {
                    soundSubId = soundPool?.load(subWav.absolutePath, 1) ?: 0
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error inicializando SoundPool", e)
        }
    }

    private fun generateCrispClickWav(isAdd: Boolean): File? {
        return try {
            val fileName = if (isAdd) "audio_sfx_stone_add.wav" else "audio_sfx_stone_sub.wav"
            val file = File(context.cacheDir, fileName)
            if (file.exists() && file.length() > 44L) {
                return file
            }

            val sampleRate = 22050
            val durationSec = if (isAdd) 0.035f else 0.040f
            val totalSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(totalSamples)

            val baseFreq = if (isAdd) 1400.0 else 750.0
            val endFreq = if (isAdd) 550.0 else 320.0
            val decayTime = if (isAdd) 0.008 else 0.011

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val progress = t / durationSec
                val freq = baseFreq + (endFreq - baseFreq) * progress
                val envelope = exp(-t / decayTime)
                val waveform = sin(2.0 * Math.PI * freq * t)
                val sampleValue = (waveform * envelope * 28000.0).toInt().coerceIn(-32768, 32767)
                samples[i] = sampleValue.toShort()
            }

            val totalAudioLen = samples.size * 2
            val totalDataLen = totalAudioLen + 36
            val byteRate = sampleRate * 2

            val header = ByteArray(44)
            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0
            header[22] = 1; header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0
            header[34] = 16; header[35] = 0
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (totalAudioLen and 0xff).toByte()
            header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

            FileOutputStream(file).use { fos ->
                fos.write(header)
                val buffer = ByteArray(totalAudioLen)
                for (j in samples.indices) {
                    val s = samples[j].toInt()
                    buffer[j * 2] = (s and 0xff).toByte()
                    buffer[j * 2 + 1] = ((s shr 8) and 0xff).toByte()
                }
                fos.write(buffer)
            }
            file
        } catch (e: Exception) {
            Log.e(tag, "Error generando WAV para SFX", e)
            null
        }
    }

    private fun playSfx(type: SoundType) {
        if (!isSfxEnabled) return

        // 1. Reproducción instantánea con SoundPool (sin bloqueos de hilo ni interferencias en Cast)
        if (type == SoundType.CARD_PLAYED || type == SoundType.PIEDRA_ADD) {
            if (soundAddId != 0 && soundPool != null) {
                val vol = getEffectiveSfxVolume(1.25f)
                soundPool?.play(soundAddId, vol, vol, 1, 0, 1.0f)
                return
            }
        } else if (type == SoundType.PIEDRA_SUBTRACT) {
            if (soundSubId != 0 && soundPool != null) {
                val vol = getEffectiveSfxVolume(1.25f)
                soundPool?.play(soundSubId, vol, vol, 1, 0, 1.0f)
                return
            }
        }

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

                    val stoneBoost = if (type == SoundType.PIEDRA_ADD || type == SoundType.PIEDRA_SUBTRACT || type == SoundType.CARD_PLAYED) 1.25f else 1.0f
                    val sfxVol = getEffectiveSfxVolume(stoneBoost)
                    sfxMediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(cachedFile.absolutePath)
                        setVolume(sfxVol, sfxVol)
                        setOnPreparedListener { it.start() }
                        setOnCompletionListener {
                            it.release()
                            if (sfxMediaPlayer == it) sfxMediaPlayer = null
                        }
                        setOnErrorListener { it, _, _ ->
                            it.release()
                            if (sfxMediaPlayer == it) sfxMediaPlayer = null
                            true
                        }
                        prepareAsync()
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

            val stoneBoost = if (assetName.contains("piedra") || assetName.contains("sumar") || assetName.contains("restar")) 1.25f else 1.0f
            val sfxVol = getEffectiveSfxVolume(stoneBoost)
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
            SoundType.ONE_STONE_TO_WIN,
            SoundType.GAME_WON,
            SoundType.LAST_DEAL_ULTIMAS -> true
            else -> false
        }
    }

    private fun playVoiceType(type: SoundType) {
        synchronized(playerLock) {
            isPlayingVoice = true
            isPlayingBuenas = (type == SoundType.ENTERED_BUENAS)
            isPlayingVictory = (type == SoundType.GAME_WON)
            isPlayingOneStoneToWin = (type == SoundType.ONE_STONE_TO_WIN)
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
                if (type == SoundType.ONE_STONE_TO_WIN) {
                    isPlayingOneStoneToWin = false
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
                if (type == SoundType.ONE_STONE_TO_WIN) {
                    isPlayingOneStoneToWin = false
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
                isPlayingOneStoneToWin = false
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
            SoundType.ONE_STONE_TO_WIN -> listOf(
                "Queda-una-piedra-para-ganar",
                "queda_una_piedra_para_ganar",
                "queda-una-piedra-para-ganar",
                "queda_una_para_ganar",
                "queda-una-para-ganar",
                "queda_una",
                "falta_una"
            )
            SoundType.GAME_WON -> listOf("victoria", "victoria_sound", "game_won", "win", "ganador")
            SoundType.LAST_DEAL_ULTIMAS -> listOf("ultimas", "ultima", "ultimo_reparto", "ultimas_cartas")
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
                setOnPreparedListener { it.start() }
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
                prepareAsync()
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
                setOnPreparedListener { it.start() }
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
                prepareAsync()
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
                SoundType.ONE_STONE_TO_WIN -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
                SoundType.GAME_WON -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                SoundType.LAST_DEAL_ULTIMAS -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                SoundType.CARD_PLAYED,
                SoundType.PIEDRA_ADD -> tg.startTone(ToneGenerator.TONE_PROP_BEEP, 85)
                SoundType.PIEDRA_SUBTRACT -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 85)
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
                        SoundType.ONE_STONE_TO_WIN -> {
                            val pattern = longArrayOf(0, 150, 80, 150)
                            val amplitudes = intArrayOf(0, 220, 0, 220)
                            v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                        }
                        SoundType.GAME_WON -> {
                            val pattern = longArrayOf(0, 250, 100, 250, 100, 450)
                            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                        }
                        SoundType.LAST_DEAL_ULTIMAS,
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

            val allFiles = (musicDirFiles + rootBgmFiles).ifEmpty {
                listOf(
                    "music/bgm_01.mp3",
                    "music/bgm_02.mp3",
                    "music/bgm_03.mp3",
                    "music/bgm_04.mp3",
                    "music/bgm_05.mp3",
                    "music/bgm_06.mp3"
                )
            }

            bgmPlaylist = allFiles.shuffled()
            Log.i(tag, "Playlist BGM cargada con ${bgmPlaylist.size} canciones (IA): $bgmPlaylist")
        } catch (e: Exception) {
            Log.e(tag, "Error cargando playlist de música BGM", e)
            bgmPlaylist = listOf(
                "music/bgm_01.mp3",
                "music/bgm_02.mp3",
                "music/bgm_03.mp3",
                "music/bgm_04.mp3",
                "music/bgm_05.mp3",
                "music/bgm_06.mp3"
            ).shuffled()
        }

        // Pre-cargar en memoria RAM las primeras pistas con prioridad THREAD_PRIORITY_AUDIO
        audioScope.launch {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Exception) {}
            bgmPlaylist.take(2).forEach { track ->
                getOrLoadTrackBytes(track)
            }
        }
    }

    /**
     * Fuente de datos multimedia en memoria RAM. Almacena 100% de la pista en memoria principal,
     * eliminando cualquier lectura de disco/flash durante la reproducción y evitando que el búfer
     * se vacíe durante micro-cortes o congestión de CPU/red por transmisión a Smart TV.
     */
    private class MemoryAudioDataSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val remaining = (data.size - position).toInt()
            val toRead = minOf(size, remaining)
            System.arraycopy(data, position.toInt(), buffer, offset, toRead)
            return toRead
        }

        override fun getSize(): Long = data.size.toLong()
        override fun close() {}
    }

    private fun getOrLoadTrackBytes(trackPath: String): ByteArray? {
        bgmRamCache[trackPath]?.let { return it }
        return try {
            val bytes = context.assets.open(trackPath).use { it.readBytes() }
            bgmRamCache[trackPath] = bytes
            Log.d(tag, "Pista de música precargada en búfer de memoria RAM (${bytes.size} bytes): $trackPath")
            bytes
        } catch (e: Exception) {
            Log.e(tag, "Error precargando pista en memoria RAM: $trackPath", e)
            null
        }
    }

    private fun preloadNextTrack(trackPath: String) {
        audioScope.launch {
            if (!bgmRamCache.containsKey(trackPath)) {
                getOrLoadTrackBytes(trackPath)
            }
        }
    }

    private fun pickNextRandomIndex(): Int {
        if (bgmPlaylist.isEmpty()) return 0
        if (bgmPlaylist.size == 1) return 0
        var next = Random.nextInt(bgmPlaylist.size)
        var attempts = 0
        while (next == currentBgmIndex && attempts < 10) {
            next = Random.nextInt(bgmPlaylist.size)
            attempts++
        }
        return next
    }

    fun startBackgroundMusic() {
        if (!isMusicEnabled) return
        requestSystemAudioFocus()
        audioScope.launch {
            synchronized(bgmLock) {
                if (bgmPlaylist.isEmpty()) {
                    loadBgmPlaylist()
                }
                if (bgmPlaylist.isEmpty()) return@synchronized
                if (bgmMediaPlayer == null) {
                    currentBgmIndex = pickNextRandomIndex()
                }
                playCurrentBgmTrack()
            }
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
        audioScope.launch {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Exception) {}
            synchronized(bgmLock) {
                if (!isMusicEnabled) return@synchronized
                if (bgmPlaylist.isEmpty()) return@synchronized
                val trackPath = bgmPlaylist[currentBgmIndex % bgmPlaylist.size]
                stopBgmMediaPlayerOnly()

                // AudioAttributes optimizados para proyección y Smart TV:
                // USAGE_MEDIA instruye a AudioFlinger a asignar un búfer nativo mayor (1-2 segundos)
                // en lugar de la baja latencia reducida de juegos que sufre microcortes en Wi-Fi.
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()

                val targetVolume = getEffectiveBgmVolume()

                try {
                    var player: MediaPlayer? = null
                    val trackBytes = getOrLoadTrackBytes(trackPath)
                    if (trackBytes != null) {
                        try {
                            player = MediaPlayer().apply {
                                setAudioAttributes(attributes)
                                setDataSource(MemoryAudioDataSource(trackBytes))
                            }
                        } catch (e: Exception) {
                            Log.w(tag, "Fallo al inicializar MediaPlayer con MemoryAudioDataSource, reintentando con descriptor", e)
                        }
                    }

                    if (player == null) {
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
                    }

                    player?.apply {
                        setVolume(0f, 0f)
                        isLooping = false
                        prepare()
                        bgmMediaPlayer = this
                        fadeInBgm(targetVolume, 1000L)

                        // Precargar la siguiente pista de fondo en memoria RAM por adelantado
                        val nextIndex = (currentBgmIndex + 1) % bgmPlaylist.size
                        preloadNextTrack(bgmPlaylist[nextIndex])

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
                    Log.i(tag, "Reproduciendo música ambiental desde búfer RAM con prioridad THREAD_PRIORITY_AUDIO: $trackPath (volumen: $targetVolume con fundido)")
                } catch (e: Exception) {
                    Log.e(tag, "Error al reproducir pista BGM: $trackPath", e)
                }
            }
        }
    }

    fun playNextBgmTrack() {
        if (!isMusicEnabled) return
        mainHandler.removeCallbacks(trackEndFadeRunnable)
        fadeOutBgm(400L) {
            audioScope.launch {
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
                } catch (_: Exception) {}
                synchronized(bgmLock) {
                    if (!isMusicEnabled) return@synchronized
                    if (bgmPlaylist.isNotEmpty()) {
                        currentBgmIndex = pickNextRandomIndex()
                        playCurrentBgmTrack()
                    }
                }
            }
        }
    }

    fun pauseBackgroundMusic() {
        mainHandler.removeCallbacks(trackEndFadeRunnable)
        bgmFadeAnimator?.cancel()
        audioScope.launch {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Exception) {}
            synchronized(bgmLock) {
                try {
                    bgmMediaPlayer?.let {
                        try {
                            it.setVolume(0f, 0f)
                        } catch (_: Exception) {}
                        if (it.isPlaying) {
                            it.pause()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error al pausar BGM", e)
                }
            }
        }
        abandonSystemAudioFocus()
    }

    fun resumeBackgroundMusic() {
        if (!isMusicEnabled) return
        requestSystemAudioFocus()
        audioScope.launch {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Exception) {}
            synchronized(bgmLock) {
                try {
                    bgmFadeAnimator?.cancel()
                    val player = bgmMediaPlayer
                    if (player != null) {
                        val targetVol = getEffectiveBgmVolume()
                        player.setVolume(targetVol, targetVol)
                        if (!player.isPlaying) {
                            player.start()
                        }
                        mainHandler.removeCallbacks(trackEndFadeRunnable)
                        val remainingMs = player.duration - player.currentPosition
                        if (remainingMs > 2500) {
                            mainHandler.postDelayed(trackEndFadeRunnable, (remainingMs - 1000L).toLong())
                        }
                    } else {
                        startBackgroundMusic()
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error al reanudar BGM", e)
                    try {
                        startBackgroundMusic()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun updateBgmVolume() {
        synchronized(bgmLock) {
            try {
                val player = bgmMediaPlayer ?: return
                if (bgmFadeAnimator?.isRunning != true) {
                    val targetVol = getEffectiveBgmVolume()
                    player.setVolume(targetVol, targetVol)
                    if (targetVol > 0f && isMusicEnabled && !player.isPlaying) {
                        try {
                            player.start()
                        } catch (_: Exception) {}
                    }
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
        soundPool?.release()
        soundPool = null
        soundAddId = 0
        soundSubId = 0
        toneGenerator?.release()
        toneGenerator = null
        try {
            audioScope.cancel()
            audioExecutor.shutdown()
            bgmRamCache.clear()
        } catch (_: Exception) {}
        try {
            val cacheFiles = context.cacheDir.listFiles { _, name -> name.startsWith("audio_") }
            cacheFiles?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
