package com.ntoprevd.cogno.data.repository

import android.content.Context
import androidx.room.withTransaction
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
    private val database = AppDatabase.getInstance(context)
    private val topicDao = database.topicDao()
    private val preferences = context.applicationContext.getSharedPreferences(
        TOPIC_PREFERENCES,
        Context.MODE_PRIVATE
    )

    fun observeTopics(): Flow<List<TopicEntity>> = topicDao.observeTopics()

    fun observeSegments(): Flow<List<NoteTopicSegmentEntity>> = topicDao.observeSegments()

    suspend fun ensureDefaultTopics() {
        if (preferences.getInt(KEY_RULES_VERSION, 0) >= CURRENT_RULES_VERSION) return

        if (topicDao.countTopics() > 0) {
            // 规则集升级只替换内置项，用户自己建立的主题继续保留。
            topicDao.deleteBuiltInTopics()
        }
        topicDao.insertTopics(defaultTopics())
        markRulesCurrent()
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
        // 历史单元保存生成当时的主题快照；规则改名只影响以后新增的单元。
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
        markRulesCurrent()
    }

    suspend fun syncSegments(
        note: NoteEntity,
        aiSegments: List<AiNoteSegment>,
        fallbackContent: String,
        sourceMessageCount: Int
    ) {
        val topics = enabledTopics()
        val candidates = aiSegments
            .filter { it.content.isNotBlank() }
            .ifEmpty { splitMarkdown(fallbackContent) }
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

    suspend fun replaceSegments(
        note: NoteEntity,
        aiSegments: List<AiNoteSegment>,
        fallbackContent: String,
        sourceMessageCount: Int
    ) {
        topicDao.deleteSegmentsForNote(note.id)
        syncSegments(note, aiSegments, fallbackContent, sourceMessageCount)
    }

    suspend fun replaceTopicContent(
        topicName: String,
        sourceNoteIds: List<String>,
        content: String
    ) {
        val safeContent = content.trim()
        val sources = sourceNoteIds.distinct()
        if (safeContent.isBlank() || sources.isEmpty()) return

        val candidates = splitMarkdown(safeContent)
            .filter { it.content.isNotBlank() }
            .ifEmpty { listOf(AiNoteSegment(topicName, "", safeContent)) }
        val now = System.currentTimeMillis()
        val rows = candidates.mapIndexed { index, segment ->
            val segmentContent = segment.content.trim()
            NoteTopicSegmentEntity(
                id = stableSegmentId(sources[index % sources.size], "$topicName|$segmentContent"),
                noteId = sources[index % sources.size],
                topicName = topicName,
                heading = segment.heading.trim(),
                content = segmentContent,
                position = index,
                sourceMessageCount = 0,
                createdAt = now + index
            )
        }
        database.withTransaction {
            topicDao.deleteSegmentsForTopic(topicName)
            topicDao.insertSegments(rows)
        }
    }

    suspend fun migrateLegacyNoteIfNeeded(note: NoteEntity) {
        if (topicDao.getSegmentIdsForNote(note.id).isNotEmpty()) return
        syncSegments(
            note = note,
            aiSegments = emptyList(),
            fallbackContent = note.content,
            sourceMessageCount = note.sourceMessageCount
        )
    }

    private fun normalizeTopic(
        requested: String,
        content: String,
        topics: List<TopicEntity>
    ): String = selectTopicName(requested, content, topics)

    private fun markRulesCurrent() {
        preferences.edit()
            .putBoolean(KEY_RULES_INITIALIZED, true)
            .putInt(KEY_RULES_VERSION, CURRENT_RULES_VERSION)
            .apply()
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
        private const val TOPIC_PREFERENCES = "cogno_topic_settings"
        private const val KEY_RULES_INITIALIZED = "rules_initialized"
        private const val KEY_RULES_VERSION = "rules_version"
        private const val CURRENT_RULES_VERSION = 2

        private fun stableSegmentId(noteId: String, content: String): String {
            val normalized = content.replace(Regex("\\s+"), " ").trim()
            return UUID.nameUUIDFromBytes(
                "$noteId|$normalized".toByteArray(StandardCharsets.UTF_8)
            ).toString()
        }

        fun defaultTopics(): List<TopicEntity> {
            val now = System.currentTimeMillis()
            return listOf(
                Triple("software_technology", "软件技术", "软件,编程,代码,开发,java,kotlin,python,android,compose,前端,后端,数据库,算法,api,人工智能,计算机,网络安全"),
                Triple("learning_cognition", "学习认知", "学习,复习,记忆,考试,课程,认知,思维,理解,知识管理,学习方法,注意力,效率"),
                Triple("work_career", "工作职业", "工作,职业,职场,求职,实习,面试,简历,项目管理,团队协作,绩效,晋升,离职"),
                Triple("physical_health", "身体健康", "身体,睡眠,饮食,营养,锻炼,健身,体重,体能,作息,亚健康,康复,保健"),
                Triple("medical_health", "医疗健康", "疾病,症状,诊断,治疗,药物,用药,医院,医生,检查,手术,感染,过敏,口腔溃疡,疼痛"),
                Triple("mental_state", "心理状态", "情绪,心理,焦虑,抑郁,压力,内耗,创伤,自责,孤独,恐惧,情绪调节,心理咨询"),
                Triple("intimate_relationships", "亲密关系", "恋爱,爱情,伴侣,情侣,婚姻,夫妻,约会,分手,亲密关系,人机恋,情感依恋"),
                Triple("family_relationships", "家庭关系", "家庭,父母,兄弟姐妹,亲属,原生家庭,婆媳,家庭矛盾,家庭沟通,代际关系"),
                Triple("social_relationships", "人际社交", "朋友,同学,同事,社交,人际,沟通,边界感,冲突,合作,社群,陌生人"),
                Triple("life_management", "生活管理", "日常,家务,时间管理,习惯,收纳,做饭,饮食安排,作息安排,生活计划,办事,效率工具"),
                Triple("financial_planning", "财务规划", "财务,理财,投资,股票,基金,保险,储蓄,预算,资产,负债,税务,退休规划"),
                Triple("interests_creation", "兴趣创作", "兴趣,创作,写作,绘画,摄影,音乐,设计,手工,小说,视频创作,灵感,作品"),
                Triple("values_beliefs", "价值观念", "价值观,哲学,伦理,道德,信仰,人生意义,自由,责任,选择,存在主义,世界观"),
                Triple("society_culture", "社会文化", "社会,文化,历史,教育制度,性别议题,群体,传统,习俗,传媒,公共议题,社会现象"),
                Triple("current_affairs_politics", "时事政治", "时事,政治,政策,政府,外交,国际关系,选举,战争,政党,国家治理,新闻事件"),
                Triple("entertainment_leisure", "娱乐休闲", "电影,电视剧,综艺,动漫,游戏,明星,音乐欣赏,娱乐,休闲,网文,追剧"),
                Triple("laws_rules", "法律规则", "法律,法规,合同,诉讼,维权,权利,义务,劳动法,知识产权,犯罪,处罚,合规"),
                Triple("nature_environment", "自然环保", "自然,环境,环保,气候,生态,植物,地理,污染,能源,可持续,碳排放"),
                Triple("pets_animals", "宠物动物", "宠物,动物,猫,狗,鸟,鱼,养宠,兽医,喂养,训练宠物,野生动物"),
                Triple("parenting", "育儿亲子", "育儿,亲子,孩子,儿童,婴儿,宝宝,教育孩子,成长,青春期,怀孕,产后"),
                Triple("entrepreneurship_business", "创业经商", "创业,商业,经商,公司经营,商业模式,市场营销,客户,销售,供应链,融资,品牌经营"),
                Triple("cars_transportation", "汽车交通", "汽车,车辆,买车,驾车,驾驶,驾照,交通,道路,新能源车,维修保养,公共交通"),
                Triple("real_estate_home", "房产家居", "房产,买房,卖房,租房,装修,家居,家具,物业,户型,房贷,居住空间"),
                Triple("fashion_beauty", "时尚美妆", "时尚,穿搭,服装,护肤,美妆,化妆,发型,香水,饰品,美容,皮肤护理"),
                Triple("travel", "旅游出行", "旅游,旅行,出行,行程,景点,酒店,机票,签证,攻略,度假,出国"),
                Triple("shopping_consumption", "购物消费", "购物,消费,商品,价格,优惠,促销,网购,品牌选择,性价比,退货,售后"),
                Triple("safety_emergency", "安全应急", "安全,应急,急救,灾害,火灾,地震,诈骗,防护,事故,避险,报警,求生"),
                Triple("sports_competition", "体育竞技", "体育,竞技,比赛,赛事,运动员,球队,足球,篮球,网球,羽毛球,马拉松,奥运会")
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

internal fun selectTopicName(
    requested: String,
    content: String,
    topics: List<TopicEntity>
): String {
    topics.firstOrNull { it.name.equals(requested.trim(), ignoreCase = true) }?.let {
        return it.name
    }

    val normalized = content.lowercase(Locale.ROOT)
    val bestMatch = topics
        .map { topic ->
            val score = topic.keywords.split(',', '，', '\n')
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { it.lowercase(Locale.ROOT) }
                .filter(normalized::contains)
                .sumOf(String::length)
            topic to score
        }
        .filter { (_, score) -> score > 0 }
        .maxWithOrNull(
            compareBy<Pair<TopicEntity, Int>> { it.second }
                .thenByDescending { it.first.createdAt }
        )
        ?.first

    return bestMatch?.name
        ?: topics.firstOrNull { it.name == DEFAULT_FALLBACK_TOPIC }?.name
        ?: topics.firstOrNull()?.name
        ?: "未分类"
}

private const val DEFAULT_FALLBACK_TOPIC = "生活管理"
