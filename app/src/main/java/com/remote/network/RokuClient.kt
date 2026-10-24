package com.remote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class RokuClient(
    private val connectTimeoutMs: Int = 3000,
    private val readTimeoutMs: Int = 3000
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .build()

    companion object {
        const val ECP_PORT = 8060

        // Common Roku ECP Key Codes
        const val KEY_HOME = "Home"
        const val KEY_BACK = "Back"
        const val KEY_SELECT = "Select"
        const val KEY_LEFT = "Left"
        const val KEY_RIGHT = "Right"
        const val KEY_DOWN = "Down"
        const val KEY_UP = "Up"
        const val KEY_PLAY = "Play"
        const val KEY_REV = "Rev"
        const val KEY_FWD = "Fwd"
        const val KEY_INSTANT_REPLAY = "InstantReplay"
        const val KEY_INFO = "Info"
        const val KEY_BACKSPACE = "Backspace"
        const val KEY_SEARCH = "Search"
        const val KEY_ENTER = "Enter"
        const val KEY_POWER = "Power"
        const val KEY_POWER_ON = "PowerOn"
        const val KEY_POWER_OFF = "PowerOff"
        const val KEY_VOLUME_UP = "VolumeUp"
        const val KEY_VOLUME_DOWN = "VolumeDown"
        const val KEY_VOLUME_MUTE = "VolumeMute"
    }

    fun buildUrl(host: String, port: Int = ECP_PORT, path: String): String {
        return "http://$host:$port/$path"
    }

    suspend fun sendKeypress(host: String, key: String, port: Int = ECP_PORT): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl(host, port, "keypress/$key")
        return@withContext executePostRequest(url)
    }

    suspend fun sendKeyDown(host: String, key: String, port: Int = ECP_PORT): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl(host, port, "keydown/$key")
        return@withContext executePostRequest(url)
    }

    suspend fun sendKeyUp(host: String, key: String, port: Int = ECP_PORT): Boolean = withContext(Dispatchers.IO) {
        val url = buildUrl(host, port, "keyup/$key")
        return@withContext executePostRequest(url)
    }

    suspend fun sendTextCharacter(host: String, char: Char, port: Int = ECP_PORT): Boolean = withContext(Dispatchers.IO) {
        val encodedChar = URLEncoder.encode(char.toString(), "UTF-8").replace("+", "%20")
        val url = buildUrl(host, port, "keypress/Lit_$encodedChar")
        return@withContext executePostRequest(url)
    }

    suspend fun sendTextString(
        host: String,
        text: String,
        port: Int = ECP_PORT,
        interKeyDelayMs: Long = 20L
    ): Boolean = withContext(Dispatchers.IO) {
        var allSuccess = true
        for (char in text) {
            val success = sendTextCharacter(host, char, port)
            if (!success) {
                allSuccess = false
            }
            if (interKeyDelayMs > 0) {
                kotlinx.coroutines.delay(interKeyDelayMs)
            }
        }
        allSuccess
    }

    private fun executePostRequest(urlString: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(urlString)
                .post(ByteArray(0).toRequestBody(null))
                .build()

            val response = httpClient.newCall(request).execute()
            response.use {
                it.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
