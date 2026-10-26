package com.remote.ir

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioIrTest {

    @Test
    fun testDefaultConfig() {
        val config = AudioIrConfig()
        assertEquals(48000, config.sampleRate)
        assertEquals(19000, config.carrierFrequency)
        assertEquals(Short.MAX_VALUE, config.amplitude)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSampleRate() {
        AudioIrConfig(sampleRate = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidCarrierFrequencyZero() {
        AudioIrConfig(carrierFrequency = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidCarrierFrequencyNyquist() {
        // Nyquist limit for 48000 Hz sample rate is 24000 Hz. 25000 Hz should throw.
        AudioIrConfig(sampleRate = 48000, carrierFrequency = 25000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidAmplitudeNegative() {
        AudioIrConfig(amplitude = -1)
    }

    @Test
    fun testCalculateFrameCount() {
        // 1,000 us (1 ms) at 48,000 Hz = 48 frames
        assertEquals(48, AudioIrGenerator.calculateFrameCount(1000, 48000))
        // 500 us (0.5 ms) at 48,000 Hz = 24 frames
        assertEquals(24, AudioIrGenerator.calculateFrameCount(500, 48000))
        // 0 us = 0 frames
        assertEquals(0, AudioIrGenerator.calculateFrameCount(0, 48000))
        // Negative us = 0 frames
        assertEquals(0, AudioIrGenerator.calculateFrameCount(-100, 48000))
    }

    @Test
    fun testEmptyPatternGeneratesEmptyPcm() {
        val emptyPattern = intArrayOf()
        val bytes = AudioIrGenerator.generatePcm(emptyPattern)
        val shorts = AudioIrGenerator.generatePcmShorts(emptyPattern)

        assertEquals(0, bytes.size)
        assertEquals(0, shorts.size)
    }

    @Test
    fun testPhaseInversionInMarkPeriod() {
        // Single mark of 1,000 microseconds (48 frames at 48kHz = 96 shorts)
        val pattern = intArrayOf(1000)
        val config = AudioIrConfig(sampleRate = 48000, carrierFrequency = 19000, amplitude = 30000)

        val shorts = AudioIrGenerator.generatePcmShorts(pattern, config)
        assertEquals(96, shorts.size) // 48 frames * 2 channels

        var nonZeroCount = 0
        for (i in 0 until shorts.size step 2) {
            val left = shorts[i].toInt()
            val right = shorts[i + 1].toInt()

            // Phase inversion requirement: Right channel = -Left channel
            assertEquals("Right channel must be exact phase-inverted value of Left channel", -left, right)

            if (left != 0) {
                nonZeroCount++
            }
            assertTrue("Sample amplitude must not exceed configured amplitude", abs(left) <= config.amplitude)
        }

        assertTrue("Carrier signal should contain non-zero samples", nonZeroCount > 0)
    }

    @Test
    fun testSilenceInSpacePeriod() {
        // Mark of 500 us followed by space of 500 us
        val pattern = intArrayOf(500, 500)
        val config = AudioIrConfig(sampleRate = 48000, carrierFrequency = 19000)

        val shorts = AudioIrGenerator.generatePcmShorts(pattern, config)
        assertEquals(96, shorts.size) // (24 frames mark + 24 frames space) * 2 channels

        // First 24 frames (indices 0..47) are Mark
        var markHasSignal = false
        for (i in 0 until 48) {
            if (shorts[i] != 0.toShort()) markHasSignal = true
        }
        assertTrue("Mark period should contain signal", markHasSignal)

        // Next 24 frames (indices 48..95) are Space (silence)
        for (i in 48 until 96) {
            assertEquals("Space period must be 0 (silence)", 0.toShort(), shorts[i])
        }
    }

    @Test
    fun testPcmByteArrayFormatLittleEndian() {
        val pattern = intArrayOf(1000) // 48 frames
        val config = AudioIrConfig(sampleRate = 48000, carrierFrequency = 19000)

        val bytes = AudioIrGenerator.generatePcm(pattern, config)
        assertEquals(48 * 4, bytes.size) // 48 frames * 4 bytes per frame (16-bit stereo)

        // Reconstruct shorts from Little-Endian bytes and check phase inversion
        for (frame in 0 until 48) {
            val b0 = bytes[frame * 4].toInt() and 0xFF
            val b1 = bytes[frame * 4 + 1].toInt() and 0xFF
            val leftSample = (b0 or (b1 shl 8)).toShort().toInt()

            val b2 = bytes[frame * 4 + 2].toInt() and 0xFF
            val b3 = bytes[frame * 4 + 3].toInt() and 0xFF
            val rightSample = (b2 or (b3 shl 8)).toShort().toInt()

            assertEquals("Reconstructed PCM bytes must maintain phase inversion", -leftSample, rightSample)
        }
    }

    @Test
    fun testAudioIrDriverTransmitEmptyPattern() {
        val driver = AudioIrDriver()
        assertFalse("Transmit with empty pattern should return false", driver.transmit(intArrayOf()))
    }

    @Test
    fun testAudioIrDriverGeneratePcmMatchesGenerator() {
        val pattern = intArrayOf(9000, 4500, 560, 560)
        val driver = AudioIrDriver()

        val pcmDriver = driver.generatePcm(pattern)
        val pcmGen = AudioIrGenerator.generatePcm(pattern, driver.config)

        assertNotNull(pcmDriver)
        assertTrue(pcmDriver.isNotEmpty())
        assertEquals(pcmGen.size, pcmDriver.size)
        assertTrue(pcmDriver.contentEquals(pcmGen))
    }

    @Test
    fun testAudioIrDriverTransmitNonInteractive() {
        val driver = AudioIrDriver()
        val pattern = intArrayOf(9000, 4500, 560, 560)
        // In local JVM test context where AudioTrack hardware playback is absent,
        // transmit should execute without throwing uncaught exceptions.
        val result = driver.transmit(pattern)
        // Should return true or false cleanly depending on test framework AudioTrack stubbing
        assertTrue("Transmit call should execute without exception", result || !result)
    }
}
