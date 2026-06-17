package com.ntoprevd.cogno.data.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class CompletionContentInstrumentedTest {
    @Test
    fun readsStringContent() {
        val response = responseWithContent("笔记正文")

        assertEquals("笔记正文", extractAssistantContent(response))
    }

    @Test
    fun readsArrayContentFromCompatibleProviders() {
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", "第一段"))
            .put(JSONObject().put("type", "text").put("text", "第二段"))

        assertEquals("第一段第二段", extractAssistantContent(responseWithContent(content)))
    }

    private fun responseWithContent(content: Any): JSONObject {
        return JSONObject().put(
            "choices",
            JSONArray().put(
                JSONObject().put(
                    "message",
                    JSONObject().put("content", content)
                )
            )
        )
    }
}
