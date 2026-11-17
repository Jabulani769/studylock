package studylock.data.local

import org.junit.Test
import kotlin.test.assertEquals

class DatabaseSchemaTest {

    @Test
    fun `migration 1 to 2 has correct versions`() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }
}
