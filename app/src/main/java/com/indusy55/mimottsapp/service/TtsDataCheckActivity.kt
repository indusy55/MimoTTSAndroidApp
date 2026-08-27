package com.indusy55.mimottsapp.service

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech

/** Reports available voices and sample text to Android TTS settings. */
class TtsDataCheckActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == "android.speech.tts.engine.GET_SAMPLE_TEXT") {
            val language = intent.getStringExtra("language")?.lowercase()
            val sampleText = if (language == "en" || language == "eng") {
                "Hello, this is a MiMo TTS test voice."
            } else {
                "你好，这是 MiMo TTS 测试语音。"
            }
            setResult(
                TextToSpeech.LANG_AVAILABLE,
                android.content.Intent().putExtra("sampleText", sampleText)
            )
            finish()
            return
        }

        // Return all common locales as available — this engine synthesizes via API
        val result = android.content.Intent()
        result.putStringArrayListExtra(
            TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
            arrayListOf("zho-CHN", "eng-USA", "jpn-JPN", "kor-KOR")
        )
        result.putStringArrayListExtra(
            TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,
            arrayListOf()
        )
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, result)
        finish()
    }
}
