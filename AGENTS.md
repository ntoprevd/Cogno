# Cogno 项目协作说明

2026-6-18更新：本文件记录 Cogno 当前长期有效的项目约束。易变化的版本号、模型清单、额度、默认主题明细和部署 URL 以代码、部署平台和 Release 为准，不在本文复制。

## 项目定位

Cogno 是 Kotlin + Jetpack Compose 编写的 Android 原生 AI 对话与结构化笔记应用。核心闭环是：

```text
对话 → 生成或增量更新笔记 → 主题归类 → 编辑、回看、导出与来源追溯
```

修改时优先保护这个闭环，不能让主题笔记等产品特色退化成只有开发者才找得到的隐藏能力。

## 技术主线

- Android：Kotlin、Jetpack Compose、Navigation Compose、ViewModel、Room、OkHttp、Gradle Kotlin DSL。
- 数据：会话、消息、笔记、主题及主题片段保存在本地 Room；聊天图片保存在应用私有目录。
- 服务端：`server/` 下的 Node.js 网关，负责体验模型白名单、访问控制、SSE 转发和临时图片上传。
- 推荐体验链路：大陆用户访问阿里云函数计算网关；文本转发至 GLM；视觉图片临时上传至香港 OSS，并通过短时签名 URL 交给视觉模型。
- `workers.dev` 在部分中国大陆网络不稳定，不重新作为默认生产入口。仓库中的旧 Worker 代码如仍保留，只视为历史或回滚参考。
- Android 客户端不得包含 GLM Key 或 OSS RAM Secret。真实密钥只放部署平台环境变量。

## 关键位置

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/CognoApp.kt`：Compose 导航和跨页面状态连接。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/chat/ChatScreen.kt`：聊天、侧边栏、输入框、图片、语音和生成笔记入口。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/chat/ChatViewModel.kt`：聊天页面状态和用户操作。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/common/BasicMarkdown.kt`：聊天与笔记 Markdown 阅读体验。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/common/MarkdownEditorToolbar.kt`：Markdown 编辑格式操作。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/notes/NotesScreen.kt`：对话笔记和主题笔记列表。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/notes/NoteDetailScreen.kt`：笔记查看、编辑、导出和来源跳转。
- `app/src/main/kotlin/com/ntoprevd/cogno/ui/settings/`：用户资料、AI 配置、主题管理、数据管理和法律说明。
- `app/src/main/kotlin/com/ntoprevd/cogno/data/network/AiChatClient.kt`：模型请求、SSE、标题和笔记生成解析。
- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/NativeChatRepository.kt`：聊天持久化、停止生成、分支编辑和笔记同步。
- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/NativeNoteRepository.kt`：笔记正文和本地主题片段同步。
- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/TopicRepository.kt`：默认主题、用户主题、分类和主题整理稿。
- `app/src/main/kotlin/com/ntoprevd/cogno/data/db/`：Room 实体、DAO、迁移与数据库定义。
- `server/src/server.js`：当前 Node 网关主实现。
- `server/README.md`：部署步骤和环境变量说明。

修改前只读取任务涉及的文件；不要因本列表存在就批量改动。

## 修改范围与数据安全

- 用户指定“只修改某功能”时，只读写该功能必需文件。
- 未明确要求时，不重构现有笔记页面框架、导航结构或整体视觉体系。
- 保留用户已有会话、图片、笔记、主题片段和手动编辑。
- 数据库升级必须提供显式迁移和测试，不使用破坏性迁移规避问题。
- 不因默认规则、提示词或主题目录升级自动重写历史正文。
- `app/schemas` 是 Room 迁移依据，应保留并提交，不属于用户数据或敏感文件。
- 删除本地图片等少量明确文件时逐个处理；禁止递归批量删除目录。

## 笔记不变量

- 首次总结生成完整对话笔记。
- 再次总结只处理尚未总结或已经变化的消息，并将新内容追加到已有正文。
- 不覆盖旧正文、标题或用户手动编辑。
- 空响应、截断 JSON、解析失败或请求取消不得错误推进笔记同步进度。
- 模型兼容问题可进行一次条件明确的降级；不得制造重复笔记或重复消息。
- 用户手动创建或编辑的笔记通过本地拆分参与主题归类，不要求额外调用 AI。
- 任何修改都要同时检查来源关系、重启后的持久化和后续增量总结。

## 话题与主题

- “话题”负责保持一段讨论的内容完整性；“主题”负责分类、检索和跨笔记聚合。
- 主题不能机械决定正文结构。同一核心话题中的哲学、情绪、技术等侧面应成为子标题或要点，不因关键词被拆成并列大话题。
- 主题分类使用最小、可理解的内容单元，但不能破坏原始对话笔记。
- 主题规则修改主要影响未来内容；历史主题片段视为快照。
- 升级默认主题时保留用户自建主题。
- 删除、改名或重置主题不得删除历史笔记正文。
- 主题整理稿可以更新该主题的片段集合，但不得反向篡改原始对话笔记，除非用户明确要求改变此语义。
- 默认主题完整清单及分类关键词以 `TopicRepository` 中的当前实现为唯一来源，避免文档漂移。

## 图片、语音与附件

- 聊天气泡始终使用本地持久化图片显示，不能依赖远程 URL 才能回看。
- 视觉模型生产链路使用对象存储短时 URL，不继续扩展 Base64 图片方案。
- 图片上传必须限制类型、大小、超时，并处理上传失败、签名失效和模型拉取失败。
- OSS Bucket 保持私有，使用短时签名 URL 和生命周期自动清理临时对象。
- 文本模型不得收到图片 Base64；历史图片不得污染后续纯文本请求。
- 文件附件在真正支持发送和解析前，不得做成看似可用却无法完成的入口；可以隐藏或明确标注未支持。
- 语音识别必须处理权限拒绝、取消、超时、引擎不可用和生命周期；不能永久停留在“正在识别”。
- 不假设所有厂商 ROM 都提供兼容的 `SpeechRecognizer` 实现，真机问题应以回调和日志定位。

## UI 与交互

- 保持 Cogno 简洁、克制、安静、内容优先的风格。
- 橙色承担品牌和关键操作，不大面积滥用。
- 菜单、弹窗和浮层沿用统一的紧凑尺寸、圆角、细边框、轻阴影，并适配深色模式。
- 不随意回退已调好的侧边栏、欢迎页、菜单尺寸、Markdown 阅读区和输入框手感。
- 生成笔记、主题笔记、管理主题等产品独有能力应有可读名称，不能只用抽象图标让用户猜。
- 核心入口贴近使用场景；设置页可以保留配置入口，但不能成为唯一发现路径。
- 所有 UI 入口必须连接真实业务状态和持久化，禁止只有界面没有功能。
- Markdown 阅读与编辑以正文为主：表格、任务列表、删除线、引用和列表换行应正确；格式栏操作当前选区并贴近输入法；编辑必须真实保存。
- 只有当操作不可逆、影响数据或结果容易误解时才增加确认；普通高频操作不要层层弹窗。
- 点击区域、TalkBack 语义、权限拒绝和错误恢复属于交付质量，不是可选装饰。

## 网络、配置与隐私

- `server/.env.example` 只放占位符；真实环境变量不得提交。
- `local.properties` 只保存本机构建需要的非供应商主密钥配置，并保持 Git 忽略。
- 体验服务的真实 GLM Key、OSS AccessKey 和部署 Token 放阿里云环境变量。
- 不向自定义 AI 服务发送稳定设备标识，除非有明确必要性、隐私说明和用户确认。
- 网络问题按链路分段判断：Android → 网关 → OSS/模型服务；不要把入口不可达误判成模型或存储故障。
- 面向大陆用户的发布验收必须覆盖无 VPN 直连。代理导致的证书问题不得通过“信任所有证书”绕过。
- 法律与隐私文案必须反映真实链路、临时图片处理和体验额度边界，不写尚未实现的能力。

## 验证

修改范围允许时，优先运行：

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
git diff --check
```

服务端修改运行：

```powershell
npm --prefix server run check
```

- 不把 `gradlew clean` 作为默认修复；如确需清理构建产物，先说明原因并征求用户意见。
- Room 变更必须验证迁移和旧数据保留。
- SSE、空流、停止生成、图片上传等网络功能应验证成功与失败路径。
- 无法由自动化确认的输入法手感、图片缩放、菜单位置和真实模型响应，明确列入用户手动验收。
- 默认不在用户主力真机运行 `connectedDebugAndroidTest`。它可能卸载重装并清空聊天、笔记和图片；应使用模拟器或专用测试设备。
- 安装、卸载、清除数据或覆盖真机 APK 前必须先取得用户同意，并说明数据影响。

## Git 与发布

- 开始前检查当前分支和工作区，只处理本轮范围。
- 提交前扫描真实密钥、临时文件、部署状态和构建产物。
- `server` 源码与 `app/schemas` 可以进入仓库；`.env`、部署 Secrets、签名密码和本地产物不可进入仓库。
- APK 不提交到 Git 历史，作为 GitHub Release Asset 发布。
- Commit 记录一次代码修改；PR 说明阶段合并；Release 面向安装用户。不要把三者混为一条超长提交信息。
- 除非用户明确要求，只提交不推送；不要替用户猜测是否要合并、打标签或发布 Release。
- 发布前冻结范围：崩溃、数据丢失、无法对话、核心入口失效必须修；非致命视觉优化和新想法进入后续版本。

## 开始与交付检查

开始工作时：

1. 读取本文件和任务涉及代码。
2. 检查分支、工作区及相关历史改动。
3. 确认用户要求的是诊断、讨论还是直接实施。
4. 对可能影响数据、费用、隐私或真机状态的操作提前说明。

交付时说明：

- 实际修改内容和未修改范围。
- 自动验证结果。
- 未能自动验证的真实设备或云端步骤。
- 数据、密钥、部署或发布方面仍需用户完成的动作。
