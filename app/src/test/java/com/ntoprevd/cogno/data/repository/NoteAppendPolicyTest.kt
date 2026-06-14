package com.ntoprevd.cogno.data.repository

import com.ntoprevd.cogno.data.db.entity.MessageEntity
import com.ntoprevd.cogno.data.db.entity.NoteEntity
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

    @Test
    fun unchangedMessagesAreUpToDate() {
        val messages = listOf(message("1", 100, 100), message("2", 200, 220))
        val note = note(count = 2, lastCreatedAt = 200, revision = 220)

        assertEquals(NoteSyncMode.UP_TO_DATE, noteSyncPlan(note, messages).mode)
    }

    @Test
    fun newlyAppendedMessagesUseIncrementalSummary() {
        val messages = listOf(
            message("1", 100, 100),
            message("2", 200, 220),
            message("3", 300, 310)
        )
        val note = note(count = 2, lastCreatedAt = 200, revision = 220)

        val plan = noteSyncPlan(note, messages)

        assertEquals(NoteSyncMode.APPEND, plan.mode)
        assertEquals(listOf("3"), plan.messages.map { it.id })
    }

    @Test
    fun editedHistoricalMessageForcesFullRebuild() {
        val messages = listOf(
            message("1", 100, 400),
            message("2", 200, 220),
            message("3", 300, 310)
        )
        val note = note(count = 2, lastCreatedAt = 200, revision = 220)

        assertEquals(NoteSyncMode.REBUILD, noteSyncPlan(note, messages).mode)
    }

    @Test
    fun deletedHistoricalMessageForcesFullRebuild() {
        val messages = listOf(message("2", 200, 220))
        val note = note(count = 2, lastCreatedAt = 200, revision = 220)

        assertEquals(NoteSyncMode.REBUILD, noteSyncPlan(note, messages).mode)
    }

    @Test
    fun conversationsBeyondTwoHundredMessagesStillAppend() {
        val messages = (1..201).map { index ->
            message(index.toString(), index.toLong(), index.toLong())
        }
        val note = note(count = 200, lastCreatedAt = 200, revision = 200)

        val plan = noteSyncPlan(note, messages)

        assertEquals(NoteSyncMode.APPEND, plan.mode)
        assertEquals(listOf("201"), plan.messages.map { it.id })
    }

    private fun note(count: Int, lastCreatedAt: Long, revision: Long): NoteEntity {
        return NoteEntity(
            id = "note",
            title = "Note",
            content = "Content",
            preview = "Content",
            sourceSessionId = "session",
            sourceMessageCount = count,
            sourceLastMessageCreatedAt = lastCreatedAt,
            sourceMessageRevision = revision,
            pinned = false,
            createdAt = 1L,
            updatedAt = 1L
        )
    }

    private fun message(id: String, createdAt: Long, updatedAt: Long): MessageEntity {
        return MessageEntity(
            id = id,
            sessionId = "session",
            role = "user",
            content = id,
            status = "completed",
            errorCode = null,
            tokenCount = null,
            feedback = null,
            imagePath = null,
            imageMimeType = null,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
