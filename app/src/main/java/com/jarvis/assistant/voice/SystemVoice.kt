package com.jarvis.assistant.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Android's built-in text-to-speech. Completion callbacks run on the main thread. */
class SystemVoice(context: Context) : VoiceEngine {
    private val main = Handler(Looper.getMainLooper())
    private val pending = ConcurrentHashMap<String, () -> Unit>()
    private var initDone = false
    private var ready = false
    private val queued = mutableListOf<Pair<String, () -> Unit>>()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        main.post {
            initDone = true
            ready = status == TextToSpeech.SUCCESS
            if (ready) configureDefaultVoice()
            queued.forEach { (t, cb) -> if (ready) speak(t, cb) else cb() }
            queued.clear()
        }
    }

    /** Configures a clear, expressive offline female English voice with natural pacing when available. */
    private fun configureDefaultVoice() {
        runCatching {
            tts.setSpeechRate(1.03f)
            tts.setPitch(1.05f)
            val voices = tts.voices.orEmpty()
            val english = voices.filter { v ->
                v.locale?.language.equals("en", ignoreCase = true) && !v.isNetworkConnectionRequired
            }
            val female = english.firstOrNull { v ->
                val name = v.name.lowercase()
                val features = v.features.orEmpty().map { it.lowercase() }
                "female" in name || features.any { "female" in it } ||
                    listOf("-sfg", "-iob", "-iol", "-tpf", "-sfb", "-gba", "-fis", "samantha", "karen", "serena", "aria", "jenny")
                        .any { it in name }
            }
            if (female != null) {
                tts.voice = female
            } else {
                tts.language = java.util.Locale.US
            }
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finish(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finish(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)
        })
    }

    private fun finish(id: String?) {
        val cb = id?.let { pending.remove(it) } ?: return
        main.post(cb)
    }

    override fun speak(text: String, onDone: () -> Unit) {
        main.post {
            if (!initDone) { queued += text to onDone; return@post }
            if (!ready) { onDone(); return@post }
            val id = UUID.randomUUID().toString()
            pending[id] = onDone
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        }
    }

    /** Stops speaking. Callbacks for the interrupted speech are dropped. */
    fun stop() {
        pending.clear()
        queued.clear()
        tts.stop()
    }

    override fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
