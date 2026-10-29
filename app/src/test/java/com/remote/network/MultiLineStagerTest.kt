package com.remote.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiLineStagerTest {

    private val stager = MultiLineStager()

    @Test
    fun testExtractTokens() {
        val template = "ip addr add {{TARGET_IP}} dev {{INTERFACE}}"
        val tokens = stager.extractTokens(template)

        assertEquals(setOf("TARGET_IP", "INTERFACE"), tokens)
    }

    @Test
    fun testGetUnboundTokens() {
        val template = "connect --host {{HOST}} --port {{PORT}}"
        val bindings = mapOf("HOST" to "10.0.0.1")

        val unbound = stager.getUnboundTokens(template, bindings)
        assertEquals(setOf("PORT"), unbound)
    }

    @Test
    fun testSubstituteTokens() {
        val template = "echo 'Connecting to {{TARGET_IP}}:{{PORT}}'"
        val bindings = mapOf("TARGET_IP" to "192.168.1.100", "PORT" to "2222")

        val result = stager.substituteTokens(template, bindings)
        assertEquals("echo 'Connecting to 192.168.1.100:2222'", result)
    }

    @Test
    fun testParseMultiLineCommands() {
        val script = """
            # This is a comment
            ls -la
            // another comment
            cat /etc/hosts
        """.trimIndent()

        val parsed = stager.parseMultiLineCommands(script)
        assertEquals(listOf("ls -la", "cat /etc/hosts"), parsed)
    }
}
