package studylock.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import studylock.domain.LearningEvent

class AcademicFeatureTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: AcademicRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("academic_feature_test_db")
        database = Room.databaseBuilder(context, AppDatabase::class.java, "academic_feature_test_db").build()
        repository = AcademicRepositoryImpl(database)
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase("academic_feature_test_db")
    }

    @Test
    fun outline_import_preserves_order_and_skips_duplicates() = runBlocking {
        val semesterId = UUID.randomUUID()
        val moduleId = UUID.randomUUID()
        database.semesterDao().insert(
            SemesterEntity(
                id = semesterId.toString(),
                academicYear = "2026",
                label = "Semester 1",
                startDate = null
            )
        )
        database.moduleDao().insert(
            ModuleEntity(id = moduleId.toString(), semesterId = semesterId.toString(), code = "CS101", name = "CS")
        )

        val titles = listOf("1. Control Flow", "2) Loops", "Control flow")
        repository.importOutline(moduleId, parseTitlesForTest(titles))

        val topics = database.topicDao().getByModule(moduleId.toString())
        assertEquals(listOf("Control Flow", "Loops"), topics.map { it.title })
        assertEquals(listOf(1, 2), topics.map { it.lecturerSequence })
        assertEquals("RECEIVED", database.moduleDao().getById(moduleId.toString())?.outlineStatus)
        assertEquals(1, database.moduleOutlineDao().getByModule(moduleId.toString()).size)
    }

    @Test
    fun assessment_events_are_persisted_and_returned() = runBlocking {
        val semesterId = UUID.randomUUID()
        val moduleId = UUID.randomUUID()
        val topicId = UUID.randomUUID()
        database.semesterDao().insert(
            SemesterEntity(
                id = semesterId.toString(),
                academicYear = "2026",
                label = "Semester 1",
                startDate = null
            )
        )
        database.moduleDao().insert(
            ModuleEntity(id = moduleId.toString(), semesterId = semesterId.toString(), code = "CS101", name = "CS")
        )
        database.topicDao().insert(
            TopicEntity(id = topicId.toString(), moduleId = moduleId.toString(), title = "Control Flow", lecturerSequence = 1)
        )
        val event = LearningEvent(
            topicId = topicId,
            eventType = "ASSESSMENT",
            timestamp = Instant.ofEpochMilli(1_000L),
            evidence = "2/5"
        )

        repository.saveLearningEvent(event)

        val saved = repository.getLearningEvents().single()
        assertEquals(event.id, saved.id)
        assertEquals("2/5", saved.evidence)
    }

    // The app layer parses pasted text; this test uses already-parsed titles.
    private fun parseTitlesForTest(lines: List<String>) = lines.map { line ->
        line.replace(Regex("^\\d+[.):]\\s*"), "").trim()
    }
}
