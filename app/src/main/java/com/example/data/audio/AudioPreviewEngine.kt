package com.example.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import com.example.model.AudioMixerSettings
import com.example.model.DialogueLanguage
import com.example.model.SceneUi
import com.example.model.VoiceMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

class AudioPreviewEngine(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var bgmJob: Job? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
        }
    }

    fun playSceneAudio(
        scope: CoroutineScope,
        scene: SceneUi,
        language: DialogueLanguage,
        voiceMode: VoiceMode,
        mixer: AudioMixerSettings
    ) {
        stop()

        // 1. Background music & SFX preview (gentle synthesized chord scaled by mixer volumes)
        val bgmGain = if (mixer.bgmMuted) 0f else mixer.bgmVolume.coerceIn(0f, 1f)
        val sfxGain = if (mixer.sfxMuted) 0f else mixer.sfxVolume.coerceIn(0f, 1f)
        if (bgmGain > 0.02f || sfxGain > 0.02f) {
            bgmJob = scope.launch(Dispatchers.Default) {
                playSoftAmbientPreview(bgmGain, sfxGain, durationMs = 2400)
            }
        }

        // 2. Spoken Voice Over / Dialog via TextToSpeech respecting VoiceMode & mutes
        if (voiceMode == VoiceMode.TANPA_SUARA || !isReady) return

        val spokenText = when (voiceMode) {
            VoiceMode.VOICE_OVER_NARRATOR -> scene.narrationText.ifBlank { scene.dialogueText }
            VoiceMode.DIALOG_KARAKTER, VoiceMode.SUARA_KARAKTER_BERBEDA -> scene.dialogueText.ifBlank { scene.narrationText }
            VoiceMode.TANPA_SUARA -> ""
        }
        if (spokenText.isBlank()) return

        val isNarration = voiceMode == VoiceMode.VOICE_OVER_NARRATOR
        val isMuted = if (isNarration) mixer.voiceOverMuted else mixer.dialogMuted
        val vol = if (isNarration) mixer.voiceOverVolume else mixer.dialogVolume
        if (isMuted || vol <= 0.02f) return

        val locale = when (language) {
            DialogueLanguage.INGGRIS -> Locale.US
            DialogueLanguage.INDONESIA, DialogueLanguage.SUNDA, DialogueLanguage.JAWA, DialogueLanguage.LAINNYA -> Locale("id", "ID")
        }
        tts?.language = locale

        // Distinct voice pitch per character when enabled
        val pitch = if (voiceMode == VoiceMode.SUARA_KARAKTER_BERBEDA) {
            val hash = (scene.dialogueSpeaker.hashCode() and 0x7FFFFFFF) % 4
            when (hash) {
                0 -> 1.18f
                1 -> 0.86f
                2 -> 1.32f
                else -> 0.96f
            }
        } else 1.0f

        tts?.setPitch(pitch)
        tts?.setSpeechRate(1.0f)

        val params = android.os.Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, vol.coerceIn(0f, 1f))
        }
        tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, params, "scene_${scene.id}")
    }

    private suspend fun playSoftAmbientPreview(bgmGain: Float, sfxGain: Float, durationMs: Int) {
        val sampleRate = 22050
        val totalSamples = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        val combinedGain = ((bgmGain * 0.12f) + (sfxGain * 0.08f)).coerceAtMost(0.22f)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = sin(PI * i / totalSamples)
            // Warm C-major pentatonic pad
            val wave = sin(2.0 * PI * 261.63 * t) * 0.5 +
                sin(2.0 * PI * 329.63 * t) * 0.3 +
                sin(2.0 * PI * 392.00 * t) * 0.2
            buffer[i] = (wave * env * combinedGain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull() ?: return

        try {
            track.play()
            var offset = 0
            while (offset < buffer.size && kotlinx.coroutines.currentCoroutineContext().isActive) {
                val chunk = minOf(1024, buffer.size - offset)
                track.write(buffer, offset, chunk)
                offset += chunk
                delay(20L)
            }
        } finally {
            runCatching { track.stop() }
            runCatching { track.release() }
        }
    }

    fun stop() {
        bgmJob?.cancel()
        bgmJob = null
        runCatching { tts?.stop() }
    }

    fun release() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }
}
