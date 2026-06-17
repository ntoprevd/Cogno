package com.ntoprevd.cogno.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntoprevd.cogno.data.settings.AppLanguagePreference
import com.ntoprevd.cogno.ui.theme.CognoBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkBackground
import com.ntoprevd.cogno.ui.theme.CognoDarkPrimary
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoText
import com.ntoprevd.cogno.ui.theme.isCognoDarkTheme

enum class LegalDocumentType {
    PRIVACY_POLICY,
    TERMS_OF_SERVICE
}

private data class LegalSection(
    val title: String,
    val body: String
)

@Composable
fun LegalDocumentScreen(
    type: LegalDocumentType,
    languagePreference: String,
    onBack: () -> Unit
) {
    val isEnglish = languagePreference == AppLanguagePreference.EN
    val isDark = isCognoDarkTheme()
    val context = LocalContext.current
    val title = when (type) {
        LegalDocumentType.PRIVACY_POLICY -> if (isEnglish) "Privacy Policy" else "隐私政策"
        LegalDocumentType.TERMS_OF_SERVICE -> if (isEnglish) "Terms of Service" else "服务条款"
    }
    val sections = legalSections(type, isEnglish)

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) CognoDarkBackground else CognoBackground)
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
                    contentDescription = if (isEnglish) "Back" else "返回",
                    tint = if (isDark) CognoDarkText else CognoText
                )
            }
            Text(
                text = title,
                color = if (isDark) CognoDarkText else CognoText,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEnglish) {
                    "Effective date: June 15, 2026"
                } else {
                    "生效日期：2026年6月15日"
                },
                color = CognoMuted,
                fontSize = 12.sp
            )
            sections.forEach { section ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDark) CognoDarkSurface else Color.White,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = section.title,
                        color = if (isDark) CognoDarkText else CognoText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = section.body,
                        color = if (isDark) CognoDarkText.copy(alpha = 0.82f) else CognoText.copy(alpha = 0.82f),
                        fontSize = 14.sp,
                        lineHeight = 23.sp
                    )
                }
            }
            Text(
                text = "github.com/ntoprevd/Cogno",
                color = if (isDark) CognoDarkPrimary else CognoPrimary,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(GITHUB_URL)
                            )
                        )
                    }
                    .padding(vertical = 14.dp)
            )
        }
    }
}

private fun legalSections(type: LegalDocumentType, isEnglish: Boolean): List<LegalSection> {
    return when {
        type == LegalDocumentType.PRIVACY_POLICY && !isEnglish -> privacySectionsZh()
        type == LegalDocumentType.TERMS_OF_SERVICE && !isEnglish -> termsSectionsZh()
        type == LegalDocumentType.PRIVACY_POLICY -> privacySectionsEn()
        else -> termsSectionsEn()
    }
}

private fun privacySectionsZh() = listOf(
    LegalSection(
        "1. 适用范围与开发者",
        "本政策适用于 Cogno Android 应用。Cogno 由“Cogno 个人开发者”维护，目前不提供账号注册、付费订阅或由开发者经营的收费项目。"
    ),
    LegalSection(
        "2. 本地保存的数据",
        "聊天会话、消息、笔记、主题、设置及聊天图片主要保存在设备应用私有目录和本地数据库中。除用户主动调用 AI、导出或系统能力外，Cogno 不会主动上传这些本地内容。卸载应用、清除应用数据或删除记录可能导致数据无法恢复。"
    ),
    LegalSection(
        "3. AI 请求与 API 配置",
        "使用体验模型时，必要的提示词、对话内容及所选图片会经 Cogno 的阿里云函数计算网关转发至对应模型服务商。使用自定义 API 时，请求会发送至用户配置的 API Base URL，服务商将依其自身政策处理数据。\n\n自定义 API Key 保存在设备本地应用配置中，目前未使用端到端加密存储。Cogno 不会把该密钥发送给个人开发者，但会在请求时将其作为鉴权信息发送至用户选择的 API 服务商。请勿在共享或不受信任的设备上保存敏感密钥。"
    ),
    LegalSection(
        "4. 视觉图片临时处理",
        "当用户主动向体验视觉模型发送图片时，应用会先压缩图片并通过 Cogno 网关上传至阿里云 OSS 香港地域的临时对象存储，再向模型服务商提供短时 HTTPS 地址。签名访问地址通常约 15 分钟有效；临时对象最长保留 24 小时，并通过生命周期规则自动删除。聊天气泡继续使用设备本地图片，不依赖远程副本。请勿上传身份证件、银行卡、医疗资料、未成年人隐私或其他不必要的敏感信息。"
    ),
    LegalSection(
        "5. 系统能力与第三方服务",
        "相机、相册和文件选择仅在用户主动操作时使用。语音输入由 Android 系统或设备提供的语音识别服务处理，可能受相应系统服务商政策约束。AI 输出、阿里云函数计算、阿里云 OSS 及模型服务商均属于第三方能力，其可用性和数据处理规则不由 Cogno 完全控制。"
    ),
    LegalSection(
        "6. 数据导出与删除",
        "用户可在“设置－显示与存储”中将聊天、笔记、主题和本地聊天图片导出为 ZIP 文件，并自行选择保存位置。用户可在应用内删除会话或笔记，也可通过 Android 系统清除应用数据。导出的文件由用户自行保管，Cogno 无法控制其后续复制、分享或泄露。"
    ),
    LegalSection(
        "7. 保存期限与安全",
        "本地数据通常保存至用户主动删除、清除应用数据或卸载应用。临时视觉图片最长保留 24 小时。Cogno 会采取与个人项目规模相适应的措施减少未授权访问，但任何本地存储、网络传输和第三方服务都无法保证绝对安全。"
    ),
    LegalSection(
        "8. 未成年人",
        "未成年人应在监护人指导下使用 Cogno。请勿使用本应用处理不满十四周岁未成年人的敏感个人信息，除非已取得监护人同意并具有合法、必要的处理目的。"
    ),
    LegalSection(
        "9. 联系与政策更新",
        "用户可通过 GitHub 项目 ntoprevd/Cogno 提交问题、更正建议或隐私请求。政策发生重要变化时，开发者将通过应用更新、项目页面或其他合理方式说明；法律法规规定的用户权利不因本政策而被排除。"
    )
)

private fun termsSectionsZh() = listOf(
    LegalSection(
        "1. 服务性质",
        "Cogno 是由“Cogno 个人开发者”维护的本地 AI 聊天与笔记工具。当前不提供由开发者经营的收费项目。使用本应用即表示用户理解并同意本条款；不同意时请停止使用相关服务。"
    ),
    LegalSection(
        "2. 体验模型与免费额度",
        "体验模型由开发者利用第三方免费额度自费或免费接入，用户无需向 Cogno 开发者付费。当前 GLM 视觉体验额度预计截至北京时间 2026年7月28日23:00；若第三方额度提前耗尽、服务商调整规则、接口故障或存在安全风险，服务可能提前暂停。\n\n体验模型由全体用户共享，不承诺请求次数、响应速度、模型列表、可用期限或持续提供。开发者可以增加、更换、限制、暂停或移除体验模型，并会尽量在项目页面或应用更新中说明明显变化。推荐长期使用者配置自己合法取得的 API。"
    ),
    LegalSection(
        "3. 自定义 API",
        "用户可自行配置兼容 API。由此产生的费用、额度、账号安全、内容政策、区域限制和服务中断由用户与相应服务商处理。Cogno 仅按用户配置发起请求，不保证所有兼容接口均可正常工作，也不对第三方计费错误或账号问题作出赔偿承诺。"
    ),
    LegalSection(
        "4. AI 输出提示",
        "AI 可能生成错误、过时、虚构、有偏差或不适当的内容。输出不构成医疗、法律、金融、安全或其他专业意见。用户应在采取重要行动前自行核实，并对输入内容、使用方式和最终决定负责。"
    ),
    LegalSection(
        "5. 内容与安全规则",
        "用户不得利用 Cogno 制作、传播或协助实施违反适用法律法规、危害国家安全和社会公共利益、暴力极端主义、恐怖主义、犯罪实施、未成年人色情或性剥削、非自愿色情、严重骚扰、自残诱导、恶意软件及其他明显有害内容。\n\n不得通过 DAN、提示词注入、伪造系统指令、编码隐藏或其他方式绕过模型和服务的安全限制。Cogno 及上游服务商可以拒绝、截断或停止存在安全风险的请求，但无法保证识别所有违规或攻击行为。"
    ),
    LegalSection(
        "6. 服务变化与中断",
        "Cogno 仍处于持续开发阶段，功能、模型和接口可能变化。开发者会尽力维护基本可用性，但不保证服务永不中断、完全无错误或满足特定目的。用户应及时导出重要数据，并为关键用途准备替代方案。"
    ),
    LegalSection(
        "7. 责任边界",
        "体验服务免费提供。因第三方模型、网络、免费额度、用户设备、自定义 API、错误输出或用户不当操作产生的损失，开发者在法律允许的最大范围内不承担间接、附带或可避免的损失责任。本条款不排除依法不能排除或限制的责任，也不影响用户依法享有的权利。"
    ),
    LegalSection(
        "8. 开源项目与反馈",
        "Cogno 项目地址为 github.com/ntoprevd/Cogno。欢迎通过 Issues 提交问题、修正和改进建议。提交代码或内容时，请确保拥有相应权利，并遵守仓库所示许可证和贡献规则。"
    ),
    LegalSection(
        "9. 条款更新",
        "开发者可因功能、第三方服务或法律要求更新条款，并通过应用更新或 GitHub 项目页面公示。更新后的条款不会排除法律法规赋予用户的权利。如用户不同意重要变更，应停止使用受影响的在线服务。"
    )
)

private fun privacySectionsEn() = listOf(
    LegalSection("1. Scope", "This policy applies to the Cogno Android app maintained by the independent Cogno developer. Cogno currently has no account registration, subscription, or developer-operated paid service."),
    LegalSection("2. Local data", "Chats, messages, notes, topics, settings, and chat images are primarily stored in the app's private local storage. They are not uploaded unless you actively use AI, export data, or invoke a system feature."),
    LegalSection("3. AI and API settings", "Experience-model requests pass necessary prompts, conversation content, and selected images through the Cogno Alibaba Cloud Function Compute gateway to the relevant model provider. Custom API requests go to the API Base URL you configure. Custom API keys are stored locally and are not currently protected by end-to-end encrypted storage."),
    LegalSection("4. Temporary vision images", "Images sent to an experience vision model are compressed and temporarily stored in Alibaba Cloud OSS in Hong Kong through the Cogno gateway. Signed URLs normally remain valid for about 15 minutes, and objects are retained for no longer than 24 hours. The chat bubble continues to use the local image."),
    LegalSection("5. Export and deletion", "You may export chats and other local data as a ZIP file to a location you select. You may delete records in the app or clear the app's Android data. You are responsible for protecting exported files."),
    LegalSection("6. Contact", "Questions, corrections, and privacy requests may be submitted through the ntoprevd/Cogno GitHub project. Statutory rights are not excluded by this policy.")
)

private fun termsSectionsEn() = listOf(
    LegalSection("1. Service", "Cogno is a locally focused AI chat and note tool maintained by the independent Cogno developer. There are currently no developer-operated paid services."),
    LegalSection("2. Free experience models", "Experience models use third-party free allowances and are shared by all users. The current GLM vision allowance is expected to end at 23:00 China Standard Time on July 28, 2026, and may end earlier if the allowance is exhausted or the provider changes its service. Models may be added, replaced, limited, paused, or removed."),
    LegalSection("3. Custom APIs", "You are responsible for provider fees, quotas, credentials, policies, and availability when using a custom API. Cogno does not guarantee compatibility with every API."),
    LegalSection("4. AI limitations", "AI output may be inaccurate, outdated, fabricated, biased, or inappropriate and is not professional medical, legal, financial, or safety advice."),
    LegalSection("5. Safety", "Do not use Cogno for unlawful activity, violent extremism, terrorism, criminal enablement, child sexual exploitation, non-consensual sexual content, severe harassment, self-harm encouragement, malware, or attempts to bypass safeguards through prompt injection or DAN-style instructions."),
    LegalSection("6. Liability and updates", "The free service is provided without a guarantee of uninterrupted availability. Liability is limited only to the extent permitted by applicable law, and rights that cannot legally be excluded remain unaffected. Updates will be announced through the app or project page.")
)

private const val GITHUB_URL = "https://github.com/ntoprevd/Cogno"
