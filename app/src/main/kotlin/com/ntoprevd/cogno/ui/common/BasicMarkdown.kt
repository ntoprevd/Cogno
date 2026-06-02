package com.ntoprevd.cogno.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntoprevd.cogno.ui.theme.CognoDarkLine
import com.ntoprevd.cogno.ui.theme.CognoDarkSurface
import com.ntoprevd.cogno.ui.theme.CognoDarkText
import com.ntoprevd.cogno.ui.theme.CognoLine
import com.ntoprevd.cogno.ui.theme.CognoMuted
import com.ntoprevd.cogno.ui.theme.CognoPrimary
import com.ntoprevd.cogno.ui.theme.CognoSurface
import com.ntoprevd.cogno.ui.theme.CognoText

@Composable
fun BasicMarkdown(
    content: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = if (isDark) CognoDarkText else CognoText
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val lines = content.lines()
        val codeBuffer = StringBuilder()
        var inCodeBlock = false
        var index = 0

        while (index < lines.size) {
            val rawLine = lines[index]
            val line = rawLine.trimEnd()
            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    MarkdownCodeBlock(codeBuffer.toString().trimEnd(), isDark)
                    codeBuffer.clear()
                }
                inCodeBlock = !inCodeBlock
                index++
                continue
            }

            if (inCodeBlock) {
                codeBuffer.appendLine(rawLine)
                index++
                continue
            }

            if (index + 1 < lines.size && isTableHeaderLine(line, lines[index + 1])) {
                val tableRows = mutableListOf(line)
                index += 2
                while (index < lines.size && lines[index].trim().contains("|")) {
                    tableRows.add(lines[index].trimEnd())
                    index++
                }
                MarkdownTable(tableRows, isDark, textColor)
                continue
            }

            MarkdownLine(line = line, isDark = isDark, textColor = textColor)
            index++
        }

        if (codeBuffer.isNotBlank()) {
            MarkdownCodeBlock(codeBuffer.toString().trimEnd(), isDark)
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<String>, isDark: Boolean, textColor: Color) {
    val parsedRows = rows.map { it.tableCells() }.filter { it.isNotEmpty() }
    if (parsedRows.isEmpty()) return
    val columnCount = parsedRows.maxOf { it.size }.coerceAtLeast(1)
    val tableScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(tableScroll)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isDark) CognoDarkLine else CognoLine,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isDark) CognoDarkSurface else CognoSurface)
    ) {
        parsedRows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier
                    .width((columnCount * 120).dp)
                    .background(
                        if (rowIndex == 0) {
                            (if (isDark) CognoDarkLine else CognoLine).copy(alpha = 0.28f)
                        } else {
                            Color.Transparent
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(columnCount) { columnIndex ->
                    Text(
                        text = parseInlineMarkdown(row.getOrNull(columnIndex).orEmpty()),
                        color = textColor.copy(alpha = if (rowIndex == 0) 0.96f else 0.88f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 0.5.dp,
                                color = if (isDark) CognoDarkLine else CognoLine
                            )
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(line: String, isDark: Boolean, textColor: Color) {
    val trimmed = line.trim()
    when {
        trimmed.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
        trimmed.matches(Regex("""^[-*_]{3,}$""")) -> MarkdownDivider(isDark)
        trimmed.startsWith(">") -> MarkdownQuote(trimmed.removePrefix(">").trim(), isDark, textColor)
        trimmed.startsWith("#") -> MarkdownHeading(trimmed, textColor)
        trimmed.isListLine() -> {
            val item = trimmed.parseListItem()
            MarkdownListItem(item.marker, item.text, textColor)
        }
        else -> MarkdownText(parseInlineMarkdown(line), textColor)
    }
}

@Composable
private fun MarkdownHeading(line: String, textColor: Color) {
    val level = line.takeWhile { it == '#' }.length.coerceIn(1, 3)
    val text = line.drop(level).trim()
    Text(
        text = parseInlineMarkdown(text),
        color = textColor,
        fontSize = when (level) {
            1 -> 22.sp
            2 -> 19.sp
            else -> 17.sp
        },
        lineHeight = when (level) {
            1 -> 29.sp
            2 -> 26.sp
            else -> 24.sp
        },
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = if (level == 1) 10.dp else 6.dp)
    )
}

@Composable
private fun MarkdownText(text: AnnotatedString, textColor: Color) {
    Text(
        text = text,
        color = textColor.copy(alpha = 0.92f),
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.sp
    )
}

@Composable
private fun MarkdownListItem(marker: String, text: String, textColor: Color) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = marker,
            color = textColor.copy(alpha = 0.72f),
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.width(if (marker.length > 2) 32.dp else 20.dp)
        )
        Text(
            text = parseInlineMarkdown(text),
            color = textColor.copy(alpha = 0.92f),
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarkdownQuote(text: String, isDark: Boolean, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(25.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) CognoDarkLine else CognoLine)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = parseInlineMarkdown(text),
            color = textColor.copy(alpha = 0.72f),
            fontSize = 15.sp,
            lineHeight = 24.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MarkdownDivider(isDark: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (isDark) CognoDarkLine else CognoLine)
    )
}

@Composable
private fun MarkdownCodeBlock(text: String, isDark: Boolean) {
    val codeScroll = rememberScrollState()
    Text(
        text = text,
        color = if (isDark) CognoDarkText else CognoText,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        style = TextStyle(fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) CognoDarkSurface else CognoSurface)
            .padding(14.dp)
            .horizontalScroll(codeScroll)
    )
}

private data class MarkdownListItemData(
    val marker: String,
    val text: String
)

private fun String.isListLine(): Boolean {
    val trimmed = trimStart()
    return trimmed.startsWith("- ") ||
        trimmed.startsWith("* ") ||
        trimmed.startsWith("+ ") ||
        Regex("""^[-*+]\s+\[[ xX]\]\s+.*""").matches(trimmed) ||
        Regex("""^\d+[.)]\s+.*""").matches(trimmed)
}

private fun String.parseListItem(): MarkdownListItemData {
    val trimmed = trimStart()
    val task = Regex("""^[-*+]\s+\[([ xX])]\s+(.*)""").matchEntire(trimmed)
    if (task != null) {
        val checked = task.groupValues[1].equals("x", ignoreCase = true)
        return MarkdownListItemData(if (checked) "☑" else "☐", task.groupValues[2])
    }
    val ordered = Regex("""^(\d+[.)])\s+(.*)""").matchEntire(trimmed)
    if (ordered != null) {
        return MarkdownListItemData(ordered.groupValues[1], ordered.groupValues[2])
    }
    return when {
        trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") ->
            MarkdownListItemData("•", trimmed.drop(2).trimStart())
        else -> MarkdownListItemData("•", trimmed)
    }
}

private fun isTableHeaderLine(line: String, separator: String): Boolean {
    val first = line.trim()
    val second = separator.trim()
    if (!first.contains("|") || !second.contains("|")) return false
    return second.tableCells().all { cell ->
        cell.matches(Regex(""":?-{3,}:?"""))
    }
}

private fun String.tableCells(): List<String> {
    return trim()
        .trim('|')
        .split("|")
        .map { it.trim() }
}

private fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        appendStyledInline(text)
    }
}

// Handles the small inline subset needed by generated replies and notes.
private fun AnnotatedString.Builder.appendStyledInline(text: String) {
    var index = 0
    while (index < text.length) {
        when {
            text.startsWith("**", index) -> {
                val end = text.indexOf("**", startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }
            text[index] == '*' -> {
                val end = text.indexOf('*', startIndex = index + 1)
                if (end > index + 1 && !text.startsWith("**", end)) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            text[index] == '[' -> {
                val labelEnd = text.indexOf("](", startIndex = index + 1)
                val urlEnd = if (labelEnd > index) text.indexOf(')', startIndex = labelEnd + 2) else -1
                if (labelEnd > index + 1 && urlEnd > labelEnd + 2) {
                    pushStyle(
                        SpanStyle(
                            color = CognoPrimary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                    append(text.substring(index + 1, labelEnd))
                    pop()
                    index = urlEnd + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            text[index] == '`' -> {
                val end = text.indexOf('`', startIndex = index + 1)
                if (end > index + 1) {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = CognoMuted.copy(alpha = 0.14f)
                        )
                    )
                    append(text.substring(index + 1, end))
                    pop()
                    index = end + 1
                } else {
                    append(text[index])
                    index++
                }
            }
            else -> {
                append(text[index])
                index++
            }
        }
    }
}
