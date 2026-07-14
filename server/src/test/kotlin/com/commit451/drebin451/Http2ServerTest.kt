package com.commit451.drebin451

import io.ktor.server.netty.NettyApplicationEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Http2ServerTest {

    @Test
    fun `cloud run connector accepts cleartext HTTP2`() {
        val configuration = NettyApplicationEngine.Configuration()

        configuration.configureCloudRunEngine(port = 9191)

        assertTrue(configuration.enableHttp2)
        assertTrue(configuration.enableH2c)
        assertEquals(9191, configuration.connectors.single().port)
    }
}
