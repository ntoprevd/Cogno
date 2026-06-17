package com.ntoprevd.cogno.data.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SseParserInstrumentedTest {
    @Test
    fun parsesContentAndUsage() {
        val event = parseSseData(
            """{"choices":[{"delta":{"content":"Hello"}}],"usage":{"total_tokens":12}}"""
        )

        assertEquals("Hello", event?.content)
        assertEquals(12, event?.totalTokens)
        assertFalse(event?.done ?: true)
    }

    @Test
    fun ignoresNullContentWithoutProducingNullText() {
        val event = parseSseData("""{"choices":[{"delta":{"content":null}}]}""")

        assertNull(event?.content)
    }

    @Test
    fun malformedPayloadIsIgnored() {
        assertNull(parseSseData("{not-json"))
    }

    @Test
    fun doneMarkerStopsStream() {
        assertTrue(parseSseData("[DONE]")?.done == true)
    }
}
