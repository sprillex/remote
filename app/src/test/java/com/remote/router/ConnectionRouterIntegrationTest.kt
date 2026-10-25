package com.remote.router

import com.remote.db.ConnectionType
import com.remote.db.ProfileType
import com.remote.db.TargetDeviceEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class ConnectionRouterIntegrationTest {

    @Test
    fun `routing properly instantiates missing network dependencies like ssh`() = runBlocking {
        val router = ConnectionRouter()
        val device = TargetDeviceEntity(
            id = 1,
            name = "SSH Test Missing Host",
            connectionType = ConnectionType.SSH_NETWORK,
            profileType = ProfileType.SSH_SESSION,
            hostAddress = null,
            authUsername = "test",
            port = 22
        )

        val result = router.routeTo(device)
        assertTrue("SSH connection should fail due to missing host", result.isFailure)
    }

    @Test
    fun `routing properly delegates ssh success`() = runBlocking {
        val router = ConnectionRouter()
        val device = TargetDeviceEntity(
            id = 1,
            name = "SSH Test Success",
            connectionType = ConnectionType.SSH_NETWORK,
            profileType = ProfileType.SSH_SESSION,
            hostAddress = "192.168.1.1",
            authUsername = "test",
            port = 22
        )

        val result = router.routeTo(device)
        assertTrue("SSH connection mock should succeed", result.isSuccess)

        val disconnectResult = router.disconnect()
        assertTrue("SSH disconnect mock should succeed", disconnectResult.isSuccess)
    }
}
