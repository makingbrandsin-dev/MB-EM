package com.example.milo

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Playback status for VoiceManager media and TTS streams.
 */
enum class VoicePlaybackState {
    IDLE,
    PREPARING,
    SPEAKING,
    PAUSED,
    MUTED,
    ERROR
}

/**
 * Dedicated VoiceManager Service.
 * Coordinates:
 * 1. Audio Focus handling (transient ducking and polite focus acquisition/release).
 * 2. Persistent and reactive Mute toggles across all app surfaces.
 * 3. Low-latency Streaming Text-To-Speech (TTS) chunking with UtteranceProgressListener.
 * 4. Synchronization with ExoPlayer playback and Android MediaSession state.
 */
class VoiceManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "VoiceManager"
        private const val PREFS_NAME = "mb_traker_app_prefs"
        private const val KEY_MILO_MUTED = "milo_voice_muted"
        private const val KEY_VOICE_OPTION = "milo_voice_option"
        private const val MEDIA_SESSION_TAG = "MiloVoiceMediaSession"

        @Volatile
        private var INSTANCE: VoiceManager? = null

        fun getInstance(context: Context): VoiceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VoiceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val audioManager: AudioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // State Flows
    private val _isMuted = MutableStateFlow(prefs.getBoolean(KEY_MILO_MUTED, false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _playbackState = MutableStateFlow(if (_isMuted.value) VoicePlaybackState.MUTED else VoicePlaybackState.IDLE)
    val playbackState: StateFlow<VoicePlaybackState> = _playbackState.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _voiceOption = MutableStateFlow(prefs.getString(KEY_VOICE_OPTION, "INDIAN_MALE") ?: "INDIAN_MALE")
    val voiceOption: StateFlow<String> = _voiceOption.asStateFlow()

    private val _streamingProgress = MutableStateFlow(0f)
    val streamingProgress: StateFlow<Float> = _streamingProgress.asStateFlow()

    private val _currentUtteranceText = MutableStateFlow<String?>(null)
    val currentUtteranceText: StateFlow<String?> = _currentUtteranceText.asStateFlow()

    // TTS Components
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechText: String? = null
    private var hasAudioFocus = false

    // Audio Focus Request for Android O+
    private var audioFocusRequest: AudioFocusRequest? = null
    private val focusLock = Any()

    // MediaSession for system media coordination
    private var mediaSession: MediaSession? = null

    // Registered ExoPlayers
    private val registeredPlayers = CopyOnWriteArrayList<WeakReference<ExoPlayer>>()

    // Audio Focus Listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost permanently. Stopping speech.")
                hasAudioFocus = false
                stopSpeakingInternal(abandonFocus = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently. Pausing speech.")
                hasAudioFocus = false
                stopSpeakingInternal(abandonFocus = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus loss can duck. Reducing TTS volume.")
                // Handled natively by audio attributes
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus regained.")
                hasAudioFocus = true
            }
        }
    }

    init {
        setupMediaSession()
        initTtsEngine()
    }

    // ==========================================
    // MediaSession Setup
    // ==========================================
    private fun setupMediaSession() {
        try {
            mediaSession = MediaSession(appContext, MEDIA_SESSION_TAG).apply {
                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        if (_isMuted.value) setMuted(false)
                    }

                    override fun onPause() {
                        stopSpeaking()
                    }

                    override fun onStop() {
                        stopSpeaking()
                    }
                })
                setPlaybackState(
                    PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
                        .setState(
                            if (_isMuted.value) PlaybackState.STATE_PAUSED else PlaybackState.STATE_STOPPED,
                            PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                            1f
                        )
                        .build()
                )
                isActive = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error setting up MediaSession: ${e.message}")
        }
    }

    private fun updateMediaSessionState(state: Int) {
        try {
            mediaSession?.setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
                    .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error updating MediaSession state: ${e.message}")
        }
    }

    // ==========================================
    // Audio Focus Handling
    // ==========================================
    private fun requestAudioFocus(): Boolean {
        synchronized(focusLock) {
            if (hasAudioFocus) return true

            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                audioFocusRequest = request
                audioManager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }

            hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            Log.d(TAG, "Audio focus request granted: $hasAudioFocus")
            return hasAudioFocus
        }
    }

    private fun releaseAudioFocus() {
        synchronized(focusLock) {
            if (!hasAudioFocus) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
            hasAudioFocus = false
            Log.d(TAG, "Audio focus released.")
        }
    }

    // ==========================================
    // Text-To-Speech (TTS) & Voice Streaming
    // ==========================================
    private fun initTtsEngine(onReady: (() -> Unit)? = null) {
        if (tts != null && isTtsInitialized) {
            onReady?.invoke()
            return
        }

        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    configureTtsVoice()
                    setupUtteranceListener()
                    isTtsInitialized = true
                    Log.d(TAG, "VoiceManager TTS initialized successfully")

                    pendingSpeechText?.let { pending ->
                        pendingSpeechText = null
                        if (!_isMuted.value) {
                            streamSpeech(pending)
                        }
                    }
                    onReady?.invoke()
                } else {
                    Log.w(TAG, "TTS Initialization failed with status $status")
                    _playbackState.value = VoicePlaybackState.ERROR
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech: ${e.message}", e)
            _playbackState.value = VoicePlaybackState.ERROR
        }
    }

    private fun configureTtsVoice() {
        val voiceOpt = _voiceOption.value
        val targetLocale = when (voiceOpt) {
            "INDIAN_FEMALE", "INDIAN_MALE" -> Locale("en", "IN")
            "UK_ACCENT" -> Locale.UK
            "US_ACCENT" -> Locale.US
            else -> Locale.getDefault()
        }

        try {
            tts?.language = targetLocale

            val voices = tts?.voices
            if (!voices.isNullOrEmpty()) {
                val selectedVoice = when (voiceOpt) {
                    "INDIAN_MALE" -> {
                        voices.firstOrNull { voice ->
                            voice.locale.language == "en" &&
                            voice.locale.country == "IN" &&
                            (voice.name.lowercase().contains("male") || voice.name.lowercase().contains("m-") || voice.name.lowercase().contains("ind"))
                        } ?: voices.firstOrNull { voice ->
                            voice.locale.language == "en" && voice.locale.country == "IN"
                        }
                    }
                    "INDIAN_FEMALE" -> {
                        voices.firstOrNull { voice ->
                            voice.locale.language == "en" &&
                            voice.locale.country == "IN" &&
                            (voice.name.lowercase().contains("female") || voice.name.lowercase().contains("f-") || voice.name.lowercase().contains("girl") || voice.name.lowercase().contains("lady"))
                        } ?: voices.firstOrNull { voice ->
                            voice.locale.language == "en" && voice.locale.country == "IN"
                        }
                    }
                    "UK_ACCENT" -> voices.firstOrNull { it.locale.language == "en" && it.locale.country == "GB" }
                    "US_ACCENT" -> voices.firstOrNull { it.locale.language == "en" && it.locale.country == "US" }
                    else -> null
                }

                if (selectedVoice != null) {
                    tts?.voice = selectedVoice
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring voice: ${e.message}")
        }

        if (voiceOpt == "INDIAN_MALE") {
            tts?.setPitch(0.98f)
        } else {
            tts?.setPitch(1.0f)
        }
        tts?.setSpeechRate(1.02f)
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    _isSpeaking.value = true
                    _playbackState.value = VoicePlaybackState.SPEAKING
                    updateMediaSessionState(PlaybackState.STATE_PLAYING)
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    _isSpeaking.value = false
                    _streamingProgress.value = 1.0f
                    _playbackState.value = if (_isMuted.value) VoicePlaybackState.MUTED else VoicePlaybackState.IDLE
                    updateMediaSessionState(PlaybackState.STATE_STOPPED)
                    releaseAudioFocus()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    _isSpeaking.value = false
                    _playbackState.value = VoicePlaybackState.ERROR
                    updateMediaSessionState(PlaybackState.STATE_STOPPED)
                    releaseAudioFocus()
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                scope.launch {
                    Log.w(TAG, "Utterance $utteranceId error code: $errorCode")
                    _isSpeaking.value = false
                    _playbackState.value = VoicePlaybackState.ERROR
                    updateMediaSessionState(PlaybackState.STATE_STOPPED)
                    releaseAudioFocus()
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                scope.launch {
                    val full = _currentUtteranceText.value ?: ""
                    if (full.isNotEmpty()) {
                        _streamingProgress.value = (end.toFloat() / full.length.toFloat()).coerceIn(0f, 1f)
                    }
                }
            }
        })
    }

    // ==========================================
    // Public Voice Control API
    // ==========================================

    /**
     * Set mute status, persistently and reactively.
     * Muting immediately stops ongoing speech, releases audio focus, and halts MediaSession.
     */
    fun setMuted(muted: Boolean) {
        prefs.edit().putBoolean(KEY_MILO_MUTED, muted).apply()
        _isMuted.value = muted

        if (muted) {
            stopSpeaking()
            _playbackState.value = VoicePlaybackState.MUTED
            updateMediaSessionState(PlaybackState.STATE_PAUSED)
            notifyPlayersMuteState(isMuted = true)
        } else {
            if (_playbackState.value == VoicePlaybackState.MUTED) {
                _playbackState.value = VoicePlaybackState.IDLE
            }
            updateMediaSessionState(PlaybackState.STATE_STOPPED)
            notifyPlayersMuteState(isMuted = false)
        }
    }

    fun toggleMute(): Boolean {
        val next = !_isMuted.value
        setMuted(next)
        return next
    }

    fun setVoiceOption(option: String) {
        prefs.edit().putString(KEY_VOICE_OPTION, option).apply()
        _voiceOption.value = option
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isTtsInitialized = false
        initTtsEngine()
    }

    /**
     * Stream speech output using Text-To-Speech with chunking for natural, responsive conversational delivery.
     */
    fun streamSpeech(text: String, force: Boolean = false) {
        if (_isMuted.value && !force) {
            Log.d(TAG, "Muted - skipping streaming speech.")
            return
        }

        if (force && _isMuted.value) {
            setMuted(false)
        }

        val cleanText = text
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("🦁", "")
            .trim()

        if (cleanText.isBlank()) return

        _currentUtteranceText.value = cleanText
        _streamingProgress.value = 0f

        if (!isTtsInitialized || tts == null) {
            pendingSpeechText = cleanText
            _playbackState.value = VoicePlaybackState.PREPARING
            initTtsEngine()
            return
        }

        // Request audio focus before starting speech
        if (!requestAudioFocus()) {
            Log.w(TAG, "Could not acquire audio focus. Proceeding anyway.")
        }

        try {
            val utteranceId = "milo_stream_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }

            // Split into conversational chunks for immediate low-latency streaming
            val chunks = splitIntoSentences(cleanText)
            if (chunks.size <= 1) {
                tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                tts?.speak(chunks[0], TextToSpeech.QUEUE_FLUSH, params, "${utteranceId}_0")
                for (i in 1 until chunks.size) {
                    tts?.speak(chunks[i], TextToSpeech.QUEUE_ADD, params, "${utteranceId}_$i")
                }
            }
            _isSpeaking.value = true
            _playbackState.value = VoicePlaybackState.SPEAKING
            updateMediaSessionState(PlaybackState.STATE_PLAYING)
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking tts.speak: ${e.message}", e)
            _playbackState.value = VoicePlaybackState.ERROR
            releaseAudioFocus()
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        val delimiters = Regex("(?<=[.!?\\n])\\s+")
        return text.split(delimiters).filter { it.isNotBlank() }
    }

    /**
     * Immediately stops active speech and releases audio focus.
     */
    fun stopSpeaking() {
        stopSpeakingInternal(abandonFocus = true)
    }

    private fun stopSpeakingInternal(abandonFocus: Boolean) {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping TTS: ${e.message}")
        }
        _isSpeaking.value = false
        _playbackState.value = if (_isMuted.value) VoicePlaybackState.MUTED else VoicePlaybackState.IDLE
        updateMediaSessionState(PlaybackState.STATE_STOPPED)

        if (abandonFocus) {
            releaseAudioFocus()
        }
    }

    // ==========================================
    // ExoPlayer Coordination
    // ==========================================
    fun registerPlayer(player: ExoPlayer) {
        registeredPlayers.add(WeakReference(player))
        // Apply current mute state
        if (_isMuted.value) {
            player.volume = 0f
        }
    }

    fun unregisterPlayer(player: ExoPlayer) {
        registeredPlayers.removeAll { it.get() == player || it.get() == null }
    }

    private fun notifyPlayersMuteState(isMuted: Boolean) {
        val iterator = registeredPlayers.iterator()
        while (iterator.hasNext()) {
            val ref = iterator.next()
            val player = ref.get()
            if (player != null) {
                try {
                    player.volume = if (isMuted) 0f else 1f
                } catch (e: Exception) {
                    Log.w(TAG, "Error setting player volume: ${e.message}")
                }
            }
        }
    }

    fun speakDailyBriefing(
        employeeName: String,
        pendingTasksCount: Int,
        activeProjectsCount: Int,
        leadsCount: Int,
        callLogsCount: Int,
        force: Boolean = false
    ) {
        val name = if (employeeName.isNotBlank() && !employeeName.equals("User", true)) employeeName else "Team Member"
        val briefingMessage = buildString {
            append("Good day, $name! ")
            append("Here is your status update from Milo. ")
            if (pendingTasksCount > 0) {
                append("You have $pendingTasksCount pending tasks requiring attention. ")
            } else {
                append("All your tasks are currently up to date. ")
            }
            if (activeProjectsCount > 0) {
                append("There are $activeProjectsCount active projects in progress. ")
            }
            if (leadsCount > 0) {
                append("You have $leadsCount active leads in the pipeline. ")
            }
            if (callLogsCount > 0) {
                append("And $callLogsCount call logs are registered. ")
            }
            append("Ready to conquer today's goals!")
        }

        streamSpeech(briefingMessage, force = force)
    }

    fun destroy() {
        try {
            stopSpeaking()
            tts?.shutdown()
            tts = null
            isTtsInitialized = false
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying VoiceManager: ${e.message}")
        }
    }
}

/**
 * Compose state integration for VoiceManager.
 */
@Composable
fun rememberVoiceManager(): VoiceManager {
    val context = LocalContext.current
    return remember(context) { VoiceManager.getInstance(context) }
}

@Composable
fun rememberVoicePlaybackState(): VoicePlaybackState {
    val voiceManager = rememberVoiceManager()
    val state by voiceManager.playbackState.collectAsState()
    return state
}

@Composable
fun rememberMiloVoiceMuteState(): Pair<Boolean, () -> Unit> {
    val voiceManager = rememberVoiceManager()
    val isMuted by voiceManager.isMuted.collectAsState()

    val toggleMute: () -> Unit = {
        voiceManager.toggleMute()
    }

    return Pair(isMuted, toggleMute)
}
