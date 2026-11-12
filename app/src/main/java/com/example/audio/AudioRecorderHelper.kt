package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

private const val TAG = "AudioRecorderHelper"

class AudioRecorderHelper(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime: Long = 0L

    var isRecording: Boolean = false
        private set

    fun startRecording(): Boolean {
        if (isRecording) return false
        return try {
            val file = File(context.cacheDir, "record_${System.currentTimeMillis()}.m4a")
            currentOutputFile = file

            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            recorder = mr
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            Log.d(TAG, "Recording started -> ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            isRecording = false
            recorder?.release()
            recorder = null
            false
        }
    }

    suspend fun stopRecording(): Pair<ByteArray, Long>? = withContext(Dispatchers.IO) {
        if (!isRecording || recorder == null) return@withContext null
        try {
            val duration = (System.currentTimeMillis() - recordingStartTime) / 1000L
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false

            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 0) {
                val bytes = FileInputStream(file).use { it.readBytes() }
                file.delete()
                Pair(bytes, duration)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            recorder?.release()
            recorder = null
            isRecording = false
            null
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording) {
                recorder?.stop()
            }
        } catch (_: Exception) {}
        try {
            recorder?.release()
        } catch (_: Exception) {}
        recorder = null
        isRecording = false
        currentOutputFile?.delete()
        currentOutputFile = null
    }
}
