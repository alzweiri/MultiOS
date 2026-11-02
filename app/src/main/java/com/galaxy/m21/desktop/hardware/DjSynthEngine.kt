package com.galaxy.m21.desktop.hardware

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Built-in Real-Time PCM Ambient Synth & Vinyl Platter Audio Engine.
 * Supports independent Music Volume gain, resonant bass filter, and tactile vinyl scrubbing.
 */
class DjSynthEngine(private val scope: CoroutineScope) {

    private var audioJob: Job? = null
    @Volatile var isPlaying: Boolean = false
    @Volatile var musicVolumeGain: Float = 0.85f
    @Volatile var pitchSpeed: Float = 1.0f
    @Volatile var filterCutoff: Float = 0.85f
    @Volatile var scratchVelocity: Float = 0f
    @Volatile var activeRemixFx: Int = 0
    @Volatile var currentScaleFreqs: FloatArray = floatArrayOf(110f, 130.81f, 146.83f, 164.81f, 196.0f, 220.0f)
    @Volatile var baseBpm: Int = 120

    fun startEngine(
        onProgressTick: (Float) -> Unit
    ) {
        if (audioJob?.isActive == true) return
        audioJob = scope.launch(Dispatchers.Default) {
            val sampleRate = 24000
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

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
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            }.getOrNull() ?: return@launch

            runCatching { track.play() }

            val chunk = ShortArray(512)
            var phase = 0.0
            var subPhase = 0.0
            var stepCounter = 0L

            try {
                while (isActive) {
                    val activeScratch = scratchVelocity
                    if (!isPlaying && abs(activeScratch) < 0.05f) {
                        Thread.sleep(25)
                        continue
                    }

                    val speed = when {
                        abs(activeScratch) >= 0.05f -> activeScratch.coerceIn(-3.0f, 3.0f)
                        activeRemixFx == 3 -> 0.38f
                        else -> pitchSpeed
                    }

                    val samplesPerBeat = (sampleRate * 60f / (baseBpm * abs(speed).coerceAtLeast(0.25f)))
                        .toInt()
                        .coerceAtLeast(600)
                    val notes = currentScaleFreqs
                    val gain = musicVolumeGain.coerceIn(0f, 1f)

                    for (i in chunk.indices) {
                        val beatStep = ((stepCounter / (samplesPerBeat / 4)).toInt()).mod(notes.size)
                        var noteFreq = notes[beatStep].toDouble() * abs(speed).coerceIn(0.4f, 2.5f)

                        when (activeRemixFx) {
                            1 -> noteFreq = 55.0
                            2 -> if ((stepCounter / (samplesPerBeat / 8)) % 2L == 1L) noteFreq = 0.0
                            4 -> noteFreq *= (1.0 + ((stepCounter % samplesPerBeat).toDouble() / samplesPerBeat))
                        }

                        if (abs(activeScratch) >= 0.05f) {
                            noteFreq = 180.0 + abs(activeScratch) * 260.0
                        }

                        phase += (2.0 * PI * noteFreq) / sampleRate
                        subPhase += (2.0 * PI * (noteFreq * 0.5)) / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        if (subPhase > 2.0 * PI) subPhase -= 2.0 * PI

                        val lead = sin(phase) * filterCutoff
                        val sub = sin(subPhase) * (1.15f - filterCutoff * 0.5f)
                        val env = 1.0 - ((stepCounter % (samplesPerBeat / 4)).toDouble() / (samplesPerBeat / 4).toDouble()) * 0.65

                        val sample = ((lead * 0.55 + sub * 0.45) * env * 9800.0 * gain).toInt().coerceIn(-32000, 32000)
                        chunk[i] = sample.toShort()
                        stepCounter++
                    }

                    scratchVelocity *= 0.82f

                    runCatching {
                        track.write(chunk, 0, chunk.size)
                    }

                    if (isPlaying) {
                        onProgressTick(0.0008f * speed)
                    }
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun stopEngine() {
        isPlaying = false
        audioJob?.cancel()
    }
}
