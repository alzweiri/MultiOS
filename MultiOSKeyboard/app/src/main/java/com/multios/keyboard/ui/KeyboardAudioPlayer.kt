package com.multios.keyboard.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.multios.keyboard.data.KeySoundProfile
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Ultra-low-latency mechanical sound engine for MultiOS Keyboard.
 * Uses synthesised PCM WAV audio loaded into [SoundPool] for zero playback latency.
 */
object KeyboardAudioPlayer {

    private var soundPool: SoundPool? = null
    private var iosTockSoundId = 0
    private var thockySoundId = 0
    private var braunClickSoundId = 0

    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()
        soundPool = sp

        val cacheDir = context.cacheDir
        val tockFile = File(cacheDir, "ios_tock.wav").apply { if (!exists()) writeBytes(generateTockWav()) }
        val thockFile = File(cacheDir, "thocky.wav").apply { if (!exists()) writeBytes(generateThockWav()) }
        val clickFile = File(cacheDir, "braun_click.wav").apply { if (!exists()) writeBytes(generateBraunClickWav()) }

        iosTockSoundId = sp.load(tockFile.absolutePath, 1)
        thockySoundId = sp.load(thockFile.absolutePath, 1)
        braunClickSoundId = sp.load(clickFile.absolutePath, 1)
    }

    fun playKeySound(profile: KeySoundProfile, volume: Float) {
        val sp = soundPool ?: return
        if (profile == KeySoundProfile.SILENT || volume <= 0.01f) return
        val soundId = when (profile) {
            KeySoundProfile.IOS_TOCK -> iosTockSoundId
            KeySoundProfile.THOCKY_LINEAR -> thockySoundId
            KeySoundProfile.BRAUN_CLICK -> braunClickSoundId
            KeySoundProfile.SILENT -> 0
        }
        if (soundId != 0) {
            val v = volume.coerceIn(0.05f, 1.0f)
            sp.play(soundId, v, v, 1, 0, 1.0f)
        }
    }

    private fun generateTockWav(): ByteArray = generatePcmWav(sampleRate = 44100, durationSec = 0.038f) { t ->
        // Sharp crisp woodblock impulse with fast exponential decay
        val env = exp(-t * 110.0)
        (sin(2.0 * PI * 1850.0 * t) * 0.7 + sin(2.0 * PI * 3400.0 * t) * 0.3) * env
    }

    private fun generateThockWav(): ByteArray = generatePcmWav(sampleRate = 44100, durationSec = 0.065f) { t ->
        // Deep thocky bottom-out sound around 320Hz + 640Hz harmonic with soft decay
        val env = exp(-t * 60.0)
        (sin(2.0 * PI * 320.0 * t) * 0.85 + sin(2.0 * PI * 680.0 * t) * 0.35) * env
    }

    private fun generateBraunClickWav(): ByteArray = generatePcmWav(sampleRate = 44100, durationSec = 0.028f) { t ->
        // High-frequency tactile mechanical snap
        val env = exp(-t * 170.0)
        (sin(2.0 * PI * 4200.0 * t) * 0.6 + sin(2.0 * PI * 2100.0 * t) * 0.4) * env
    }

    private fun generatePcmWav(sampleRate: Int, durationSec: Float, synth: (Double) -> Double): ByteArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize
        val out = java.io.ByteArrayOutputStream(totalSize + 8)

        // RIFF header
        out.write("RIFF".toByteArray())
        out.write(intToByteArray(totalSize))
        out.write("WAVE".toByteArray())

        // fmt chunk
        out.write("fmt ".toByteArray())
        out.write(intToByteArray(16)) // Subchunk1Size (16 for PCM)
        out.write(shortToByteArray(1))  // AudioFormat (1 = PCM)
        out.write(shortToByteArray(1))  // NumChannels (1 = Mono)
        out.write(intToByteArray(sampleRate))
        out.write(intToByteArray(sampleRate * 2)) // ByteRate
        out.write(shortToByteArray(2))  // BlockAlign
        out.write(shortToByteArray(16)) // BitsPerSample

        // data chunk
        out.write("data".toByteArray())
        out.write(intToByteArray(dataSize))

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val sample = (synth(t).coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
            out.write(sample.toInt() and 0xFF)
            out.write((sample.toInt() shr 8) and 0xFF)
        }
        return out.toByteArray()
    }

    private fun intToByteArray(value: Int): ByteArray = byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )

    private fun shortToByteArray(value: Short): ByteArray = byteArrayOf(
        (value.toInt() and 0xFF).toByte(),
        ((value.toInt() shr 8) and 0xFF).toByte()
    )
}
