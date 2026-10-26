package com.remote.ir

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlin.math.sin

/**
 * Configuration parameters for the Audio IR 19kHz dual-channel phase-inverted carrier generator.
 */
data class AudioIrConfig(
    val sampleRate: Int = DEFAULT_SAMPLE_RATE,
    val carrierFrequency: Int = DEFAULT_CARRIER_FREQ,
    val amplitude: Short = Short.MAX_VALUE
) {
    companion object {
        const val DEFAULT_SAMPLE_RATE = 48000
        const val DEFAULT_CARRIER_FREQ = 19000
    }

    init {
        require(sampleRate > 0) { "Sample rate must be greater than 0" }
        require(carrierFrequency > 0) { "Carrier frequency must be greater than 0" }
        require(carrierFrequency < sampleRate / 2) {
            "Carrier frequency ($carrierFrequency Hz) must be less than Nyquist frequency (${sampleRate / 2} Hz)"
        }
        require(amplitude >= 0) { "Amplitude must be non-negative" }
    }
}

/**
 * Generator for 19kHz dual-channel phase-inverted PCM audio signals used for 3.5mm IR emitters.
 */
object AudioIrGenerator {

    /**
     * Calculates the number of audio frames needed for a given duration in microseconds.
     */
    fun calculateFrameCount(durationUs: Int, sampleRate: Int): Int {
        if (durationUs <= 0 || sampleRate <= 0) return 0
        return ((durationUs.toLong() * sampleRate + 500_000L) / 1_000_000L).toInt()
    }

    /**
     * Generates a 16-bit PCM stereo byte array from an IR mark/space timing pattern (in microseconds).
     * Alternating values in [pattern] represent mark (index 0, 2, 4...) and space (index 1, 3, 5...) durations.
     * Left channel contains a 19kHz carrier sine wave during mark intervals.
     * Right channel contains a 180-degree phase-inverted carrier sine wave ($R = -L$) during mark intervals.
     */
    fun generatePcm(pattern: IntArray, config: AudioIrConfig = AudioIrConfig()): ByteArray {
        val shorts = generatePcmShorts(pattern, config)
        val pcmBytes = ByteArray(shorts.size * 2)

        for (i in shorts.indices) {
            val sample = shorts[i].toInt()
            pcmBytes[i * 2] = (sample and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
        }

        return pcmBytes
    }

    /**
     * Generates 16-bit PCM stereo samples as a [ShortArray] from an IR timing pattern.
     * Interleaved stereo layout: [Left_0, Right_0, Left_1, Right_1, ...].
     */
    fun generatePcmShorts(pattern: IntArray, config: AudioIrConfig = AudioIrConfig()): ShortArray {
        if (pattern.isEmpty()) return ShortArray(0)

        val totalFrames = pattern.sumOf { durationUs ->
            calculateFrameCount(durationUs, config.sampleRate)
        }

        if (totalFrames <= 0) return ShortArray(0)

        val shorts = ShortArray(totalFrames * 2) // 2 channels per frame
        var currentShortIndex = 0
        var globalFrameIndex = 0L

        val doublePiFcByFs = 2.0 * Math.PI * config.carrierFrequency / config.sampleRate.toDouble()
        val maxAmp = config.amplitude.toDouble()

        for (i in pattern.indices) {
            val durationUs = pattern[i]
            val numFrames = calculateFrameCount(durationUs, config.sampleRate)
            val isMark = (i % 2 == 0)

            for (frame in 0 until numFrames) {
                if (isMark) {
                    val angle = globalFrameIndex * doublePiFcByFs
                    val leftVal = (sin(angle) * maxAmp).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    val rightVal = -leftVal

                    shorts[currentShortIndex++] = leftVal.toShort()
                    shorts[currentShortIndex++] = rightVal.toShort()
                } else {
                    shorts[currentShortIndex++] = 0
                    shorts[currentShortIndex++] = 0
                }
                globalFrameIndex++
            }
        }

        return shorts
    }
}

/**
 * Driver for playing 19kHz dual-channel phase-inverted carrier IR signals through Android [AudioTrack].
 */
class AudioIrDriver(val config: AudioIrConfig = AudioIrConfig()) {

    /**
     * Transmits an IR pattern (microsecond timing array) over the 3.5mm audio jack.
     * Returns true if the PCM audio was successfully generated and queued/played via [AudioTrack].
     */
    fun transmit(pattern: IntArray): Boolean {
        if (pattern.isEmpty()) return false

        val pcmBytes = AudioIrGenerator.generatePcm(pattern, config)
        if (pcmBytes.isEmpty()) return false

        return playPcmBytes(pcmBytes)
    }

    /**
     * Helper to generate PCM bytes directly using the configured settings.
     */
    fun generatePcm(pattern: IntArray): ByteArray {
        return AudioIrGenerator.generatePcm(pattern, config)
    }

    private fun playPcmBytes(pcmBytes: ByteArray): Boolean {
        return try {
            val bufferSize = pcmBytes.size
            val audioTrack = createAudioTrack(bufferSize) ?: return false

            val writeResult = audioTrack.write(pcmBytes, 0, pcmBytes.size)
            if (writeResult < 0) {
                audioTrack.release()
                return false
            }

            audioTrack.play()
            true
        } catch (e: Throwable) {
            false
        } finally {
            // Note: In non-static mode or full lifecycle, track management would handle release after playback.
            // For static mode buffer playback, track releases on complete or error.
        }
    }

    @Suppress("DEPRECATION")
    private fun createAudioTrack(bufferSize: Int): AudioTrack? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
                            .setSampleRate(config.sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    config.sampleRate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )
            }
        } catch (e: Throwable) {
            null
        }
    }
}
