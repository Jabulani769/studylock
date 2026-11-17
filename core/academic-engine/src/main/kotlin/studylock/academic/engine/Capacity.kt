package studylock.academic.engine

import studylock.domain.CalendarException
import studylock.domain.DataStatus
import studylock.domain.Exam
import studylock.domain.StudyAvailability
import studylock.domain.Topic
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure, deterministic capacity calculation shared by the scheduler and the
 * progressive academic data (PADM) workflows. No Android or persistence types.
 */
object CapacityCalculator {

    /** Dates on which a normal study session is possible within the horizon. */
    fun eligibleStudyDates(
        startDate: LocalDate,
        endDate: LocalDate,
        availability: List<StudyAvailability>,
        calendarExceptions: List<CalendarException> = emptyList(),
        exams: List<Exam> = emptyList(),
        zoneId: ZoneId = ZoneId.of("UTC"),
        examBlockWindowDays: Int = 0
    ): List<LocalDate> {
        val unavailableDays = availability.filter { !it.available }.map { it.dayOfWeek }.toSet()
        val availableDays = availability.map { it.dayOfWeek }.toSet()
        val exceptionDates = calendarExceptions.filter { it.isUnavailable }.map { it.date }.toSet()
        val examDates = exams
            .filter { it.dateStatus in RELIABLE_DATE_STATUSES && it.dateTime != null }
            .flatMap { exam ->
                val date = exam.dateTime!!.atZone(zoneId).toLocalDate()
                (-examBlockWindowDays..examBlockWindowDays).map { date.plusDays(it.toLong()) }
            }
            .toSet()

        val dates = mutableListOf<LocalDate>()
        var date = startDate
        while (!date.isAfter(endDate)) {
            val dayOfWeek = date.dayOfWeek.value // 1=Mon .. 7=Sun
            val isWeekend = dayOfWeek >= 6
            val isAvailable = if (availability.isEmpty()) {
                true
            } else {
                dayOfWeek in availableDays && dayOfWeek !in unavailableDays
            }
            if (!isWeekend && isAvailable && date !in exceptionDates && date !in examDates) {
                dates.add(date)
            }
            date = date.plusDays(1)
        }
        return dates
    }

    /**
     * Quantifies whether the remaining work fits the available sessions.
     * "New learning" capacity is deliberately separated from revision.
     */
    fun capacityReport(
        startDate: LocalDate,
        endDate: LocalDate,
        availability: List<StudyAvailability>,
        remainingTopics: List<Topic>,
        calendarExceptions: List<CalendarException> = emptyList(),
        exams: List<Exam> = emptyList(),
        config: SchedulerConfig = SchedulerConfig(),
        zoneId: ZoneId = ZoneId.of("UTC")
    ): CapacityReport {
        val eligible = eligibleStudyDates(
            startDate, endDate, availability, calendarExceptions, exams,
            zoneId, config.examBlockWindowDays
        )
        val availableSessions = eligible.size

        var totalWeekdays = 0
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            if (cursor.dayOfWeek.value <= 5) totalWeekdays++
            cursor = cursor.plusDays(1)
        }

        val effectiveDurations = remainingTopics.map {
            (it.durationEstimateMinutes ?: config.defaultDurationMinutes).coerceAtLeast(1)
        }
        val newLearningSessions = effectiveDurations.sumOf {
            ceilDiv(it, config.newLearningMinutesPerSession.coerceAtLeast(1))
        }
        // Reserve a conservative slice for revision/assessment; it must never
        // consume more than a third of the available sessions in the estimate.
        val revisionSessions = (effectiveDurations.size / 3).coerceAtMost(availableSessions / 3)
        val requiredSessions = newLearningSessions + revisionSessions
        val shortfall = (requiredSessions - availableSessions).coerceAtLeast(0)

        val warnings = mutableListOf<String>()
        if (shortfall > 0) {
            warnings.add(
                "INSUFFICIENT_CAPACITY: about $requiredSessions session(s) needed but " +
                    "$availableSessions available (shortfall $shortfall). " +
                    "Reduce optional revision before essential coverage; no full coverage is claimed."
            )
        }
        if (remainingTopics.any { it.durationEstimateMinutes == null }) {
            warnings.add("DEFAULT_ESTIMATES_USED: some topic durations/difficulties are estimates, not official.")
        }

        return CapacityReport(
            totalWeekdays = totalWeekdays,
            availableSessions = availableSessions,
            excludedWeekends = totalWeekdays - availableSessions,
            newLearningSessions = newLearningSessions,
            revisionSessions = revisionSessions,
            requiredSessions = requiredSessions,
            shortfallSessions = shortfall,
            feasible = shortfall == 0,
            warnings = warnings
        )
    }

    private val RELIABLE_DATE_STATUSES = setOf(DataStatus.VERIFIED, DataStatus.RECEIVED)

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
}

data class CapacityReport(
    val totalWeekdays: Int,
    val availableSessions: Int,
    val excludedWeekends: Int,
    val newLearningSessions: Int,
    val revisionSessions: Int,
    val requiredSessions: Int,
    val shortfallSessions: Int,
    val feasible: Boolean,
    val warnings: List<String>
)
