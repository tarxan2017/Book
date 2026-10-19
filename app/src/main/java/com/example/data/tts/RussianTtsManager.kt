package com.example.data.tts

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class RussianTtsManager(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    
    private var currentSpeed = 1.0f

    // === СИСТЕМА ПРЕДЗАГРУЗКИ ===
    private var preloadedFile: File? = null
    private var preloadedText: String? = null
    private var preloadedVoice: String? = null
    private var preloadJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    var onAudioFinishedListener: (() -> Unit)? = null

    fun preloadNext(text: String, voice: String, speed: Float) {
        if (text.isBlank()) return
        
        // Если текст и голос уже в кэше — ничего не делаем
        if (text == preloadedText && voice == preloadedVoice) return
        
        preloadJob?.cancel()
        preloadJob = scope.launch {
            try {
                val json = JSONObject().apply {
                    put("text", text)
                    put("voice", voice)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://voicecafe-final.taxo2015.workers.dev/api/tts")
                    .post(body)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (!response.isSuccessful) return@launch

                val responseBody = response.body ?: return@launch
                val inputStream = responseBody.byteStream()

                val tempFile = File(context.cacheDir, "tts_next_${System.currentTimeMillis()}.mp3")
                withContext(Dispatchers.IO) {
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                }
                
                // Удаляем старый предзагруженный файл, если он был
                preloadedFile?.delete()
                
                preloadedFile = tempFile
                preloadedText = text
                preloadedVoice = voice
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun speak(text: String, voice: String = "ru-RU-SvetlanaNeural", speed: Float = 1.0f) {
        if (text.isBlank()) return
        
        currentSpeed = speed

        // Если текст и голос совпадают с кэшем — играем мгновенно
        if (text == preloadedText && voice == preloadedVoice && preloadedFile != null) {
            val fileToPlay = preloadedFile!!
            preloadedFile = null
            preloadedText = null
            preloadedVoice = null
            preloadJob?.cancel()
            
            _isSpeaking.value = true
            _isPaused.value = false
            playAudioFile(fileToPlay)
            return
        }
        
        // Если качается прямо сейчас — ждем
        if (text == preloadedText && voice == preloadedVoice && preloadedFile == null) {
            scope.launch {
                preloadJob?.join()
                if (preloadedFile != null && voice == preloadedVoice) {
                    val fileToPlay = preloadedFile!!
                    preloadedFile = null
                    preloadedText = null
                    preloadedVoice = null
                    
                    _isSpeaking.value = true
                    _isPaused.value = false
                    playAudioFile(fileToPlay)
                } else {
                    startDownloading(text, voice)
                }
            }
            return
        }
        
        // Если голос другой — качаем заново
        startDownloading(text, voice)
    }

    private fun startDownloading(text: String, voice: String) {
        // Сбрасываем старый кэш, так как он может быть от другого голоса
        preloadJob?.cancel()
        preloadedFile?.delete()
        preloadedFile = null
        preloadedText = null
        preloadedVoice = null

        _isSpeaking.value = true
        _isPaused.value = false
        
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                val json = JSONObject().apply {
                    put("text", text)
                    put("voice", voice)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://voicecafe-final.taxo2015.workers.dev/api/tts")
                    .post(body)
                    .build()

                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (!response.isSuccessful) throw Exception("Ошибка сервера: ${response.code}")

                val responseBody = response.body ?: throw Exception("Пустой ответ")
                val inputStream = responseBody.byteStream()

                val tempFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
                withContext(Dispatchers.IO) {
                    tempFile.outputStream().use { output ->
                        inputStream.copyTo(output)
                    }
                }

                playAudioFile(tempFile)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _isSpeaking.value = false
            }
        }
    }

    private fun playAudioFile(file: File) {
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            
            setOnPreparedListener { mp ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        mp.playbackParams = mp.playbackParams.setSpeed(currentSpeed)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                mp.start()
            }
            
            setOnCompletionListener {
                _isSpeaking.value = false
                _isPaused.value = false
                file.delete()
                
                handler.postDelayed({
                    onAudioFinishedListener?.invoke()
                }, 100)
            }
            
            setOnErrorListener { _, _, _ ->
                _isSpeaking.value = false
                _isPaused.value = false
                file.delete()
                true
            }
            
            prepareAsync()
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            _isPaused.value = true
        }
    }

    fun resume() {
        if (_isPaused.value) {
            mediaPlayer?.start()
            _isPaused.value = false
        }
    }

    fun seek(deltaMs: Int) {
        mediaPlayer?.let {
            val currentPos = it.currentPosition
            val newPos = (currentPos + deltaMs).coerceIn(0, it.duration)
            it.seekTo(newPos)
        }
    }

    fun stop() {
        currentJob?.cancel()
        currentJob = null
        preloadJob?.cancel()
        preloadJob = null
        
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        _isSpeaking.value = false
        _isPaused.value = false
    }

    fun openTtsSettings() {}

    fun shutdown() {
        stop()
        preloadedFile?.delete()
        preloadedFile = null
        preloadedText = null
        preloadedVoice = null
        context.cacheDir.listFiles()?.filter { it.name.startsWith("tts_") }?.forEach { it.delete() }
    }
}
