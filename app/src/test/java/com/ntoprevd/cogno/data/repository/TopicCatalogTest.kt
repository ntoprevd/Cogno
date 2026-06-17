package com.ntoprevd.cogno.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicCatalogTest {
    @Test
    fun defaultTopicsMatchProductCatalog() {
        val names = TopicRepository.defaultTopics().map { it.name }

        assertEquals(
            listOf(
                "软件技术", "学习认知", "工作职业", "身体健康", "医疗健康", "心理状态",
                "亲密关系", "家庭关系", "人际社交", "生活管理", "财务规划", "兴趣创作",
                "价值观念", "社会文化", "时事政治", "娱乐休闲", "法律规则", "自然环保",
                "宠物动物", "育儿亲子", "创业经商", "汽车交通", "房产家居", "时尚美妆",
                "旅游出行", "购物消费", "安全应急", "体育竞技"
            ),
            names
        )
        assertEquals(28, names.distinct().size)
        assertTrue(TopicRepository.defaultTopics().all { it.keywords.isNotBlank() })
    }

    @Test
    fun exactAiTopicSelectionTakesPriority() {
        val topics = TopicRepository.defaultTopics()

        assertEquals(
            "价值观念",
            selectTopicName("价值观念", "这段内容也提到了恋爱关系", topics)
        )
    }

    @Test
    fun keywordScoringDistinguishesPreviouslyBroadHealthAndRelationshipTopics() {
        val topics = TopicRepository.defaultTopics()

        assertEquals("医疗健康", selectTopicName("", "反复口腔溃疡应该如何治疗和用药", topics))
        assertEquals("身体健康", selectTopicName("", "调整睡眠和饮食并开始健身", topics))
        assertEquals("家庭关系", selectTopicName("", "原生家庭与父母沟通造成的创伤", topics))
        assertEquals("亲密关系", selectTopicName("", "讨论人机恋、爱情与情感依恋", topics))
    }

    @Test
    fun unknownContentUsesLifeManagementInsteadOfFirstTopic() {
        assertEquals(
            "生活管理",
            selectTopicName("", "一些暂时无法明确归类的零散记录", TopicRepository.defaultTopics())
        )
    }
}
