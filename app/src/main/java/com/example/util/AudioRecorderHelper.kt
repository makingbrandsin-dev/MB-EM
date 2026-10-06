package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

object AudioRecorderHelper {
    private const val TAG = "AudioRecorderHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var isRecording = false
    private var currentPlayingPath: String? = null

    fun isCurrentlyRecording(): Boolean = isRecording

    fun getCurrentlyPlayingPath(): String? = currentPlayingPath

    /**
     * Starts recording audio into app cache directory.
     */
    fun startRecording(context: Context): File? {
        stopPlaying()
        return try {
            val audioDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val outputFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = outputFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            stopRecording()
            null
        }
    }

    /**
     * Stops audio recording and returns the recorded file.
     */
    fun stopRecording(): File? {
        val file = currentOutputFile
        try {
            if (isRecording) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder: ${e.message}")
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return file
    }

    /**
     * Cancels recording and deletes the temporary file.
     */
    fun cancelRecording() {
        val file = stopRecording()
        try {
            file?.delete()
        } catch (_: Exception) {}
        currentOutputFile = null
    }

    /**
     * Plays a voice note from a local file path or remote Firebase Storage URL.
     */
    fun playAudio(filePathOrUrl: String, onPrepared: () -> Unit = {}, onCompletion: () -> Unit = {}) {
        stopPlaying()
        try {
            currentPlayingPath = filePathOrUrl
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePathOrUrl)
                setOnPreparedListener { mp ->
                    mp.start()
                    onPrepared()
                }
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    currentPlayingPath = null
                    onCompletion()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.w(TAG, "MediaPlayer error: what=$what, extra=$extra for $filePathOrUrl")
                    mp.release()
                    mediaPlayer = null
                    currentPlayingPath = null
                    onCompletion()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio: ${e.message}")
            currentPlayingPath = null
            onCompletion()
        }
    }

    /**
     * Stops any actively playing audio.
     */
    fun stopPlaying() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
        currentPlayingPath = null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    fun isAudioPlaying(pathOrUrl: String?): Boolean {
        if (pathOrUrl.isNullOrBlank()) return false
        return isPlaying() && currentPlayingPath == pathOrUrl
    }
}
