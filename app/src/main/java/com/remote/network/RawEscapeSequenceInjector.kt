package com.remote.network

object RawEscapeSequenceInjector {
    const val CTRL_C = "\u0003"
    const val CTRL_Z = "\u001a"
    const val CTRL_BACKSLASH = "\u001c"
    const val ESC = "\u001b"
    const val TAB = "\t"
    const val ENTER = "\r\n"

    fun getCtrlCode(ch: Char): String {
        val upper = ch.uppercaseChar()
        return if (upper in 'A'..'Z') {
            (upper - 'A' + 1).toChar().toString()
        } else when (ch) {
            '[' -> "\u001b"
            '\\' -> "\u001c"
            ']' -> "\u001d"
            '^' -> "\u001e"
            '_' -> "\u001f"
            '?' -> "\u007f"
            else -> ch.toString()
        }
    }

    fun parseHexSequence(hexString: String): ByteArray {
        val cleaned = hexString
            .replace("\\x", "")
            .replace("0x", "")
            .replace("\\", "")
            .replace("\\s+".toRegex(), "")

        if (cleaned.length % 2 != 0) return byteArrayOf()

        val result = ByteArray(cleaned.length / 2)
        for (i in result.indices) {
            val byteStr = cleaned.substring(i * 2, i * 2 + 2)
            val byteVal = byteStr.toIntOrNull(16) ?: return byteArrayOf()
            result[i] = byteVal.toByte()
        }
        return result
    }
}
