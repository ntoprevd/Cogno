# Cogno 项目说明

## 项目概述

Cogno 是一个 Android 应用原型，采用原生 Android WebView 承载本地 HTML/CSS/JavaScript 页面。当前核心场景包括 AI 对话首页、历史会话侧边栏、笔记库、笔记详情、设置页、深色模式、语言选择、模型/API 配置等前端交互。

项目现阶段定位：高保真前端原型已经具备继续接入原生后端的基础，但真实数据、Room 数据库、网络层、流式 AI 调用、语音与文件能力仍需分阶段实现。

目标用户是移动端 AI 助手/学习笔记产品使用者，典型场景是对话、从对话生成/管理笔记、配置模型和界面偏好。

## 技术栈

- 语言：Java、HTML、CSS、JavaScript、Gradle Kotlin DSL。
- Android：单模块 `:app`，`namespace/applicationId` 为 `com.ntoprevd.cogno`。
- 构建：Gradle Wrapper 9.2.1、Android Gradle Plugin 9.0.1、Java 17。
- SDK：`minSdk 29`、`targetSdk 36`、`compileSdk 36.1`。
- 主要依赖：AndroidX AppCompat、Material、Activity、ConstraintLayout、JUnit、AndroidX Test、Espresso。
- 前端依赖：页面目前通过 CDN 加载 Tailwind CSS、Font Awesome、Google Fonts；正式发布前应评估本地化资源。

## 项目目录结构说明

- `settings.gradle.kts`：Gradle 项目入口，声明根项目 `Cogno` 并包含 `:app` 模块。
- `build.gradle.kts`：根构建脚本，统一声明 Android Application 插件。
- `gradle/libs.versions.toml`：集中管理 AGP、AndroidX、测试库版本。
- `app/build.gradle.kts`：Android 应用模块配置、SDK、版本号、依赖、Java 17 编译选项，以及 WebView 调试/发布开关。
- `app/src/main/AndroidManifest.xml`：应用清单，声明网络和录音权限、启动 Activity、明文流量占位开关。
- `app/src/main/java/com/ntoprevd/cogno/MainActivity.java`：原生入口 Activity，初始化 WebView，加载 `file:///android_asset/index.html`，提供 JSBridge 与系统栏同步能力。
- `app/src/main/res/layout/activity_main.xml`：只包含一个全屏 `WebView`。
- `app/src/main/assets/`：WebView 加载的前端页面与资源。
- `product-diagram/`：产品高保真静态原型与设计说明，供参考，不是 Android 运行时主入口。
- `app/src/test/`、`app/src/androidTest/`：默认示例单元测试和仪器测试。
- `local.properties`：本机 Android SDK 路径配置，应由开发者本地生成，不应提交。

## 核心模块/组件介绍

- `MainActivity`：负责沉浸式系统栏、WebView 安全配置、页面加载、返回键处理、JSBridge 协议入口。
- `assets/index.html` + `js/index.js`：AI 对话首页，包含欢迎语打字效果、表情切换、侧边栏入口、输入栏、语音遮罩等 UI。
- `assets/js/common.js`：跨页面公共逻辑，包括深色模式持久化、侧边栏拖拽、历史会话长按菜单、重命名弹窗、状态栏/导航栏主题同步。
- `assets/note.html` + `js/note.js`：笔记库列表页，提供搜索栏展开和会话/主题模式切换。
- `assets/note-detail.html` + `js/note-detail.js`：笔记详情页，包含查看/编辑图标切换和分享菜单展示。
- `assets/setting.html` + `js/setting.js`：设置页，包含模型选择、自定义 API 面板、深色模式、语言选择弹层、本地缓存点击反馈等。
- `assets/css/common.css`：全局样式、深色模式、Markdown 正文、菜单动画、侧边栏和设置页细节样式。

## 前后端边界

当前项目采用“Web 前端负责展示与交互，原生层负责数据、网络、权限与系统能力”的分工。

- 前端负责：页面结构、视觉状态、用户交互、输入框状态、滚动位置、临时 UI 动画、Markdown 展示。
- 原生层负责：Room 持久化、AI 网络请求、SSE 流式输出、文件选择与解析、录音/ASR、敏感配置存储、权限申请、系统栏/键盘/平台能力。
- 前端不得直接保存关键业务数据作为唯一数据源；`localStorage` 仅用于主题、语言等低风险偏好或临时 UI 状态。
- 原生层通过 JSBridge 向前端返回结构化数据和事件，不把数据库实体直接暴露给页面，避免未来迁移困难。
- 长会话、流式消息、笔记增量更新应由原生层维护状态，前端只消费“追加、替换、完成、失败”等事件。

## JSBridge 协议

统一入口：`Android.postMessage(JSON.stringify(request))`。旧方法 `Android.setNavigationBarColor()`、`Android.setStatusBarDarkMode()` 暂时保留，后续逐步迁移到 `ui.setSystemBars`。

请求结构：

```json
{
  "version": 1,
  "requestId": "uuid-or-client-id",
  "command": "chat.sendMessage",
  "payload": {}
}
```

成功返回：

```json
{
  "version": 1,
  "requestId": "uuid-or-client-id",
  "ok": true,
  "data": {}
}
```

失败返回：

```json
{
  "version": 1,
  "requestId": "uuid-or-client-id",
  "ok": false,
  "error": {
    "code": "NOT_IMPLEMENTED",
    "message": "Command is defined but not implemented yet."
  }
}
```

已定义命令：

- `system.getCapabilities`
- `ui.setSystemBars`
- `chat.createSession`
- `chat.listSessions`
- `chat.getMessages`
- `chat.sendMessage`
- `chat.cancelStream`
- `note.list`
- `note.get`
- `note.save`
- `note.generateFromSession`
- `setting.get`
- `setting.set`
- `attachment.pickFile`
- `voice.startAsr`
- `voice.stopAsr`

错误码：

- `INVALID_REQUEST`：请求不是合法 JSON、缺少 command、payload 不符合要求。
- `UNSUPPORTED_VERSION`：协议版本不支持。
- `UNKNOWN_COMMAND`：命令未定义。
- `NOT_IMPLEMENTED`：命令已定义但尚未实现。
- `INTERNAL_ERROR`：原生层内部异常。

## WebView 安全策略

- Debug 构建：`BuildConfig.WEBVIEW_DEV_MODE=true`，允许 WebView 调试，Manifest 可开启明文流量，便于本地接口调试。
- Release 构建：`BuildConfig.WEBVIEW_DEV_MODE=false`，禁用 WebView 调试，默认禁用明文流量。
- WebView 默认关闭多窗口、自动弹窗、Web SQL Database、file URL 跨域访问、universal file URL 访问。
- 混合内容：Debug 使用兼容模式，Release 使用 `MIXED_CONTENT_NEVER_ALLOW`。
- `file:///android_asset/` 是正式入口；若后续需要更强的资源隔离，可迁移到 `WebViewAssetLoader`。

## 后端开发优先级

P0：

- 固化 JSBridge 协议与错误码。
- 设计 Room 表结构和实体关系。
- 实现 Session/Message 的本地读写与前端列表/消息渲染对接。

P1：

- 接入 Retrofit/OkHttp 和 SSE 流式输出。
- 实现发送消息、取消生成、失败重试、生成状态持久化。
- 实现 Note 的创建、编辑、列表、详情和来源关联。

P2：

- 笔记生成提示词、增量更新、主题聚合。
- 文件选择、附件入库、PDF/文档解析。
- 语音录制、ASR、结果回填输入框。

P3：

- CDN 资源本地化。
- 长会话虚拟列表或分段渲染。
- 横屏、平板、大字体、弱网、离线体验完善。

## 常见开发任务

- 构建 Debug APK：`.\gradlew.bat :app:assembleDebug`
- 构建 Release APK：`.\gradlew.bat :app:assembleRelease`
- 运行 JVM 单元测试：`.\gradlew.bat :app:testDebugUnitTest`
- 运行 Android 仪器测试：`.\gradlew.bat :app:connectedDebugAndroidTest`
- 新增 Android 依赖：优先在 `gradle/libs.versions.toml` 添加版本和库，再在 `app/build.gradle.kts` 引用。
- 修改前端页面：编辑 `app/src/main/assets/` 下的 HTML/JS/CSS；需要同步原型时，再参考 `product-diagram/`。
- 修改入口或 WebView 能力：编辑 `MainActivity.java` 和 `AndroidManifest.xml`。

## 注意事项

- 不要批量删除文件或目录；禁止使用 `del /s`、`rd /s`、`rmdir /s`、`Remove-Item -Recurse`、`rm -rf`。如需批量删除，先停止并让用户手动处理。
- 只修改用户明确要求的内容，避免顺手重构无关代码。
- 代码需要有必要注释，说明功能、作用区域和非显而易见的实现原因。
- 项目内历史文件曾出现中文注释/文案乱码，新增或重写文件必须保持 UTF-8。
- 前端页面依赖 CDN，离线或 WebView 网络受限时 Tailwind、图标和字体可能无法加载。
- 设置页中的 API Key、Base URL、费用等目前是静态展示/占位内容，不要提交真实密钥；如接入真实接口，应使用安全存储和本地配置。
- `local.properties`、签名文件、真实 API 配置属于本地/敏感信息，应由开发者自行创建并保持忽略。
