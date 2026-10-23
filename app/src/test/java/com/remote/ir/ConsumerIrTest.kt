package com.remote.ir

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ConsumerIrTest {

    private class MockContext : android.content.ContextWrapper(null) {
        override fun getSystemService(name: String): Any? {
            return null
        }
    }

    @Test
    fun testDriverWithoutIrHardware() {
        val driver = ConsumerIrDriver(MockContext())
        assertFalse("Should return false when IR hardware is absent", driver.hasIrEmitter())
        assertNull("Carrier frequencies should be null when IR hardware is absent", driver.getCarrierFrequencies())
        assertFalse("38000Hz should not be supported when IR hardware is absent", driver.isFrequencySupported(38000))
        assertFalse("Transmit should return false when IR hardware is absent", driver.transmit(38000, intArrayOf(9000, 4500, 560, 560)))
    }
}
