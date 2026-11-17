package studylock.common.clock

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

interface StudyClock {
    val instant: Instant
    val zoneId: ZoneId
    val millis: Long

    /**
     * Monotonic milliseconds for measuring elapsed time. This resets when the
     * device reboots, so callers must fall back to wall time if it moves
     * backwards relative to a persisted anchor.
     */
    val elapsedRealtimeMillis: Long
}

object SystemClock : StudyClock {
    private val delegate: Clock = Clock.systemDefaultZone()
    override val instant: Instant get() = delegate.instant()
    override val zoneId: ZoneId get() = delegate.zone
    override val millis: Long get() = delegate.millis()
    override val elapsedRealtimeMillis: Long get() = System.nanoTime() / 1_000_000L
}

class FixedClock(
    override val instant: Instant,
    override val zoneId: ZoneId = ZoneId.systemDefault(),
    override val elapsedRealtimeMillis: Long = instant.toEpochMilli()
) : StudyClock {
    override val millis: Long get() = instant.toEpochMilli()
}
