package com.ntoprevd.cogno.ui.common

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditorToolbarTest {
    @Test
    fun wrapsSelectedTextAndKeepsItSelected() {
        val result = applyMarkdownFormat(
            TextFieldValue("一段文字", selection = TextRange(2, 4)),
            MarkdownFormat.BOLD
        )

        assertEquals("一段**文字**", result.text)
        assertEquals(TextRange(4, 6), result.selection)
    }

    @Test
    fun insertsSelectablePlaceholderWithoutSelection() {
        val result = applyMarkdownFormat(
            TextFieldValue("内容", selection = TextRange(2)),
            MarkdownFormat.STRIKETHROUGH
        )

        assertEquals("内容~~删除线~~", result.text)
        assertEquals("删除线", result.text.substring(result.selection.min, result.selection.max))
    }

    @Test
    fun formatsEverySelectedLineAsNumberedList() {
        val result = applyMarkdownFormat(
            TextFieldValue("甲\n乙\n丙", selection = TextRange(0, 3)),
            MarkdownFormat.NUMBERED_LIST
        )

        assertEquals("1. 甲\n2. 乙\n丙", result.text)
    }

    @Test
    fun insertsTableOnItsOwnLines() {
        val result = applyMarkdownFormat(
            TextFieldValue("前文后文", selection = TextRange(2)),
            MarkdownFormat.TABLE
        )

        assertEquals(
            "前文\n| 列 1 | 列 2 |\n| --- | --- |\n| 内容 | 内容 |\n后文",
            result.text
        )
    }

    @Test
    fun enterContinuesBulletNumberTaskAndQuote() {
        assertEquals("- 项目\n- ", continued("- 项目"))
        assertEquals("3. 项目\n4. ", continued("3. 项目"))
        assertEquals("- [ ] 项目\n- [ ] ", continued("- [ ] 项目"))
        assertEquals("> 内容\n> ", continued("> 内容"))
    }

    @Test
    fun enterOnEmptyMarkerEndsTheList() {
        val previous = TextFieldValue("- ", selection = TextRange(2))
        val proposed = TextFieldValue("- \n", selection = TextRange(3))

        assertEquals("", continueMarkdownInput(previous, proposed).text)
    }

    @Test
    fun togglesTaskMarkerWithoutChangingText() {
        assertEquals(
            "前文\n- [x] 完成\n后文",
            toggleTaskLine("前文\n- [ ] 完成\n后文", 1, true)
        )
    }

    private fun continued(line: String): String {
        val previous = TextFieldValue(line, selection = TextRange(line.length))
        val proposed = TextFieldValue("$line\n", selection = TextRange(line.length + 1))
        return continueMarkdownInput(previous, proposed).text
    }
}
