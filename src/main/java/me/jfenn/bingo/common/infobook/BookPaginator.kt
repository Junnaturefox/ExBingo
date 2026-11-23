package me.jfenn.bingo.common.infobook

import me.jfenn.bingo.platform.text.IText
import me.jfenn.bingo.platform.text.ITextFactory

/**
 * Splits the tutorial text into written-book pages.
 *
 * A vanilla book page renders at most `128 / 9 = 14` wrapped lines
 * (`BookViewScreen` clamps the rendered line count to `Math.min(128 / 9, lines.size())`) and
 * silently drops everything after that. Sections longer than that used to be cut off, so each
 * section is now broken into as many pages as it needs; every section starts on a new page.
 */
internal object BookPaginator {

    /** Text width of a book page in GUI pixels (`BookViewScreen#TEXT_WIDTH`). */
    const val PAGE_WIDTH = 114

    /** The vanilla screen fits `128 / 9 = 14` lines; keep one line as a safety margin. */
    const val PAGE_MAX_LINES = 13

    private const val NARROW_CHAR_WIDTH = 6
    private const val WIDE_CHAR_WIDTH = 9

    fun paginate(sections: List<List<IText>>, textFactory: ITextFactory): List<IText> =
        sections.flatMap { paginateSection(it, textFactory) }

    private fun paginateSection(lines: List<IText>, textFactory: ITextFactory): List<IText> {
        val pages = mutableListOf<IText>()
        var page = mutableListOf<IText>()
        var usedLines = 0

        fun flush() {
            // blank lines used as spacing must not leave trailing empty rows on a page
            while (page.isNotEmpty() && page.last().value.string.isEmpty()) {
                page.removeAt(page.lastIndex)
            }

            if (page.isNotEmpty()) {
                val text = textFactory.empty()
                page.forEachIndexed { index, line ->
                    if (index > 0) text.append("\n")
                    text.append(line)
                }
                pages += text
            }

            page = mutableListOf()
            usedLines = 0
        }

        for (line in lines) {
            // don't start a page with blank spacing lines
            if (page.isEmpty() && line.value.string.isEmpty()) continue

            val lineCount = estimateVisualLines(line.value.string)
            if (usedLines + lineCount > PAGE_MAX_LINES && page.isNotEmpty()) flush()

            page += line
            usedLines += lineCount
        }

        flush()
        return pages
    }

    /**
     * Number of wrapped lines the vanilla book screen would need for [text] at [width].
     *
     * The client's real glyph widths are unavailable on a dedicated server, so every glyph is
     * measured as a full-width (CJK) character. That over-estimates narrow Latin text slightly but
     * stays correct for every language, which is what matters for not dropping content.
     */
    fun estimateVisualLines(text: String, width: Int = PAGE_WIDTH): Int {
        if (text.isEmpty()) return 1

        var total = 0
        for (paragraph in text.split('\n')) {
            total += estimateParagraphLines(paragraph, width)
        }
        return total
    }

    private fun estimateParagraphLines(paragraph: String, width: Int): Int {
        if (paragraph.isEmpty()) return 1

        var lines = 1
        var currentWidth = 0
        var index = 0

        while (index < paragraph.length) {
            val codePoint = Character.codePointAt(paragraph, index)
            index += Character.charCount(codePoint)

            val charWidth = if (codePoint == ' '.code) NARROW_CHAR_WIDTH else WIDE_CHAR_WIDTH
            if (currentWidth + charWidth > width) {
                lines++
                currentWidth = charWidth
            } else {
                currentWidth += charWidth
            }
        }

        return lines
    }
}
