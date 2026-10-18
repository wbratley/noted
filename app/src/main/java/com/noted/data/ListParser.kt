package com.noted.data

/**
 * Clipboard text that was already a list: the items, plus an optional title —
 * a heading line that appeared before the first bullet (e.g. "Butlins packing").
 */
data class ParsedList(val title: String?, val items: List<String>)

/**
 * Parses text that is already a checklist — bullet or numbered lines, or a single
 * plain line — into items locally, without an LLM. Returns null when the text is
 * not a list (e.g. prose), so callers can fall back to Claude.
 */
object ListParser {
    private val BULLET_CHARS: Set<Char> = "•◦▪‣●○◂►*+-–—·‒".toSet()
    private val NUMBERED_PREFIX = Regex("^\\d{1,2}[.)]")

    fun parse(text: String): ParsedList? {
        val lines = text.lineSequence()
            .map { clean(it).trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) return null

        // A single plain line is almost certainly one item, not a heading.
        if (lines.size == 1 && !isBulleted(lines[0])) return ParsedList(null, lines)

        val firstBullet = lines.indexOfFirst { isBulleted(it) }
        if (firstBullet == -1) return null

        val title = if (firstBullet > 0) lines.subList(0, firstBullet).joinToString(" ") else null

        val items = mutableListOf<String>()
        for (line in lines.subList(firstBullet, lines.size)) {
            if (isBulleted(line)) {
                val item = stripBullet(line)
                if (item.isNotEmpty()) items.add(item)
            } else if (items.isNotEmpty()) {
                // No bullet — a wrapped line continuing the previous item.
                items[items.lastIndex] += " $line"
            }
        }

        if (items.isEmpty()) return null
        return ParsedList(title, items)
    }

    private fun isBulleted(line: String): Boolean =
        line.firstOrNull() in BULLET_CHARS || NUMBERED_PREFIX.containsMatchIn(line)

    private fun stripBullet(line: String): String {
        var rest = line
        if (rest.firstOrNull() in BULLET_CHARS) rest = rest.substring(1)
        return NUMBERED_PREFIX.replace(rest, "").trim()
    }

    /** Strips invisible characters that often sneak in from copy & paste. */
    private fun clean(line: String): String =
        line.replace('\u2060', '').replace('\u200B', '').replace('\uFEFF', '')
}
