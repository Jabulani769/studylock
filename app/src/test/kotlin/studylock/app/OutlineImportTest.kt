package studylock.app

import org.junit.Test
import kotlin.test.assertEquals

class OutlineImportTest {

    @Test
    fun `parses numbered and bulleted outlines in order`() {
        val text = """
            1. Control Flow
            2) Loops
            - Recursion
            • Arrays
        """.trimIndent()
        assertEquals(
            listOf("Control Flow", "Loops", "Recursion", "Arrays"),
            parseOutline(text)
        )
    }

    @Test
    fun `ignores blank lines and topic prefixes`() {
        val text = "Topic 1: Introduction\n\nUnit 2 - Data Types\n"
        assertEquals(listOf("Introduction", "Data Types"), parseOutline(text))
    }
}
