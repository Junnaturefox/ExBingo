package me.jfenn.bingo.common.infobook

import me.jfenn.bingo.impl.TextFactoryImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookPaginatorTest {

    private val textFactory = TextFactoryImpl()

    @Test
    fun `short section fits on a single page`() {
        val section = listOf(
            textFactory.literal("title"),
            textFactory.literal(""),
            textFactory.literal("body"),
        )

        val pages = BookPaginator.paginate(listOf(section), textFactory)

        assertEquals(1, pages.size)
        assertEquals("title\n\nbody", pages[0].value.string)
    }

    @Test
    fun `long section is split so that no page exceeds the rendered line limit`() {
        val section = (1..40).map { textFactory.literal("line $it") }

        val pages = BookPaginator.paginate(listOf(section), textFactory)

        assertTrue(pages.size > 1, "expected the section to be split across pages")
        pages.forEach { page ->
            assertTrue(
                BookPaginator.estimateVisualLines(page.value.string) <= BookPaginator.PAGE_MAX_LINES,
                "page exceeds the ${BookPaginator.PAGE_MAX_LINES} line limit: ${page.value.string}",
            )
        }

        // no line may be dropped by the pagination
        assertEquals(
            section.joinToString("\n") { it.value.string },
            pages.joinToString("\n") { it.value.string },
        )
    }

    @Test
    fun `wrapped lines are counted when splitting`() {
        // 114px wide page / 9px per glyph = 12 glyphs per line, so every line here takes two rows
        val longLine = "一".repeat(20)
        val section = (1..10).map { textFactory.literal(longLine) }

        val pages = BookPaginator.paginate(listOf(section), textFactory)

        assertTrue(pages.size > 1, "expected wrapped lines to force an extra page")
        pages.forEach { page ->
            assertTrue(
                BookPaginator.estimateVisualLines(page.value.string) <= BookPaginator.PAGE_MAX_LINES,
                "page exceeds the ${BookPaginator.PAGE_MAX_LINES} line limit",
            )
        }
    }

    @Test
    fun `every section starts on a new page`() {
        val first = listOf(textFactory.literal("first"))
        val second = listOf(textFactory.literal("second"))

        val pages = BookPaginator.paginate(listOf(first, second), textFactory)

        assertEquals(listOf("first", "second"), pages.map { it.value.string })
    }

    @Test
    fun `blank spacing lines are trimmed at page edges`() {
        val section = listOf(
            textFactory.literal(""),
            textFactory.literal("content"),
            textFactory.literal(""),
            textFactory.literal(""),
        )

        val pages = BookPaginator.paginate(listOf(section), textFactory)

        assertEquals(listOf("content"), pages.map { it.value.string })
    }

    @Test
    fun `visual line estimate uses the book page width`() {
        // 114px / 9px = 12 full-width glyphs per line
        assertEquals(1, BookPaginator.estimateVisualLines("一".repeat(12)))
        assertEquals(2, BookPaginator.estimateVisualLines("一".repeat(13)))
        assertEquals(1, BookPaginator.estimateVisualLines("short"))
        assertEquals(2, BookPaginator.estimateVisualLines("a\nb"))
        assertEquals(1, BookPaginator.estimateVisualLines(""))
    }

    @Test
    fun `translatable lines are measured by their fallback text`() {
        // the book is built on the server, where the client's language is unavailable; the text
        // we can measure is the key's fallback, so it must not stringify to nothing
        val component = net.minecraft.network.chat.Component.translatableWithFallback(
            "bingo.test.pagination",
            "fallback used for measurement",
        )

        assertEquals("fallback used for measurement", component.string)

        val page = BookPaginator.paginate(
            listOf(listOf(textFactory.from(component))),
            textFactory,
        )
        assertEquals(1, page.size)
    }
}
