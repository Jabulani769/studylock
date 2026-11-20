package studylock.app.documents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocumentTypesTest {

    @Test
    fun detects_supported_formats() {
        assertEquals(DocumentType.PDF, DocumentTypes.fromMime("application/pdf", "notes.pdf"))
        assertEquals(DocumentType.IMAGE, DocumentTypes.fromMime("image/png", "photo.png"))
        assertEquals(DocumentType.TEXT, DocumentTypes.fromMime("text/plain", "outline.txt"))
        assertEquals(DocumentType.TEXT, DocumentTypes.fromMime(null, "syllabus.md"))
    }

    @Test
    fun rejects_unsupported_formats() {
        assertNull(DocumentTypes.fromMime("video/mp4", "clip.mp4"))
        assertNull(DocumentTypes.fromMime(null, "unknown.bin"))
    }

    @Test
    fun position_is_clamped_to_available_pages() {
        assertEquals(0, DocumentTypes.clampPosition(5, 1))
        assertEquals(3, DocumentTypes.clampPosition(9, 4))
        assertEquals(0, DocumentTypes.clampPosition(-3, 4))
        assertEquals(2, DocumentTypes.clampPosition(2, 8))
    }

    @Test
    fun file_extensions_are_stable() {
        assertEquals("pdf", DocumentType.PDF.extension)
        assertEquals("img", DocumentType.IMAGE.extension)
        assertEquals("txt", DocumentType.TEXT.extension)
    }
}
