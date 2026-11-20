package studylock.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeTest {

    private val zone = ZoneId.of("UTC")
    private val start = 18 * 60

    private fun trigger(date: String, hour: Int, minute: Int = 0) =
        ReminderTime.nextWeekdayTrigger(
            ZonedDateTime.of(LocalDate.parse(date).atTime(hour, minute), zone),
            start
        )

    @Test
    fun before_the_preferred_time_uses_the_same_weekday() {
        val next = trigger("2026-01-12", 9) // Monday
        assertEquals("2026-01-12T18:00", next.toLocalDateTime().toString())
    }

    @Test
    fun after_the_preferred_time_rolls_to_next_weekday() {
        val next = trigger("2026-01-12", 19) // Monday
        assertEquals("2026-01-13T18:00", next.toLocalDateTime().toString())
    }

    @Test
    fun friday_evening_rolls_to_monday() {
        val next = trigger("2026-01-16", 19) // Friday
        assertEquals("2026-01-19T18:00", next.toLocalDateTime().toString())
    }

    @Test
    fun weekends_are_skipped() {
        assertEquals("2026-01-19T18:00", trigger("2026-01-17", 10).toLocalDateTime().toString())
        assertEquals("2026-01-19T18:00", trigger("2026-01-18", 10).toLocalDateTime().toString())
    }

    @Test
    fun format_is_stable() {
        assertEquals("18:00", ReminderTime.format(18 * 60))
        assertEquals("07:05", ReminderTime.format(7 * 60 + 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalid_minutes_are_rejected() {
        ReminderTime.nextWeekdayTrigger(
            ZonedDateTime.of(LocalDate.parse("2026-01-12").atTime(9, 0), zone),
            1440
        )
    }
}
