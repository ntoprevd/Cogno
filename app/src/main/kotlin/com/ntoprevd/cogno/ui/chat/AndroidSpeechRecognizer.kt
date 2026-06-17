package com.ntoprevd.cogno.ui.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import java.util.Locale

/**
 * Owns the platform SpeechRecognizer lifecycle and exposes only the callbacks
 * needed by the chat input UI.
 */
internal class AndroidSpeechRecognizer(
    context: Context,
    private val listener: Listener
) : RecognitionListener {
    private val packageName = context.packageName
    private val recognizer = preferredRecognitionService(context)?.let { component ->
        Log.d(TAG, "using recognition service $component")
        SpeechRecognizer.createSpeechRecognizer(context.applicationContext, component)
    } ?: SpeechRecognizer.createSpeechRecognizer(context.applicationContext)

    init {
        recognizer.setRecognitionListener(this)
    }

    fun start(languageTag: String?) {
        Log.d(TAG, "startListening language=${languageTag ?: "system-default"}")
        recognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                if (!languageTag.isNullOrBlank()) {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                }
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
        )
    }

    fun stop() {
        Log.d(TAG, "stopListening")
        recognizer.stopListening()
    }

    fun cancel() {
        Log.d(TAG, "cancel")
        recognizer.cancel()
    }

    fun destroy() {
        recognizer.destroy()
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        listener.onReady()
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
        listener.onSpeechStarted()
    }

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        listener.onProcessing()
    }

    override fun onError(error: Int) {
        Log.w(TAG, "onError code=$error")
        listener.onError(error)
    }

    override fun onResults(results: Bundle?) {
        val text = results.bestSpeechResult()
        Log.d(TAG, "onResults hasText=${text.isNotBlank()}")
        listener.onResult(text)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults.bestSpeechResult()
        Log.d(TAG, "onPartialResults hasText=${text.isNotBlank()}")
        listener.onPartialResult(text)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    interface Listener {
        fun onReady()
        fun onSpeechStarted()
        fun onProcessing()
        fun onPartialResult(text: String)
        fun onResult(text: String)
        fun onError(error: Int)
    }

    companion object {
        private const val TAG = "CognoSpeech"
    }
}

internal fun speechRecognitionLanguageTag(languagePreference: String): String? {
    return if (languagePreference == AppLanguagePreference.EN) {
        Locale.US.toLanguageTag()
    } else {
        Locale.SIMPLIFIED_CHINESE.toLanguageTag()
    }
}

private fun preferredRecognitionService(context: Context): ComponentName? {
    val speechServicesByGoogle = ComponentName(
        "com.google.android.tts",
        "com.google.android.apps.speech.tts.googletts.service.GoogleTTSRecognitionService"
    )
    return if (isInstalledAndEnabled(context, speechServicesByGoogle)) {
        speechServicesByGoogle
    } else {
        null
    }
}

private fun isInstalledAndEnabled(context: Context, component: ComponentName): Boolean {
    return runCatching {
        val serviceInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getServiceInfo(
                component,
                PackageManager.ComponentInfoFlags.of(
                    PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
                )
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getServiceInfo(
                component,
                PackageManager.MATCH_DISABLED_COMPONENTS
            )
        }
        serviceInfo.enabled && serviceInfo.applicationInfo.enabled
    }.getOrDefault(false)
}

internal fun hasUsableRecognitionService(context: Context): Boolean {
    if (preferredRecognitionService(context) != null) return true
    val defaultService = Settings.Secure.getString(
        context.contentResolver,
        "voice_recognition_service"
    ).orEmpty()
    return defaultService.isNotBlank() &&
        !defaultService.startsWith("com.xiaomi.mibrain.speech/")
}

@Suppress("DEPRECATION")
private fun Bundle?.bestSpeechResult(): String {
    return this
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        .orEmpty()
        .trim()
}
