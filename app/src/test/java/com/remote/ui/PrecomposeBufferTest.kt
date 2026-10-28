package com.remote.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrecomposeBufferTest {

    @Test
    fun testInjectText_SuccessfulInjection() = runBlocking {
        val reports = mutableListOf<ByteArray>()
        val progressList = mutableListOf<Pair<Int, Int>>()

        val text = "aB"
        val result = PrecomposeBufferInjector.injectText(
            text = text,
            interKeyDelayMs = 1L,
            sendReport = { report ->
                reports.add(report.copyOf())
                true
            },
            onProgress = { current, total ->
                progressList.add(current to total)
            }
        )

        assertEquals(2, result.totalChars)
        assertEquals(2, result.injectedChars)
        assertEquals(0, result.skippedChars)

        // For 'a': KeyDown (modifier 0, key 0x04), KeyUp (0, 0)
        // For 'B': KeyDown (modifier 2, key 0x05), KeyUp (0, 0)
        assertEquals(4, reports.size)

        // 'a' KeyDown
        assertEquals(0x00.toByte(), reports[0][0])
        assertEquals(0x04.toByte(), reports[0][2])

        // 'a' KeyUp
        assertEquals(0x00.toByte(), reports[1][0])
        assertEquals(0x00.toByte(), reports[1][2])

        // 'B' KeyDown (Shift)
        assertEquals(0x02.toByte(), reports[2][0])
        assertEquals(0x05.toByte(), reports[2][2])

        // 'B' KeyUp
        assertEquals(0x00.toByte(), reports[3][0])
        assertEquals(0x00.toByte(), reports[3][2])

        assertEquals(2, progressList.size)
        assertEquals(1 to 2, progressList[0])
        assertEquals(2 to 2, progressList[1])
    }

    @Test
    fun testInjectText_WithUnsupportedCharacters() = runBlocking {
        val reports = mutableListOf<ByteArray>()

        // '\u0000' is unsupported
        val text = "a\u0000b"
        val result = PrecomposeBufferInjector.injectText(
            text = text,
            interKeyDelayMs = 0L,
            sendReport = { report ->
                reports.add(report.copyOf())
                true
            }
        )

        assertEquals(3, result.totalChars)
        assertEquals(2, result.injectedChars)
        assertEquals(1, result.skippedChars)

        // 2 supported chars * 2 reports each (down + up) = 4 reports
        assertEquals(4, reports.size)
    }

    @Test
    fun testInjectText_Cancellation() = runBlocking {
        val reports = mutableListOf<ByteArray>()

        val text = "abcdefghij"
        val job = launch {
            PrecomposeBufferInjector.injectText(
                text = text,
                interKeyDelayMs = 50L,
                sendReport = { report ->
                    reports.add(report.copyOf())
                    true
                }
            )
        }

        // Allow at least 1 key to send then cancel
        kotlinx.coroutines.delay(20)
        job.cancel()
        job.join()

        // Should have cancelled before completing all 10 characters
        assertTrue("Reports size (${reports.size}) should be less than total expected (20)", reports.size < 20)
    }
}
