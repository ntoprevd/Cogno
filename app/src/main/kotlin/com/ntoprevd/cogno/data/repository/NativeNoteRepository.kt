package com.ntoprevd.cogno.data.repository

import android.content.Context
import com.ntoprevd.cogno.data.db.AppDatabase
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class NativeNoteRepository(context: Context) {
    private val noteDao = AppDatabase.getInstance(context).noteDao()

    fun observeNotes(): Flow<List<NoteEntity>> =
        noteDao.observeAllNotesOrderByUpdatedAtDesc()

    fun observeNote(noteId: String): Flow<NoteEntity?> =
        noteDao.observeNoteById(noteId)

    suspend fun createNote(title: String, content: String): NoteEntity {
        val now = System.currentTimeMillis()
        val safeContent = content.trim().ifBlank { title.trim() }
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "新建笔记" },
            content = safeContent,
            preview = preview(safeContent),
            sourceSessionId = null,
            sourceMessageCount = 0,
            sourceLastMessageCreatedAt = 0L,
            sourceMessageRevision = 0L,
            pinned = false,
            createdAt = now,
            updatedAt = now
        )
        noteDao.insertNote(note)
        return note
    }

    suspend fun renameNote(noteId: String, title: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        val nextTitle = title.trim()
        if (nextTitle.isBlank()) return

        note.title = nextTitle
        note.updatedAt = System.currentTimeMillis()
        noteDao.updateNote(note)
    }

    suspend fun setNotePinned(noteId: String, pinned: Boolean) {
        val note = noteDao.getNoteById(noteId) ?: return
        note.pinned = pinned
        note.updatedAt = System.currentTimeMillis()
        noteDao.updateNote(note)
    }

    suspend fun updateNoteContent(noteId: String, content: String) {
        val note = noteDao.getNoteById(noteId) ?: return
        val nextContent = content.trim()
        if (nextContent.isBlank()) return

        note.content = nextContent
        note.preview = preview(nextContent)
        note.updatedAt = System.currentTimeMillis()
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(noteId: String) {
        noteDao.deleteNoteById(noteId)
    }

    private fun preview(content: String): String {
        val plainText = content
            .replace(Regex("[#>*_`\\-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return if (plainText.length <= PREVIEW_LIMIT) plainText else plainText.take(PREVIEW_LIMIT)
    }

    companion object {
        private const val PREVIEW_LIMIT = 80
    }
}
