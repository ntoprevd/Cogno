package com.ntoprevd.cogno.data.repository

import android.content.Context
import com.ntoprevd.cogno.data.db.AppDatabase
import com.ntoprevd.cogno.data.db.entity.NoteEntity
import com.ntoprevd.cogno.data.db.entity.NoteTopicSegmentEntity
import com.ntoprevd.cogno.data.db.entity.TopicEntity
import com.ntoprevd.cogno.data.network.AiNoteSegment
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class TopicRepository(context: Context) {
    private val topicDao = AppDatabase.getInstance(context).topicDao()

    fun observeTopics(): Flow<List<TopicEntity>> = topicDao.observeTopics()

    fun observeSegments(): Flow<List<NoteTopicSegmentEntity>> = topicDao.observeSegments()

    suspend fun ensureDefaultTopics() {
        if (topicDao.getEnabledTopics().isNotEmpty()) return
        topicDao.insertTopics(defaultTopics())
    }

    suspend fun enabledTopics(): List<TopicEntity> {
        ensureDefaultTopics()
        return topicDao.getEnabledTopics()
    }

    suspend fun addTopic(name: String, keywords: String) {
        val safeName = name.trim()
        if (safeName.isBlank()) return
        val now = System.currentTimeMillis()
        topicDao.insertTopic(
            TopicEntity(
                id = UUID.randomUUID().toString(),
                name = safeName,
                keywords = keywords.trim(),
                isBuiltIn = false,
                enabled = true,
                pinned = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renameTopic(topic: TopicEntity, name: String, keywords: String) {
        val safeName = name.trim()
        if (safeName.isBlank()) return
        topicDao.updateTopic(
            topic.copy(
                name = safeName,
                keywords = keywords.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
        // 用户主动重命名时同步历史标签名称，但不重新分析或改写段落内容。
        if (topic.name != safeName) topicDao.renameSegmentTopic(topic.name, safeName)
    }

    suspend fun deleteTopic(topic: TopicEntity) {
        topicDao.deleteTopic(topic.id)
    }

    suspend fun setPinned(topic: TopicEntity, pinned: Boolean) {
        topicDao.updateTopic(topic.copy(pinned = pinned, updatedAt = System.currentTimeMillis()))
    }

    suspend fun resetTopics() {
        topicDao.deleteAllTopics()
        topicDao.insertTopics(defaultTopics())
    }

    suspend fun syncSegments(
        note: NoteEntity,
        aiSegments: List<AiNoteSegment>,
        sourceMessageCount: Int
    ) {
        val topics = enabledTopics()
        val candidates = aiSegments
            .filter { it.content.isNotBlank() }
            .ifEmpty { splitMarkdown(note.content) }
        val existingIds = topicDao.getSegmentIdsForNote(note.id).toSet()
        val now = System.currentTimeMillis()
        val rows = candidates.mapIndexedNotNull { index, segment ->
            val content = segment.content.trim()
            if (content.isBlank()) return@mapIndexedNotNull null
            val topicName = normalizeTopic(segment.topic, content, topics)
            val id = stableSegmentId(note.id, content)
            if (id in existingIds) return@mapIndexedNotNull null
            NoteTopicSegmentEntity(
                id = id,
                noteId = note.id,
                topicName = topicName,
                heading = segment.heading.trim(),
                content = content,
                position = index,
                sourceMessageCount = sourceMessageCount,
                createdAt = now + index
            )
        }
        if (rows.isNotEmpty()) topicDao.insertSegments(rows)
    }

    private fun normalizeTopic(
        requested: String,
        content: String,
        topics: List<TopicEntity>
    ): String {
        topics.firstOrNull { it.name.equals(requested.trim(), ignoreCase = true) }?.let { return it.name }
        val normalized = content.lowercase(Locale.ROOT)
        return topics.firstOrNull { topic ->
            topic.keywords.split(',', '，', '\n')
                .map(String::trim)
                .filter(String::isNotBlank)
                .any { normalized.contains(it.lowercase(Locale.ROOT)) }
        }?.name ?: topics.firstOrNull()?.name ?: "综合知识"
    }

    private fun splitMarkdown(content: String): List<AiNoteSegment> {
        val result = mutableListOf<AiNoteSegment>()
        var heading = ""
        val body = StringBuilder()

        fun flush() {
            val text = body.toString().trim()
            if (text.isNotBlank()) result += AiNoteSegment("", heading, text)
            body.clear()
        }

        content.lines().forEach { line ->
            if (line.trimStart().startsWith("#")) {
                flush()
                heading = line.trim().trimStart('#').trim()
            } else {
                body.appendLine(line)
            }
        }
        flush()
        return result
    }

    companion object {
        private fun stableSegmentId(noteId: String, content: String): String {
            val normalized = content.replace(Regex("\\s+"), " ").trim()
            return UUID.nameUUIDFromBytes(
                "$noteId|$normalized".toByteArray(StandardCharsets.UTF_8)
            ).toString()
        }

        fun defaultTopics(): List<TopicEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                Triple("technology", "技术学习", "java,kotlin,android,compose,api,数据库,代码,开发"),
                Triple("study", "学习方法", "学习,复习,记忆,考试,课程,计划,效率"),
                Triple("work", "工作与项目", "工作,项目,需求,产品,团队,方案,任务"),
                Triple("health", "健康与生活", "健康,运动,睡眠,饮食,身体,生活,习惯"),
                Triple("emotion", "情绪与心理", "情绪,焦虑,压力,心理,关系,沟通,边界"),
                Triple("finance", "财务与消费", "财务,投资,股票,基金,预算,消费,资产"),
                Triple("ideas", "观点与思考", "观点,思考,哲学,意义,分析,判断,结论"),
                Triple("general", "综合知识", "")
            ).mapIndexed { index, (id, name, keywords) ->
                TopicEntity(
                    id = id,
                    name = name,
                    keywords = keywords,
                    isBuiltIn = true,
                    enabled = true,
                    pinned = false,
                    createdAt = now + index,
                    updatedAt = now + index
                )
            }
        }
    }
}
