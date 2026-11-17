package studylock.app

import org.junit.Test
import studylock.focus.FocusSession
import studylock.focus.SessionState
import java.time.Instant
import kotlin.test.assertEquals

class SessionTimeTest {

    @Test
    fun `clock string treats the value as seconds`() {
        assertEquals("00:07", 7L.asClockString())
        assertEquals("00:00", 0L.asClockString())
        assertEquals("01:00", 60L.asClockString())
        assertEquals("59:59", 3599L.asClockString())
        assertEquals("1:00:00", 3600L.asClockString())
        assertEquals("1:59:00", 7140L.asClockString())
        assertEquals("00:00", (-5L).asClockString())
    }

    @Test
    fun `remaining time counts down from the active segment`() {
        val start = Instant.ofEpochMilli(1_000_000L)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = start,
            targetMinutes = 120
        )

        val remaining = session.remainingSeconds(start.toEpochMilli() + 120_000L)

        assertEquals(7080L, remaining)
        assertEquals("1:58:00", remaining.asClockString())
    }

    @Test
    fun `paused session subtracts persisted elapsed minutes`() {
        val session = FocusSession(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = 60,
            targetMinutes = 120
        )

        assertEquals(3600L, session.remainingSeconds(nowMillis = 0L))
        assertEquals("1:00:00", session.remainingSeconds(0L).asClockString())
    }

    @Test
    fun `remaining time never goes below zero or above the target`() {
        val session = FocusSession(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = 500,
            targetMinutes = 120
        )

        assertEquals(0L, session.remainingSeconds(0L))
    }

    @Test
    fun `clock moving backwards does not add time`() {
        val start = Instant.ofEpochMilli(10_000_000L)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = start,
            targetMinutes = 120
        )

        val remaining = session.remainingSeconds(start.toEpochMilli() - 60_000L)

        assertEquals(7200L, remaining)
    }

    @Test
    fun `negative persisted elapsed time cannot inflate the clock`() {
        val session = FocusSession(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = -398,
            targetMinutes = 120
        )

        assertEquals(7200L, session.remainingSeconds(0L))
    }

    @Test
    fun `monotonic anchor is used while the session is active`() {
        val wallStart = Instant.ofEpochMilli(1_000_000L)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = wallStart,
            targetMinutes = 120,
            segmentStartElapsedMillis = 500_000L
        )

        // Wall clock is one minute behind the start, but monotonic time has
        // advanced one minute. The monotonic value wins.
        val remaining = session.remainingSeconds(
            nowMillis = wallStart.toEpochMilli() - 60_000L,
            elapsedRealtimeMillis = 560_000L
        )

        assertEquals(7140L, remaining)
    }

    @Test
    fun `persisted seconds retain sub-minute elapsed time`() {
        val session = FocusSession(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = 0,
            elapsedActiveSeconds = 90,
            targetMinutes = 120
        )

        assertEquals(7110L, session.remainingSeconds(0L))
    }

    @Test
    fun `an anchor from before a reboot falls back to wall time`() {
        val wallStart = Instant.ofEpochMilli(1_000_000L)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = wallStart,
            targetMinutes = 120,
            segmentStartElapsedMillis = 9_000_000L
        )

        val remaining = session.remainingSeconds(
            nowMillis = wallStart.toEpochMilli() + 60_000L,
            elapsedRealtimeMillis = 20_000L
        )

        assertEquals(7140L, remaining)
    }
}
