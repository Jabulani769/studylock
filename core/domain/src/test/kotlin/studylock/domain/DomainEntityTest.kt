package studylock.domain

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.time.LocalDate
import java.util.UUID

class DomainEntityTest {

    @Test
    fun semester_defaults() {
        val s = Semester(academicYear = "2026", label = "S1", startDate = null)
        assertNotNull(s.id)
        assertEquals("2026", s.academicYear)
        assertEquals("S1", s.label)
        assertNull(s.startDate)
        assertEquals("UTC", s.timezone)
    }

    @Test
    fun module_defaults() {
        val m = Module(semesterId = UUID.randomUUID(), code = "CS101", name = "Intro to CS")
        assertNotNull(m.id)
        assertEquals("CS101", m.code)
        assertEquals("Intro to CS", m.name)
        assertEquals("NOT_PROVIDED", m.outlineStatus)
        assertNull(m.workloadEstimateMinutes)
    }

    @Test
    fun topic_defaults() {
        val t = Topic(moduleId = UUID.randomUUID(), title = "Variables", lecturerSequence = 1)
        assertNotNull(t.id)
        assertEquals("Variables", t.title)
        assertEquals(1, t.lecturerSequence)
        assertEquals("NOT_PROVIDED", t.sourceStatus)
        assertEquals("ACTIVE", t.lifecycleStatus)
    }

    @Test
    fun data_status_enum_values() {
        assertEquals(6, DataStatus.values().size)
        assertTrue(DataStatus.NOT_PROVIDED == DataStatus.valueOf("NOT_PROVIDED"))
        assertTrue(DataStatus.ESTIMATED == DataStatus.valueOf("ESTIMATED"))
        assertTrue(DataStatus.RECEIVED == DataStatus.valueOf("RECEIVED"))
        assertTrue(DataStatus.VERIFIED == DataStatus.valueOf("VERIFIED"))
        assertTrue(DataStatus.SUPERSEDED == DataStatus.valueOf("SUPERSEDED"))
        assertTrue(DataStatus.CONFLICTING == DataStatus.valueOf("CONFLICTING"))
    }

    @Test
    fun academic_source_defaults() {
        val source = AcademicSource(
            entityType = "MODULE",
            entityId = UUID.randomUUID(),
            sourceName = "Lecturer Outline"
        )
        assertNotNull(source.id)
        assertEquals("MODULE", source.entityType)
        assertEquals("Lecturer Outline", source.sourceName)
        assertEquals(DataStatus.NOT_PROVIDED, source.status)
        assertNull(source.receivedDate)
        assertNull(source.effectiveDate)
    }

    @Test
    fun module_outline_defaults() {
        val outline = ModuleOutline(moduleId = UUID.randomUUID())
        assertNotNull(outline.id)
        assertEquals(DataStatus.NOT_PROVIDED, outline.status)
        assertTrue(outline.lecturerOrderPreserved)
        assertNull(outline.importedAt)
    }

    @Test
    fun study_availability_defaults() {
        val avail = StudyAvailability(
            semesterId = UUID.randomUUID(),
            dayOfWeek = 1
        )
        assertNotNull(avail.id)
        assertEquals(1, avail.dayOfWeek)
        assertTrue(avail.available)
        assertEquals(120, avail.commitmentMinutes)
        assertNull(avail.preferredStartTimeMinutes)
    }

    @Test
    fun calendar_exception_creation() {
        val date = LocalDate.of(2026, 3, 17)
        val exc = CalendarException(
            semesterId = UUID.randomUUID(),
            date = date,
            reason = "Public Holiday"
        )
        assertNotNull(exc.id)
        assertEquals("Public Holiday", exc.reason)
        assertTrue(exc.isUnavailable)
    }

    @Test
    fun topic_alias_defaults() {
        val alias = TopicAlias(
            topicId = UUID.randomUUID(),
            aliasTitle = "Variables Intro"
        )
        assertNotNull(alias.id)
        assertEquals("Variables Intro", alias.aliasTitle)
        assertEquals(0.0, alias.matchConfidence, 0.001)
        assertFalse(alias.confirmed)
    }

    @Test
    fun learning_topic_match_defaults() {
        val match = LearningTopicMatch(
            existingTopicId = UUID.randomUUID(),
            newTopicId = UUID.randomUUID(),
            matchConfidence = 0.85,
            matchReason = "Similar title and module"
        )
        assertNotNull(match.id)
        assertEquals(0.85, match.matchConfidence, 0.001)
        assertTrue(match.requiresConfirmation)
        assertFalse(match.confirmed)
        assertNull(match.confirmedAt)
    }

    @Test
    fun exam_defaults() {
        val exam = Exam(
            moduleId = UUID.randomUUID(),
            examType = "MIDSEM"
        )
        assertNotNull(exam.id)
        assertEquals("MIDSEM", exam.examType)
        assertEquals(DataStatus.NOT_PROVIDED, exam.dateStatus)
        assertNull(exam.dateTime)
        assertEquals(DataStatus.NOT_PROVIDED, exam.verificationStatus)
        assertEquals("UTC", exam.timezone)
    }

    @Test
    fun exam_section_defaults() {
        val section = ExamSection(
            examId = UUID.randomUUID(),
            questionsOffered = 5,
            questionsCompulsory = 2,
            questionsToAnswer = 3,
            marksPerQuestion = 20
        )
        assertNotNull(section.id)
        assertEquals(5, section.questionsOffered)
        assertEquals(2, section.questionsCompulsory)
        assertEquals(3, section.questionsToAnswer)
        assertEquals(20, section.marksPerQuestion)
    }

    @Test
    fun exam_question_rule_defaults() {
        val rule = ExamQuestionRule(
            sectionId = UUID.randomUUID(),
            topicCoverage = "Section A topics"
        )
        assertNotNull(rule.id)
        assertEquals("Section A topics", rule.topicCoverage)
        assertFalse(rule.isCompulsory)
    }

    @Test
    fun stable_ids_are_unique() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        assertTrue(id1 != id2)
    }

    @Test
    fun topic_lifecycle_statuses() {
        val active = Topic(
            moduleId = UUID.randomUUID(),
            title = "Topic",
            lecturerSequence = 1,
            lifecycleStatus = "ACTIVE"
        )
        assertEquals("ACTIVE", active.lifecycleStatus)

        val superseded = Topic(
            moduleId = UUID.randomUUID(),
            title = "Topic",
            lecturerSequence = 2,
            lifecycleStatus = "SUPERSEDED"
        )
        assertEquals("SUPERSEDED", superseded.lifecycleStatus)
    }
}
