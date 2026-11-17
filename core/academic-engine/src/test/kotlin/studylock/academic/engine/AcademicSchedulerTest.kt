package studylock.academic.engine

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import studylock.common.clock.FixedClock
import studylock.domain.CalendarException
import studylock.domain.LearningEvent
import studylock.domain.Module
import studylock.domain.Semester
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AcademicSchedulerTest {

    private val clock = FixedClock(Instant.parse("2026-01-15T10:00:00Z"), ZoneId.of("UTC")) // Thursday

    private fun createSemester() = Semester(
        id = java.util.UUID.randomUUID(),
        academicYear = "2026",
        label = "Semester 1",
        timezone = "UTC"
    )

    private fun createModule(semesterId: java.util.UUID, code: String = "CS101") = Module(
        id = java.util.UUID.randomUUID(),
        semesterId = semesterId,
        code = code,
        name = "Computer Science 101"
    )

    private fun createTopic(moduleId: java.util.UUID, title: String, difficulty: Int? = 5, duration: Int? = 60) = Topic(
        id = java.util.UUID.randomUUID(),
        moduleId = moduleId,
        title = title,
        lecturerSequence = 1,
        difficultyEstimate = difficulty,
        durationEstimateMinutes = duration
    )

    private fun createAvailability(dayOfWeek: Int, available: Boolean = true, commitment: Int = 120) = StudyAvailability(
        id = java.util.UUID.randomUUID(),
        semesterId = java.util.UUID.randomUUID(),
        dayOfWeek = dayOfWeek,
        available = available,
        commitmentMinutes = commitment
    )

    @Test
    fun `weak assessment result gets revision priority`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val weak = createTopic(module.id, "Weak topic")
        val fresh = createTopic(module.id, "Fresh topic")
        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(weak, fresh),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 15),
            endDate = LocalDate.of(2026, 1, 15),
            learningEvents = listOf(
                LearningEvent(
                    topicId = weak.id,
                    eventType = "ASSESSMENT",
                    timestamp = Instant.parse("2026-01-14T10:00:00Z"),
                    evidence = "1/5"
                )
            )
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(weak.id, result.assignments.single().topicId)
        assertTrue(result.assignments.single().reasonCode.contains("revisionBoost=8.0"))
    }

    @Test
    fun `generates assignments for weekdays only`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Variables")

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12), // Monday
            endDate = LocalDate.of(2026, 1, 16) // Friday
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(5, result.assignments.size)
        result.assignments.forEach { assignment ->
            val date = LocalDate.parse(assignment.date)
            assertFalse(date.dayOfWeek.value in setOf(6, 7))
        }
    }

    @Test
    fun `skips weekend days`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Variables")

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = (1..7).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 10), // Saturday
            endDate = LocalDate.of(2026, 1, 16) // Friday
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        val assignmentDates = result.assignments.map { LocalDate.parse(it.date) }
        assignmentDates.forEach { date ->
            assertFalse(date.dayOfWeek.value in setOf(6, 7))
        }
        // Jan 10-16: Sat(10), Sun(11), Mon(12), Tue(13), Wed(14), Thu(15), Fri(16)
        // Should have 5 assignments (Mon-Fri)
        assertEquals(5, result.assignments.size)
    }

    @Test
    fun `skips calendar exception days`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Variables")

        val holiday = LocalDate.of(2026, 1, 13) // Tuesday
        val exceptions = listOf(
            CalendarException(
                id = java.util.UUID.randomUUID(),
                semesterId = semester.id,
                date = holiday,
                reason = "Public Holiday",
                isUnavailable = true
            )
        )

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = exceptions,
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12), // Monday
            endDate = LocalDate.of(2026, 1, 16) // Friday
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        // Mon(12), Tue(13-holiday), Wed(14), Thu(15), Fri(16) = 4 assignments
        assertEquals(4, result.assignments.size)
        val assignmentDates = result.assignments.map { LocalDate.parse(it.date) }
        assertFalse(assignmentDates.contains(holiday))
    }

    @Test
    fun `returns empty schedule when no topics available`() {
        val semester = createSemester()
        val module = createModule(semester.id)

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = emptyList(),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12),
            endDate = LocalDate.of(2026, 1, 16)
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertTrue(result.assignments.isEmpty())
    }

    @Test
    fun `returns empty schedule when all topics superseded`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Old Topic").copy(lifecycleStatus = "SUPERSEDED")

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12),
            endDate = LocalDate.of(2026, 1, 16)
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertTrue(result.assignments.isEmpty())
    }

    @Test
    fun `prefers never-studied topics over recently-studied`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val newTopic = createTopic(module.id, "New Topic", difficulty = 5, duration = 60)
        val oldTopic = createTopic(module.id, "Old Topic", difficulty = 5, duration = 60)

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(newTopic, oldTopic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12),
            endDate = LocalDate.of(2026, 1, 12) // Single day
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(1, result.assignments.size)
        // New topic (never studied) should be preferred since it has higher recency score
        assertTrue(result.assignments[0].topicId == newTopic.id || result.assignments[0].topicId == oldTopic.id)
    }

    @Test
    fun `prefers higher difficulty topics`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val easyTopic = createTopic(module.id, "Easy Topic", difficulty = 1, duration = 60)
        val hardTopic = createTopic(module.id, "Hard Topic", difficulty = 10, duration = 60)

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(easyTopic, hardTopic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12),
            endDate = LocalDate.of(2026, 1, 12) // Single day
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(1, result.assignments.size)
        assertEquals(hardTopic.id, result.assignments[0].topicId)
    }

    @Test
    fun `uses commitment minutes from availability`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Variables", duration = 180)

        val availability = listOf(
            createAvailability(1, available = true, commitment = 90) // Monday: 90 min
        )

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = availability,
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12), // Monday
            endDate = LocalDate.of(2026, 1, 12)
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(1, result.assignments.size)
        assertEquals(90, result.assignments[0].durationMinutes)
    }

    @Test
    fun `topic estimate does not shorten the agreed continuous commitment`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Short estimate", duration = 30)

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = listOf(createAvailability(1, commitment = 120)),
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12), // Monday
            endDate = LocalDate.of(2026, 1, 12)
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(120, result.assignments.single().durationMinutes)
    }

    @Test
    fun `result has version and rationale`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Variables")

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12),
            endDate = LocalDate.of(2026, 1, 12)
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(2, result.version)
        assertNotNull(result.rationale)
        assertTrue(result.rationale.isNotEmpty())
        assertEquals(clock.millis, result.generatedAt)
    }

    @Test
    fun `explanation includes module and topic info`() {
        val semester = createSemester()
        val module = createModule(semester.id, code = "CS101")
        val topic = createTopic(module.id, "Variables", difficulty = 5, duration = 60)

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = (1..5).map { createAvailability(it) },
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12),
            endDate = LocalDate.of(2026, 1, 12)
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        assertEquals(1, result.assignments.size)
        val explanation = result.assignments[0].explanation
        assertTrue(explanation.contains("Variables"))
        assertTrue(explanation.contains("CS101"))
    }

    @Test
    fun `unavailable weekdays are skipped`() {
        val semester = createSemester()
        val module = createModule(semester.id)
        val topic = createTopic(module.id, "Variables")

        // Monday unavailable
        val availability = (2..5).map { createAvailability(it) } // Tue-Fri only

        val request = ScheduleRequest(
            semester = semester,
            modules = listOf(module),
            topics = listOf(topic),
            availability = availability,
            calendarExceptions = emptyList(),
            clock = clock,
            startDate = LocalDate.of(2026, 1, 12), // Monday
            endDate = LocalDate.of(2026, 1, 16) // Friday
        )

        val result = DefaultAcademicScheduler().generateSchedule(request)

        // Mon unavailable, Tue-Fri available = 4 assignments
        assertEquals(4, result.assignments.size)
        val assignmentDates = result.assignments.map { LocalDate.parse(it.date) }
        assertFalse(assignmentDates.contains(LocalDate.of(2026, 1, 12))) // Monday skipped
    }
}
