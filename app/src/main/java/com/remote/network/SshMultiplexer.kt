package com.remote.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class BroadcastResult(
    val sessionId: String,
    val result: Result<String>
)

class SshMultiplexer {

    private val activeSessions = ConcurrentHashMap<String, SshSession>()

    fun registerSession(id: String, session: SshSession) {
        activeSessions[id] = session
    }

    fun unregisterSession(id: String) {
        activeSessions.remove(id)
    }

    fun getActiveSessionIds(): List<String> = activeSessions.keys.toList()

    fun clearSessions() {
        activeSessions.clear()
    }

    suspend fun broadcastExecute(
        command: String,
        targetSessionIds: List<String>? = null,
        workingDir: String? = null,
        mode: ExecutionMode = ExecutionMode.BUFFERED_EXEC
    ): List<BroadcastResult> = withContext(Dispatchers.IO) {
        val targets = if (targetSessionIds.isNullOrEmpty()) {
            activeSessions.toMap()
        } else {
            activeSessions.filterKeys { targetSessionIds.contains(it) }
        }

        if (targets.isEmpty()) return@withContext emptyList()

        targets.map { (id, session) ->
            async {
                val res = session.executeCommand(command, workingDir, mode)
                BroadcastResult(sessionId = id, result = res)
            }
        }.awaitAll()
    }
}
