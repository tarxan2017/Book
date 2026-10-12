package com.example.data.tts

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class RussianTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isRussianSupported = MutableStateFlow(true)
    val isRussianSupported: StateFlow<Boolean> = _isRussianSupported.asStateFlow()

    private val russianLocale = Locale("ru", "RU")

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(russianLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _isRussianSupported.value = false
            } else {
                _isRussianSupported.value = true
            }
            _isInitialized.value = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
        } else {
            _isInitialized.value = false
        }
    }

    fun speak(text: String, speed: Float = 1.0f) {
        if (text.isBlank()) return
        tts?.setSpeechRate(speed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LIVING_PAGE_TTS_${System.currentTimeMillis()}")
        _isSpeaking.value = true
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun openTtsSettings() {
        try {
            val intent = Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
