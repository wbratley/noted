package com.noted

import com.noted.data.ClaudeRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClaudeParserTest {
    private fun parse(raw: String) = ClaudeRepository.parseItems(raw)

    @Test
    fun plainJsonArray() {
        assertEquals(listOf("buy milk", "walk dog"), parse("""["buy milk", "walk dog"]"""))
    }

    @Test
    fun stripsJsonCodeFence() {
        assertEquals(listOf("item1"), parse("```json\n[\"item1\"]\n```"))
    }

    @Test
    fun stripsPlainCodeFence() {
        assertEquals(listOf("item1"), parse("```\n[\"item1\"]\n```"))
    }

    @Test
    fun ignoresLeadingProse() {
        assertEquals(listOf("a", "b"), parse("Here are the items:\n[\"a\", \"b\"]"))
    }

    @Test
    fun ignoresTrailingProse() {
        assertEquals(listOf("a"), parse("[\"a\"]\nLet me know if you need more."))
    }

    @Test
    fun filtersBlankEntries() {
        assertEquals(
            listOf("valid", "also valid"),
            parse("""["valid", "", "  ", "also valid"]"""),
        )
    }

    @Test
    fun singleItem() {
        assertEquals(listOf("just one"), parse("""["just one"]"""))
    }

    @Test
    fun noArrayThrows() {
        assertThrows(Exception::class.java) { parse("This has no array at all") }
    }
}
