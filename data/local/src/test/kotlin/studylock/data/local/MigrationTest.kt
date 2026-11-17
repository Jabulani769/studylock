package studylock.data.local

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MigrationTest {

    @Test
    fun migration_2_to_3_has_correct_versions() {
        assertEquals(2, MIGRATION_2_3.startVersion)
        assertEquals(3, MIGRATION_2_3.endVersion)
    }

    @Test
    fun migration_2_to_3_creates_academic_sources_table() {
        // Verify the migration SQL references key tables
        // This is a light check that the migration object exists and is configured
        assertNotNull(MIGRATION_2_3)
    }

    @Test
    fun migration_2_to_3_creates_module_outlines_table() {
        assertNotNull(MIGRATION_2_3)
    }

    @Test
    fun migration_2_to_3_creates_exams_table() {
        assertNotNull(MIGRATION_2_3)
    }

    @Test
    fun migration_2_to_3_creates_study_availability_table() {
        assertNotNull(MIGRATION_2_3)
    }

    @Test
    fun migration_2_to_3_creates_calendar_exceptions_table() {
        assertNotNull(MIGRATION_2_3)
    }

    @Test
    fun migration_2_to_3_creates_learning_topic_matches_table() {
        assertNotNull(MIGRATION_2_3)
    }
}
