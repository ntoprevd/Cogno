# Cogno 项目交接说明

## 工作规则

- 禁止批量删除文件或目录。
- 不要使用 `del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse`、`rm -rf`。
- 如需批量删除文件，停止操作并让用户手动处理。
- 只完成用户明确提到的要求，其他代码保持原样，不随意重构。
- 代码需要保留必要注释，说明功能、作用区域和非显而易见的实现原因。
- 新增或重写文件保持 UTF-8。项目历史文件曾出现中文乱码，后续修改时要特别注意编码。

## 项目定位

Cogno 是一个 Android 应用原型，使用原生 Android WebView 承载本地 HTML/CSS/JavaScript 页面。当前目标是从高保真前端原型逐步接入原生数据层、JSBridge、网络层和 AI 能力。

当前产品方向：

- 首页聊天：欢迎页、对话消息列表、输入框、语音入口、侧边栏历史会话。
- 笔记：笔记列表、笔记详情、从对话生成/管理笔记。
- 设置：模型/API 配置、深色模式、语言选择、缓存操作。

核心分工：

- Web 前端负责展示、交互、动画、输入框状态、滚动位置、Markdown 展示。
- Android 原生层负责 Room 持久化、Repository、JSBridge、网络请求、SSE 流式输出、权限、文件、语音、敏感配置。
- `localStorage` 只用于主题、语言等低风险偏好，不作为关键业务数据源。

## 技术栈

- Java
- HTML/CSS/JavaScript
- Gradle Kotlin DSL
- Android WebView
- Room
- AndroidX AppCompat / Material / Activity / ConstraintLayout
- JUnit / AndroidX Test / Espresso

构建信息：

- 单模块：`:app`
- `namespace/applicationId`：`com.ntoprevd.cogno`
- Gradle Wrapper：9.2.1
- Android Gradle Plugin：9.0.1
- Java：17
- `minSdk 29`
- `targetSdk 36`
- `compileSdk 36.1`

## 关键目录

- `app/src/main/java/com/ntoprevd/cogno/MainActivity.java`
  - 原生入口 Activity。
  - 初始化 WebView。
  - 加载 `file:///android_asset/index.html`。
  - 配置系统栏、WebView 安全策略、JSBridge 挂载。

- `app/src/main/java/com/ntoprevd/cogno/bridge/CognoJSBridge.java`
  - 当前 JSBridge 实现。
  - 暴露给 WebView 的对象名包括 `Android`、`chat`、`session`。
  - 已接入 `ChatRepositoryImpl`，可创建会话、保存用户消息、查询会话和消息。

- `app/src/main/java/com/ntoprevd/cogno/data/db/`
  - Room 数据库层。
  - 包含 `AppDatabase`、`Converters`、DAO、Entity。

- `app/src/main/java/com/ntoprevd/cogno/data/repository/`
  - Repository 层。
  - `ChatRepository` 是异步接口。
  - `ChatRepositoryImpl` 使用单线程后台执行数据库操作，并把结果回调到主线程。

- `app/src/androidTest/java/com/ntoprevd/cogno/data/db/AppDatabaseInstrumentedTest.java`
  - Room 真机仪器测试。
  - 验证 Session 排序、Message 分页、外键级联删除。

- `app/src/main/assets/`
  - WebView 加载的前端页面、样式和脚本。

- `product-diagram/`
  - 高保真静态原型参考，不是 Android 运行时入口。

## 已完成内容

### 1. 侧边栏转场修复

文件：

- `app/src/main/assets/js/common.js`

完成点：

- 修复侧边栏和主聊天界面之间过渡动画不同步的问题。
- 统一抽屉、主视图、蒙层的动画生命周期。
- 使用 `requestAnimationFrame`、`transitionend` 和兜底 timer 做动画收口。
- 拖拽中断或连续点击时会清理旧动画，降低蒙层残留、灰色遮罩错位等问题。

### 2. WebView 安全配置

文件：

- `app/src/main/java/com/ntoprevd/cogno/MainActivity.java`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`

完成点：

- 新增 `BuildConfig.WEBVIEW_DEV_MODE`。
- Debug：允许 WebView 调试，允许明文流量，方便本地调试。
- Release：禁用 WebView 调试，默认禁用明文流量。
- WebView 关闭多窗口、自动弹窗、Web SQL Database、file URL 跨域、universal file URL 访问。
- Release 混合内容使用 `MIXED_CONTENT_NEVER_ALLOW`。

### 3. Room 数据库基础层

文件：

- `SessionEntity.java`
- `MessageEntity.java`
- `SessionDao.java`
- `MessageDao.java`
- `AppDatabase.java`
- `Converters.java`

表结构：

- `sessions`
  - `id`
  - `title`
  - `model_id`
  - `pinned`
  - `archived`
  - `created_at`
  - `updated_at`
  - `last_message_preview`

- `messages`
  - `id`
  - `session_id`
  - `role`
  - `content`
  - `status`
  - `error_code`
  - `token_count`
  - `created_at`
  - `updated_at`

关系：

- `messages.session_id` 外键关联 `sessions.id`。
- 删除 Session 时，对应 Message 通过 `ForeignKey.CASCADE` 自动删除。

### 4. AndroidTest 真机测试

文件：

- `AppDatabaseInstrumentedTest.java`

已验证：

- 插入 Session 后，可按 `pinned DESC, updated_at DESC` 查询。
- 插入多个 Message 后，可按 `created_at ASC` 做 `limit + offset` 分页查询。
- 删除 Session 后，关联 Message 自动级联删除。

真机验证结果：

- 命令：`.\gradlew.bat :app:connectedDebugAndroidTest`
- 设备：`23117RK66C - 14`
- 结果：`BUILD SUCCESSFUL`
- 测试数量：4 个测试全部通过，包含 3 个数据库测试和 1 个示例测试。

### 5. Repository 异步层

文件：

- `ChatRepository.java`
- `ChatRepositoryImpl.java`
- `OnResultCallback.java`

完成点：

- `ChatRepository` 已改为异步接口。
- 所有数据库操作通过 `Executors.newSingleThreadExecutor()` 在后台线程执行。
- 结果通过 `OnResultCallback<T>` 返回。
- 回调切回主线程，便于 JSBridge 和 UI 层安全使用。

### 6. JSBridge 初版

文件：

- `CognoJSBridge.java`
- `MainActivity.java`

WebView 注入对象：

- `Android`
- `chat`
- `session`

已实现 JS 可调用命令：

```js
chat.createSession(title, modelId, callbackId)
chat.sendMessage(sessionId, userContent, callbackId)
chat.getMessages(sessionId, page, limit, callbackId)
session.getAllSessions(callbackId)
```

也支持协议式入口：

```js
Android.postMessage(JSON.stringify({
  version: 1,
  requestId: "req-1",
  command: "chat.createSession",
  callbackId: "cb",
  payload: {
    title: "测试会话",
    modelId: "deepseek-v3"
  }
}))
```

回调机制：

```js
window.cb = function (res) {
  console.log("Native result:", res)
}

chat.createSession("测试会话", "deepseek-v3", "cb")
```

当前 `sendMessage` 仅保存用户消息，不调用 AI 网络接口，不做流式输出。保存成功后会更新 Session 的 `updatedAt` 和 `lastMessagePreview`。

## 当前未完成内容

### P0：下一步优先级最高

- 在真机 WebView console 中手动验证 JSBridge 命令：
  - `chat.createSession`
  - `chat.sendMessage`
  - `chat.getMessages`
  - `session.getAllSessions`
- 把首页前端输入框和侧边栏会话列表接入 JSBridge。
- 实现聊天界面状态切换：
  - 默认显示欢迎页。
  - 发送第一条消息后切到对话消息列表。
  - 点击侧边栏会话后加载对应消息并切到对话界面。
- 补 JSBridge 层测试或手动验证流程记录。

### P1：核心聊天能力

- 接入真实 AI 网络层。
- 推荐使用 OkHttp/Retrofit。
- 实现 SSE 流式输出。
- 将 assistant 消息按状态持久化：
  - `pending`
  - `streaming`
  - `completed`
  - `failed`
  - `cancelled`
- 支持取消生成、失败重试。

### P2：笔记系统

- 新增 Note、Theme、NoteThemeRef 等表。
- 从会话或消息生成结构化笔记。
- 实现笔记列表、详情、编辑与来源关联。
- 设计提示词与增量更新策略。

### P3：语音、文件与适配

- 语音录制、ASR、结果回填输入框。
- 文件选择、附件入库、PDF/文档解析。
- CDN 资源本地化。
- 长会话虚拟列表或分段渲染。
- 横屏、平板、大字体、弱网、离线体验。

## 聊天界面状态切换需求

用户已确认需求：

- 默认显示欢迎页。
- 当用户发送第一条消息，切换到对话界面并显示消息气泡列表。
- 当用户点击侧边栏已有会话，加载对应消息并切换到对话界面。
- 切换过程应顺滑。
- 数据由原生层 Room + Repository + JSBridge 提供。

建议实现方式：

- 保持单个 `index.html`。
- 在 WebView 内通过 JS/CSS 控制欢迎页和消息列表显示隐藏。
- 不建议每次切换都让原生层重新加载整个 WebView 页面，这样会丢 UI 状态且体验不够顺。

## JSBridge 当前接口说明

### 直接调用方式

```js
window.cb = function (res) {
  console.log(res)
}

chat.createSession("测试会话", "deepseek-v3", "cb")
session.getAllSessions("cb")
```

创建会话返回数据包含：

- `id`
- `title`
- `modelId`
- `pinned`
- `archived`
- `createdAt`
- `updatedAt`
- `lastMessagePreview`

发送消息：

```js
chat.sendMessage("session-id", "你好", "cb")
```

返回数据包含：

- `session`
- `message`

查询消息：

```js
chat.getMessages("session-id", 0, 20, "cb")
```

返回数据包含：

- `sessionId`
- `page`
- `limit`
- `messages`

查询会话：

```js
session.getAllSessions("cb")
```

返回数据包含：

- `sessions`

### 协议式调用

`Android.postMessage(JSON.stringify(request))` 当前可用于同步返回 `accepted`，异步结果仍通过 `callbackId` 触发前端回调。

## 常用验证命令

编译 Debug Java：

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac
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

## 用户参与点

用户不需要理解所有实现细节，但需要参与这些判断：

- 真机上侧边栏动画是否顺滑。
- 欢迎页切到对话界面的体验是否自然。
- 消息气泡、字体、间距、时间显示是否符合预期。
- 侧边栏历史会话排序、标题、预览是否符合产品感觉。
- 设置页中模型/API 配置项是否符合实际使用习惯。
- 后续接入 AI 服务时，提供测试用的非敏感配置或明确使用哪类模型服务。

不要让用户提交真实 API Key 到仓库。真实密钥后续应走安全存储或本地配置。

## 当前风险与注意事项

- 前端仍依赖 CDN：Tailwind、Font Awesome、Google Fonts。离线或网络受限时样式和图标可能异常。
- 多处历史 HTML/JS/CSS 文案仍可能存在乱码，后续改页面时需逐步修复。
- `sendMessage` 目前只保存用户消息，没有 assistant 回复。
- JSBridge 已可编译，但还需要在 WebView console 中手动验证真实调用链。
- Repository 当前每个 `ChatRepositoryImpl` 实例持有一个单线程 executor，后续如果实例变多，应考虑单例或生命周期释放。
- `AppDatabase` 当前 `exportSchema=false`，适合早期开发；正式迁移设计前应打开 schema 导出并保存迁移记录。

## 给下一个 Agent 的建议路线

1. 先运行：

```powershell
.\gradlew.bat :app:compileDebugJavaWithJavac :app:compileDebugAndroidTestJavaWithJavac
```

2. 如手机连接，运行：

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

3. 安装/启动 Debug App 后，通过 WebView console 手动验证 JSBridge。

4. 验证通过后，修改 `app/src/main/assets/index.html`、`index.js`、`common.js`：

- 输入框发送消息时调用 `chat.createSession` 或 `chat.sendMessage`。
- 收到回调后渲染消息列表。
- 从欢迎页切换到对话界面。
- 侧边栏点击会话时调用 `chat.getMessages`。

5. 再进入 AI 网络层，不要在 JSBridge 尚未稳定前直接接 SSE。
