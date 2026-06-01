# Cogno 项目交接说明

本文档用于给后续 Agent 快速接手当前项目。更新时间：2026-06-01。

## 工作规则

- 禁止批量删除文件或目录。
- 不要使用 `del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse`、`rm -rf`。
- 如需批量删除文件，停止操作并让用户手动处理。
- 只完成用户明确提到的要求，其他代码保持原样，不随意重构。
- 修改前先读相关代码，优先沿用现有架构和 UI 风格。
- 代码保留必要注释，说明功能、作用区域和非显而易见的实现原因。
- 新增或重写文件保持 UTF-8。项目历史文件曾出现中文乱码，修改文案或文档时要特别注意编码。
- 用户非常关注视觉细节和交互手感。涉及 UI 时，应对照 `app/src/main/assets/` 和 `product-diagram/` 的 WebView/高保真原型，小范围迭代。
- 不要让用户提交真实 API Key 到仓库。真实密钥只应保存在本地配置或安全存储中。

## 项目定位

Cogno 是一个 Android 原生应用原型，当前主线已经从早期 WebView 页面迁移到 Kotlin + Jetpack Compose。

产品核心闭环：

- 聊天：本地会话、消息持久化、真实 AI API、SSE 流式回复。
- 笔记：从对话生成结构化笔记，保存到笔记库，支持查看、搜索、编辑、重命名、置顶、删除。
- 设置：模型/API 配置、API Key 遮罩、连接测试、深色模式偏好。

当前阶段已经不是“搭功能骨架”，而是进入“产品化打磨”：阅读体验、Markdown、语言切换、欢迎页、UI 审美统一、笔记主题化、设置真实化等都属于必须逐步完成的内容。

## 技术栈

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

## 关键目录和文件

- `app/src/main/kotlin/com/ntoprevd/cogno/MainActivity.kt`
  - 原生入口 Activity。
  - 使用 `setContent { CognoApp(...) }` 启动 Compose。
  - 配置 edge-to-edge 系统栏、状态栏/导航栏明暗主题。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/CognoApp.kt`
  - Compose 导航入口。
  - 路由包含聊天、笔记列表、笔记详情、设置。
  - 支持从笔记详情跳回来源会话。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/chat/ChatScreen.kt`
  - 聊天首页和侧边栏主要实现。
  - 包含顶部栏、欢迎页、消息列表、底部输入框、侧边栏、会话长按菜单、消息长按菜单、笔记生成入口。
  - 侧边栏手势、遮罩、菜单、输入框是用户重点关注区域，修改前必须先读现有实现。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/chat/ChatViewModel.kt`
  - 聊天页面状态管理。
  - 管理当前会话、消息列表、输入框、抽屉开关、发送状态、错误状态、笔记生成状态。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/notes/NotesScreen.kt`
  - Room 驱动的笔记列表页面。
  - 支持搜索框下滑展开、长按菜单、重命名、置顶、删除。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/notes/NoteDetailScreen.kt`
  - Room 驱动的笔记详情页面。
  - 支持查看/编辑模式切换，顶栏铅笔/眼睛图标切换，内容保存，跳回来源会话。

- `app/src/main/kotlin/com/ntoprevd/cogno/ui/settings/SettingsScreen.kt`
  - 设置页。
  - 已接入 API Base URL、Model ID、API Key 遮罩、连接测试、系统提示词、深色模式偏好。
  - 账号信息和 API 消耗统计当前仍为占位展示。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/network/AiChatClient.kt`
  - AI 网络层。
  - 支持非流式请求、SSE 流式聊天、连接测试、笔记草稿生成。
  - SSE 中 `delta.content == null` 已跳过，避免回复中出现连续 `null`。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/settings/`
  - `AiSettings.kt`、`AiSettingsStore.kt`：AI 服务配置。
  - `AppSettings.kt`、`AppSettingsStore.kt`：App 偏好，目前包含深色模式。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/db/`
  - Room 数据库层。
  - 当前包含 Session、Message、Note 相关 Entity/DAO。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/NativeChatRepository.kt`
  - 聊天 Repository。
  - 负责会话/消息 Flow、发送消息、SSE 回复持久化、重命名、置顶、删除、编辑用户消息并重新生成、重新生成 assistant 回复、消息反馈、从会话生成/更新笔记。

- `app/src/main/kotlin/com/ntoprevd/cogno/data/repository/NativeNoteRepository.kt`
  - 笔记 Repository。
  - 负责笔记列表/详情 Flow、重命名、置顶、删除、内容更新。

- `app/src/main/assets/`
  - 早期 WebView 静态页面、样式和脚本。
  - 当前不是运行主线，但仍是 UI/交互参考，尤其是 note 页面搜索框和弹窗样式。

- `product-diagram/`
  - 高保真静态原型参考，不是 Android 运行时入口。

## 当前已完成内容

### 1. Compose 主线和基础 UI

- `MainActivity.kt` 已使用 Compose 启动应用。
- `CognoApp.kt` 已配置聊天、笔记列表、笔记详情、设置页面导航。
- 聊天页欢迎态、消息列表、底部输入框、顶部栏、侧边栏已经基本可用。
- 侧边栏支持按钮打开、左侧边缘滑动打开、打开状态拖动关闭。
- 会话列表接入 Room，支持搜索、重命名、置顶、删除。
- 顶栏图标已使用用户提供的 drawable：
  - `R.drawable.dehaze_24px`
  - `R.drawable.wand_shine_24px`
  - `R.drawable.maps_ugc_24px`

### 2. 真实 AI 聊天能力

- 已引入 OkHttp。
- 已新增 AI 配置存储：
  - 默认 Base URL：`https://api.deepseek.com/v1`
  - 默认模型：`deepseek-v4-flash`
  - 支持系统提示词。
- 已实现真实 API 对话。
- 已实现 SSE 流式输出。
- assistant 消息按状态持久化：
  - `pending`
  - `streaming`
  - `completed`
  - `failed`
- 支持失败提示、重新生成、编辑用户消息后重新生成。
- 用户已真机验证 DeepSeek API 可正常对话。

### 3. 消息长按菜单

- 用户消息长按：
  - 复制
  - 修改
- 模型消息长按：
  - 顶部显示年月日时间
  - 复制
  - 点赞
  - 点踩
  - 重新生成
- 已新增 `MessageEntity.feedback` 字段。
- 已修复“修改用户消息后没有重新生成回复”的问题。

### 4. 设置页真实可用化

- API Base URL 可编辑。
- Model ID 可编辑。
- API Key 已做遮罩、显示/隐藏、清空。
- 支持连接测试。
- 支持系统提示词编辑。
- 支持 DeepSeek 模型预设 chip。
- 支持深色模式偏好：
  - 跟随系统
  - 浅色
  - 深色
- 账号信息和 API 消耗统计目前保留为占位，后续再真实化。

### 5. 笔记系统基础版

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

### 6. 数据库状态

当前 Room 主要实体：

- `sessions`
- `messages`
- `notes`

重要字段：

- `messages.feedback`：assistant 点赞/点踩。
- `notes.sourceSessionId`：来源会话。
- `notes.sourceMessageCount`：生成笔记时使用的消息数量，用于判断是否需要更新。
- `notes.content`：笔记正文。
- `notes.title`：笔记标题。
- `notes.pinned`：置顶。

数据库版本已经历迁移，注意不要假设仍是早期 v1 结构。

## 当前已知问题和产品化缺口

### P0：阅读与输入体验

这是下一阶段最建议优先做的内容。

- AI 回复目前仍偏“气泡消息”形态。用户希望改成主流大模型的全宽正文块，类似 ChatGPT/Claude 回复样式。
- AI 回复缺少 Markdown 渲染。
- 笔记详情编辑模式下，输入法会遮盖下半部分内容，需要修复为可滚动并被键盘正确挤上去。
- 代码块、列表、标题、粗体、引用等基础 Markdown 要优先支持。

### P1：设置和状态同步

- 聊天页顶栏模型名目前仍可能是静态展示，应同步 `AiSettingsStore` 当前模型。
- 语言切换功能尚未唤醒。用户希望至少支持：
  - 简体中文
  - English
- 建议先把主要界面文案迁移到 Android string resources，再做 locale preference。
- API 消耗统计目前是占位。后续如果接口返回 usage，应记录真实 token；否则只能明确标注为估算。
- API Key 当前已有遮罩，但后续应升级到 Android Keystore / EncryptedSharedPreferences 一类安全存储。
- 清理本地缓存功能尚未真实实现。第一版建议只清理临时缓存，不清理聊天和笔记数据。

### P2：欢迎页和整体 UI 审美

- 当前欢迎页用户不满意：缺少软件图标，也没有达到想要的品牌效果。
- 需要重新设计 Cogno 欢迎页：
  - 有明确 Cogno 品牌信号。
  - 有软件图标或视觉核心。
- 长按菜单、Toast、弹窗目前功能可用但不够美观，后续需要统一视觉语言。
- 需要检查浅色/深色模式下所有页面、菜单、输入框、弹窗、列表的观感。

### P3：笔记系统增强

- 目前笔记按“对话”生成和展示。
- 用户长期希望支持“按对话 / 按主题”切换。
- 主题模式建议后续增加：
  - `ThemeEntity`
  - `NoteThemeRef`
  - 用户自建主题
  - AI 建议主题
  - 笔记可多标签/多主题关联
- 总结偏好可以采用两层设计：
  - 设置页配置默认总结风格。
  - 每个对话生成笔记时允许临时选择风格。
- 笔记详情后续需要简单 Markdown 编辑工具栏：
  - 标题
  - 加粗
  - 列表
  - 代码块

### P4：账户、安全、数据管理

- 账号设置目前是静态占位。
- 后续可以先做本地用户资料，不必马上做云账号：
  - 昵称
  - 头像
  - 默认偏好
- 需要本地数据管理：
  - 导出聊天
  - 导出笔记
  - 清理临时缓存
  - 清理失败消息
  - 数据库体积提示
- 长会话后续需要考虑分页、虚拟列表或分段渲染。
- 语音、文件、PDF/文档解析仍未完成。

## 建议下一阶段执行计划

下一个 Agent 最建议从“阅读体验与内容呈现升级阶段”开始。原因：它直接影响聊天和笔记两个核心闭环，是从毛坯走向可长期使用的关键。

建议第一批小范围执行：

1. AI 回复改为全宽正文样式
   - 用户消息可以继续保留气泡。
   - assistant 消息改成主流大模型正文块。
   - 注意不要破坏长按菜单、复制、点赞、点踩、重新生成。

2. AI 回复支持基础 Markdown 渲染
   - 先支持标题、粗体、列表、引用、行内代码、代码块、分割线。
   - 不需要一次性实现完整 CommonMark。
   - 优先做稳定、易读、可维护。

3. 笔记详情查看模式支持基础 Markdown 渲染
   - 可复用聊天 Markdown 渲染组件。
   - 注意笔记内容较长时滚动性能。

4. 修复笔记编辑键盘遮挡
   - 编辑模式应配合 `imePadding()`、滚动容器或焦点滚动，确保光标和下方内容不会被输入法遮住。
   - 真机验证是必须的。

5. 聊天顶栏模型名称同步设置
   - 从 `AiSettingsStore` 读取当前模型。
   - 顶栏展示应随设置页修改实时或重新进入后同步。

这一批不建议同时做语言切换、主题笔记、账户系统和缓存清理，避免范围过大。

## 后续阶段建议

### 第二阶段：设置页与语言切换

- 将主要页面文案抽到 `strings.xml`。
- 增加 English 资源。
- 增加 App 语言偏好。
- 继续完善 API 配置说明和内置模型预设。
- 规划 API Key 安全存储升级。

### 第三阶段：欢迎页和 UI 风格统一

- 重新设计欢迎页。
- 统一菜单、弹窗、Toast、列表、按钮、输入框。
- 对照 WebView 原型和用户真机反馈进行小步精修。

### 第四阶段：笔记主题化

- 设计 Theme/Tag 数据模型。
- 支持按对话/按主题切换。
- 支持用户自建主题和 AI 建议主题。
- 支持笔记多主题关联。

### 第五阶段：账户、统计、缓存和导出

- 本地用户资料。
- token usage 记录或估算。
- 清理临时缓存。
- 导出聊天和笔记。
- 弱网、API 失败、Key 失效的错误恢复。

## 常用验证命令

编译 Debug Kotlin + Java：

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```

编译 AndroidTest：

```powershell
.\gradlew.bat :app:compileDebugAndroidTestJavaWithJavac
```

真机运行仪器测试：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

构建 Debug APK：

```powershell
.\gradlew.bat :app:assembleDebug
```

如 Gradle 增量编译出现不合理的 `Conflicting overloads`，可先尝试：

```powershell
.\gradlew.bat :app:compileDebugKotlin --rerun-tasks
```

不要把 `gradlew clean` 作为默认手段；如确需清理构建产物，先说明原因并征求用户意见。

## 真机重点验收清单

- 聊天输入框点击、输入法吸附、发送按钮。
- AI SSE 回复是否正常流式显示，不能出现 `nullnull...`。
- 用户消息长按复制/修改是否正常。
- assistant 消息长按复制/点赞/点踩/重新生成是否正常。
- 修改用户消息后是否重新生成 assistant 回复。
- 顶部按钮打开侧边栏。
- 左侧边缘滑动打开侧边栏。
- 打开状态拖动关闭侧边栏。
- 侧边栏搜索框点击弹出输入法。
- 会话长按菜单和重命名弹窗。
- 设置页 API Key 遮罩、显示/隐藏、连接测试。
- 深色模式偏好是否影响主要页面。
- 生成笔记按钮、风格选择、生成中提示、成功提示。
- 重复生成同一会话笔记是否提示已是最新。
- 新消息后再次生成是否更新已有笔记。
- 笔记库搜索、重命名、置顶、删除。
- 笔记详情查看/编辑切换、保存、跳回来源会话。

## 当前风险与注意事项

- Compose 侧边栏手势仍是敏感区域。过去曾出现全屏手势层遮挡输入框、慢速滑动半开卡住、关闭动画遮罩挡点击等问题。
- Popup 菜单和 Toast 样式目前功能可用但不够精致，用户接受暂时粗糙，但后续需要统一打磨。
- AI 配置可用，但不同供应商的接口兼容性仍需谨慎。当前实现主要按 OpenAI-compatible Chat Completions/SSE 思路处理。
- API Key 已遮罩但安全存储仍可升级。
- 笔记生成依赖大模型，提示词和格式要求后续需要让用户可配置。
- Markdown 尚未完善，是下一阶段重点。
- 语言切换尚未实现，不要误认为设置页语言选项已经真实可用。
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
- `NativeChatRepository.kt`
- `NoteDetailScreen.kt`
- `NotesScreen.kt`
- `SettingsScreen.kt`
- `AiSettingsStore.kt`
- `AiChatClient.kt`

4. 优先执行“阅读体验与内容呈现升级阶段”：

- AI 回复全宽正文样式。
- AI 回复基础 Markdown。
- 笔记详情基础 Markdown。
- 笔记编辑键盘遮挡修复。
- 顶栏模型名同步。

5. 完成后运行编译，并让用户真机验收。

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:compileDebugJavaWithJavac
```

6. 不要一次性启动语言切换、主题笔记、账户系统和缓存清理。它们都重要，但应分阶段做。
