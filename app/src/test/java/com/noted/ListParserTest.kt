package com.noted

import com.noted.data.ListParser
import com.noted.data.ParsedList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListParserTest {
    @Test
    fun titleLineBeforeBullets() {
        val parsed =
            ListParser.parse(
                "Butlins packing\n" +
                    "◦Tablet boxes\n" +
                    "◦Fan\n" +
                    "◦Dressings",
            )
        assertEquals(ParsedList("Butlins packing", listOf("Tablet boxes", "Fan", "Dressings")), parsed)
    }

    @Test
    fun bulletWithoutSpace() {
        assertEquals(listOf("Tablet boxes"), ListParser.parse("◦Tablet boxes")?.items)
    }

    @Test
    fun bulletWithSpace() {
        assertEquals(listOf("mikey calm me down box"), ListParser.parse("• mikey calm me down box")?.items)
    }

    @Test
    fun stripsInvisibleWordJoiner() {
        assertEquals(listOf("calming cube"), ListParser.parse("• \u2060calming cube")?.items)
    }

    @Test
    fun numberedItems() {
        assertEquals(listOf("a", "b"), ListParser.parse("1. a\n2) b")?.items)
    }

    @Test
    fun wrappedLineContinuesPreviousItem() {
        val parsed = ListParser.parse("◦ first item\ncontinued text\n◦ second")
        assertEquals(listOf("first item continued text", "second"), parsed?.items)
    }

    @Test
    fun singlePlainLineIsOneItem() {
        assertEquals(ParsedList(null, listOf("Butlins packing")), ListParser.parse("Butlins packing"))
    }

    @Test
    fun multiLineProseWithoutBulletsIsNotAList() {
        assertNull(ListParser.parse("Remember to pack the usual stuff.\nAnd don't forget the snacks."))
    }

    @Test
    fun emptyTextIsNotAList() {
        assertNull(ListParser.parse("   \n  \n"))
    }

    @Test
    fun emptyBulletLinesAreSkipped() {
        assertEquals(listOf("a", "b"), ListParser.parse("•\n• a\n\n• b")?.items)
    }

    @Test
    fun realWorldButlinsPaste() {
        // The exact paste this parser was built for: heading, ◦ bullets with no space,
        // • bullets with a space, and a hidden word-joiner (\u2060) before "calming cube".
        val lines =
            listOf(
                "Butlins packing",
                "◦Tablet boxes",
                "◦.git/Entistillar",
                "◦Fan",
                "◦Dressings",
                "◦Diabetes kit",
                "◦Blood sugar monitor",
                "◦Sharpie box",
                "◦Insulin (new pen)",
                "◦Ondansetron",
                "◦Calpol and syringe",
                "◦Oramorph and syringes",
                "◦Spare Dexcom sticker/reader",
                "◦Mobility scooter and battery charger",
                "◦Blue badge",
                "◦Inhalers",
                "◦Anti chafing shorts",
                "◦Head scarves",
                "◦Hairbrush",
                "◦Wayne nose spray",
                "◦iPad and charger",
                "◦Watch and charger",
                "◦Phone and charger",
                "◦Kindle and charger",
                "◦Palma and charger",
                "◦Phone and charger",
                "◦Steam deck and charger",
                "◦Puzzle book and pencil",
                "◦Sanitary towels",
                "◦Snack foods for steroid hunger?",
                "◦Sugar free drinks for room",
                "◦Mikey ear defenders",
                "◦Mikey headphones",
                "◦Mikey tablet and charger",
                "◦Bedtime books",
                "◦Bun bun and blanket",
                "◦Water bottle",
                "◦Wayne e reader and charger",
                "◦Wayne steam deck and charger",
                "◦Wayne watch and charger",
                "◦Wayne phone and charger",
                "◦Mikey Halloween dress up outfit",
                "◦Hoodies!",
                "◦Coat for Mikey",
                "◦Hats and gloves etc for Mikey",
                "◦Comfy shoes",
                "◦Warm blanket for scooter",
                "◦Video camera and charger/SD Card",
                "◦Mikey digital camera and SD card/Charger",
                "◦Laura underwear socks and bra",
                "◦Wayne underwear and socks",
                "◦Mikey underwear and socks",
                "◦Towels",
                "◦Mikey swimming stuff",
                "◦Wayne swimming stuff",
                "◦Mikey t shirts and trousers",
                "◦Laura t shirt and trousers",
                "◦Wayne T-shirts and trousers",
                "◦Jelly",
                "◦Laura pjs",
                "◦Mikey pjs",
                "◦Wayne pjs",
                "◦Toiletries",
                "◦Extension leads",
                "◦Headphones",
                "◦Bag for dirty laundry/wet clothes?",
                "◦Booking info",
                "• mikey calm me down box",
                "• \u2060calming cube",
            )

        val parsed = ListParser.parse(lines.joinToString("\n"))
        assertEquals("Butlins packing", parsed?.title)
        val items = parsed?.items
        assertEquals(68, items?.size)
        assertEquals("Tablet boxes", items?.first())
        assertEquals(".git/Entistillar", items?.get(1))
        assertEquals("Snack foods for steroid hunger?", items?.get(28))
        assertEquals("Jelly", items?.get(57))
        assertEquals("Booking info", items?.get(65))
        assertEquals("mikey calm me down box", items?.get(66))
        assertEquals("calming cube", items?.last())
    }
}
