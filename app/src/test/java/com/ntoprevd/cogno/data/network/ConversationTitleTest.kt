package com.ntoprevd.cogno.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationTitleTest {
    @Test
    fun removesMarkdownQuotesAndExtraLines() {
        assertEquals(
            "人机关系与情感边界",
            sanitizeConversationTitle("# “人机关系与情感边界”\n这是解释")
        )
    }

    @Test
    fun limitsUnexpectedlyLongTitle() {
        assertEquals(24, sanitizeConversationTitle("很长".repeat(30)).length)
    }
}
