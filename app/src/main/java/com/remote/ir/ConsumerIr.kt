package com.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager

class ConsumerIrDriver(private val context: Context) {

    private val irManager: ConsumerIrManager? by lazy {
        context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
    }

    fun hasIrEmitter(): Boolean {
        return irManager?.hasIrEmitter() ?: false
    }

    fun getCarrierFrequencies(): Array<ConsumerIrManager.CarrierFrequencyRange>? {
        return if (hasIrEmitter()) {
            irManager?.carrierFrequencies
        } else {
            null
        }
    }

    fun isFrequencySupported(frequency: Int): Boolean {
        val ranges = getCarrierFrequencies() ?: return false
        return ranges.any { frequency >= it.minFrequency && frequency <= it.maxFrequency }
    }

    fun transmit(carrierFrequency: Int, pattern: IntArray): Boolean {
        val manager = irManager ?: return false
        if (!manager.hasIrEmitter()) return false
        if (pattern.isEmpty()) return false

        return try {
            manager.transmit(carrierFrequency, pattern)
            true
        } catch (e: Exception) {
            false
        }
    }
}
