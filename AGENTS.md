# Cogno 项目交接说明

本文档用于给后续 Agent 快速接手当前项目。更新时间：2026-06-02。

## 工作规则

- 禁止批量删除文件或目录。
- 不要使用 `del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse`、`rm -rf`。
- 如需批量删除文件，停止操作并让用户手动处理。
- 只完成用户明确提到的要求，其他代码保持原样，不随意重构。
- 修改前先读相关代码，优先沿用现有架构和 UI 风格。
- 代码保留必要注释，说明功能、作用区域和非显而易见的实现原因。
- 新增或重写文件保持 UTF-8。修改中文文案或文档时要特别注意编码。
- 用户非常关注视觉细节和交互手感。涉及 UI 时，应对照 `app/src/main/assets/` 和 `product-diagram/` 的 WebView/高保真原型，小范围迭代。
- 不要让用户提交真实 API Key 到仓库。真实密钥只应保存在本地配置或安全存储中。

## 项目定位

Cogno 是一个 Android 原生应用原型，当前主线已经从早期 WebView 页面迁移到 Kotlin + Jetpack Compose。

产品核心闭环：

- 聊天：本地会话、消息持久化、真实 AI API、SSE 流式回复。
- 笔记：从对话生成结构化笔记，保存到笔记库，支持查看、搜索、编辑、重命名、置顶、删除。
- 设置：模型/API 配置、API Key 遮罩、连接测试、系统提示词、输出风格、深色模式偏好、语言偏好。

当前阶段已经进入“产品化打磨”：阅读体验、Markdown、设置真实化、菜单手感、欢迎页品牌信号都已完成一轮，后续更适合做小步精修，而不是大范围重构。

## 技术栈与构建信息

- Kotlin
- Jetpack Compose / Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle ViewModel Compose
- Room
- OkHttp
- Gradle Kotlin DSL
- Java 17
- JUnit / AndroidX Test / Espresso
- 保留的视觉参考资源：HTML/CSS/JavaScript/Tailwind/Font Awesome/Google Fonts

构建信息：

- 单模块：`:app`
- `namespace/applicationId`：`com.ntoprevd.cogno`
- Gradle Wrapper：9.2.1
- Android Gradle Plugin：9.0.1
- Kotlin：2.3.21
- Compose BOM：2026.04.01
- `minSdk 29`
- `targetSdk 36`
- `compileSdk 36.1`
- `versionName`：`1.0`

常用验证命令：

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```

如 Gradle 增量编译出现不合理的 `Conflicting overloads`，可先尝试：

```powershell
.\gradlew.bat :app:compileDebugKotlin --rerun-tasks
```

不要把 `gradlew clean` 作为默认手段；如确需清理构建产物，先说明原因并征求用户意见。

## 关键目录和文件

- `app/src/main/kotlin/com/ntoprevd/cogno/MainActivity.kt`
  - 原生入口 Activity。
  - 使用 `setContent { CognoApp(...) }` 启动 Compose。
  - 配置 edge-to-edge 系统栏、状态栏/导航栏明暗主题。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/CognoApp.kt`
  - Compose 导航入口。
  - 路由包含聊天、笔记列表、笔记详情、设置。
  - 管理深色模式偏好、语言偏好、当前模型名，并把设置变更同步到聊天顶栏。
  - 支持从笔记详情跳回来源会话。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/chat/ChatScreen.kt`
  - 聊天首页和侧边栏主要实现。
  - 包含顶部栏、品牌欢迎页、消息列表、底部输入框、侧边栏、会话长按菜单、用户消息长按菜单、assistant 消息工具栏、笔记生成入口。
  - 侧边栏手势、遮罩、菜单、输入框、底部用户条是用户重点关注区域，修改前必须先读现有实现。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/chat/ChatViewModel.kt`
  - 聊天页面状态管理。
  - 管理当前会话、消息列表、输入框、抽屉开关、发送状态、错误状态、笔记生成状态。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/common/BasicMarkdown.kt`
  - 聊天 assistant 回复使用的基础 Markdown 渲染组件。
  - 支持标题、粗体、斜体、列表、任务列表、引用、分割线、行内代码、代码块、链接样式、表格。
  - 不是完整 CommonMark，实现以稳定和可读为主。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/notes/NotesScreen.kt`
  - Room 驱动的笔记列表页面。
  - 支持搜索框下滑展开、长按菜单、重命名、置顶、删除。
  - 长按菜单已改成较窄、淡阴影、带图标的样式。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/notes/NoteDetailScreen.kt`
  - Room 驱动的笔记详情页面。
  - 支持查看/编辑模式切换，顶栏铅笔/眼睛图标切换，内容保存，跳回来源会话。
  - 编辑模式已增加键盘避让，减少输入法遮挡。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/settings/SettingsScreen.kt`
  - 设置页。
  - 已接入 API Base URL、Model ID、API Key 局部遮罩、连接测试、系统提示词、输出风格、深色模式偏好、语言偏好、离开前保存提醒。
  - 账号信息和 API 消耗统计当前仍为占位展示。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/network/AiChatClient.kt`
  - AI 网络层。
  - 支持非流式请求、SSE 流式聊天、连接测试、笔记草稿生成。
  - SSE 中 `delta.content == null` 已跳过，避免回复中出现连续 `null`。
  - 请求会带上 `temperature`，并把输出风格约束追加到 system prompt。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/settings/`
  - `AiSettings.kt`、`AiSettingsStore.kt`：AI 服务配置、输出风格、temperature。
  - `AppSettings.kt`、`AppSettingsStore.kt`：App 偏好，目前包含深色模式和语言偏好。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/db/`
  - Room 数据库层。
  - 当前包含 Session、Message、Note 相关 Entity/DAO。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/NativeChatRepository.kt`
  - 聊天 Repository。
  - 负责会话/消息 Flow、发送消息、SSE 回复持久化、重命名、置顶、删除、编辑用户消息并重新生成、重新生成 assistant 回复、消息反馈、从会话生成/更新笔记。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/NativeNoteRepository.kt`
  - 笔记 Repository。
  - 负责笔记列表/详情 Flow、重命名、置顶、删除、内容更新。

- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - 当前 App adaptive icon 的 Cogno 品牌标识资源。

- `app/src/main/assets/`
  - 早期 WebView 静态页面、样式和脚本。
  - 当前不是运行主线，但仍是 UI/交互参考。

- `product-diagram/`
  - 高保真静态原型参考，不是 Android 运行时入口。

## 当前已完成内容

### 1. Compose 主线和基础 UI

- `MainActivity.kt` 已使用 Compose 启动应用。
- `CognoApp.kt` 已配置聊天、笔记列表、笔记详情、设置页面导航。
- 聊天页消息列表、底部输入框、顶部栏、侧边栏已经可用。
- 侧边栏支持按钮打开、左侧边缘滑动打开、打开状态拖动关闭。
- 会话列表接入 Room，支持搜索、重命名、置顶、删除。
- 侧边栏搜索历史会话已真实过滤标题和最近消息预览。
- 顶栏图标使用用户提供的 drawable：
  - `R.drawable.dehaze_24px`
  - `R.drawable.wand_shine_24px`
  - `R.drawable.maps_ugc_24px`

### 2. 真实 AI 聊天能力

- 已引入 OkHttp。
- 已新增 AI 配置存储：
  - 默认 Base URL：`https://api.deepseek.com/v1`
  - 默认模型：`deepseek-v4-flash`
  - 支持系统提示词。
  - 支持输出风格和 temperature。
- 已实现真实 API 对话。
- 已实现 SSE 流式输出。
- assistant 消息按状态持久化：
  - `pending`
  - `streaming`
  - `completed`
  - `failed`
- 支持失败提示、重新生成、编辑用户消息后重新生成。
- 用户已真机验证 DeepSeek API 可正常对话。

### 3. 阅读体验与 Markdown

- 用户消息保留气泡。
- assistant 消息改为接近全宽的正文块，适合长文本、列表、代码和总结。
- assistant 消息下方常驻工具栏：
  - 复制
  - 点赞
  - 点踩
  - 重新生成
- assistant 消息不再使用长按弹窗。
- `BasicMarkdown` 支持基础常见格式：
  - 标题
  - 粗体
  - 斜体
  - 列表
  - 编号列表
  - 任务列表
  - 引用
  - 分割线
  - 行内代码
  - 代码块
  - 链接样式
  - 表格
- 笔记详情编辑模式已增加 `imePadding()`、滚动和导航栏避让，缓解键盘遮挡。

### 4. 消息和菜单交互

- 用户消息长按：
  - 顶部显示年月日时间
  - 复制
  - 修改
- assistant 回复下方工具栏：
  - 复制
  - 点赞
  - 点踩
  - 重新生成
- 侧边栏会话长按菜单已收窄、降低阴影、添加图标：
  - 重命名
  - 置顶/取消置顶
  - 删除
- 笔记库条目长按菜单已模仿侧边栏菜单样式，添加图标。
- `messages.feedback` 字段用于 assistant 点赞/点踩。
- 已修复“修改用户消息后没有重新生成回复”的问题。

### 5. 设置页真实化

- API Base URL 可编辑。
- Model ID 可编辑。
- API Key 做局部遮罩：显示前几位和后几位，中间加密，不提供显示全部。
- 支持连接测试，按钮位于 `API 配置` 标题行右侧。
- 支持系统提示词编辑。
- 支持 DeepSeek 模型预设弹窗选择。
- 支持输出风格弹窗选择：
  - 理智
  - 全面
  - 简短
  - 友好
  - 热情
- 输出风格会映射 `temperature` 并附加简短行为约束。
- 支持深色模式偏好弹窗选择：
  - 跟随系统
  - 浅色
  - 深色
- 支持 UI 语言偏好弹窗选择：
  - 简体中文
  - English
- 语言偏好目前主要影响设置页部分文案，不是完整全站资源化 locale。
- 设置页离开时会检测未保存的 AI/API 相关修改；顶部返回和 Android 系统返回键都会弹出保存确认。
- 清理临时缓存入口为安全提示版本，不执行批量删除，遵守项目禁止批量删除规则。
- 账号信息和 API 消耗统计目前保留为占位。

### 6. 笔记系统基础版

- 已新增 `NoteEntity`、`NoteDao`。
- `AppDatabase` 已包含 notes 表。
- 已新增 `NativeNoteRepository`。
- `NotesScreen.kt` 已从 sample state 改为 Room 驱动。
- 笔记列表支持：
  - 展开式搜索框
  - 重命名
  - 置顶
  - 删除
  - 空状态
- `NoteDetailScreen.kt` 已从 Room 读取笔记。
- 笔记详情支持：
  - 查看/编辑模式切换
  - 保存内容
  - 显示来源信息
  - 跳回来源聊天会话
- 聊天顶部“生成笔记”按钮已接入真实 AI 总结。
- 笔记生成支持风格选择：
  - 简洁摘要
  - 标准笔记
  - 详细复习
- 同一会话无变化时重复点击不会反复新建笔记，会提示“笔记已是最新”。
- 会话新增消息后再次生成，会更新已有笔记。
- 笔记生成期间有顶部下方提示，不遮挡顶栏。

### 7. 品牌欢迎页和 App 图标

- 聊天欢迎页已从旧文案状态改成品牌欢迎页。
- 新增 Cogno 产品标识：偏“思维晶核/记忆核心”的橙色标识。
- 欢迎页标识下方会随机选择中英文短句并流式打印。
- adaptive launcher icon 的 foreground/background 已更新为同一套 Cogno 标识语言。

## 数据库状态

当前 Room 主要实体：

- `sessions`
- `messages`
- `notes`

重要字段：

- `sessions.modelId`：会话使用的模型。
- `messages.feedback`：assistant 点赞/点踩。
- `notes.sourceSessionId`：来源会话。
- `notes.sourceMessageCount`：生成笔记时使用的消息数量，用于判断是否需要更新。
- `notes.content`：笔记正文。
- `notes.title`：笔记标题。
- `notes.pinned`：置顶。

数据库版本已经历迁移，注意不要假设仍是早期 v1 结构。

## 当前已知问题和产品化缺口

### P0：稳定性和视觉细修

- 欢迎页标识和流式文案只是第一版，用户还可能继续微调图形、句子、字体、间距。
- Markdown 表格是轻量实现，不是完整 CommonMark 表格解析。

### P1：设置页和语言体系

- 清理临时缓存目前未真实删除文件，因为项目规则禁止批量删除；若要做真实清理，需要重新定义安全边界并征求用户确认。

### P2：笔记系统增强

主题模式建议后续增加：

- `ThemeEntity`
- `NoteThemeRef`
- 用户自建主题
- AI 建议主题
- 笔记可多标签/多主题关联

### P3：账户、安全、数据管理

- 需要本地数据管理：
  - 导出聊天
  - 导出笔记
  - 数据库体积提示
  - 清理失败消息
  - 文件、PDF/文档解析仍未完成。

## 建议下一阶段执行计划

下一阶段不建议再大范围改 UI。更适合做“可见度高的小闭环”，每次只做 1-2 个点：

1. 真机微调欢迎页
   - 图标形态
   - 文案句子池
3. 笔记详情查看模式复用 `BasicMarkdown`
   - 当前聊天 assistant 已使用 `BasicMarkdown`。
   - 笔记详情查看模式仍有自己的 `MarkdownLikeContent`，后续可考虑统一，但要小心长笔记滚动性能。
5. 轻量本地数据管理
   - 导出笔记
   - 导出聊天
   - 失败消息清理
   - 数据库体积显示

## 真机重点验收清单

- 聊天输入框点击、输入法吸附、发送按钮。
- AI SSE 回复是否正常流式显示，不能出现 `nullnull...`。
- assistant 回复是否为全宽正文块。
- assistant Markdown：标题、列表、代码块、表格是否可读。
- assistant 回复下方复制/点赞/点踩/重新生成是否正常。
- 用户消息长按复制/修改是否正常，时间是否显示正常。
- 修改用户消息后是否重新生成 assistant 回复。
- 顶部按钮打开侧边栏。
- 左侧边缘滑动打开侧边栏。
- 打开状态拖动关闭侧边栏。
- 侧边栏搜索框是否能按关键词过滤会话。
- 会话长按菜单、重命名、置顶、删除是否正常。
- 侧边栏底部用户信息条位置和点击区域是否自然。
- 设置页 API Key 局部遮罩、连接测试。
- 设置页离开时未保存提醒：顶部返回和系统返回键都要测。
- 深色模式偏好是否影响主要页面。
- UI 语言偏好是否能在设置页局部生效。
- 生成笔记按钮、风格选择、生成中提示、成功提示。
- 重复生成同一会话笔记是否提示已是最新。
- 新消息后再次生成是否更新已有笔记。
- 笔记库搜索、重命名、置顶、删除。
- 笔记库长按菜单样式和图标是否正常。
- 笔记详情查看/编辑切换、保存、跳回来源会话。

## 当前风险与注意事项

- 不要随意回退用户已调好的 UI 细节，尤其是侧边栏底部用户条高度、欢迎页文案节奏、菜单尺寸。
- AI 配置可用，但不同供应商的接口兼容性仍需谨慎。当前实现主要按 OpenAI-compatible Chat Completions/SSE 思路处理。
- API Key 已遮罩但安全存储仍可升级。
- 笔记生成依赖大模型，提示词和格式要求后续需要让用户可配置。
- `AppDatabase` 当前 `exportSchema=false`，适合早期开发；正式迁移设计前应考虑打开 schema 导出并保存迁移记录。
- `app/src/main/assets/` 仍可能依赖 CDN。当前主要用于参考，不要回到 JSBridge/WebView 路线，除非用户明确要求。

## 给下一个 Agent 的建议路线

1. 先确认当前分支和工作区：

```powershell
git branch
git status
```

2. 先编译当前 Compose 主线：

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```

3. 阅读这些文件后再动手：

- `ChatScreen.kt`
- `ChatViewModel.kt`
- `BasicMarkdown.kt`
- `NativeChatRepository.kt`
- `NoteDetailScreen.kt`
- `NotesScreen.kt`
- `SettingsScreen.kt`
- `AiSettingsStore.kt`
- `AiChatClient.kt`

4. 后续优先小步迭代：

- 欢迎页品牌视觉和流式文案微调。
- 数据导出和本地数据管理。

5. 完成后运行编译，并让用户真机验收：

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```
