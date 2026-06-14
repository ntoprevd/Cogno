package com.ntoprevd.cogno.data.network

import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import okio.Timeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingCallControllerTest {
    @Test
    fun cancelBeforeRegister_cancelsNextCall() {
        val controller = StreamingCallController()
        val call = FakeCall()

        controller.cancel()
        controller.register(call)

        assertTrue(call.isCanceled())
    }

    @Test
    fun cancelAfterRegister_cancelsActiveCall() {
        val controller = StreamingCallController()
        val call = FakeCall()

        controller.register(call)
        assertFalse(call.isCanceled())
        controller.cancel()

        assertTrue(call.isCanceled())
    }

    private class FakeCall : Call {
        private var canceled = false

        override fun request(): Request = Request.Builder()
            .url("https://example.invalid")
            .build()

        override fun execute(): Response = throw IOException("Not used")

        override fun enqueue(responseCallback: Callback) = Unit

        override fun cancel() {
            canceled = true
        }

        override fun isExecuted(): Boolean = false

        override fun isCanceled(): Boolean = canceled

        override fun timeout(): Timeout = Timeout.NONE

        override fun clone(): Call = FakeCall()
    }
}
