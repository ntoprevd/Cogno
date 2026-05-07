# 2026-4-29 Cogno v1

## 第一部分

第一部分：AI进行功能补充，产品设计提示词，并根据需求笔者加以修改。

### 一、 会话管理模块 (Session Management)

该模块负责用户与 AI 交互的组织与持久化。

*   **1.1 多维度会话操作**
    *   **核心操作：** 新建会话、重命名（支持 AI 自动根据首句生成标题）、单条/批量删除、置顶。
    *   **历史检索：** 支持通过关键词搜索历史会话标题或特定的聊天记录。
    *   **侧边栏逻辑：** 参考 GPT 风格，按时间轴（年月日）对会话进行分组。
*   **1.2 消息持久化**
    *   **本地存储：** 采用 Room 数据库存储所有聊天记录，确保无网状态下也可流畅浏览。
*   **1.3 交互细节**
    *   **流式渲染：** 实时流式输出（SSE/WebSocket），提供平滑的打字机效果。
    *   **时间戳显示：** 长按用户信息会显示详细的“年月日 时:分:秒”。

### 二、 核心 AI 能力模块 (AI Core Capabilities)
该模块决定了 App 的“大脑”性能和灵活性。

*   **2.1 多模型与自定义接口**
    *   **模型切换：** 内置主流模型（DeepSeek，豆包 等）预设。
    *   **自定义 API：** 支持用户输入自己的 API Key，自定义 Base URL，调整参数（Temperature, Top_p, Max Tokens 等）。
    
*   **2.2 联网搜索（Search-Augmented Generation）**
    
    *   **手动触发：** 按钮式开关。开启后，AI 先调用搜索工具获取实时信息（新闻、天气、技术文档等）再回答。
    
    **2.3 语音交互**
    
    *   **语音输入：** 集成 ASR（语音转文字），支持点击录音和长按录音。

### 三、 结构化笔记系统 (Structured Note System) —— **Cogno 核心特色**
该模块是这款 App 区别于普通聊天工具的关键，强调知识的沉淀与整理。

*   **3.1 智能总结归纳**
    *   **多主题识别：** 会话中初次点击“总结”时，AI 自动识别此会话内容中，每一段对话中的逻辑断点（如从“Java”转向“恋爱”再转向“生活常识”等），并按层级生成 Markdown 标题。
    *   **增量更新（核心）：** 点击“总结”时，AI 自动对比当前笔记内容与新产生的对话记录，仅追加新知识点，保持旧内容的连贯性。
*   **3.2 笔记库入口**
    *   **独立空间：** 笔记以列表形式独立呈现，不与会话混杂。
    *   **编辑功能：** 完整的 Markdown 编辑器，支持手动修补 AI 生成的内容。
*   **3.3 笔记-会话联动 (Bi-directional Linking)**
    *   **跳转锚点：** 笔记中的每个小节可关联一个消息 ID。点击笔记旁的“跳转”图标，App 自动切换回原始会话并精准滚动定位到相关对话处。
*   **3.4 导出与分享**
    *   **导出：** Markdown (.md)格式

### 四、 文件处理 (File & Knowledge Base)
该模块扩展了 AI 的认知范围，使其能处理私有文档。

*   **4.1 多格式解析上传**
    *   **格式：** txt, md, pdf

### 五、 UI/UX 表现层 (Presentation & Experience)
该模块负责 App 的视觉美感和多设备适配。

*   **5.1 响应式布局**
    *   **移动端：** 单栏设计，侧边栏通过侧滑/主页面左上角某图标触发。自动获取屏幕尺寸，并采取对应的弹性布局。
*   **5.2 渲染引擎**
    *   **Markdown 全支持：** 多级列表、任务清单。
    *   **代码高亮：** 支持主流常用代码语言高亮，提供一键“复制”代码块功能。
*   **5.3 个性化视觉**
    *   **暗色模式：** 手动切换。

### 六、 数据安全与系统工具 (Security & Utilities)
该模块保障用户资产的安全。

*   **6.1 隐私保护**
    *   **API 消耗统计：** 统计每个模型使用的 Token 数量，预估消费金额。

## 第二部分

第二部分：AI生成UI设计规范的提示词，笔者根据自身需要进行修改

我想开发一个AI智能助手类app，现在需要输出高保真的原型图，请通过以下方式帮我完成所有界面的原型设计，并确保这些原型界面可以直接用于开发：

1、 用户体验分析：先分析这个app的主要功能和用户需求，确定核心交互逻辑

2、 产品界面规划：作为产品经理，定义关键界面，确保信息架构合理。

3、 高保真UI设计，作为UI设计师，设计贴近真实android设计规范的界面，使用现代化的UI元素，使其具有良好的视觉体验

4、 HTML原型实现：使用HTML+Tailwind CSS(或 Bootstrap)生成所有原型界面，并使用FontAwesome（或其他开源UI组件）让界面更加精美，接近真实App设计，拆分代码文件，保持结构清晰

5、 真实感增强：界面圆角化，使其贴近真是手机界面，使用真实的UI图片，而非占位符图片（可从Unsplash、pexels、Apple官方资源中选择）。

UI 设计规范词（Design Tokens & Specification）旨在为 **Cogno** 打造一套既有 **Claude 式温润感**，又不失**生产力工具专业度**的视觉体系：

---

### 1. 基础全局规范 (Foundational Tokens)

#### 1.1 色彩系统 (Color Palette)
*   **品牌主色 (Brand Primary):**
    *   `Color-Primary`: `#E66A3C` (浅色模式) / `#F28B62` (深色模式)
    *   `Color-Primary-Hover`: `#D55F35` / `#F49C7A`
    *   `Color-Primary-Alpha-10`: `rgba(230, 106, 60, 0.1)` (用于选中背景/高亮)
*   **中性色 (Neutral/Text):**
    *   `Text-Primary`: `#2C2A27` (深灰，避免纯黑以减少视觉疲劳)
    *   `Text-Secondary`: `#9B9188` (辅助/时间戳)
    *   `Text-Inverted`: `#FFFFFF`
*   **背景与表面 (Background & Surface):**
    *   `Bg-Main`: `#FEFAF5` (奶油白) / `#141210` (深暖黑)
    *   `Surface-Card`: `#FFFFFF` / `#1E1B18`
    *   `Surface-Sidebar`: `#FFFFFF` / `#1E1B18`
    *   `Border-Subtle`: `#EFE9E4` / `#2F2A26`

#### 1.2 字体与排版 (Typography)
*   **正文字体 (Body):** `Inter`, `-apple-system`, `PingFang SC` (无衬线，追求极简清晰)
*   **代码字体 (Monospace):** `Fira Code`, `JetBrains Mono` (支持连字，提升可读性)
*   **层级标准:**
    *   `Display-L`: 24px / Bold (会话标题)
    *   `Body-M`: 16px / Regular (聊天正文，行高 1.6)
    *   `Note-Body`: 15px / Regular (笔记内容，行高 1.7)
    *   `Caption-S`: 12px / Medium (时间戳、Token 消耗统计)

#### 1.3 形状与间距 (Layout & Radius)
*   **圆角 (Radius):**
    *   `Radius-S`: 4px (小图标背景)
    *   `Radius-M`: 12px (消息气泡、卡片)
    *   `Radius-L`: 20px (输入框、侧边栏)
*   **间距 (Spacing):** 采用 4px 步进系统 (4, 8, 16, 24, 32, 48)。

---

### 2. 会话管理模块规范 (Session UI)

*   **侧边栏 (Sidebar):**
    *   `Sidebar-Item-Height`: 44px
    *   `Group-Title`: 12px / Semibold / Uppercase (例如：TODAY, YESTERDAY)
    *   `Active-Indicator`: 左侧 3px 宽度的品牌色垂直条。
*   **消息气泡 (Chat Bubbles):**
    *   `User-Bubble`: 背景 `Color-Primary-Alpha-10`，右对齐，直角圆角混合（右下角为小圆角）。
    *   `AI-Bubble`: 纯白背景/深色卡片色，左对齐，带微弱阴影 `Shadow-Subtle`。
*   **流式交互 (Streaming Animation):**
    *   `Cursor-Blink`: 品牌色 2px 宽竖线，呼吸动画（Opacity 1.0 -> 0.2）。
*   **时间戳 (Timestamp):**
    *   `Tooltip-Style`: 长按浮窗显示，深色半透明背景 `rgba(0,0,0,0.75)`。

---

### 3. 核心能力与工具规范 (AI Core & Tools)

*   **模型切换器 (Model Switcher):**
    *   `Segmented-Control` 风格：胶囊形状切换，当前选中项带有亮色滑块。
*   **联网搜索状态 (Search UI):**
    *   `Search-Badge`: AI 回答顶部的状态条，显示“🔍 正在搜索：深度学习最新进展...”。
    *   `Source-Tag`: 引用来源小卡片，横向滚动显示，圆角 4px。
*   **语音交互 (Voice UI):**
    *   `Waveform-Animation`: 实时声波，波峰高度随音量变化，颜色渐变从 `Color-Primary` 到浅色。
    *   `Recording-Indicator`: 红色呼吸灯圆点。

---

### 4. 结构化笔记系统 (Cogno Notes - 核心特色)

*   **笔记列表 (Note Library):**
    *   `Entry-Point`: 采用独立 Tab 或侧边栏切换，卡片式布局，显示“标题+摘要(2行)+最后更新时间”。
*   **编辑器 (Markdown Editor):**
    *   `H1/H2/H3`: 品牌色左侧修饰线或加粗强调。
    *   `Note-Link-Icon`: 每一个 Markdown 标题旁隐藏/悬浮显示“🔗”图标，点击触发跳转会话动画。
*   **增量更新提示 (Incremental Toast):**
    *   当 AI 总结新内容时，新追加的文本区域背景色短暂高亮（淡黄色/淡橙色），持续 2 秒后消退。
*   **联动动画 (Linking Motion):**
    *   `Cross-Fade`: 从笔记跳转回会话时，使用平滑的左右推移或淡入淡出。

---

### 5. 文件与代码渲染 (Markdown & Files)

*   **代码块 (Code Block):**
    *   `Code-Header`: 深色背景栏，左侧显示语言名称（如 Python），右侧显示“Copy”按钮。
    *   `Syntax-Highlighting`: 使用 `Atom One Dark` 或 `Github` 风格调色盘。
*   **文件上传 (File Upload):**
    *   `File-Card`: 包含文件类型图标（PDF/MD/TXT）、文件名、大小、进度条。
    *   `Success-State`: 边框变为绿色 `Color-Success: #52C41A`。

---

### 6. 系统工具与响应式 (System & UX)

*   **API 统计 (Usage Stats):**
    *   `Progress-Bar-Linear`: 细长进度条显示已用额度，超过 80% 变为黄色，100% 变为红色。
*   **响应式断点 (Breakpoints):**
    *   `Mobile`: < 768px (隐藏侧边栏，启用侧滑手势)。
    *   `Desktop`: > 768px (侧边栏常驻/可收纳)。
*   **交互反馈 (Feedback):**
    *   `Haptic-Feedback`: 移动端在长按、点击发送、语音结束时提供轻微触觉反馈。
    *   `Skeleton-Screen`: 历史记录加载过程中的骨架屏（流光效果）。



随手记

看起来好高大上，已经不是我能触及的领域了。而且好快.......

出问题了，先找gemini，把功能要求写成UI设计规范，然后转换成具体的产品原型图，产品原型图出问题了，而且笔者不知道怎么用专业的术语来指导它进行修改。尝试中....

试试ds呢。