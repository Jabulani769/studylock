package studylock.app.reminders

import java.time.DayOfWeek
import java.time.ZonedDateTime

/**
 * Pure next-trigger calculation for weekday study reminders (D-21).
 * Weekends are rest days, so a Friday reminder rolls to Monday. The caller
 * supplies a concrete time, keeping this deterministic and testable.
 */
object ReminderTime {

    fun nextWeekdayTrigger(now: ZonedDateTime, startMinutes: Int): ZonedDateTime {
        require(startMinutes in 0..1439) { "startMinutes must be within a day" }
        var candidate = now.toLocalDate().atStartOfDay(now.zone)
            .plusMinutes(startMinutes.toLong())
        while (candidate.dayOfWeek == DayOfWeek.SATURDAY ||
            candidate.dayOfWeek == DayOfWeek.SUNDAY || candidate.isBefore(now)
        ) {
            candidate = candidate.plusDays(1).toLocalDate().atStartOfDay(now.zone)
                .plusMinutes(startMinutes.toLong())
        }
        return candidate
    }

    fun format(startMinutes: Int): String {
        val hours = startMinutes / 60
        val minutes = startMinutes % 60
        return "%02d:%02d".format(hours, minutes)
    }
}
