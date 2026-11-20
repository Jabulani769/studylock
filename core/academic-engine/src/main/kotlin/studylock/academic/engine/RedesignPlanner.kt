package studylock.academic.engine

import studylock.domain.CalendarException
import studylock.domain.Exam
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.LocalDate
import java.time.ZoneId

/**
 * D-23: when the workload does not fit the remaining weekdays, propose a
 * redesign in increasing order of intrusion. Compression comes first; extra
 * weekday and weekend capacity are never applied without explicit opt-in.
 * The result is advisory and must never claim a goal is met when the numbers
 * say otherwise.
 */
enum class RedesignOption {
    NONE,
    COMPRESS,
    EXTRA_WEEKDAY,
    WEEKEND_AND_EXTRA
}

data class RedesignProposal(
    val needed: Boolean,
    val recommendedOption: RedesignOption,
    val goalAchievable: Boolean,
    val requiredSessions: Int,
    val availableSessions: Int,
    val shortfallSessions: Int,
    val summary: String,
    val reasons: List<String>
)

object RedesignPlanner {

    fun propose(
        startDate: LocalDate,
        endDate: LocalDate,
        availability: List<StudyAvailability>,
        remainingTopics: List<Topic>,
        calendarExceptions: List<CalendarException> = emptyList(),
        exams: List<Exam> = emptyList(),
        zoneId: ZoneId = ZoneId.of("UTC"),
        config: SchedulerConfig = SchedulerConfig()
    ): RedesignProposal {
        fun report(compressed: Boolean, extra: Boolean, weekend: Boolean) =
            CapacityCalculator.capacityReport(
                startDate, endDate, availability, remainingTopics,
                calendarExceptions, exams,
                config.copy(
                    useCompressedPlan = compressed,
                    extraWeekdaySession = extra,
                    weekendStudy = weekend
                ),
                zoneId
            )

        val base = report(compressed = false, extra = false, weekend = false)
        if (base.feasible) {
            return RedesignProposal(
                needed = false,
                recommendedOption = RedesignOption.NONE,
                goalAchievable = true,
                requiredSessions = base.requiredSessions,
                availableSessions = base.availableSessions,
                shortfallSessions = 0,
                summary = "Your current plan fits the time you have. No redesign is needed.",
                reasons = listOf("CAPACITY_FEASIBLE")
            )
        }

        val compressed = report(compressed = true, extra = false, weekend = false)
        if (compressed.feasible) {
            return proposal(
                RedesignOption.COMPRESS, compressed,
                "Compressing how much new material you cover in each 120-minute " +
                    "weekday session would fit the remaining work. Optional revision " +
                    "is deferred; no topics are invented.",
                base, listOf("COMPRESSED_COVERAGE", "REVISION_DEFERRED")
            )
        }

        val extra = report(compressed = true, extra = true, weekend = false)
        if (extra.feasible) {
            return proposal(
                RedesignOption.EXTRA_WEEKDAY, extra,
                "Compression alone is not enough. One extra 120-minute block on " +
                    "study weekdays would make the goal reachable. This is time-boxed " +
                    "and only applies if you choose it.",
                base, listOf("COMPRESSED_COVERAGE", "EXTRA_WEEKDAY_BLOCK_OPTIONAL")
            )
        }

        val weekend = report(compressed = true, extra = true, weekend = true)
        if (weekend.feasible) {
            return proposal(
                RedesignOption.WEEKEND_AND_EXTRA, weekend,
                "An extra weekday block plus optional weekend study would make the " +
                    "goal reachable. Weekends are normally rest days; this only applies " +
                    "if you explicitly choose it.",
                base, listOf("COMPRESSED_COVERAGE", "WEEKEND_STUDY_OPTIONAL")
            )
        }

        return RedesignProposal(
            needed = true,
            recommendedOption = RedesignOption.NONE,
            goalAchievable = false,
            requiredSessions = base.requiredSessions,
            availableSessions = weekend.availableSessions,
            shortfallSessions = weekend.shortfallSessions,
            summary = "Even with compressed coverage, an extra weekday block and " +
                "optional weekend study, about ${weekend.shortfallSessions} session(s) " +
                "are still missing. This goal cannot be claimed as met.",
            reasons = base.warnings + listOf(
                "INFEASIBLE_EVEN_WITH_REDESIGN", "NO_FABRICATED_TOPICS_OR_DATES"
            )
        )
    }

    private fun proposal(
        option: RedesignOption,
        report: CapacityReport,
        summary: String,
        base: CapacityReport,
        reasons: List<String>
    ) = RedesignProposal(
        needed = true,
        recommendedOption = option,
        goalAchievable = true,
        requiredSessions = base.requiredSessions,
        availableSessions = report.availableSessions,
        shortfallSessions = 0,
        summary = summary,
        reasons = reasons
    )
}
