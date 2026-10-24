package com.remote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.regex.Pattern

class SsdpDiscovery(
    private val ssdpTimeoutMs: Int = 3000,
    private val sweepTimeoutMs: Int = 500
) {
    companion object {
        const val MULTICAST_ADDRESS = "239.255.255.250"
        const val MULTICAST_PORT = 1900
        const val ROKU_ECP_PORT = 8060
        private const val SEARCH_MESSAGE =
            "M-SEARCH * HTTP/1.1\r\n" +
            "Host: 239.255.255.250:1900\r\n" +
            "Man: \"ssdp:discover\"\r\n" +
            "ST: roku:ecp\r\n" +
            "\r\n"

        // Regex to parse IP from LOCATION header, e.g., LOCATION: http://192.168.1.100:8060/
        private val LOCATION_REGEX = Pattern.compile("LOCATION:\\s*http://([^:/]+):[0-9]+.*", Pattern.CASE_INSENSITIVE)
    }

    suspend fun discoverRokuDevices(localIpAddress: String): Set<String> = withContext(Dispatchers.IO) {
        val devices = mutableSetOf<String>()

        // 1. Try SSDP Discovery first
        try {
            val ssdpResult = performSsdpDiscovery()
            devices.addAll(ssdpResult)
        } catch (e: Exception) {
            // Ignore exceptions during SSDP, move to fallback
        }

        // 2. Fallback to subnet sweep if SSDP found nothing
        if (devices.isEmpty()) {
            val sweepResult = performSubnetSweep(localIpAddress)
            devices.addAll(sweepResult)
        }

        return@withContext devices
    }

    private suspend fun performSsdpDiscovery(): Set<String> = withContext(Dispatchers.IO) {
        val foundIps = mutableSetOf<String>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = ssdpTimeoutMs

            val sendData = SEARCH_MESSAGE.toByteArray()
            val address = InetAddress.getByName(MULTICAST_ADDRESS)
            val sendPacket = DatagramPacket(sendData, sendData.size, address, MULTICAST_PORT)
            socket.send(sendPacket)

            val receiveData = ByteArray(1024)
            val endTime = System.currentTimeMillis() + ssdpTimeoutMs

            while (System.currentTimeMillis() < endTime) {
                try {
                    val receivePacket = DatagramPacket(receiveData, receiveData.size)
                    socket.receive(receivePacket)
                    val response = String(receivePacket.data, 0, receivePacket.length)
                    val ip = parseLocationIp(response)
                    if (ip != null) {
                        foundIps.add(ip)
                    }
                } catch (e: SocketTimeoutException) {
                    break
                }
            }
        } finally {
            socket?.close()
        }
        return@withContext foundIps
    }

    // visible for testing
    internal fun parseLocationIp(response: String): String? {
        val lines = response.split("\r\n", "\n")
        for (line in lines) {
            val matcher = LOCATION_REGEX.matcher(line)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    // visible for testing
    internal fun getSubnetAddresses(localIpAddress: String): List<String> {
        val parts = localIpAddress.split(".")
        if (parts.size != 4) return emptyList()
        val prefix = "${parts[0]}.${parts[1]}.${parts[2]}"
        return (1..254).map { "$prefix.$it" }
    }

    private suspend fun performSubnetSweep(localIpAddress: String): Set<String> = withContext(Dispatchers.IO) {
        val ips = getSubnetAddresses(localIpAddress)
        if (ips.isEmpty()) return@withContext emptySet()

        val deferredResults = ips.map { ip ->
            async(Dispatchers.IO) {
                checkPortOpen(ip, ROKU_ECP_PORT, sweepTimeoutMs)
            }
        }

        return@withContext deferredResults.awaitAll().filterNotNull().toSet()
    }

    private suspend fun checkPortOpen(ip: String, port: Int, timeout: Int): String? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            withTimeoutOrNull(timeout.toLong() + 100) {
                socket = Socket()
                socket?.connect(InetSocketAddress(ip, port), timeout)
                ip // return IP if successful
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // ignore close errors
            }
        }
    }
}
