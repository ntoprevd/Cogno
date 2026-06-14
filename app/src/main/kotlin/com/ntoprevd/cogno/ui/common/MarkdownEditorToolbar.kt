package com.ntoprevd.cogno.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ntoprevd.cogno.ui.theme.CognoDarkLine
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoLine
import com.ntoprevd.cogno.ui.theme.CognoSurface
import com.ntoprevd.cogno.ui.theme.CognoText

@Composable
fun MarkdownEditorToolbar(
    value: TextFieldValue,
    isDark: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    val actions = listOf(
        MarkdownToolbarAction(Icons.Default.FormatBold, "粗体", MarkdownFormat.BOLD),
        MarkdownToolbarAction(Icons.Default.FormatItalic, "斜体", MarkdownFormat.ITALIC),
        MarkdownToolbarAction(Icons.Default.StrikethroughS, "删除线", MarkdownFormat.STRIKETHROUGH),
        MarkdownToolbarAction(Icons.Default.Title, "二级标题", MarkdownFormat.HEADING),
        MarkdownToolbarAction(Icons.Default.FormatListBulleted, "无序列表", MarkdownFormat.BULLET_LIST),
        MarkdownToolbarAction(Icons.Default.FormatListNumbered, "有序列表", MarkdownFormat.NUMBERED_LIST),
        MarkdownToolbarAction(Icons.Default.Checklist, "任务列表", MarkdownFormat.TASK_LIST),
        MarkdownToolbarAction(Icons.Default.FormatQuote, "引用", MarkdownFormat.QUOTE),
        MarkdownToolbarAction(Icons.Default.Code, "行内代码", MarkdownFormat.INLINE_CODE),
        MarkdownToolbarAction(Icons.Default.Link, "链接", MarkdownFormat.LINK),
        MarkdownToolbarAction(Icons.Default.TableChart, "表格", MarkdownFormat.TABLE)
    )

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(if (isDark) CognoDarkSurface else CognoSurface)
            .border(
                width = 1.dp,
                color = if (isDark) CognoDarkLine else CognoLine
            )
            .padding(horizontal = 4.dp)
    ) {
        actions.forEach { action ->
            IconButton(
                onClick = { onValueChange(applyMarkdownFormat(value, action.format)) },
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.description,
                    tint = if (isDark) CognoDarkText else CognoText
                )
            }
        }
    }
}

private data class MarkdownToolbarAction(
    val icon: ImageVector,
    val description: String,
    val format: MarkdownFormat
)

internal enum class MarkdownFormat {
    BOLD,
    ITALIC,
    STRIKETHROUGH,
    HEADING,
    BULLET_LIST,
    NUMBERED_LIST,
    TASK_LIST,
    QUOTE,
    INLINE_CODE,
    LINK,
    TABLE
}

internal fun applyMarkdownFormat(
    value: TextFieldValue,
    format: MarkdownFormat
): TextFieldValue {
    return when (format) {
        MarkdownFormat.BOLD -> wrapSelection(value, "**", "**", "粗体")
        MarkdownFormat.ITALIC -> wrapSelection(value, "*", "*", "斜体")
        MarkdownFormat.STRIKETHROUGH -> wrapSelection(value, "~~", "~~", "删除线")
        MarkdownFormat.INLINE_CODE -> wrapSelection(value, "`", "`", "代码")
        MarkdownFormat.LINK -> insertLink(value)
        MarkdownFormat.HEADING -> prefixSelectedLines(value) { _, line -> "## $line" }
        MarkdownFormat.BULLET_LIST -> prefixSelectedLines(value) { _, line -> "- $line" }
        MarkdownFormat.NUMBERED_LIST -> prefixSelectedLines(value) { index, line -> "${index + 1}. $line" }
        MarkdownFormat.TASK_LIST -> prefixSelectedLines(value) { _, line -> "- [ ] $line" }
        MarkdownFormat.QUOTE -> prefixSelectedLines(value) { _, line -> "> $line" }
        MarkdownFormat.TABLE -> insertTemplate(
            value,
            "| 列 1 | 列 2 |\n| --- | --- |\n| 内容 | 内容 |"
        )
    }
}

internal fun continueMarkdownInput(
    previous: TextFieldValue,
    proposed: TextFieldValue
): TextFieldValue {
    if (!previous.selection.collapsed || !proposed.selection.collapsed) return proposed
    if (proposed.text.length != previous.text.length + 1) return proposed
    val cursor = proposed.selection.start
    if (cursor <= 0 || proposed.text[cursor - 1] != '\n') return proposed

    val previousLineEnd = cursor - 1
    val previousLineStart = if (previousLineEnd == 0) {
        0
    } else {
        proposed.text.lastIndexOf('\n', previousLineEnd - 1).let { if (it < 0) 0 else it + 1 }
    }
    val previousLine = proposed.text.substring(previousLineStart, previousLineEnd)
    val continuation = markdownContinuation(previousLine) ?: return proposed

    if (continuation.removeEmptyMarker) {
        val text = proposed.text.removeRange(previousLineStart, cursor)
        return proposed.copy(text = text, selection = TextRange(previousLineStart))
    }

    val text = proposed.text.replaceRange(cursor, cursor, continuation.prefix)
    val nextCursor = cursor + continuation.prefix.length
    return proposed.copy(text = text, selection = TextRange(nextCursor))
}

private data class MarkdownContinuation(
    val prefix: String,
    val removeEmptyMarker: Boolean = false
)

private fun markdownContinuation(line: String): MarkdownContinuation? {
    val indent = line.takeWhile(Char::isWhitespace)
    val content = line.removePrefix(indent)
    val rules = listOf(
        Regex("""^([-*+])\s+\[([ xX])]\s+(.*)$""") to { match: MatchResult ->
            MarkdownContinuation(
                "$indent${match.groupValues[1]} [ ] ",
                match.groupValues[3].isBlank()
            )
        },
        Regex("""^([-*+])\s+(.*)$""") to { match: MatchResult ->
            MarkdownContinuation("$indent${match.groupValues[1]} ", match.groupValues[2].isBlank())
        },
        Regex("""^(\d+)([.)])\s+(.*)$""") to { match: MatchResult ->
            val next = match.groupValues[1].toIntOrNull()?.plus(1) ?: 1
            MarkdownContinuation(
                "$indent$next${match.groupValues[2]} ",
                match.groupValues[3].isBlank()
            )
        },
        Regex("""^>\s?(.*)$""") to { match: MatchResult ->
            MarkdownContinuation("$indent> ", match.groupValues[1].isBlank())
        }
    )
    return rules.firstNotNullOfOrNull { (regex, transform) ->
        regex.matchEntire(content)?.let(transform)
    }
}

internal fun toggleTaskLine(content: String, lineIndex: Int, checked: Boolean): String {
    val lines = content.lines().toMutableList()
    if (lineIndex !in lines.indices) return content
    val replacement = if (checked) "[x]" else "[ ]"
    lines[lineIndex] = lines[lineIndex].replaceFirst(
        Regex("""^(\s*[-*+]\s+)\[[ xX]]"""),
        "$1$replacement"
    )
    return lines.joinToString("\n")
}

private fun wrapSelection(
    value: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val selected = value.text.substring(start, end).ifBlank { placeholder }
    val replacement = "$prefix$selected$suffix"
    val text = value.text.replaceRange(start, end, replacement)
    val selectionStart = start + prefix.length
    return value.copy(
        text = text,
        selection = TextRange(selectionStart, selectionStart + selected.length)
    )
}

private fun insertLink(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val label = value.text.substring(start, end).ifBlank { "链接文字" }
    val url = "https://"
    val replacement = "[$label]($url)"
    val text = value.text.replaceRange(start, end, replacement)
    val urlStart = start + label.length + 3
    return value.copy(
        text = text,
        selection = TextRange(urlStart, urlStart + url.length)
    )
}

private fun prefixSelectedLines(
    value: TextFieldValue,
    transform: (Int, String) -> String
): TextFieldValue {
    val selectionStart = value.selection.min
    val selectionEnd = value.selection.max
    val lineStart = if (selectionStart == 0) {
        0
    } else {
        value.text.lastIndexOf('\n', selectionStart - 1).let { if (it < 0) 0 else it + 1 }
    }
    val nextBreak = value.text.indexOf('\n', selectionEnd)
    val lineEnd = if (nextBreak < 0) value.text.length else nextBreak
    val source = value.text.substring(lineStart, lineEnd)
    val replacement = source.lines().mapIndexed(transform).joinToString("\n")
    val text = value.text.replaceRange(lineStart, lineEnd, replacement)
    return value.copy(
        text = text,
        selection = TextRange(lineStart, lineStart + replacement.length)
    )
}

private fun insertTemplate(value: TextFieldValue, template: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val needsLeadingBreak = start > 0 && value.text[start - 1] != '\n'
    val needsTrailingBreak = end < value.text.length && value.text[end] != '\n'
    val replacement = buildString {
        if (needsLeadingBreak) append('\n')
        append(template)
        if (needsTrailingBreak) append('\n')
    }
    val text = value.text.replaceRange(start, end, replacement)
    return value.copy(
        text = text,
        selection = TextRange(start, start + replacement.length)
    )
}
