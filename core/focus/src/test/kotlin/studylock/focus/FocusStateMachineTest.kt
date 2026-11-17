package studylock.focus

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import studylock.common.clock.FixedClock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class FocusStateMachineTest {

    private val fixedInstant = Instant.parse("2026-01-15T10:00:00Z")
    private val clock = FixedClock(fixedInstant, ZoneId.of("UTC"))

    @Test
    fun `start transitions IDLE to ACTIVE with config`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.IDLE)
        val config = SessionConfig(moduleId = UUID.randomUUID(), topicId = UUID.randomUUID(), targetMinutes = 120)

        val started = machine.start(session, config)

        assertEquals(SessionState.ACTIVE, started.state)
        assertEquals(config.moduleId, started.moduleId)
        assertEquals(config.topicId, started.topicId)
        assertEquals(120, started.targetMinutes)
        assertEquals(fixedInstant, started.startTime)
        assertEquals(0, started.elapsedActiveMinutes)
    }

    @Test
    fun `cannot start an already active session`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.ACTIVE, startTime = fixedInstant)
        val config = SessionConfig()

        assertFailsWith<IllegalArgumentException> { machine.start(session, config) }
    }

    @Test
    fun `can start from EARLY_EXIT after reset`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.EARLY_EXIT)
        val config = SessionConfig()

        val started = machine.start(session, config)

        assertEquals(SessionState.ACTIVE, started.state)
    }

    @Test
    fun `pause transitions ACTIVE to PAUSED and accumulates elapsed time`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(300) // 5 minutes ago
        )

        val clock5MinLater = FixedClock(fixedInstant.plusSeconds(300), ZoneId.of("UTC")) // 10 minutes total elapsed
        val machine2 = FocusStateMachine(clock5MinLater)
        val paused = machine2.pause(session)

        assertEquals(SessionState.PAUSED, paused.state)
        assertEquals(10, paused.elapsedActiveMinutes) // 5 + 5 = 10 minutes
    }

    @Test
    fun `cannot pause a paused session`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.PAUSED)

        assertFailsWith<IllegalArgumentException> { machine.pause(session) }
    }

    @Test
    fun `resume transitions PAUSED to ACTIVE`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = 30
        )

        val resumed = machine.resume(session)

        assertEquals(SessionState.ACTIVE, resumed.state)
        assertEquals(30, resumed.elapsedActiveMinutes)
    }

    @Test
    fun `cannot resume an active session`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.ACTIVE)

        assertFailsWith<IllegalArgumentException> { machine.resume(session) }
    }

    @Test
    fun `complete records end time and total elapsed`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(600),
            elapsedActiveMinutes = 50
        )

        val clockLater = FixedClock(fixedInstant.plusSeconds(600), ZoneId.of("UTC"))
        val machine2 = FocusStateMachine(clockLater)
        val completed = machine2.complete(session)

        assertEquals(SessionState.COMPLETED, completed.state)
        assertNotNull(completed.endTime)
        assertEquals(70, completed.elapsedActiveMinutes) // 50 + 20 minutes elapsed
    }

    @Test
    fun `can complete from paused state`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = 90
        )

        val completed = machine.complete(session)

        assertEquals(SessionState.COMPLETED, completed.state)
        assertEquals(90, completed.elapsedActiveMinutes)
    }

    @Test
    fun `interrupt records reason and increments interruption count`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(120),
            interruptionCount = 1
        )

        val clockLater = FixedClock(fixedInstant.plusSeconds(120), ZoneId.of("UTC"))
        val machine2 = FocusStateMachine(clockLater)
        val interrupted = machine2.interrupt(session, "app_switch")

        assertEquals(SessionState.INTERRUPTED, interrupted.state)
        assertEquals("app_switch", interrupted.reason)
        assertEquals(2, interrupted.interruptionCount)
        assertEquals(4, interrupted.elapsedActiveMinutes) // 2 + 2 minutes
    }

    @Test
    fun `early exit records confirmation state`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.ACTIVE, startTime = fixedInstant)

        val exited = machine.earlyExit(session, confirmed = true)

        assertEquals(SessionState.EARLY_EXIT, exited.state)
        assertEquals("user_confirmed", exited.reason)
    }

    @Test
    fun `early exit unconfirmed`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.ACTIVE, startTime = fixedInstant)

        val exited = machine.earlyExit(session, confirmed = false)

        assertEquals(SessionState.EARLY_EXIT, exited.state)
        assertEquals("user_unconfirmed", exited.reason)
    }

    @Test
    fun `reset returns session to IDLE`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.COMPLETED,
            moduleId = UUID.randomUUID(),
            topicId = UUID.randomUUID(),
            targetMinutes = 120,
            startTime = fixedInstant,
            endTime = fixedInstant.plusSeconds(3600),
            elapsedActiveMinutes = 120,
            breakCount = 2,
            interruptionCount = 1,
            reason = "completed"
        )

        val reset = machine.reset(session)

        assertEquals(SessionState.IDLE, reset.state)
        assertNull(reset.moduleId)
        assertNull(reset.topicId)
        assertEquals(120, reset.targetMinutes)
        assertNull(reset.startTime)
        assertNull(reset.endTime)
        assertEquals(0, reset.elapsedActiveMinutes)
        assertEquals(0, reset.breakCount)
        assertEquals(0, reset.interruptionCount)
        assertNull(reset.reason)
    }

    @Test
    fun `minutes_remaining returns full target when not active`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(state = SessionState.IDLE, targetMinutes = 120)

        assertEquals(120, machine.minutesRemaining(session))
    }

    @Test
    fun `minutes_remaining decreases during active session`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(600), // 10 minutes ago
            targetMinutes = 120
        )

        assertEquals(110, machine.minutesRemaining(session))
    }

    @Test
    fun `progress_percent calculates correctly`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(600), // 10 minutes elapsed
            targetMinutes = 120
        )

        assertEquals(8, machine.progressPercent(session)) // 10/120 * 100 = 8.33 -> 8
    }

    @Test
    fun `progress_percent caps at 100`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(9000), // way over target
            targetMinutes = 120,
            elapsedActiveMinutes = 120
        )

        assertEquals(100, machine.progressPercent(session))
    }

    @Test
    fun `is_session_active returns true only for ACTIVE`() {
        val machine = FocusStateMachine(clock)
        assertTrue(machine.isSessionActive(FocusSession(state = SessionState.ACTIVE)))
        assertTrue(!machine.isSessionActive(FocusSession(state = SessionState.IDLE)))
        assertTrue(!machine.isSessionActive(FocusSession(state = SessionState.PAUSED)))
        assertTrue(!machine.isSessionActive(FocusSession(state = SessionState.COMPLETED)))
        assertTrue(!machine.isSessionActive(FocusSession(state = SessionState.INTERRUPTED)))
        assertTrue(!machine.isSessionActive(FocusSession(state = SessionState.EARLY_EXIT)))
    }

    @Test
    fun `clock moving backwards never produces negative elapsed minutes`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.plusSeconds(3600), // recorded start is one hour ahead
            targetMinutes = 120
        )

        val paused = machine.pause(session)

        assertEquals(0, paused.elapsedActiveMinutes)
        assertEquals(120, machine.minutesRemaining(paused))
        assertEquals(0, machine.progressPercent(paused))
    }

    @Test
    fun `monotonic time keeps counting when the wall clock moves backwards`() {
        val mutable = MutableTestClock(
            instant = Instant.parse("2026-01-15T10:00:00Z"),
            elapsedRealtimeMillis = 1_000_000L
        )
        val started = FocusStateMachine(mutable).start(
            FocusSession(),
            SessionConfig(targetMinutes = 120)
        )

        mutable.elapsedRealtimeMillis += 30 * 60_000L
        mutable.instant = mutable.instant.minusSeconds(3_600)

        val paused = FocusStateMachine(mutable).pause(started)

        assertEquals(30, paused.elapsedActiveMinutes)
        assertEquals(90, FocusStateMachine(mutable).minutesRemaining(paused))
    }

    @Test
    fun `falls back to wall time when a monotonic anchor predates a reboot`() {
        val machine = FocusStateMachine(clock)
        val session = FocusSession(
            state = SessionState.ACTIVE,
            startTime = fixedInstant.minusSeconds(600),
            segmentStartElapsedMillis = clock.elapsedRealtimeMillis + 5_000_000L
        )

        val paused = machine.pause(session)

        assertEquals(10, paused.elapsedActiveMinutes)
    }

    @Test
    fun `pause keeps sub-minute elapsed time`() {
        val mutable = MutableTestClock(
            instant = Instant.parse("2026-01-15T10:00:00Z"),
            elapsedRealtimeMillis = 1_000_000L
        )
        val started = FocusStateMachine(mutable).start(
            FocusSession(),
            SessionConfig(targetMinutes = 120)
        )

        mutable.elapsedRealtimeMillis += 30_000L
        mutable.instant = mutable.instant.plusSeconds(30)

        val paused = FocusStateMachine(mutable).pause(started)

        assertEquals(30, paused.elapsedActiveSeconds)
        assertEquals(0, paused.elapsedActiveMinutes)
        assertEquals(119, FocusStateMachine(mutable).minutesRemaining(paused))
    }

    private class MutableTestClock(
        override var instant: Instant,
        override var elapsedRealtimeMillis: Long,
        override val zoneId: ZoneId = ZoneId.of("UTC")
    ) : studylock.common.clock.StudyClock {
        override val millis: Long get() = instant.toEpochMilli()
    }
}
