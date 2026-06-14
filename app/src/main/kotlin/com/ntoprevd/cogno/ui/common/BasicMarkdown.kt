package com.ntoprevd.cogno.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
    textColor: Color = if (isDark) CognoDarkText else CognoText,
    onTaskToggle: ((lineIndex: Int, checked: Boolean) -> Unit)? = null
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

            MarkdownLine(
                line = line,
                lineIndex = index,
                isDark = isDark,
                textColor = textColor,
                onTaskToggle = onTaskToggle
            )
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
    val lineColor = if (isDark) CognoDarkLine else CognoLine

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = lineColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(if (isDark) CognoDarkSurface else CognoSurface)
    ) {
        val naturalWidth = (columnCount * 128).dp
        val tableWidth = if (naturalWidth < maxWidth) maxWidth else naturalWidth
        Column(modifier = Modifier.horizontalScroll(tableScroll)) {
            parsedRows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .width(tableWidth)
                        .height(IntrinsicSize.Min)
                        .background(
                            if (rowIndex == 0) lineColor.copy(alpha = 0.22f)
                            else Color.Transparent
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(columnCount) { columnIndex ->
                        if (columnIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(lineColor)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(horizontal = 11.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = parseInlineMarkdown(row.getOrNull(columnIndex).orEmpty()),
                                color = textColor.copy(alpha = if (rowIndex == 0) 0.96f else 0.88f),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                if (rowIndex < parsedRows.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(tableWidth)
                            .height(1.dp)
                            .background(lineColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownLine(
    line: String,
    lineIndex: Int,
    isDark: Boolean,
    textColor: Color,
    onTaskToggle: ((lineIndex: Int, checked: Boolean) -> Unit)?
) {
    val trimmed = line.trim()
    when {
        trimmed.isBlank() -> Spacer(modifier = Modifier.height(4.dp))
        trimmed.matches(Regex("""^[-*_]{3,}$""")) -> MarkdownDivider(isDark)
        trimmed.startsWith(">") -> MarkdownQuote(trimmed.removePrefix(">").trim(), isDark, textColor)
        trimmed.startsWith("#") -> MarkdownHeading(trimmed, textColor)
        trimmed.isListLine() -> {
            val item = trimmed.parseListItem()
            MarkdownListItem(
                marker = item.marker,
                text = item.text,
                textColor = textColor,
                onMarkerClick = item.checked?.let { checked ->
                    onTaskToggle?.let { callback -> { callback(lineIndex, !checked) } }
                }
            )
        }
        else -> MarkdownText(parseInlineMarkdown(line), textColor)
    }
}

@Composable
private fun MarkdownHeading(line: String, textColor: Color) {
    val markerCount = line.takeWhile { it == '#' }.length.coerceAtLeast(1)
    val displayLevel = markerCount.coerceIn(1, 3)
    val text = line.drop(markerCount).trim()
    Text(
        text = parseInlineMarkdown(text),
        color = textColor,
        fontSize = when (displayLevel) {
            1 -> 22.sp
            2 -> 19.sp
            else -> 17.sp
        },
        lineHeight = when (displayLevel) {
            1 -> 29.sp
            2 -> 26.sp
            else -> 24.sp
        },
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = if (displayLevel == 1) 10.dp else 6.dp)
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
private fun MarkdownListItem(
    marker: String,
    text: String,
    textColor: Color,
    onMarkerClick: (() -> Unit)? = null
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = marker,
            color = textColor.copy(alpha = 0.72f),
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier
                .width(if (marker.length > 2) 32.dp else 20.dp)
                .then(
                    if (onMarkerClick != null) Modifier.clickable(onClick = onMarkerClick)
                    else Modifier
                )
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
    val text: String,
    val checked: Boolean? = null
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
        return MarkdownListItemData(
            marker = if (checked) "☑" else "☐",
            text = task.groupValues[2],
            checked = checked
        )
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
    val source = trim().trim('|')
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var escaped = false
    var inInlineCode = false

    source.forEach { char ->
        when {
            escaped -> {
                current.append(char)
                escaped = false
            }
            char == '\\' -> escaped = true
            char == '`' -> {
                inInlineCode = !inInlineCode
                current.append(char)
            }
            char == '|' && !inInlineCode -> {
                cells += current.toString().trim()
                current.clear()
            }
            else -> current.append(char)
        }
    }
    if (escaped) current.append('\\')
    cells += current.toString().trim()
    return cells
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
            text.startsWith("~~", index) -> {
                val end = text.indexOf("~~", startIndex = index + 2)
                if (end > index + 2) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(text.substring(index + 2, end))
                    pop()
                    index = end + 2
                } else {
                    append(text[index])
                    index++
                }
            }
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
