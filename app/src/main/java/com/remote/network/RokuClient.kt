package com.remote.network

import kotlinx.coroutines.Dispatchers
import android.util.Log
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

    suspend fun sendKeypress(host: String, key: String, port: Int = ECP_PORT): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = buildUrl(host, port, "keypress/$key")
        return@withContext executePostRequest(url)
    }

    suspend fun sendKeyDown(host: String, key: String, port: Int = ECP_PORT): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = buildUrl(host, port, "keydown/$key")
        return@withContext executePostRequest(url)
    }

    suspend fun sendKeyUp(host: String, key: String, port: Int = ECP_PORT): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = buildUrl(host, port, "keyup/$key")
        return@withContext executePostRequest(url)
    }

    suspend fun sendTextCharacter(host: String, char: Char, port: Int = ECP_PORT): Result<Boolean> = withContext(Dispatchers.IO) {
        val encodedChar = URLEncoder.encode(char.toString(), "UTF-8").replace("+", "%20")
        val url = buildUrl(host, port, "keypress/Lit_$encodedChar")
        return@withContext executePostRequest(url)
    }

    suspend fun sendTextString(
        host: String,
        text: String,
        port: Int = ECP_PORT,
        interKeyDelayMs: Long = 20L
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var allSuccess = true
        var lastFailure: Throwable? = null
        for (char in text) {
            val result = sendTextCharacter(host, char, port)
            if (result.isFailure) {
                lastFailure = result.exceptionOrNull()
                allSuccess = false
                break // Break early on failure as per code review feedback
            } else if (result.getOrNull() == false) {
                allSuccess = false
                break // Break early on failure as per code review feedback
            }
            if (interKeyDelayMs > 0) {
                kotlinx.coroutines.delay(interKeyDelayMs)
            }
        }
        return@withContext if (lastFailure != null) Result.failure(lastFailure) else Result.success(allSuccess)
    }

    private fun executePostRequest(urlString: String): Result<Boolean> {
        return try {
            val request = Request.Builder()
                .url(urlString)
                .post(ByteArray(0).toRequestBody(null))
                .build()

            val response = httpClient.newCall(request).execute()
            val success = response.use {
                it.isSuccessful
            }
            Result.success(success)
        } catch (e: Exception) {
            try {
                Log.e("RokuClient", "Network exception", e)
            } catch (mockError: Throwable) {
                // Fallback for tests if Log is not mocked
                println("RokuClient Network exception: ${e.message}")
                e.printStackTrace()
            }
            Result.failure(e)
        }
    }
}
