package com.example.milo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Feature 2: Hands-Free Voice Mode (MiloVoiceHelper.kt)
 * Integrates Android's native SpeechRecognizer directly into Jetpack Compose so field agents
 * can talk to Milo while driving or visiting client sites.
 * Also provides Milo Text-To-Speech audio briefing on app open with centralized reactive mute controls!
 */
class MiloVoiceHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onErrorState: (String) -> Unit = {}
) : RecognitionListener {

    companion object {
        private const val TAG = "MiloVoiceHelper"

        val isMutedFlow: StateFlow<Boolean>
            get() = VoiceManager.getInstance(com.example.MBTrakerApp.instance).isMuted

        fun syncMuteState(context: Context): Boolean {
            return VoiceManager.getInstance(context).isMuted.value
        }

        fun isMuted(context: Context): Boolean {
            return VoiceManager.getInstance(context).isMuted.value
        }

        fun setMuted(context: Context, muted: Boolean) {
            VoiceManager.getInstance(context).setMuted(muted)
        }

        fun toggleMuted(context: Context): Boolean {
            return VoiceManager.getInstance(context).toggleMute()
        }

        fun isSpeaking(): Boolean {
            return VoiceManager.getInstance(com.example.MBTrakerApp.instance).isSpeaking.value
        }

        fun getVoiceOption(context: Context): String {
            return VoiceManager.getInstance(context).voiceOption.value
        }

        fun setVoiceOption(context: Context, option: String) {
            VoiceManager.getInstance(context).setVoiceOption(option)
        }

        fun speakDailyBriefing(
            context: Context,
            employeeName: String,
            pendingTasksCount: Int,
            activeProjectsCount: Int,
            leadsCount: Int,
            callLogsCount: Int,
            force: Boolean = false
        ) {
            VoiceManager.getInstance(context).speakDailyBriefing(
                employeeName = employeeName,
                pendingTasksCount = pendingTasksCount,
                activeProjectsCount = activeProjectsCount,
                leadsCount = leadsCount,
                callLogsCount = callLogsCount,
                force = force
            )
        }

        fun speakText(context: Context, text: String, force: Boolean = false) {
            com.example.util.MiloHaptics.performRoarResponse(context)
            VoiceManager.getInstance(context).streamSpeech(text, force = force)
        }

        fun stopSpeaking() {
            VoiceManager.getInstance(com.example.MBTrakerApp.instance).stopSpeaking()
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@MiloVoiceHelper)
            }
        }
    }

    fun startListening() {
        if (speechRecognizer == null) {
            onErrorState("Speech recognition not available on device")
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            com.example.util.MiloHaptics.performListening(context)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("MiloVoiceHelper", "Failed to start speech listener", e)
            onErrorState("Error initializing voice: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("MiloVoiceHelper", "Error stopping listener", e)
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("MiloVoiceHelper", "Error destroying speech recognizer", e)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {
        com.example.util.MiloHaptics.performListening(context)
    }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Speech permissions required"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
            else -> "Voice input failed ($error)"
        }
        Log.w("MiloVoiceHelper", "Speech recognizer error: $errorMessage")
        onErrorState(errorMessage)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            Log.d("MiloVoiceHelper", "Recognized voice text: $text")
            com.example.util.MiloHaptics.performActionSuccess(context)
            onResult(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            onResult(matches[0])
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}

/**
 * State object for Jetpack Compose Voice Mode.
 */
data class MiloVoiceState(
    val isListening: Boolean = false,
    val spokenText: String = "",
    val error: String? = null
)

@Composable
fun rememberMiloVoiceState(
    onCommandRecognized: (String) -> Unit
): Pair<MiloVoiceState, () -> Unit> {
    val context = LocalContext.current
    var voiceState by remember { mutableStateOf(MiloVoiceState()) }

    val helper = remember {
        MiloVoiceHelper(
            context = context,
            onResult = { resultText ->
                voiceState = voiceState.copy(
                    isListening = false,
                    spokenText = resultText,
                    error = null
                )
                onCommandRecognized(resultText)
            },
            onErrorState = { err ->
                voiceState = voiceState.copy(
                    isListening = false,
                    error = err
                )
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            helper.destroy()
        }
    }

    val toggleListening = {
        if (voiceState.isListening) {
            helper.stopListening()
            voiceState = voiceState.copy(isListening = false)
        } else {
            voiceState = voiceState.copy(isListening = true, error = null)
            helper.startListening()
        }
    }

    return Pair(voiceState, toggleListening)
}

@Composable
fun rememberMiloMuteState(): Pair<Boolean, () -> Unit> {
    val context = LocalContext.current
    val isMuted by MiloVoiceHelper.isMutedFlow.collectAsState()

    LaunchedEffect(context) {
        MiloVoiceHelper.syncMuteState(context)
    }

    val toggleMute: () -> Unit = {
        MiloVoiceHelper.toggleMuted(context)
        com.example.util.MiloHaptics.performButtonTap(context)
    }

    return Pair(isMuted, toggleMute)
}

