package studylock.focus

import studylock.common.clock.StudyClock

class FocusStateMachine(private val clock: StudyClock) {

    fun start(session: FocusSession, config: SessionConfig): FocusSession {
        require(session.state == SessionState.IDLE || session.state == SessionState.EARLY_EXIT) {
            "Cannot start session in state ${session.state}"
        }
        return session.copy(
            state = SessionState.ACTIVE,
            moduleId = config.moduleId,
            topicId = config.topicId,
            targetMinutes = config.targetMinutes,
            startTime = clock.instant,
            endTime = null,
            elapsedActiveMinutes = 0,
            elapsedActiveSeconds = 0,
            breakCount = 0,
            interruptionCount = 0,
            interruptionSeconds = 0,
            learningEvidence = null,
            reason = null,
            segmentStartElapsedMillis = clock.elapsedRealtimeMillis,
            lastUpdated = clock.instant
        )
    }

    fun pause(session: FocusSession): FocusSession {
        require(session.state == SessionState.ACTIVE) {
            "Cannot pause session in state ${session.state}"
        }
        val totalSeconds = totalElapsedSeconds(session)
        return session.copy(
            state = SessionState.PAUSED,
            elapsedActiveMinutes = totalSeconds / 60,
            elapsedActiveSeconds = totalSeconds,
            segmentStartElapsedMillis = null,
            lastUpdated = clock.instant
        )
    }

    fun resume(session: FocusSession): FocusSession {
        require(session.state == SessionState.PAUSED) {
            "Cannot resume session in state ${session.state}"
        }
        return session.copy(
            state = SessionState.ACTIVE,
            startTime = clock.instant,
            segmentStartElapsedMillis = clock.elapsedRealtimeMillis,
            lastUpdated = clock.instant
        )
    }

    fun complete(session: FocusSession): FocusSession {
        require(session.state == SessionState.ACTIVE || session.state == SessionState.PAUSED) {
            "Cannot complete session in state ${session.state}"
        }
        val total = totalElapsedSeconds(session)
        return session.copy(
            state = SessionState.COMPLETED,
            endTime = clock.instant,
            elapsedActiveMinutes = total / 60,
            elapsedActiveSeconds = total,
            segmentStartElapsedMillis = null,
            lastUpdated = clock.instant
        )
    }

    fun interrupt(session: FocusSession, reason: String): FocusSession {
        require(session.state == SessionState.ACTIVE || session.state == SessionState.PAUSED) {
            "Cannot interrupt session in state ${session.state}"
        }
        val total = totalElapsedSeconds(session)
        return session.copy(
            state = SessionState.INTERRUPTED,
            endTime = clock.instant,
            elapsedActiveMinutes = total / 60,
            elapsedActiveSeconds = total,
            interruptionCount = session.interruptionCount + 1,
            reason = reason,
            segmentStartElapsedMillis = null,
            lastUpdated = clock.instant
        )
    }

    fun earlyExit(session: FocusSession, confirmed: Boolean): FocusSession {
        require(session.state == SessionState.ACTIVE || session.state == SessionState.PAUSED) {
            "Cannot early exit session in state ${session.state}"
        }
        val total = totalElapsedSeconds(session)
        return session.copy(
            state = SessionState.EARLY_EXIT,
            endTime = clock.instant,
            elapsedActiveMinutes = total / 60,
            elapsedActiveSeconds = total,
            reason = if (confirmed) "user_confirmed" else "user_unconfirmed",
            segmentStartElapsedMillis = null,
            lastUpdated = clock.instant
        )
    }

    fun reset(session: FocusSession): FocusSession {
        return session.copy(
            state = SessionState.IDLE,
            moduleId = null,
            topicId = null,
            targetMinutes = 120,
            startTime = null,
            endTime = null,
            elapsedActiveMinutes = 0,
            elapsedActiveSeconds = 0,
            breakCount = 0,
            interruptionCount = 0,
            interruptionSeconds = 0,
            learningEvidence = null,
            reason = null,
            segmentStartElapsedMillis = null,
            lastUpdated = clock.instant
        )
    }

    fun isSessionActive(session: FocusSession): Boolean {
        return session.state == SessionState.ACTIVE
    }

    fun minutesRemaining(session: FocusSession): Int {
        if (session.state != SessionState.ACTIVE && session.state != SessionState.PAUSED) {
            return session.targetMinutes
        }
        val remainingSeconds = session.targetMinutes * 60 - totalElapsedSeconds(session)
        return maxOf(0, remainingSeconds / 60)
    }

    fun progressPercent(session: FocusSession): Int {
        if (session.targetMinutes <= 0) return 0
        val totalElapsed = totalElapsedSeconds(session)
        val targetSeconds = session.targetMinutes * 60L
        return minOf(100, (totalElapsed * 100L / targetSeconds).toInt())
    }

    private fun totalElapsedSeconds(session: FocusSession): Int {
        val persisted = if (session.elapsedActiveSeconds > 0) {
            session.elapsedActiveSeconds
        } else {
            session.elapsedActiveMinutes.coerceAtLeast(0) * 60
        }
        return if (session.state == SessionState.ACTIVE) {
            persisted + activeSegmentSeconds(session)
        } else {
            persisted
        }
    }

    private fun activeSegmentSeconds(session: FocusSession): Int {
        val anchor = session.segmentStartElapsedMillis
        if (anchor != null && clock.elapsedRealtimeMillis >= anchor) {
            return ((clock.elapsedRealtimeMillis - anchor) / 1000L).toInt().coerceAtLeast(0)
        }
        // No usable monotonic anchor (for example after a reboot): fall back
        // to wall time. A backwards clock still cannot produce negative time.
        val start = session.startTime ?: return 0
        val millis = clock.instant.toEpochMilli() - start.toEpochMilli()
        return (millis / 1000L).toInt().coerceAtLeast(0)
    }
}
