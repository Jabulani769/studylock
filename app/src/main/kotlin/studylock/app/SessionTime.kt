package studylock.app

import studylock.focus.FocusSession
import studylock.focus.SessionState

/**
 * Display-only time helpers. The domain state machine remains the source of
 * truth for transitions and persisted elapsed minutes.
 */
fun FocusSession.elapsedSeconds(
    nowMillis: Long,
    elapsedRealtimeMillis: Long? = null
): Long {
    val persisted = if (elapsedActiveSeconds > 0) {
        elapsedActiveSeconds.toLong()
    } else {
        elapsedActiveMinutes.coerceAtLeast(0) * 60L
    }
    val currentSegment = if (state == SessionState.ACTIVE) {
        val anchor = segmentStartElapsedMillis
        if (anchor != null && elapsedRealtimeMillis != null && elapsedRealtimeMillis >= anchor) {
            ((elapsedRealtimeMillis - anchor) / 1000L).coerceAtLeast(0L)
        } else {
            // No usable monotonic anchor (for example after a reboot): fall
            // back to wall time with a backwards-clock guard.
            val startedAt = startTime?.toEpochMilli() ?: nowMillis
            ((nowMillis - startedAt) / 1000L).coerceAtLeast(0L)
        }
    } else {
        0L
    }
    return persisted + currentSegment
}

fun FocusSession.remainingSeconds(
    nowMillis: Long,
    elapsedRealtimeMillis: Long? = null
): Long {
    val target = targetMinutes * 60L
    return (target - elapsedSeconds(nowMillis, elapsedRealtimeMillis)).coerceIn(0L, target)
}

/**
 * Formats a duration in whole seconds as m:ss, or h:mm:ss past one hour.
 */
fun Long.asClockString(): String {
    val totalSeconds = coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
