package com.ntoprevd.cogno.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntoprevd.cogno.BuildConfig
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkLine
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoLine
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoText
import com.ntoprevd.cogno.ui.theme.isCognoDarkTheme

private data class RoadmapItem(val title: String, val description: String)

private data class VersionRoadmapCopy(
    val screenTitle: String,
    val back: String,
    val introduction: String,
    val currentTitle: String,
    val futureTitle: String,
    val currentItems: List<RoadmapItem>,
    val futureItems: List<RoadmapItem>,
    val disclaimer: String
)

@Composable
fun VersionRoadmapScreen(
    languagePreference: String,
    onBack: () -> Unit
) {
    val isDark = isCognoDarkTheme()
    val copy = versionRoadmapCopy(languagePreference)
    val textColor = if (isDark) CognoDarkText else CognoText
    val background = if (isDark) CognoDarkBackground else CognoBackground

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = copy.back,
                    tint = textColor
                )
            }
            Text(
                text = copy.screenTitle,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Cogno",
                    color = if (isDark) CognoDarkPrimary else CognoPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "v${BuildConfig.VERSION_NAME}",
                    color = CognoMuted,
                    fontSize = 13.sp
                )
                Text(
                    text = copy.introduction,
                    color = textColor.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            RoadmapSection(
                title = copy.currentTitle,
                items = copy.currentItems,
                future = false,
                isDark = isDark
            )
            RoadmapSection(
                title = copy.futureTitle,
                items = copy.futureItems,
                future = true,
                isDark = isDark
            )
            Text(
                text = copy.disclaimer,
                color = CognoMuted,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )
        }
    }
}

@Composable
private fun RoadmapSection(
    title: String,
    items: List<RoadmapItem>,
    future: Boolean,
    isDark: Boolean
) {
    val textColor = if (isDark) CognoDarkText else CognoText
    val accent = if (isDark) CognoDarkPrimary else CognoPrimary
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            color = textColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (future) Icons.Default.Explore else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (future) CognoMuted else accent,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(19.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.description,
                        color = textColor.copy(alpha = 0.68f),
                        fontSize = 13.sp,
                        lineHeight = 21.sp
                    )
                }
            }
            if (index != items.lastIndex) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp)
                        .height(1.dp)
                        .clip(CircleShape)
                        .background(if (isDark) CognoDarkLine else CognoLine)
                )
            }
        }
    }
}

private fun versionRoadmapCopy(languagePreference: String): VersionRoadmapCopy {
    return if (languagePreference == AppLanguagePreference.EN) {
        VersionRoadmapCopy(
            screenTitle = "Version & Roadmap",
            back = "Back",
            introduction = "Cogno turns scattered information from conversations into personal knowledge that remains traceable, editable, and reusable.",
            currentTitle = "Core capabilities",
            futureTitle = "Exploration roadmap",
            currentItems = listOf(
                RoadmapItem("AI-assisted structured notes", "Generate concise, standard, or detailed notes from conversations and update them as the discussion continues."),
                RoadmapItem("Conversation and topic views", "Preserve the source conversation while extracting fragments from multiple notes into topic-based views."),
                RoadmapItem("Manageable topics", "Create and maintain topic rules with keywords, renaming, pinning, and deletion."),
                RoadmapItem("Local-first knowledge", "Store chats, notes, topics, and images on device with search, editing, and data export.")
            ),
            futureItems = listOf(
                RoadmapItem("Lightweight Agent workflows", "Let the model select controlled tools for multi-step tasks, with exploration of MCP-based tool access."),
                RoadmapItem("Cogno MCP service", "Expose conversation summarization, structured notes, and topic organization to compatible AI clients."),
                RoadmapItem("Broader model ecosystem", "Add more experience models and improve compatibility with OpenAI-compatible API providers."),
                RoadmapItem("Desktop experience", "Adapt Cogno for desktop workflows and explore secure data transfer across devices.")
            ),
            disclaimer = "These items describe exploration directions rather than committed features or release dates. Priorities may change as feasibility and user needs evolve."
        )
    } else {
        VersionRoadmapCopy(
            screenTitle = "版本与路线",
            back = "返回",
            introduction = "Cogno 致力于把对话中零散的信息，沉淀为可追溯、可编辑、可复用的个人知识。",
            currentTitle = "当前核心能力",
            futureTitle = "后续探索方向",
            currentItems = listOf(
                RoadmapItem("AI 辅助生成结构化笔记", "从对话生成简洁摘要、标准笔记或详细复习内容，并在讨论继续后更新已有笔记。"),
                RoadmapItem("对话笔记与主题视图", "保留笔记与来源会话的关联，同时从多篇笔记提取片段，按主题聚合查看。"),
                RoadmapItem("可管理的主题体系", "支持维护主题规则与关键词，并提供重命名、置顶和删除等管理能力。"),
                RoadmapItem("本地优先的知识管理", "会话、笔记、主题和图片主要保存在设备本地，支持搜索、编辑与数据导出。")
            ),
            futureItems = listOf(
                RoadmapItem("轻量 Agent 工作流", "让模型在受控范围内选择工具，完成检索、整理、导出等多步骤任务，并探索 MCP 工具接入。"),
                RoadmapItem("Cogno MCP 服务", "将对话总结、结构化笔记和主题整理能力封装为 MCP 服务，供兼容的 AI 客户端调用。"),
                RoadmapItem("更广的模型与 API 生态", "增加体验模型，并持续完善对 OpenAI-compatible API 与不同模型平台的适配。"),
                RoadmapItem("桌面端体验", "适配桌面场景下的阅读与整理流程，并探索安全的数据迁移和跨设备工作方式。")
            ),
            disclaimer = "以上内容为产品探索方向，不代表确定的功能承诺或发布时间；后续优先级将根据可行性与实际需求调整。"
        )
    }
}
