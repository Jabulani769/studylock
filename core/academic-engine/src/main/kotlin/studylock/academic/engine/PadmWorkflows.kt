package studylock.academic.engine

import studylock.domain.AcademicSource
import studylock.domain.CalendarException
import studylock.domain.DataStatus
import studylock.domain.Exam
import studylock.domain.LearningTopicMatch
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Progressive Academic Data Management workflows (PADM).
 *
 * Pure domain operations: no Android, Room or UI types. Every transition keeps
 * prior records intact so source history and completed facts remain immutable.
 */
object PadmWorkflows {

    private val RELIABLE = setOf(DataStatus.VERIFIED, DataStatus.RECEIVED)

    /** Late outline: compute remaining capacity and report infeasibility risk. */
    fun lateOutlineCapacity(request: LateOutlineRequest): LateOutlineReport {
        val planningStart = maxOf(request.today, request.outlineReceivedDate)
        val remaining = request.topics.filter { it.id !in request.coveredTopicIds }
        val capacity = CapacityCalculator.capacityReport(
            startDate = planningStart,
            endDate = request.horizonEnd,
            availability = request.availability,
            remainingTopics = remaining,
            calendarExceptions = request.calendarExceptions,
            exams = request.exams,
            config = request.config,
            zoneId = request.zoneId
        )
        val risks = mutableListOf<String>()
        if (planningStart.isAfter(request.outlineReceivedDate) || request.outlineReceivedDate.isAfter(request.today)) {
            risks.add("LATE_OUTLINE")
        }
        if (!capacity.feasible) risks.add("INSUFFICIENT_CAPACITY")
        if (remaining.isEmpty()) risks.add("NO_NEW_TOPICS: use revision/consolidation, not fabricated topics.")
        return LateOutlineReport(
            planningStartDate = planningStart,
            remainingSessions = capacity.availableSessions,
            requiredSessions = capacity.requiredSessions,
            shortfallSessions = capacity.shortfallSessions,
            feasible = capacity.feasible,
            riskCodes = risks.distinct()
        )
    }

    /**
     * Replace an estimated exam date with an official one. The prior estimate
     * becomes a SUPERSEDED source record; nothing is deleted or overwritten.
     */
    fun applyOfficialExamDate(
        exam: Exam,
        officialDateTime: LocalDateTime,
        sourceName: String,
        now: Long = System.currentTimeMillis()
    ): ExamDateUpdate {
        val hadPriorValue = exam.dateTime != null || exam.dateStatus == DataStatus.ESTIMATED
        val superseded = if (hadPriorValue) {
            listOf(
                AcademicSource(
                    entityType = "EXAM",
                    entityId = exam.id,
                    sourceName = exam.sourceDocument ?: "Prior estimate",
                    receivedDate = exam.dateTime?.atZone(ZoneId.of(exam.timezone))?.toInstant()?.toEpochMilli(),
                    status = DataStatus.SUPERSEDED,
                    notes = "Replaced by official date from $sourceName",
                    createdAt = now
                )
            )
        } else {
            emptyList()
        }
        val updated = exam.copy(
            dateTime = officialDateTime,
            dateStatus = DataStatus.VERIFIED,
            verificationStatus = DataStatus.VERIFIED,
            sourceDocument = sourceName
        )
        val newSource = AcademicSource(
            entityType = "EXAM",
            entityId = exam.id,
            sourceName = sourceName,
            receivedDate = now,
            effectiveDate = officialDateTime.atZone(ZoneId.of(exam.timezone)).toInstant().toEpochMilli(),
            status = DataStatus.VERIFIED,
            notes = "Official exam date",
            createdAt = now
        )
        return ExamDateUpdate(updated, superseded, newSource)
    }

    /** Detect two reliable-but-different dates for the same paper. */
    fun detectExamDateConflicts(exams: List<Exam>): List<ExamDateConflict> {
        return exams
            .filter { it.dateStatus in RELIABLE && it.dateTime != null }
            .groupBy { it.moduleId to it.examType.lowercase() }
            .mapValues { (_, group) -> group.distinctBy { it.dateTime } }
            .filterValues { it.size > 1 }
            .map { (key, group) ->
                ExamDateConflict(
                    moduleId = key.first,
                    examType = key.second,
                    examIds = group.map { it.id },
                    dates = group.map { it.dateTime!! }
                )
            }
    }

    /**
     * Resolve a conflict with an owner/student choice. Other dates are marked
     * SUPERSEDED with source records; no date is chosen silently by the app.
     */
    fun resolveExamDateConflict(
        exams: List<Exam>,
        conflict: ExamDateConflict,
        chosenExamId: UUID,
        chosenDate: LocalDateTime,
        sourceName: String,
        now: Long = System.currentTimeMillis()
    ): ExamConflictResolution {
        require(chosenExamId in conflict.examIds) { "Chosen exam is not part of the conflict" }
        val superseded = mutableListOf<AcademicSource>()
        val updated = exams.map { exam ->
            when (exam.id) {
                chosenExamId -> exam.copy(
                    dateTime = chosenDate,
                    dateStatus = DataStatus.VERIFIED,
                    verificationStatus = DataStatus.VERIFIED,
                    sourceDocument = sourceName
                )
                in conflict.examIds -> {
                    superseded.add(
                        AcademicSource(
                            entityType = "EXAM",
                            entityId = exam.id,
                            sourceName = exam.sourceDocument ?: "Conflicting date",
                            status = DataStatus.SUPERSEDED,
                            notes = "Resolved in favour of $chosenExamId",
                            createdAt = now
                        )
                    )
                    exam.copy(dateStatus = DataStatus.SUPERSEDED, verificationStatus = DataStatus.SUPERSEDED)
                }
                else -> exam
            }
        }
        val newSource = AcademicSource(
            entityType = "EXAM",
            entityId = chosenExamId,
            sourceName = sourceName,
            receivedDate = now,
            status = DataStatus.VERIFIED,
            notes = "Conflict resolution choice",
            createdAt = now
        )
        return ExamConflictResolution(updated, superseded, newSource)
    }

    /** Structure known but no reliable date → generic prep, never a countdown. */
    fun examPreparationMode(
        exam: Exam?,
        hasKnownStructure: Boolean,
        today: LocalDate,
        config: SchedulerConfig = SchedulerConfig()
    ): ExamReadiness {
        if (exam == null) {
            return ExamReadiness(ExamPrepMode.NO_EXAM_DATA, "EXAM_DATA_NOT_PROVIDED")
        }
        if (exam.dateStatus in RELIABLE && exam.dateTime != null) {
            val examDate = exam.dateTime!!.toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(today, examDate)
            return if (daysUntil in 0..config.examApproachWindowDays.toLong()) {
                ExamReadiness(ExamPrepMode.DATE_SPECIFIC, "EXAM_APPROACHING")
            } else {
                ExamReadiness(ExamPrepMode.GENERIC, "EXAM_DATE_CONFIRMED_NOT_DUE")
            }
        }
        return if (hasKnownStructure) {
            ExamReadiness(ExamPrepMode.GENERIC, "EXAM_STRUCTURE_KNOWN_DATE_UNKNOWN")
        } else {
            ExamReadiness(ExamPrepMode.GENERIC, "EXAM_DATE_ESTIMATED_NO_COUNTDOWN")
        }
    }

    /** Ambiguous prior-learning matches require explicit confirmation. */
    fun matchesRequiringConfirmation(
        matches: List<LearningTopicMatch>,
        threshold: Double = 0.85
    ): List<LearningTopicMatch> = matches.map {
        it.copy(requiresConfirmation = it.matchConfidence < threshold)
    }

    fun pendingConfirmations(matches: List<LearningTopicMatch>): List<LearningTopicMatch> =
        matches.filter { it.requiresConfirmation && !it.confirmed }
}

data class LateOutlineRequest(
    val outlineReceivedDate: LocalDate,
    val today: LocalDate,
    val horizonEnd: LocalDate,
    val topics: List<Topic>,
    val availability: List<StudyAvailability>,
    val coveredTopicIds: Set<UUID> = emptySet(),
    val calendarExceptions: List<CalendarException> = emptyList(),
    val exams: List<Exam> = emptyList(),
    val config: SchedulerConfig = SchedulerConfig(),
    val zoneId: ZoneId = ZoneId.of("UTC")
)

data class LateOutlineReport(
    val planningStartDate: LocalDate,
    val remainingSessions: Int,
    val requiredSessions: Int,
    val shortfallSessions: Int,
    val feasible: Boolean,
    val riskCodes: List<String>
)

data class ExamDateUpdate(
    val updatedExam: Exam,
    val supersededSources: List<AcademicSource>,
    val newSource: AcademicSource
)

data class ExamDateConflict(
    val moduleId: UUID,
    val examType: String,
    val examIds: List<UUID>,
    val dates: List<LocalDateTime>
)

data class ExamConflictResolution(
    val exams: List<Exam>,
    val supersededSources: List<AcademicSource>,
    val newSource: AcademicSource
)

enum class ExamPrepMode { DATE_SPECIFIC, GENERIC, NO_EXAM_DATA }

data class ExamReadiness(val mode: ExamPrepMode, val reasonCode: String)
