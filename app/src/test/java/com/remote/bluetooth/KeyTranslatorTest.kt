package com.remote.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyTranslatorTest {

    @Test
    fun testLowercaseLetters() {
        val aReport = KeyTranslator.translate('a')
        assertEquals(0x04.toByte(), aReport?.usageId)
        assertEquals(0x00.toByte(), aReport?.modifier)

        val zReport = KeyTranslator.translate('z')
        assertEquals(0x1D.toByte(), zReport?.usageId)
        assertEquals(0x00.toByte(), zReport?.modifier)
    }

    @Test
    fun testUppercaseLetters() {
        val aReport = KeyTranslator.translate('A')
        assertEquals(0x04.toByte(), aReport?.usageId)
        assertEquals(0x02.toByte(), aReport?.modifier) // Shift modifier

        val zReport = KeyTranslator.translate('Z')
        assertEquals(0x1D.toByte(), zReport?.usageId)
        assertEquals(0x02.toByte(), zReport?.modifier)
    }

    @Test
    fun testNumbers() {
        val oneReport = KeyTranslator.translate('1')
        assertEquals(0x1E.toByte(), oneReport?.usageId)
        assertEquals(0x00.toByte(), oneReport?.modifier)

        val zeroReport = KeyTranslator.translate('0')
        assertEquals(0x27.toByte(), zeroReport?.usageId)
        assertEquals(0x00.toByte(), zeroReport?.modifier)
    }

    @Test
    fun testSymbolsAndShiftNumbers() {
        val exclReport = KeyTranslator.translate('!')
        assertEquals(0x1E.toByte(), exclReport?.usageId) // Same as '1'
        assertEquals(0x02.toByte(), exclReport?.modifier) // Shift modifier

        val atReport = KeyTranslator.translate('@')
        assertEquals(0x1F.toByte(), atReport?.usageId) // Same as '2'
        assertEquals(0x02.toByte(), atReport?.modifier) // Shift modifier

        val questionReport = KeyTranslator.translate('?')
        assertEquals(0x38.toByte(), questionReport?.usageId)
        assertEquals(0x02.toByte(), questionReport?.modifier)
    }

    @Test
    fun testSpecialCharacters() {
        val spaceReport = KeyTranslator.translate(' ')
        assertEquals(0x2C.toByte(), spaceReport?.usageId)
        assertEquals(0x00.toByte(), spaceReport?.modifier)

        val enterReport = KeyTranslator.translate('\n')
        assertEquals(0x28.toByte(), enterReport?.usageId)
        assertEquals(0x00.toByte(), enterReport?.modifier)

        val tabReport = KeyTranslator.translate('\t')
        assertEquals(0x2B.toByte(), tabReport?.usageId)
        assertEquals(0x00.toByte(), tabReport?.modifier)
    }

    @Test
    fun testUnsupportedCharacter() {
        // e.g., a non-ASCII Unicode character that doesn't have a direct keyboard mapping
        val report = KeyTranslator.translate('§')
        assertNull(report)
    }

    @Test
    fun testMediaKeyConstants() {
        assertEquals(0x00CD, MediaKey.PLAY_PAUSE)
        assertEquals(0x00B5, MediaKey.NEXT_TRACK)
        assertEquals(0x00B6, MediaKey.PREV_TRACK)
        assertEquals(0x00B7, MediaKey.STOP)
        assertEquals(0x00E9, MediaKey.VOLUME_UP)
        assertEquals(0x00EA, MediaKey.VOLUME_DOWN)
        assertEquals(0x00E2, MediaKey.MUTE)
        assertEquals(0x0223, MediaKey.HOME)
        assertEquals(0x0224, MediaKey.BACK)
    }
}
