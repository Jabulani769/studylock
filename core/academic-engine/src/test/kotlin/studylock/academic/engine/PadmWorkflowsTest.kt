package studylock.academic.engine

import org.junit.Test
import studylock.domain.DataStatus
import studylock.domain.Exam
import studylock.domain.LearningTopicMatch
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PadmWorkflowsTest {

    private fun topic(duration: Int) = Topic(
        moduleId = UUID.randomUUID(), title = "T", lecturerSequence = 1,
        durationEstimateMinutes = duration
    )

    private fun availability() = (1..5).map {
        StudyAvailability(semesterId = UUID.randomUUID(), dayOfWeek = it, available = true)
    }

    private fun exam(date: LocalDateTime, status: DataStatus) = Exam(
        moduleId = UUID.randomUUID(), examType = "MIDSEM",
        dateTime = date, dateStatus = status, verificationStatus = status
    )

    @Test
    fun `late outline reports infeasible risk with remaining capacity`() {
        val report = PadmWorkflows.lateOutlineCapacity(
            LateOutlineRequest(
                outlineReceivedDate = LocalDate.of(2026, 1, 20),
                today = LocalDate.of(2026, 1, 25),
                horizonEnd = LocalDate.of(2026, 2, 10),
                topics = (1..10).map { topic(600) },
                availability = availability()
            )
        )
        assertEquals(LocalDate.of(2026, 1, 25), report.planningStartDate)
        assertFalse(report.feasible)
        assertTrue(report.shortfallSessions > 0)
        assertTrue("LATE_OUTLINE" in report.riskCodes)
        assertTrue("INSUFFICIENT_CAPACITY" in report.riskCodes)
    }

    @Test
    fun `late outline feasible case has no capacity risk`() {
        val report = PadmWorkflows.lateOutlineCapacity(
            LateOutlineRequest(
                outlineReceivedDate = LocalDate.of(2026, 1, 20),
                today = LocalDate.of(2026, 1, 25),
                horizonEnd = LocalDate.of(2026, 2, 10),
                topics = listOf(topic(60), topic(60)),
                availability = availability()
            )
        )
        assertTrue(report.feasible)
        assertTrue("LATE_OUTLINE" in report.riskCodes)
        assertFalse("INSUFFICIENT_CAPACITY" in report.riskCodes)
    }

    @Test
    fun `official exam date supersedes estimate while preserving history`() {
        val estimate = exam(LocalDateTime.of(2026, 3, 1, 10, 0), DataStatus.ESTIMATED)
            .copy(sourceDocument = "draft timetable")
        val update = PadmWorkflows.applyOfficialExamDate(
            estimate, LocalDateTime.of(2026, 3, 10, 10, 0), "official calendar", now = 1000L
        )
        assertEquals(DataStatus.VERIFIED, update.updatedExam.dateStatus)
        assertEquals(DataStatus.VERIFIED, update.newSource.status)
        assertEquals(1, update.supersededSources.size)
        assertEquals(DataStatus.SUPERSEDED, update.supersededSources.single().status)
        // Original record is immutable.
        assertEquals(DataStatus.ESTIMATED, estimate.dateStatus)
    }

    @Test
    fun `conflicting dates are detected and resolved with an explicit choice`() {
        val moduleId = UUID.randomUUID()
        val first = exam(LocalDateTime.of(2026, 3, 1, 10, 0), DataStatus.VERIFIED).copy(moduleId = moduleId)
        val second = exam(LocalDateTime.of(2026, 3, 5, 10, 0), DataStatus.VERIFIED).copy(moduleId = moduleId)
        val exams = listOf(first, second)
        val conflicts = PadmWorkflows.detectExamDateConflicts(exams)
        assertEquals(1, conflicts.size)
        assertEquals(2, conflicts.single().examIds.size)

        val resolution = PadmWorkflows.resolveExamDateConflict(
            exams, conflicts.single(), second.id,
            LocalDateTime.of(2026, 3, 5, 10, 0), "owner confirmation", now = 2000L
        )
        val resolved = resolution.exams.associateBy { it.id }
        assertEquals(DataStatus.VERIFIED, resolved[second.id]!!.dateStatus)
        assertEquals(DataStatus.SUPERSEDED, resolved[first.id]!!.dateStatus)
        assertEquals(1, resolution.supersededSources.size)
        assertEquals(DataStatus.VERIFIED, resolution.newSource.status)
    }

    @Test
    fun `structure known but date unknown uses generic preparation only`() {
        val estimated = exam(LocalDateTime.of(2026, 3, 1, 10, 0), DataStatus.ESTIMATED)
        val readiness = PadmWorkflows.examPreparationMode(estimated, true, LocalDate.of(2026, 1, 15))
        assertEquals(ExamPrepMode.GENERIC, readiness.mode)
        assertEquals("EXAM_STRUCTURE_KNOWN_DATE_UNKNOWN", readiness.reasonCode)

        val none = PadmWorkflows.examPreparationMode(null, false, LocalDate.of(2026, 1, 15))
        assertEquals(ExamPrepMode.NO_EXAM_DATA, none.mode)
    }

    @Test
    fun `verified near exam switches to date specific priority`() {
        val verified = exam(LocalDateTime.of(2026, 1, 20, 10, 0), DataStatus.VERIFIED)
        val near = PadmWorkflows.examPreparationMode(verified, true, LocalDate.of(2026, 1, 15))
        assertEquals(ExamPrepMode.DATE_SPECIFIC, near.mode)
        assertEquals("EXAM_APPROACHING", near.reasonCode)

        val far = exam(LocalDateTime.of(2026, 4, 20, 10, 0), DataStatus.VERIFIED)
        val generic = PadmWorkflows.examPreparationMode(far, true, LocalDate.of(2026, 1, 15))
        assertEquals(ExamPrepMode.GENERIC, generic.mode)
    }

    @Test
    fun `ambiguous prior learning matches require confirmation`() {
        val low = LearningTopicMatch(
            existingTopicId = UUID.randomUUID(), newTopicId = UUID.randomUUID(),
            matchConfidence = 0.6, matchReason = "Similar title"
        )
        val high = LearningTopicMatch(
            existingTopicId = UUID.randomUUID(), newTopicId = UUID.randomUUID(),
            matchConfidence = 0.95, matchReason = "Exact title"
        )
        val decided = PadmWorkflows.matchesRequiringConfirmation(listOf(low, high), threshold = 0.85)
        assertTrue(decided[0].requiresConfirmation)
        assertFalse(decided[1].requiresConfirmation)
        assertEquals(1, PadmWorkflows.pendingConfirmations(decided).size)

        val confirmed = decided[0].copy(confirmed = true)
        assertTrue(PadmWorkflows.pendingConfirmations(listOf(confirmed, decided[1])).isEmpty())
    }
}
