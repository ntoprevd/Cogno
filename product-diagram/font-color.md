### 浅色模式（Light Theme）

| 用途                            | 颜色      | 备注                |
| :------------------------------ | :-------- | :------------------ |
| 主色调（App Bar、选中态、按钮） | `#E66A3C` | 暖橙色，Claude 风格 |
| 背景色                          | `#FEFAF5` | 奶油白              |
| 表面卡片/侧边栏                 | `#FFFFFF` | 纯白                |
| 用户消息气泡                    | `#F3E8E1` | 淡暖灰              |
| AI 消息气泡                     | `#FFFFFF` | 白                  |
| 主要文字                        | `#2C2A27` | 深灰                |
| 次要文字（时间戳、提示）        | `#9B9188` | 暖灰                |
| 分割线                          | `#EFE9E4` |                     |



| 层级          | 字体大小 (sp) | 行高 (sp) | 字重 (Weight)     | 使用场景                                                 |
| :------------ | :------------ | :-------- | :---------------- | :------------------------------------------------------- |
| **大标题**    | 24            | 30        | **Medium** (500)  | 会话侧边栏标题、设置页面的主要标题                       |
| **标题**      | 20            | 28        | **Medium** (500)  | 每个会话卡片或笔记卡片的标题，App主界面标题栏            |
| **正文**      | 16            | 24        | **Regular** (400) | 出现在输入框里的提问，AI的文本回复，笔记的正文内容       |
| **副文**      | 14            | 20        | **Regular** (400) | AI回复下方的时间戳，消息状态，侧边栏里每条会话的预览文字 |
| **辅助/注释** | 12            | 16        | **Regular** (400) | 设置页面的辅助说明，或某些极小的提示类文字               |

```javascript
<script>
        tailwind.config = {
            darkMode: 'class', // 必须有这一行
            theme: {
                extend: {
                    colors: {
                        cogno: {
                            primary: '#E66A3C',
                            bg: '#FEFAF5', // 聊天背景奶油白
                            surface: '#FFFFFF', // 标题栏纯白
                            userBubble: '#F3E8E1',
                            text: '#2C2A27',
                            muted: '#9B9188',
                            line: '#EFE9E4',

                            // 深色模式配色方案
                        darkPrimary: '#F28B62',
                        darkBg: '#141210',
                        darkSurface: '#1E1B18',
                        darkText: '#F0EBE6',
                        darkLine: '#2D2824',
                        darkUserBubble: '#2F2A26'
                        }
                    }
                }
            }
        }
    </script>
    <style>
        body { font-family: 'Inter', sans-serif; background-color: #f0f2f5; }
        
        .phone-shell {
            width: 390px; height: 844px;
            background: #FEFAF5; 
            border-radius: 50px;
            border: 9px solid #1a1a1a;
            position: relative;
            margin: 20px auto;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.4);
        }

        .no-scrollbar::-webkit-scrollbar { display: none; }
        .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

        .btn-tap:active { transform: scale(0.95); }
        .transition-soft { transition: all 0.2s ease; }

        /* 置顶图标旋转 */
        .pin-icon { transform: rotate(45deg); }
    </style>
```

