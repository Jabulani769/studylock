package studylock.focus

interface SessionRepository {
    suspend fun save(session: FocusSession)
    suspend fun getById(id: java.util.UUID): FocusSession?
    suspend fun getCurrent(): FocusSession?
    suspend fun getHistory(limit: Int): List<FocusSession>
    suspend fun saveInterruption(interruption: SessionInterruption)
    suspend fun getInterruptions(sessionId: java.util.UUID): List<SessionInterruption>
    suspend fun getOpenInterruption(sessionId: java.util.UUID): SessionInterruption?
    suspend fun deleteAll()
}
