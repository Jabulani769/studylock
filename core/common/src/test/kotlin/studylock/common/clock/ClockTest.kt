package studylock.common.clock

import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals

class ClockTest {

    @Test
    fun `system clock returns non-null instant and millis`() {
        val millis = SystemClock.millis
        val instantMillis = SystemClock.instant.toEpochMilli()
        kotlin.test.assertTrue(kotlin.math.abs(millis - instantMillis) < 1_000L)
        assertEquals(ZoneId.systemDefault(), SystemClock.zoneId)
        kotlin.test.assertTrue(SystemClock.elapsedRealtimeMillis > 0L)
    }

    @Test
    fun `fixed clock returns configured instant and millis`() {
        val fixed = FixedClock(Instant.parse("2026-01-15T10:30:00Z"), ZoneId.of("UTC"))
        assertEquals(Instant.parse("2026-01-15T10:30:00Z"), fixed.instant)
        assertEquals(ZoneId.of("UTC"), fixed.zoneId)
        assertEquals(1768473000000L, fixed.millis)
    }

    @Test
    fun `fixed clock defaults to system zone`() {
        val fixed = FixedClock(Instant.parse("2026-01-15T10:30:00Z"))
        assertEquals(ZoneId.systemDefault(), fixed.zoneId)
    }
}
