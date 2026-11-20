package studylock.app.documents

/** Local study material only; no remote URLs or cloud references. */
enum class DocumentType {
    PDF,
    IMAGE,
    TEXT;

    val extension: String get() = when (this) {
        PDF -> "pdf"
        IMAGE -> "img"
        TEXT -> "txt"
    }
}

data class DocumentMeta(
    val id: String,
    val displayName: String,
    val type: DocumentType,
    val fileName: String,
    val lastPosition: Int = 0,
    /** UUID string of the topic this document belongs to, when imported from a topic. */
    val topicId: String? = null
)

object DocumentTypes {
    fun fromMime(mime: String?, displayName: String): DocumentType? {
        val value = mime?.lowercase().orEmpty()
        return when {
            value == "application/pdf" || displayName.endsWith(".pdf", true) -> DocumentType.PDF
            value.startsWith("image/") -> DocumentType.IMAGE
            value == "text/plain" || value.startsWith("text/") ||
                displayName.endsWith(".txt", true) ||
                displayName.endsWith(".md", true) -> DocumentType.TEXT
            else -> null
        }
    }

    /** Pages are 1-based; text uses a line/scroll position. */
    fun clampPosition(position: Int, pageCount: Int): Int =
        if (pageCount <= 1) 0 else position.coerceIn(0, pageCount - 1)
}
