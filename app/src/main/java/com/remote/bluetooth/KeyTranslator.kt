package com.remote.bluetooth

/**
 * Translates UTF-8 characters to standard USB HID Usage Tables (Page 0x07) bytes.
 * Also determines if a shift modifier is required for the character.
 */
object MediaKey {
    const val PLAY_PAUSE = 0x00CD
    const val PLAY = 0x00B0
    const val PAUSE = 0x00B1
    const val NEXT_TRACK = 0x00B5
    const val PREV_TRACK = 0x00B6
    const val STOP = 0x00B7
    const val FAST_FORWARD = 0x00B3
    const val REWIND = 0x00B4
    const val VOLUME_UP = 0x00E9
    const val VOLUME_DOWN = 0x00EA
    const val MUTE = 0x00E2
    const val HOME = 0x0223
    const val BACK = 0x0224
    const val RECENTS = 0x0225
    const val SEARCH = 0x0221
    const val NOTIFICATIONS = 0x0227
    const val POWER = 0x0030
}

object KeyTranslator {

    data class HidReport(val usageId: Byte, val modifier: Byte)

    private const val MODIFIER_NONE: Byte = 0x00
    private const val MODIFIER_SHIFT: Byte = 0x02

    fun translate(char: Char): HidReport? {
        // Map based on USB HID Usage Tables Page 0x07
        return when (char) {
            'a' -> HidReport(0x04, MODIFIER_NONE)
            'b' -> HidReport(0x05, MODIFIER_NONE)
            'c' -> HidReport(0x06, MODIFIER_NONE)
            'd' -> HidReport(0x07, MODIFIER_NONE)
            'e' -> HidReport(0x08, MODIFIER_NONE)
            'f' -> HidReport(0x09, MODIFIER_NONE)
            'g' -> HidReport(0x0A, MODIFIER_NONE)
            'h' -> HidReport(0x0B, MODIFIER_NONE)
            'i' -> HidReport(0x0C, MODIFIER_NONE)
            'j' -> HidReport(0x0D, MODIFIER_NONE)
            'k' -> HidReport(0x0E, MODIFIER_NONE)
            'l' -> HidReport(0x0F, MODIFIER_NONE)
            'm' -> HidReport(0x10, MODIFIER_NONE)
            'n' -> HidReport(0x11, MODIFIER_NONE)
            'o' -> HidReport(0x12, MODIFIER_NONE)
            'p' -> HidReport(0x13, MODIFIER_NONE)
            'q' -> HidReport(0x14, MODIFIER_NONE)
            'r' -> HidReport(0x15, MODIFIER_NONE)
            's' -> HidReport(0x16, MODIFIER_NONE)
            't' -> HidReport(0x17, MODIFIER_NONE)
            'u' -> HidReport(0x18, MODIFIER_NONE)
            'v' -> HidReport(0x19, MODIFIER_NONE)
            'w' -> HidReport(0x1A, MODIFIER_NONE)
            'x' -> HidReport(0x1B, MODIFIER_NONE)
            'y' -> HidReport(0x1C, MODIFIER_NONE)
            'z' -> HidReport(0x1D, MODIFIER_NONE)

            'A' -> HidReport(0x04, MODIFIER_SHIFT)
            'B' -> HidReport(0x05, MODIFIER_SHIFT)
            'C' -> HidReport(0x06, MODIFIER_SHIFT)
            'D' -> HidReport(0x07, MODIFIER_SHIFT)
            'E' -> HidReport(0x08, MODIFIER_SHIFT)
            'F' -> HidReport(0x09, MODIFIER_SHIFT)
            'G' -> HidReport(0x0A, MODIFIER_SHIFT)
            'H' -> HidReport(0x0B, MODIFIER_SHIFT)
            'I' -> HidReport(0x0C, MODIFIER_SHIFT)
            'J' -> HidReport(0x0D, MODIFIER_SHIFT)
            'K' -> HidReport(0x0E, MODIFIER_SHIFT)
            'L' -> HidReport(0x0F, MODIFIER_SHIFT)
            'M' -> HidReport(0x10, MODIFIER_SHIFT)
            'N' -> HidReport(0x11, MODIFIER_SHIFT)
            'O' -> HidReport(0x12, MODIFIER_SHIFT)
            'P' -> HidReport(0x13, MODIFIER_SHIFT)
            'Q' -> HidReport(0x14, MODIFIER_SHIFT)
            'R' -> HidReport(0x15, MODIFIER_SHIFT)
            'S' -> HidReport(0x16, MODIFIER_SHIFT)
            'T' -> HidReport(0x17, MODIFIER_SHIFT)
            'U' -> HidReport(0x18, MODIFIER_SHIFT)
            'V' -> HidReport(0x19, MODIFIER_SHIFT)
            'W' -> HidReport(0x1A, MODIFIER_SHIFT)
            'X' -> HidReport(0x1B, MODIFIER_SHIFT)
            'Y' -> HidReport(0x1C, MODIFIER_SHIFT)
            'Z' -> HidReport(0x1D, MODIFIER_SHIFT)

            '1' -> HidReport(0x1E, MODIFIER_NONE)
            '2' -> HidReport(0x1F, MODIFIER_NONE)
            '3' -> HidReport(0x20, MODIFIER_NONE)
            '4' -> HidReport(0x21, MODIFIER_NONE)
            '5' -> HidReport(0x22, MODIFIER_NONE)
            '6' -> HidReport(0x23, MODIFIER_NONE)
            '7' -> HidReport(0x24, MODIFIER_NONE)
            '8' -> HidReport(0x25, MODIFIER_NONE)
            '9' -> HidReport(0x26, MODIFIER_NONE)
            '0' -> HidReport(0x27, MODIFIER_NONE)

            '!' -> HidReport(0x1E, MODIFIER_SHIFT)
            '@' -> HidReport(0x1F, MODIFIER_SHIFT)
            '#' -> HidReport(0x20, MODIFIER_SHIFT)
            '$' -> HidReport(0x21, MODIFIER_SHIFT)
            '%' -> HidReport(0x22, MODIFIER_SHIFT)
            '^' -> HidReport(0x23, MODIFIER_SHIFT)
            '&' -> HidReport(0x24, MODIFIER_SHIFT)
            '*' -> HidReport(0x25, MODIFIER_SHIFT)
            '(' -> HidReport(0x26, MODIFIER_SHIFT)
            ')' -> HidReport(0x27, MODIFIER_SHIFT)

            '\n' -> HidReport(0x28, MODIFIER_NONE) // Enter
            // 0x29 Escape
            '\b' -> HidReport(0x2A, MODIFIER_NONE) // Backspace
            '\t' -> HidReport(0x2B, MODIFIER_NONE) // Tab
            ' '  -> HidReport(0x2C, MODIFIER_NONE) // Space

            '-' -> HidReport(0x2D, MODIFIER_NONE)
            '_' -> HidReport(0x2D, MODIFIER_SHIFT)
            '=' -> HidReport(0x2E, MODIFIER_NONE)
            '+' -> HidReport(0x2E, MODIFIER_SHIFT)
            '[' -> HidReport(0x2F, MODIFIER_NONE)
            '{' -> HidReport(0x2F, MODIFIER_SHIFT)
            ']' -> HidReport(0x30, MODIFIER_NONE)
            '}' -> HidReport(0x30, MODIFIER_SHIFT)
            '\\' -> HidReport(0x31, MODIFIER_NONE)
            '|' -> HidReport(0x31, MODIFIER_SHIFT)
            ';' -> HidReport(0x33, MODIFIER_NONE)
            ':' -> HidReport(0x33, MODIFIER_SHIFT)
            '\'' -> HidReport(0x34, MODIFIER_NONE)
            '"' -> HidReport(0x34, MODIFIER_SHIFT)
            '`' -> HidReport(0x35, MODIFIER_NONE)
            '~' -> HidReport(0x35, MODIFIER_SHIFT)
            ',' -> HidReport(0x36, MODIFIER_NONE)
            '<' -> HidReport(0x36, MODIFIER_SHIFT)
            '.' -> HidReport(0x37, MODIFIER_NONE)
            '>' -> HidReport(0x37, MODIFIER_SHIFT)
            '/' -> HidReport(0x38, MODIFIER_NONE)
            '?' -> HidReport(0x38, MODIFIER_SHIFT)

            else -> null // Unsupported character
        }
    }
}
