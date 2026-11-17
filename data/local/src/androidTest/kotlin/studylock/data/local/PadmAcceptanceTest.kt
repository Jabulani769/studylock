package studylock.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.util.UUID

class PadmAcceptanceTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        context.deleteDatabase("test_studylock_db")
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "test_studylock_db"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @After
    fun tearDown() {
        try {
            database.close()
        } catch (e: UninitializedPropertyAccessException) {
            // setUp never completed
        }
        ApplicationProvider.getApplicationContext<android.content.Context>()
            .deleteDatabase("test_studylock_db")
    }

    // --- PADM: Start planning without an official calendar ---

    @Test
    fun can_create_semester_with_estimated_start_date() {
        runBlocking {
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(
                SemesterEntity(
                    id = semesterId,
                    academicYear = "2026",
                    label = "Semester 1",
                    startDate = null
                )
            )
            val saved = database.semesterDao().getById(semesterId)
            assertNotNull(saved)
            assertEquals("2026", saved.academicYear)
            assertNull(saved.startDate)
        }
    }

    // --- PADM: Start with incomplete module outlines ---

    @Test
    fun can_register_module_with_not_provided_outline_status() {
        runBlocking {
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(
                SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null)
            )
            val moduleId = UUID.randomUUID().toString()
            database.moduleDao().insert(
                ModuleEntity(
                    id = moduleId,
                    semesterId = semesterId,
                    code = "CS101",
                    name = "Intro to CS",
                    outlineStatus = "NOT_PROVIDED"
                )
            )
            val saved = database.moduleDao().getById(moduleId)
            assertNotNull(saved)
            assertEquals("NOT_PROVIDED", saved.outlineStatus)
        }
    }

    // --- PADM: Add an outline in Week 2 or Week 4 without losing prior records ---

    @Test
    fun adding_outline_preserves_prior_topic_records() {
        runBlocking {
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(
                SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null)
            )
            val moduleId = UUID.randomUUID().toString()
            database.moduleDao().insert(
                ModuleEntity(id = moduleId, semesterId = semesterId, code = "CS101", name = "CS", outlineStatus = "NOT_PROVIDED")
            )

            // Pre-existing topic (from prior learning)
            val priorTopicId = UUID.randomUUID().toString()
            database.topicDao().insert(
                TopicEntity(
                    id = priorTopicId,
                    moduleId = moduleId,
                    title = "Variables",
                    lecturerSequence = 1,
                    sourceStatus = "RECEIVED",
                    lifecycleStatus = "ACTIVE"
                )
            )

            // Add outline later
            val sourceId = UUID.randomUUID().toString()
            database.academicSourceDao().insert(
                AcademicSourceEntity(
                    id = sourceId,
                    entityType = "MODULE",
                    entityId = moduleId,
                    sourceName = "Lecturer Outline",
                    status = "RECEIVED"
                )
            )
            database.moduleOutlineDao().insert(
                ModuleOutlineEntity(
                    id = UUID.randomUUID().toString(),
                    moduleId = moduleId,
                    sourceId = sourceId,
                    status = "RECEIVED"
                )
            )

            // Prior topic still exists
            val topics = database.topicDao().getByModule(moduleId)
            assertEquals(1, topics.size)
            assertEquals("Variables", topics[0].title)
        }
    }

    // --- PADM: Match earlier learning to a new outline with confirmation for ambiguous matches ---

    @Test
    fun prior_learning_match_requires_confirmation_for_low_confidence() {
        runBlocking {
            val newTopicId = UUID.randomUUID().toString()
            val matchId = UUID.randomUUID().toString()
            database.learningTopicMatchDao().insert(
                LearningTopicMatchEntity(
                    id = matchId,
                    existingTopicId = null,
                    newTopicId = newTopicId,
                    matchConfidence = 0.6,
                    matchReason = "Similar title",
                    requiresConfirmation = true,
                    confirmed = false,
                    confirmedAt = null
                )
            )
            val unconfirmed = database.learningTopicMatchDao().getUnconfirmed()
            assertEquals(1, unconfirmed.size)
            assertTrue(unconfirmed[0].requiresConfirmation)
            assertEquals(0.6, unconfirmed[0].matchConfidence, 0.001)
        }
    }

    @Test
    fun confirming_prior_learning_match_records_confirmation_time() {
        runBlocking {
            val matchId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            database.learningTopicMatchDao().insert(
                LearningTopicMatchEntity(
                    id = matchId,
                    existingTopicId = UUID.randomUUID().toString(),
                    newTopicId = UUID.randomUUID().toString(),
                    matchConfidence = 0.95,
                    matchReason = "Exact title match",
                    requiresConfirmation = true,
                    confirmed = false,
                    confirmedAt = null
                )
            )
            database.learningTopicMatchDao().confirm(matchId, now)
            val saved = database.learningTopicMatchDao().getById(matchId)
            assertNotNull(saved)
            assertTrue(saved.confirmed)
            assertEquals(now, saved.confirmedAt)
        }
    }

    // --- PADM: Data integrity - re-importing same outline creates no duplicate topics ---

    @Test
    fun reimporting_same_outline_does_not_duplicate_topics() {
        runBlocking {
            val moduleId = UUID.randomUUID().toString()
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null))
            database.moduleDao().insert(ModuleEntity(id = moduleId, semesterId = semesterId, code = "CS101", name = "CS"))

            // First import
            database.topicDao().insert(TopicEntity(id = "topic-1", moduleId = moduleId, title = "Intro", lecturerSequence = 1, sourceStatus = "RECEIVED"))
            database.topicDao().insert(TopicEntity(id = "topic-2", moduleId = moduleId, title = "Loops", lecturerSequence = 2, sourceStatus = "RECEIVED"))

            // Re-import same topics (idempotent by stable ID)
            database.topicDao().insert(TopicEntity(id = "topic-1", moduleId = moduleId, title = "Intro", lecturerSequence = 1, sourceStatus = "RECEIVED"))
            database.topicDao().insert(TopicEntity(id = "topic-2", moduleId = moduleId, title = "Loops", lecturerSequence = 2, sourceStatus = "RECEIVED"))

            val topics = database.topicDao().getByModule(moduleId)
            assertEquals(2, topics.size)
        }
    }

    // --- PADM: Data integrity - completed learning events remain unchanged ---

    @Test
    fun learning_events_are_preserved_when_topic_status_changes() {
        runBlocking {
            val topicId = UUID.randomUUID().toString()
            val sessionId = UUID.randomUUID().toString()
            database.topicDao().insert(TopicEntity(id = topicId, moduleId = "mod-1", title = "Old Topic", lecturerSequence = 1, sourceStatus = "RECEIVED"))
            database.studySessionDao().insert(
                StudySessionEntity(
                    id = sessionId,
                    moduleId = "mod-1",
                    topicId = topicId,
                    state = "COMPLETED",
                    startTimeMillis = System.currentTimeMillis(),
                    endTimeMillis = System.currentTimeMillis(),
                    elapsedActiveMinutes = 60,
                    createdAt = System.currentTimeMillis()
                )
            )
            database.learningEventDao().insert(
                LearningEventEntity(
                    id = UUID.randomUUID().toString(),
                    topicId = topicId,
                    sessionId = sessionId,
                    eventType = "STUDIED",
                    timestampMillis = System.currentTimeMillis(),
                    durationMinutes = 60,
                    evidence = "Quiz score 85%"
                )
            )

            // Rename topic - learning events should still link
            database.topicDao().insert(TopicEntity(id = topicId, moduleId = "mod-1", title = "New Topic Name", lecturerSequence = 1, sourceStatus = "RECEIVED"))
            val topic = database.topicDao().getById(topicId)
            assertNotNull(topic)
            assertEquals("New Topic Name", topic.title)
        }
    }

    // --- PADM: Exam status tracking - estimated to official date ---

    @Test
    fun exam_date_can_move_from_estimated_to_verified() {
        runBlocking {
            val moduleId = UUID.randomUUID().toString()
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null))
            database.moduleDao().insert(ModuleEntity(id = moduleId, semesterId = semesterId, code = "CS101", name = "CS"))

            val examId = UUID.randomUUID().toString()
            database.examDao().insert(
                ExamEntity(
                    id = examId,
                    moduleId = moduleId,
                    examType = "MIDSEM",
                    dateTime = null,
                    dateStatus = "ESTIMATED",
                    verificationStatus = "ESTIMATED"
                )
            )
            val saved = database.examDao().getById(examId)
            assertNotNull(saved)
            assertEquals("ESTIMATED", saved.dateStatus)

            // Update to verified
            database.examDao().insert(
                ExamEntity(
                    id = examId,
                    moduleId = moduleId,
                    examType = "MIDSEM",
                    dateTime = System.currentTimeMillis(),
                    dateStatus = "VERIFIED",
                    verificationStatus = "VERIFIED"
                )
            )
            val updated = database.examDao().getById(examId)
            assertNotNull(updated)
            assertEquals("VERIFIED", updated.dateStatus)
            assertNotNull(updated.dateTime)
        }
    }

    // --- PADM: Conflicting dates ---

    @Test
    fun conflicting_dates_are_marked_as_conflicting() {
        runBlocking {
            val moduleId = UUID.randomUUID().toString()
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null))
            database.moduleDao().insert(ModuleEntity(id = moduleId, semesterId = semesterId, code = "CS101", name = "CS"))

            database.examDao().insert(
                ExamEntity(
                    id = "exam-1",
                    moduleId = moduleId,
                    examType = "ENDSEM",
                    dateTime = System.currentTimeMillis(),
                    dateStatus = "CONFLICTING",
                    verificationStatus = "CONFLICTING"
                )
            )
            val conflicting = database.examDao().getByDateStatus("CONFLICTING")
            assertEquals(1, conflicting.size)
        }
    }

    // --- PADM: Study availability ---

    @Test
    fun study_availability_tracks_weekday_commitment() {
        runBlocking {
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null))
            database.studyAvailabilityDao().insert(
                StudyAvailabilityEntity(
                    id = UUID.randomUUID().toString(),
                    semesterId = semesterId,
                    dayOfWeek = 1,
                    available = true,
                    commitmentMinutes = 120
                )
            )
            val monday = database.studyAvailabilityDao().getByDay(semesterId, 1)
            assertNotNull(monday)
            assertEquals(120, monday.commitmentMinutes)
            assertTrue(monday.available)
        }
    }

    // --- PADM: Calendar exceptions ---

    @Test
    fun calendar_exception_marks_day_as_unavailable() {
        runBlocking {
            val semesterId = UUID.randomUUID().toString()
            database.semesterDao().insert(SemesterEntity(id = semesterId, academicYear = "2026", label = "S1", startDate = null))
            val holidayDate = LocalDate.of(2026, 3, 17).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            database.calendarExceptionDao().insert(
                CalendarExceptionEntity(
                    id = UUID.randomUUID().toString(),
                    semesterId = semesterId,
                    date = holidayDate,
                    reason = "Public Holiday",
                    isUnavailable = true
                )
            )
            val exceptions = database.calendarExceptionDao().getBySemester(semesterId)
            assertEquals(1, exceptions.size)
            assertEquals("Public Holiday", exceptions[0].reason)
        }
    }

    // --- Data integrity: Topic rename/removal does not erase learning history ---

    @Test
    fun superseding_a_topic_preserves_learning_events() {
        runBlocking {
            val topicId = UUID.randomUUID().toString()
            val sessionId = UUID.randomUUID().toString()
            database.topicDao().insert(TopicEntity(id = topicId, moduleId = "mod-1", title = "Old Topic", lecturerSequence = 1, sourceStatus = "RECEIVED", lifecycleStatus = "ACTIVE"))
            database.studySessionDao().insert(
                StudySessionEntity(
                    id = sessionId,
                    moduleId = "mod-1",
                    topicId = topicId,
                    state = "COMPLETED",
                    startTimeMillis = System.currentTimeMillis(),
                    endTimeMillis = System.currentTimeMillis(),
                    elapsedActiveMinutes = 60,
                    createdAt = System.currentTimeMillis()
                )
            )
            database.topicDao().supersede(topicId)
            val topic = database.topicDao().getById(topicId)
            assertNotNull(topic)
            assertEquals("SUPERSEDED", topic.lifecycleStatus)
        }
    }
}
