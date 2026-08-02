package com.nopal.tunascreen.feature.workshop.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraStreamViewTest {

    @Test
    fun relativeUrlUsesPairedServerAndToken() {
        val request = resolveCameraStream(
            streamUrl = "/api/cameras/usb/front/stream",
            baseUrl = "http://192.168.0.86:8420",
            token = "secret",
        )

        assertEquals("http://192.168.0.86:8420/api/cameras/usb/front/stream", request?.url)
        assertEquals("Bearer secret", request?.authorization)
    }

    @Test
    fun absoluteSameOriginUsesToken() {
        val request = resolveCameraStream(
            streamUrl = "http://nopal.local:8420/camera/stream",
            baseUrl = "http://nopal.local:8420",
            token = "secret",
        )

        assertEquals("Bearer secret", request?.authorization)
    }

    @Test
    fun externalCameraNeverReceivesNopalToken() {
        val request = resolveCameraStream(
            streamUrl = "http://192.168.0.50:8080/stream",
            baseUrl = "http://192.168.0.86:8420",
            token = "secret",
        )

        assertEquals("http://192.168.0.50:8080/stream", request?.url)
        assertNull(request?.authorization)
    }

    @Test
    fun unsupportedOrUnresolvableStreamsAreRejected() {
        assertNull(resolveCameraStream("rtsp://camera.local/live", "http://nopal.local:8420", "secret"))
        assertNull(resolveCameraStream("/relative/stream", null, "secret"))
        assertNull(resolveCameraStream(null, "http://nopal.local:8420", "secret"))
    }
}
