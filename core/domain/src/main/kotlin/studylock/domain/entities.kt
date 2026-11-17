package studylock.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Semester(
    val id: UUID = UUID.randomUUID(),
    val academicYear: String,
    val label: String,
    val startDate: LocalDate? = null,
    val timezone: String = "UTC",
    val createdAt: Long = System.currentTimeMillis()
)

data class Module(
    val id: UUID = UUID.randomUUID(),
    val semesterId: UUID,
    val code: String?,
    val name: String,
    val outlineStatus: String = "NOT_PROVIDED",
    val workloadEstimateMinutes: Int? = null,
    /** ACTIVE or ARCHIVED. Archived modules are hidden but history is kept. */
    val lifecycleStatus: String = "ACTIVE"
)

data class Topic(
    val id: UUID = UUID.randomUUID(),
    val moduleId: UUID,
    val title: String,
    val lecturerSequence: Int,
    val sourceStatus: String = "NOT_PROVIDED",
    val difficultyEstimate: Int? = null,
    val durationEstimateMinutes: Int? = null,
    val lifecycleStatus: String = "ACTIVE"
)

enum class DataStatus {
    NOT_PROVIDED, ESTIMATED, RECEIVED, VERIFIED, SUPERSEDED, CONFLICTING
}

data class AcademicSource(
    val id: UUID = UUID.randomUUID(),
    val entityType: String,
    val entityId: UUID,
    val sourceName: String? = null,
    val receivedDate: Long? = null,
    val effectiveDate: Long? = null,
    val enteredBy: String? = null,
    val status: DataStatus = DataStatus.NOT_PROVIDED,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ModuleOutline(
    val id: UUID = UUID.randomUUID(),
    val moduleId: UUID,
    val sourceId: UUID? = null,
    val status: DataStatus = DataStatus.NOT_PROVIDED,
    val lecturerOrderPreserved: Boolean = true,
    val importedAt: Long? = null,
    val sourceDocument: String? = null,
    val notes: String? = null
)

data class StudyAvailability(
    val id: UUID = UUID.randomUUID(),
    val semesterId: UUID,
    val dayOfWeek: Int,
    val available: Boolean = true,
    val preferredStartTimeMinutes: Int? = null,
    val commitmentMinutes: Int = 120
)

data class CalendarException(
    val id: UUID = UUID.randomUUID(),
    val semesterId: UUID,
    val date: LocalDate,
    val reason: String,
    val isUnavailable: Boolean = true
)

data class TopicAlias(
    val id: UUID = UUID.randomUUID(),
    val topicId: UUID,
    val aliasTitle: String,
    val matchConfidence: Double = 0.0,
    val confirmed: Boolean = false
)

data class LearningTopicMatch(
    val id: UUID = UUID.randomUUID(),
    val existingTopicId: UUID?,
    val newTopicId: UUID?,
    val matchConfidence: Double,
    val matchReason: String?,
    val requiresConfirmation: Boolean = true,
    val confirmed: Boolean = false,
    val confirmedAt: Long? = null
)

data class Exam(
    val id: UUID = UUID.randomUUID(),
    val moduleId: UUID,
    val examType: String,
    val dateTime: LocalDateTime? = null,
    val dateStatus: DataStatus = DataStatus.NOT_PROVIDED,
    val durationMinutes: Int? = null,
    val marksAvailable: Int? = null,
    val timezone: String = "UTC",
    val sourceDocument: String? = null,
    val verificationStatus: DataStatus = DataStatus.NOT_PROVIDED
)

data class ExamSection(
    val id: UUID = UUID.randomUUID(),
    val examId: UUID,
    val name: String? = null,
    val questionsOffered: Int,
    val questionsCompulsory: Int,
    val questionsToAnswer: Int,
    val marksPerQuestion: Int
)

data class ExamQuestionRule(
    val id: UUID = UUID.randomUUID(),
    val sectionId: UUID,
    val topicCoverage: String?,
    val isCompulsory: Boolean = false
)

/** An explicit assessment or study record. Never derived from timer time. */
data class LearningEvent(
    val id: UUID = UUID.randomUUID(),
    val topicId: UUID,
    val sessionId: UUID? = null,
    val eventType: String,
    val timestamp: java.time.Instant = java.time.Instant.now(),
    val durationMinutes: Int = 0,
    val evidence: String? = null
)
