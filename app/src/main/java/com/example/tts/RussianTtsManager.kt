package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class RussianTtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isRussianSupported = MutableStateFlow(false)
    val isRussianSupported: StateFlow<Boolean> = _isRussianSupported.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentUtteranceId = MutableStateFlow<String?>(null)
    val currentUtteranceId: StateFlow<String?> = _currentUtteranceId.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val russianLocale = Locale("ru", "RU")
            val result = tts?.setLanguage(russianLocale) ?: TextToSpeech.LANG_MISSING_DATA

            val supported = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            _isRussianSupported.value = supported

            if (supported) {
                tts?.setSpeechRate(0.92f) // Gentle, calm reading pace
                tts?.setPitch(1.0f)
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                    _currentUtteranceId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                    _currentUtteranceId.value = null
                    onCompletionCallback?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    _isPlaying.value = false
                    _currentUtteranceId.value = null
                }
            })

            _isReady.value = true
        } else {
            _isReady.value = false
            _isRussianSupported.value = false
        }
    }

    fun speak(text: String, utteranceId: String = "living_page_tts", onDone: () -> Unit = {}) {
        if (!_isReady.value || text.isBlank()) return

        stop()
        this.onCompletionCallback = onDone
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentUtteranceId.value = null
    }

    fun testRussianVoice(sampleText: String = "Здравствуйте! Приятного чтения в приложении Живая Страница.") {
        speak(sampleText, "test_voice")
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
