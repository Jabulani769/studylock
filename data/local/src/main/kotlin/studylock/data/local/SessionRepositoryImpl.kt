package studylock.data.local

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID
import studylock.focus.FocusSession
import studylock.focus.LearningEvidence
import studylock.focus.SessionInterruption
import studylock.focus.SessionRepository
import studylock.focus.SessionState

class SessionRepositoryImpl(
    private val sessionDao: SessionDao,
    private val interruptionDao: SessionInterruptionDao,
    // Room's suspending DAO calls already dispatch to its query executor;
    // IO keeps blocking database work off the CPU-bound Default pool.
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SessionRepository {

    override suspend fun save(session: FocusSession) = withContext(dispatcher) {
        sessionDao.insert(toEntity(session))
    }

    override suspend fun getById(id: UUID): FocusSession? = withContext(dispatcher) {
        sessionDao.getById(id.toString())?.let { toDomain(it) }
    }

    override suspend fun getCurrent(): FocusSession? = withContext(dispatcher) {
        sessionDao.getActive()?.let { toDomain(it) }
    }

    override suspend fun getHistory(limit: Int): List<FocusSession> = withContext(dispatcher) {
        sessionDao.getRecent(limit).map { toDomain(it) }
    }

    override suspend fun saveInterruption(interruption: SessionInterruption) =
        withContext(dispatcher) {
            interruptionDao.upsert(toEntity(interruption))
        }

    override suspend fun getInterruptions(sessionId: UUID): List<SessionInterruption> =
        withContext(dispatcher) {
            interruptionDao.getBySession(sessionId.toString()).map { toDomain(it) }
        }

    override suspend fun getOpenInterruption(sessionId: UUID): SessionInterruption? =
        withContext(dispatcher) {
            interruptionDao.getOpen(sessionId.toString())?.let { toDomain(it) }
        }

    override suspend fun deleteAll() = withContext(dispatcher) {
        interruptionDao.deleteAll()
        sessionDao.deleteAll()
    }

    private fun toEntity(session: FocusSession): FocusSessionEntity {
        return FocusSessionEntity(
            id = session.id.toString(),
            state = session.state.name,
            moduleId = session.moduleId?.toString(),
            topicId = session.topicId?.toString(),
            targetMinutes = session.targetMinutes,
            startTimeMillis = session.startTime?.toEpochMilli(),
            endTimeMillis = session.endTime?.toEpochMilli(),
            elapsedActiveMinutes = session.elapsedActiveMinutes,
            elapsedActiveSeconds = session.elapsedActiveSeconds,
            breakCount = session.breakCount,
            interruptionCount = session.interruptionCount,
            interruptionSeconds = session.interruptionSeconds,
            learningEvidence = session.learningEvidence?.name,
            reason = session.reason,
            segmentStartElapsedMillis = session.segmentStartElapsedMillis,
            lastUpdatedMillis = session.lastUpdated.toEpochMilli()
        )
    }

    private fun toDomain(entity: FocusSessionEntity): FocusSession {
        return FocusSession(
            id = UUID.fromString(entity.id),
            state = SessionState.valueOf(entity.state),
            moduleId = entity.moduleId?.let { UUID.fromString(it) },
            topicId = entity.topicId?.let { UUID.fromString(it) },
            targetMinutes = entity.targetMinutes,
            startTime = entity.startTimeMillis?.let { Instant.ofEpochMilli(it) },
            endTime = entity.endTimeMillis?.let { Instant.ofEpochMilli(it) },
            elapsedActiveMinutes = entity.elapsedActiveMinutes,
            elapsedActiveSeconds = entity.elapsedActiveSeconds,
            breakCount = entity.breakCount,
            interruptionCount = entity.interruptionCount,
            interruptionSeconds = entity.interruptionSeconds,
            learningEvidence = entity.learningEvidence?.let { LearningEvidence.valueOf(it) },
            reason = entity.reason,
            segmentStartElapsedMillis = entity.segmentStartElapsedMillis,
            lastUpdated = Instant.ofEpochMilli(entity.lastUpdatedMillis)
        )
    }

    private fun toEntity(interruption: SessionInterruption): SessionInterruptionEntity {
        return SessionInterruptionEntity(
            id = interruption.id.toString(),
            sessionId = interruption.sessionId.toString(),
            startedAtMillis = interruption.startedAt.toEpochMilli(),
            endedAtMillis = interruption.endedAt?.toEpochMilli(),
            durationSeconds = interruption.durationSeconds,
            reason = interruption.reason
        )
    }

    private fun toDomain(entity: SessionInterruptionEntity): SessionInterruption {
        return SessionInterruption(
            id = UUID.fromString(entity.id),
            sessionId = UUID.fromString(entity.sessionId),
            startedAt = Instant.ofEpochMilli(entity.startedAtMillis),
            endedAt = entity.endedAtMillis?.let { Instant.ofEpochMilli(it) },
            durationSeconds = entity.durationSeconds,
            reason = entity.reason
        )
    }
}
