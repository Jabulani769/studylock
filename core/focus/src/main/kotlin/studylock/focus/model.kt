package studylock.focus

import java.time.Instant
import java.util.UUID

enum class SessionState {
    IDLE,
    ACTIVE,
    PAUSED,
    COMPLETED,
    INTERRUPTED,
    EARLY_EXIT
}

data class SessionConfig(
    val moduleId: UUID? = null,
    val topicId: UUID? = null,
    val targetMinutes: Int = 120,
    val allowBreaks: Boolean = true
)

/**
 * Learning status is recorded only from an explicit user choice or an
 * assessment result. It is never inferred from elapsed timer time.
 */
enum class LearningEvidence {
    STUDIED,
    UNDERSTOOD,
    MASTERED
}

data class SessionInterruption(
    val id: UUID = UUID.randomUUID(),
    val sessionId: UUID,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val durationSeconds: Int = 0,
    val reason: String = "user_pause"
)

data class SessionSummary(
    val id: UUID,
    val moduleId: UUID?,
    val topicId: UUID?,
    val state: SessionState,
    val startTime: Instant?,
    val endTime: Instant?,
    val elapsedActiveMinutes: Int,
    val targetMinutes: Int,
    val breakCount: Int,
    val interruptionCount: Int,
    val reason: String?
)

sealed class SessionEvent {
    data class SessionStarted(
        val sessionId: UUID,
        val moduleId: UUID?,
        val topicId: UUID?,
        val startedAt: Instant
    ) : SessionEvent()

    data class SessionPaused(
        val sessionId: UUID,
        val pausedAt: Instant,
        val elapsedActiveMinutesAtPause: Int
    ) : SessionEvent()

    data class SessionResumed(
        val sessionId: UUID,
        val resumedAt: Instant
    ) : SessionEvent()

    data class SessionCompleted(
        val sessionId: UUID,
        val completedAt: Instant,
        val elapsedActiveMinutes: Int
    ) : SessionEvent()

    data class SessionInterrupted(
        val sessionId: UUID,
        val interruptedAt: Instant,
        val reason: String,
        val elapsedActiveMinutes: Int
    ) : SessionEvent()

    data class SessionEarlyExit(
        val sessionId: UUID,
        val exitedAt: Instant,
        val elapsedActiveMinutes: Int,
        val confirmed: Boolean
    ) : SessionEvent()
}

data class FocusSession(
    val id: UUID = UUID.randomUUID(),
    val state: SessionState = SessionState.IDLE,
    val moduleId: UUID? = null,
    val topicId: UUID? = null,
    val targetMinutes: Int = 120,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val elapsedActiveMinutes: Int = 0,
    val elapsedActiveSeconds: Int = 0,
    val breakCount: Int = 0,
    val interruptionCount: Int = 0,
    val interruptionSeconds: Int = 0,
    val learningEvidence: LearningEvidence? = null,
    val reason: String? = null,
    val segmentStartElapsedMillis: Long? = null,
    val lastUpdated: Instant = Instant.now()
)
