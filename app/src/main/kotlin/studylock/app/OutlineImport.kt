package studylock.app

/** Parses a pasted lecturer outline into topic titles, preserving order. */
internal fun parseOutline(text: String): List<String> =
    text.lineSequence()
        .map { line -> line.trim() }
        .map { line -> OUTLINE_PREFIX.replace(line, "").trim() }
        .filter { it.isNotEmpty() }
        .toList()

private val OUTLINE_PREFIX = Regex(
    """^(?:(?:\d+[.):-])|(?:[-•*])|(?:topic|unit|chapter)\s+\d+\s*[:.)-])\s*""",
    RegexOption.IGNORE_CASE
)
