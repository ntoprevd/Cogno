package com.ntoprevd.cogno.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteAppendPolicyTest {
    @Test
    fun appendsNewSummaryWithoutChangingExistingContent() {
        val existing = "# 手工修改后的标题\n\n用户保留的正文"
        val addition = "## 新增知识\n\n- 新增结论"

        assertEquals(
            "$existing\n\n$addition",
            appendNoteContent(existing, addition)
        )
    }

    @Test
    fun blankAdditionKeepsExistingContent() {
        assertEquals(
            "已有内容",
            appendNoteContent("已有内容", "   ")
        )
    }

    @Test
    fun firstSummaryUsesGeneratedContentDirectly() {
        assertEquals(
            "## 首次总结",
            appendNoteContent("", "## 首次总结")
        )
    }
}
