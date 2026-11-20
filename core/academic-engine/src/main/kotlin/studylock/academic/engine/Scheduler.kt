package studylock.academic.engine

import studylock.common.clock.StudyClock
import studylock.domain.CalendarException
import studylock.domain.DataStatus
import studylock.domain.Exam
import studylock.domain.LearningEvent
import studylock.domain.Module
import studylock.domain.Semester
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

data class ScheduleRequest(
    val semester: Semester,
    val modules: List<Module>,
    val topics: List<Topic>,
    val availability: List<StudyAvailability>,
    val calendarExceptions: List<CalendarException>,
    val clock: StudyClock,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val learningEvents: List<LearningEvent> = emptyList(),
    val exams: List<Exam> = emptyList(),
    /** Previously generated/finished assignments. Used to avoid double workload. */
    val completedAssignments: List<ScheduledAssignment> = emptyList(),
    /** Dates that were missed; recorded but never turned into extra sessions. */
    val missedAssignmentDates: List<LocalDate> = emptyList(),
    val config: SchedulerConfig = SchedulerConfig()
)

data class ScheduledAssignment(
    val date: String,
    val moduleId: java.util.UUID,
    val topicId: java.util.UUID?,
    val activity: String,
    val durationMinutes: Int,
    val reasonCode: String,
    val explanation: String,
    /** Stable reason codes such as MIDSEM_COVERAGE_GAP or EXAM_APPROACHING. */
    val reasonCodes: List<String> = emptyList()
)

data class ScheduleResult(
    val assignments: List<ScheduledAssignment>,
    val version: Int,
    val generatedAt: Long,
    val rationale: String,
    val phase: AcademicPhase,
    val capacity: CapacityReport,
    val academicDataVersion: String,
    val reasonCodes: List<String>
)

interface AcademicScheduler {
    fun generateSchedule(request: ScheduleRequest): ScheduleResult
}

enum class AcademicPhase {
    /** First-half target in every module while dates are unconfirmed. */
    PRE_MIDSEM,
    /** Confirmed midsem papers prioritised by date; other modules retained. */
    MIDSEM_PERIOD,
    /** Remaining/second-half topics plus revision of earlier material. */
    POST_MIDSEM,
    /** Upcoming papers, weak topics, comprehensive revision and practice. */
    ENDSSEM_PREP,
    /** All priority coverage done: scheduled repeat/revision, never fabricated topics. */
    REPEAT_REVISION,
    /** No reliable exam dates: conservative rolling weekly mode. */
    ROLLING
}

data class TopicScore(
    val topicId: java.util.UUID,
    val score: Double,
    val reason: String,
    val codes: List<String>
)

class DefaultAcademicScheduler : AcademicScheduler {

    private val COVERED_EVENT_TYPES = setOf("STUDY", "STUDIED", "UNDERSTOOD", "MASTERED")
    private val WEAK_EVENT_TYPE = "ASSESSMENT"

    override fun generateSchedule(request: ScheduleRequest): ScheduleResult {
        val config = request.config
        val zoneId = ZoneId.of(request.semester.timezone)
        val clockDate = request.clock.instant.atZone(zoneId).toLocalDate()

        val topicById = request.topics.associateBy { it.id }
        val moduleById = request.modules.associateBy { it.id }

        val reliableExams = request.exams.filter { isReliable(it) }
        val slots = CapacityCalculator.studySlots(
            request.startDate, request.endDate, request.availability,
            request.calendarExceptions, request.exams, zoneId, config
        )

        // History: topics with explicit learning evidence or a completed assignment.
        val coveredTopicIds = request.learningEvents
            .filter { it.eventType in COVERED_EVENT_TYPES }
            .map { it.topicId }
            .toMutableSet()
        coveredTopicIds.addAll(request.completedAssignments.mapNotNull { it.topicId })

        val topicLastStudied = request.learningEvents
            .filter { it.eventType in COVERED_EVENT_TYPES }
            .groupBy { it.topicId }
            .mapValues { (_, events) ->
                events.maxByOrNull { it.timestamp }!!.timestamp.atZone(zoneId).toLocalDate()
            }
            .toMutableMap()
        request.completedAssignments.forEach { assignment ->
            val date = runCatching { LocalDate.parse(assignment.date) }.getOrNull() ?: return@forEach
            assignment.topicId?.let { topicLastStudied[it] = date }
        }

        val revisionBoost = request.learningEvents
            .filter { it.eventType == WEAK_EVENT_TYPE }
            .groupBy { it.topicId }
            .mapValues { (_, events) -> assessmentBoost(events.maxByOrNull { it.timestamp }!!.evidence) }

        // Ordered per-module syllabus with the ceil(T/2) midsem target.
        val orderedByModule = request.topics
            .filter { it.lifecycleStatus == "ACTIVE" }
            .groupBy { it.moduleId }
            .mapValues { (_, topics) ->
                topics.sortedWith(compareBy<Topic> { it.lecturerSequence }.thenBy { it.title })
            }
        val midsemTargetByModule = orderedByModule.mapValues { (_, topics) -> ceil(topics.size / 2.0).toInt() }
        val firstHalfIds = orderedByModule.flatMap { (moduleId, topics) ->
            topics.take(midsemTargetByModule[moduleId] ?: 0).map { it.id }
        }.toSet()

        val phase = determinePhase(clockDate, reliableExams, config, coveredTopicIds, firstHalfIds)
        val allPriorityCovered = when (phase) {
            AcademicPhase.ROLLING -> orderedByModule.values.flatten().all { it.id in coveredTopicIds }
            AcademicPhase.PRE_MIDSEM, AcademicPhase.MIDSEM_PERIOD ->
                firstHalfIds.all { it in coveredTopicIds }
            else -> orderedByModule.values.flatten().all { it.id in coveredTopicIds }
        }
        val effectivePhase = if (allPriorityCovered && phase != AcademicPhase.MIDSEM_PERIOD) {
            AcademicPhase.REPEAT_REVISION
        } else {
            phase
        }

        val remainingNewTopics = orderedByModule.values.flatten().filter { it.id !in coveredTopicIds }
        val capacity = CapacityCalculator.capacityReport(
            request.startDate, request.endDate, request.availability, remainingNewTopics,
            request.calendarExceptions, request.exams, config, zoneId
        )

        val assignments = mutableListOf<ScheduledAssignment>()
        val assignmentsPerModule = mutableMapOf<java.util.UUID, Int>()
        for (slot in slots) {
            val date = slot.date
            val eligible = eligibleNewTopics(
                effectivePhase, remainingNewTopics, firstHalfIds, coveredTopicIds
            )
            val score = if (eligible.isNotEmpty()) {
                eligible.map { topic ->
                    scoreTopic(
                        topic = topic,
                        lastStudied = topicLastStudied[topic.id],
                        clockDate = clockDate,
                        date = date,
                        revisionBoost = revisionBoost[topic.id] ?: 0.0,
                        phase = effectivePhase,
                        firstHalfIds = firstHalfIds,
                        exams = reliableExams,
                        zoneId = zoneId,
                        assignmentsPerModule = assignmentsPerModule,
                        config = config
                    )
                }.maxWith(compareBy<TopicScore> { it.score }
                    .thenByDescending { topicById[it.topicId]?.lecturerSequence ?: 0 }
                    .thenBy { it.topicId.toString() })
            } else {
                null
            }

            if (score != null) {
                val topic = topicById[score.topicId] ?: continue
                assignmentsPerModule[topic.moduleId] = (assignmentsPerModule[topic.moduleId] ?: 0) + 1
                val module = moduleById[topic.moduleId]
                val codes = score.codes + when {
                    effectivePhase == AcademicPhase.REPEAT_REVISION -> listOf("REPEAT_REVISION")
                    else -> emptyList()
                }
                assignments.add(
                    ScheduledAssignment(
                        date = date.toString(),
                        moduleId = topic.moduleId,
                        topicId = topic.id,
                        activity = "STUDY",
                        durationMinutes = calculateDuration(date, request.availability),
                        reasonCode = "${codes.joinToString("|")}|${score.reason}",
                        explanation = buildExplanation(topic, module, score.score, date, codes),
                        reasonCodes = codes
                    )
                )
                topicLastStudied[topic.id] = date
            } else if (topicById.isNotEmpty()) {
                // All targets already covered: schedule a deliberate revision of
                // weak or least-recently-seen material. Never fabricate topics.
                val revision = revisionCandidate(
                    orderedByModule.values.flatten(), topicLastStudied, revisionBoost
                )
                if (revision != null) {
                    val codes = listOfNotNull(
                        if ((revisionBoost[revision.id] ?: 0.0) > 0.0) "LOW_RECALL_SCORE" else null,
                        "REVISION_DUE", "REPEAT_REVISION"
                    )
                    assignments.add(
                        ScheduledAssignment(
                            date = date.toString(),
                            moduleId = revision.moduleId,
                            topicId = revision.id,
                            activity = "REVISE",
                            durationMinutes = calculateDuration(date, request.availability),
                            reasonCode = "${codes.joinToString("|")}|revision-of-covered-material",
                            explanation = "Consolidation of '${revision.title}' on $date. " +
                                "All priority topics are covered; no new topics are invented.",
                            reasonCodes = codes
                        )
                    )
                }
            }
        }

        val allCodes = assignments.flatMap { it.reasonCodes }.distinct().sorted()
        val missedNote = if (request.missedAssignmentDates.isEmpty()) {
            ""
        } else {
            " ${request.missedAssignmentDates.size} missed session(s) recorded; future work " +
                "redistributed without adding extra sessions or doubling workload."
        }
        val rationale = buildRationale(
            assignments, request.startDate, request.endDate, effectivePhase, capacity, config, missedNote
        )

        return ScheduleResult(
            assignments = assignments,
            version = config.priorVersion + 1,
            generatedAt = request.clock.millis,
            rationale = rationale,
            phase = effectivePhase,
            capacity = capacity,
            academicDataVersion = config.academicDataVersion,
            reasonCodes = allCodes
        )
    }

    private fun determinePhase(
        today: LocalDate,
        reliableExams: List<Exam>,
        config: SchedulerConfig,
        covered: Set<java.util.UUID>,
        firstHalfIds: Set<java.util.UUID>
    ): AcademicPhase {
        // "Stick to my routine": no exam-driven phasing, still automatic topic
        // choice and one continuous session per available day.
        if (config.routineMode) return AcademicPhase.ROLLING
        fun datesOf(typeToken: String) = reliableExams
            .filter { it.dateTime != null && it.examType.contains(typeToken, ignoreCase = true) }
            .mapNotNull { it.dateTime?.toLocalDate() }
            .sorted()

        val midsemDate = datesOf("MID").firstOrNull()
        val endsemDate = (datesOf("END") + datesOf("FINAL")).sorted().firstOrNull()
        if (midsemDate == null && endsemDate == null) return AcademicPhase.ROLLING

        val allFirstHalfCovered = firstHalfIds.isNotEmpty() && firstHalfIds.all { it in covered }
        if (midsemDate != null && !today.isAfter(midsemDate.plusDays(config.midsemApproachWindowDays.toLong()))) {
            return AcademicPhase.MIDSEM_PERIOD
        }
        if (endsemDate != null &&
            !today.isBefore(endsemDate.minusDays(config.examApproachWindowDays.toLong()))
        ) {
            return AcademicPhase.ENDSSEM_PREP
        }
        return if (midsemDate != null && today.isAfter(midsemDate)) {
            AcademicPhase.POST_MIDSEM
        } else {
            // Only an endsem date is known, or we are still before midsem.
            if (allFirstHalfCovered) AcademicPhase.POST_MIDSEM else AcademicPhase.PRE_MIDSEM
        }
    }

    private fun eligibleNewTopics(
        phase: AcademicPhase,
        remaining: List<Topic>,
        firstHalfIds: Set<java.util.UUID>,
        covered: Set<java.util.UUID>
    ): List<Topic> = when (phase) {
        // No confirmed midsem date: the ceil(T/2) target cannot define a hard
        // eligibility rule, so operate conservatively in rolling weekly mode.
        AcademicPhase.ROLLING -> remaining.filter { it.id !in covered }
        AcademicPhase.PRE_MIDSEM ->
            remaining.filter { it.id in firstHalfIds && it.id !in covered }
        AcademicPhase.MIDSEM_PERIOD, AcademicPhase.POST_MIDSEM, AcademicPhase.ENDSSEM_PREP ->
            remaining.filter { it.id !in covered }
        AcademicPhase.REPEAT_REVISION -> emptyList()
    }

    private fun scoreTopic(
        topic: Topic,
        lastStudied: LocalDate?,
        clockDate: LocalDate,
        date: LocalDate,
        revisionBoost: Double,
        phase: AcademicPhase,
        firstHalfIds: Set<java.util.UUID>,
        exams: List<Exam>,
        zoneId: ZoneId,
        assignmentsPerModule: Map<java.util.UUID, Int>,
        config: SchedulerConfig
    ): TopicScore {
        val w = config.weights
        var score = 0.0
        val reasons = mutableListOf<String>()
        val codes = mutableListOf<String>()

        val difficulty = topic.difficultyEstimate ?: config.defaultDifficulty
        score += difficulty * 2.0
        reasons.add("difficulty=$difficulty")

        val daysSince = if (lastStudied == null) 999.0
        else ChronoUnit.DAYS.between(lastStudied, clockDate).toDouble().coerceAtLeast(0.0)
        score += if (daysSince >= 30.0) 30.0 else daysSince
        reasons.add("daysSinceLastStudied=$daysSince")

        if (revisionBoost > 0.0) {
            codes.add("LOW_RECALL_SCORE")
        }
        score += revisionBoost
        reasons.add("revisionBoost=$revisionBoost")

        val duration = topic.durationEstimateMinutes ?: config.defaultDurationMinutes
        score += duration / 30.0
        reasons.add("durationEstimate=${topic.durationEstimateMinutes}")

        // Coverage urgency by phase (AIE-07/08).
        val inFirstHalf = topic.id in firstHalfIds
        val coverageApplicable = when (phase) {
            AcademicPhase.PRE_MIDSEM, AcademicPhase.MIDSEM_PERIOD -> inFirstHalf
            AcademicPhase.POST_MIDSEM, AcademicPhase.ENDSSEM_PREP -> !inFirstHalf
            AcademicPhase.ROLLING, AcademicPhase.REPEAT_REVISION -> false
        }
        if (coverageApplicable) {
            score += w.coverageUrgency
            codes.add(if (inFirstHalf) "MIDSEM_COVERAGE_GAP" else "SECOND_HALF_COVERAGE")
        }

        // Exam urgency only from reliable (VERIFIED/RECEIVED) dates (AIE-12, D-17).
        val examForModule = exams.filter { it.moduleId == topic.moduleId && it.dateTime != null }
            .minByOrNull { it.dateTime!! }
        if (examForModule != null) {
            val examDate = examForModule.dateTime!!.atZone(zoneId).toLocalDate()
            val daysUntil = ChronoUnit.DAYS.between(date, examDate)
            if (daysUntil in 0..config.examApproachWindowDays.toLong()) {
                val proximity = 1.0 - daysUntil.toDouble() / config.examApproachWindowDays
                score += w.examinationUrgency * proximity
                codes.add("EXAM_APPROACHING")
            }
        } else if (exams.any { it.moduleId == topic.moduleId }) {
            // Structure known but no reliable date: continue generic preparation.
            codes.add("EXAM_STRUCTURE_KNOWN_DATE_UNKNOWN")
        }

        // Module balancing: modules with fewer sessions so far get a workload-
        // weighted lift so none is systematically neglected (AIE-05).
        val maxAssigned = assignmentsPerModule.values.maxOrNull() ?: 0
        val thisAssigned = assignmentsPerModule[topic.moduleId] ?: 0
        val behind = maxAssigned - thisAssigned
        if (behind > 0) {
            score += behind * w.moduleNeglect
            codes.add("MODULE_BEHIND")
        }

        if (lastStudied != null && daysSince < config.revisionIntervalDays.toDouble()) {
            score += w.revisionDue
            codes.add("REVISION_DUE")
        }

        return TopicScore(topic.id, score, reasons.joinToString(", "), codes.distinct())
    }

    private fun revisionCandidate(
        topics: List<Topic>,
        lastStudied: Map<java.util.UUID, LocalDate>,
        revisionBoost: Map<java.util.UUID, Double>
    ): Topic? {
        if (topics.isEmpty()) return null
        return topics.maxWith(
            compareBy<Topic> { revisionBoost[it.id] ?: 0.0 }
                .thenBy { lastStudied[it.id] ?: LocalDate.MIN }
                .thenBy { it.title }
        )
    }

    private fun calculateDuration(date: LocalDate, availability: List<StudyAvailability>): Int {
        val avail = availability.find { it.dayOfWeek == date.dayOfWeek.value }
        return (avail?.commitmentMinutes ?: 120).coerceAtLeast(1)
    }

    private fun buildExplanation(
        topic: Topic,
        module: Module?,
        score: Double,
        date: LocalDate,
        codes: List<String>
    ): String {
        val moduleLabel = module?.code ?: module?.name ?: "Unknown module"
        return "Selected '${topic.title}' from $moduleLabel for $date " +
            "(AIE score: ${score.format("%.1f")}). " +
            "Reasons: ${codes.ifEmpty { listOf("GENERAL_COVERAGE") }.joinToString(", ")}. " +
            "Estimates used where missing are defaults, not official data."
    }

    private fun buildRationale(
        assignments: List<ScheduledAssignment>,
        startDate: LocalDate,
        endDate: LocalDate,
        phase: AcademicPhase,
        capacity: CapacityReport,
        config: SchedulerConfig,
        missedNote: String
    ): String {
        val windowNote = "Exam approach windows (${config.examApproachWindowDays}d) are " +
            "configurable hypotheses, not guaranteed rules (D-17)."
        val estimateNote = "Missing difficulty/time use clearly-marked defaults, not official data (D-13)."
        return buildString {
            append("Generated ${assignments.size} study assignment(s) for $startDate..$endDate. ")
            append("Phase: $phase. ")
            append("Capacity: ${capacity.availableSessions} session(s) available, ")
            append("${capacity.requiredSessions} estimated required, ")
            append("shortfall ${capacity.shortfallSessions}. ")
            append(if (capacity.feasible) "Feasible under current assumptions. " else "Not fully feasible; risk reported. ")
            append(capacity.warnings.joinToString(" "))
            append(if (capacity.warnings.isEmpty()) "" else " ")
            append(windowNote).append(' ').append(estimateNote).append(missedNote)
            if (config.academicDataVersion.isNotBlank()) {
                append(" Source-data version: ${config.academicDataVersion}.")
            }
        }
    }

    private fun isReliable(exam: Exam): Boolean =
        exam.dateStatus in setOf(DataStatus.VERIFIED, DataStatus.RECEIVED) && exam.dateTime != null

    private fun assessmentBoost(evidence: String?): Double {
        val parts = evidence?.split("/") ?: return 0.0
        if (parts.size != 2) return 0.0
        val correct = parts[0].trim().toDoubleOrNull() ?: return 0.0
        val total = parts[1].trim().toDoubleOrNull() ?: return 0.0
        if (total <= 0.0) return 0.0
        return (1.0 - (correct / total).coerceIn(0.0, 1.0)) * 10.0
    }

    private fun Double.format(fmt: String): String = java.lang.String.format(fmt, this)
}
