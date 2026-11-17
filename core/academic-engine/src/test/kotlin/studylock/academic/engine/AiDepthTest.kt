package studylock.academic.engine

import org.junit.Test
import studylock.common.clock.FixedClock
import studylock.domain.CalendarException
import studylock.domain.DataStatus
import studylock.domain.Exam
import studylock.domain.LearningEvent
import studylock.domain.Module
import studylock.domain.Semester
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AiDepthTest {

    private val zone = ZoneId.of("UTC")
    private val clock = FixedClock(Instant.parse("2026-01-15T10:00:00Z"), zone)

    private fun semester() = Semester(academicYear = "2026", label = "S1", timezone = "UTC")

    private fun module(name: String = "CS") =
        Module(semesterId = UUID.randomUUID(), code = name, name = name)

    private fun topic(m: Module, seq: Int, difficulty: Int = 5, duration: Int = 60) = Topic(
        moduleId = m.id, title = "T$seq", lecturerSequence = seq,
        difficultyEstimate = difficulty, durationEstimateMinutes = duration
    )

    private fun availability() = (1..5).map {
        StudyAvailability(semesterId = UUID.randomUUID(), dayOfWeek = it, available = true)
    }

    private fun exam(moduleId: UUID, date: LocalDate, status: DataStatus = DataStatus.VERIFIED, type: String = "MIDSEM") =
        Exam(
            moduleId = moduleId, examType = type,
            dateTime = date.atTime(10, 0), dateStatus = status, verificationStatus = status
        )

    private fun request(
        s: Semester, modules: List<Module>, topics: List<Topic>,
        start: LocalDate, end: LocalDate,
        exams: List<Exam> = emptyList(),
        events: List<LearningEvent> = emptyList(),
        exceptions: List<CalendarException> = emptyList(),
        missed: List<LocalDate> = emptyList(),
        config: SchedulerConfig = SchedulerConfig()
    ) = ScheduleRequest(
        semester = s, modules = modules, topics = topics, availability = availability(),
        calendarExceptions = exceptions, clock = clock, startDate = start, endDate = end,
        learningEvents = events, exams = exams, missedAssignmentDates = missed, config = config
    )

    @Test
    fun `pre-midsem target is ceil of half and selects only first-half topics`() {
        val s = semester()
        val m = module()
        val topics = (1..5).map { topic(m, it) } // ceil(5/2) = 3 in first half
        val exams = listOf(exam(m.id, LocalDate.of(2026, 3, 15)))
        val result = DefaultAcademicScheduler().generateSchedule(
            request(s, listOf(m), topics, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), exams)
        )
        val firstHalf = topics.take(3).map { it.id }
        val chosen = result.assignments.single().topicId
        assertTrue(chosen in firstHalf)
        assertTrue("MIDSEM_COVERAGE_GAP" in result.assignments.single().reasonCodes)
    }

    @Test
    fun `module balancing prevents systematic neglect`() {
        val s = semester()
        val a = module("A")
        val b = module("B")
        val topics = listOf(topic(a, 1), topic(a, 2), topic(b, 1), topic(b, 2))
        val exams = listOf(exam(a.id, LocalDate.of(2026, 3, 15)), exam(b.id, LocalDate.of(2026, 3, 15)))
        val result = DefaultAcademicScheduler().generateSchedule(
            request(s, listOf(a, b), topics, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 16), exams)
        )
        val counts = result.assignments.groupingBy { it.moduleId }.eachCount()
        assertEquals(2, counts.size)
        assertTrue((counts.values.maxOrNull()!! - counts.values.minOrNull()!!) <= 1)
        assertTrue(result.reasonCodes.contains("MODULE_BEHIND"))
    }

    @Test
    fun `verified exam date near the paper boosts that module`() {
        val s = semester()
        val a = module("A")
        val b = module("B")
        val topics = listOf(topic(a, 1), topic(b, 1))
        val exams = listOf(exam(a.id, LocalDate.of(2026, 1, 18)))
        val result = DefaultAcademicScheduler().generateSchedule(
            request(s, listOf(a, b), topics, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), exams)
        )
        assertEquals(a.id, result.assignments.single().moduleId)
        assertTrue("EXAM_APPROACHING" in result.assignments.single().reasonCodes)
    }

    @Test
    fun `estimated exam date never drives a countdown`() {
        val s = semester()
        val m = module()
        val topics = listOf(topic(m, 1))
        val exams = listOf(exam(m.id, LocalDate.of(2026, 1, 18), DataStatus.ESTIMATED))
        val result = DefaultAcademicScheduler().generateSchedule(
            request(s, listOf(m), topics, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), exams)
        )
        assertFalse(result.reasonCodes.contains("EXAM_APPROACHING"))
        assertTrue(result.phase == AcademicPhase.ROLLING)
        assertTrue(result.rationale.contains("rolling", ignoreCase = true))
    }

    @Test
    fun `endsem phase prioritises second-half coverage`() {
        val s = semester()
        val m = module()
        val topics = (1..4).map { topic(m, it) }
        val exams = listOf(
            exam(m.id, LocalDate.of(2026, 3, 1), type = "MIDSEM"),
            exam(m.id, LocalDate.of(2026, 5, 10), type = "ENDSEM")
        )
        val request = request(s, listOf(m), topics, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 1), exams)
            .copy(clock = FixedClock(Instant.parse("2026-05-01T10:00:00Z"), zone))
        val result = DefaultAcademicScheduler().generateSchedule(request)
        assertEquals(AcademicPhase.ENDSSEM_PREP, result.phase)
        assertTrue("SECOND_HALF_COVERAGE" in result.assignments.single().reasonCodes)
    }

    @Test
    fun `all targets covered early schedules revision without fabricating topics`() {
        val s = semester()
        val m = module()
        val topics = (1..4).map { topic(m, it) }
        val events = topics.map {
            LearningEvent(topicId = it.id, eventType = "STUDY", timestamp = Instant.parse("2026-01-14T10:00:00Z"))
        }
        val result = DefaultAcademicScheduler().generateSchedule(
            request(s, listOf(m), topics, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15), events = events)
        )
        val a = result.assignments.single()
        assertEquals("REVISE", a.activity)
        assertNotNull(a.topicId)
        assertTrue(a.topicId in topics.map { it.id })
        assertTrue("REPEAT_REVISION" in a.reasonCodes)
    }

    @Test
    fun `missed sessions are recorded but do not add extra sessions`() {
        val s = semester()
        val m = module()
        val topics = (1..5).map { topic(m, it) }
        val result = DefaultAcademicScheduler().generateSchedule(
            request(
                s, listOf(m), topics, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 16),
                missed = listOf(LocalDate.of(2026, 1, 12))
            )
        )
        assertEquals(5, result.assignments.size)
        assertTrue(result.rationale.contains("missed session(s) recorded"))
        assertTrue(result.rationale.contains("without adding extra sessions"))
    }

    @Test
    fun `exam days and calendar exceptions are excluded`() {
        val s = semester()
        val m = module()
        val topics = (1..3).map { topic(m, it) }
        val exception = CalendarException(
            semesterId = s.id, date = LocalDate.of(2026, 1, 13),
            reason = "Holiday", isUnavailable = true
        )
        val exams = listOf(exam(m.id, LocalDate.of(2026, 1, 15)))
        val result = DefaultAcademicScheduler().generateSchedule(
            request(
                s, listOf(m), topics, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 16),
                exams = exams, exceptions = listOf(exception)
            )
        )
        assertEquals(3, result.assignments.size)
        val dates = result.assignments.map { LocalDate.parse(it.date) }
        assertFalse(LocalDate.of(2026, 1, 13) in dates)
        assertFalse(LocalDate.of(2026, 1, 15) in dates)
    }

    @Test
    fun `capacity shortfall and risk are reported`() {
        val s = semester()
        val m = module()
        val topics = (1..10).map { topic(m, it, duration = 600) }
        val result = DefaultAcademicScheduler().generateSchedule(
            request(s, listOf(m), topics, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15))
        )
        assertFalse(result.capacity.feasible)
        assertTrue(result.capacity.shortfallSessions > 0)
        assertTrue(result.capacity.warnings.any { it.startsWith("INSUFFICIENT_CAPACITY") })
    }

    @Test
    fun `version and academic data version are preserved`() {
        val s = semester()
        val m = module()
        val result = DefaultAcademicScheduler().generateSchedule(
            request(
                s, listOf(m), listOf(topic(m, 1)),
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 15),
                config = SchedulerConfig(priorVersion = 4, academicDataVersion = "src-9")
            )
        )
        assertEquals(5, result.version)
        assertEquals("src-9", result.academicDataVersion)
        assertTrue(result.rationale.contains("src-9"))
    }

    @Test
    fun `identical inputs produce identical assignments`() {
        val s = semester()
        val m = module()
        val topics = (1..4).map { topic(m, it) }
        val r = request(s, listOf(m), topics, LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 16))
        val a = DefaultAcademicScheduler().generateSchedule(r)
        val b = DefaultAcademicScheduler().generateSchedule(r)
        assertEquals(
            a.assignments.map { Triple(it.date, it.topicId, it.reasonCodes) },
            b.assignments.map { Triple(it.date, it.topicId, it.reasonCodes) }
        )
    }
}
