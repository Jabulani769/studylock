package studylock.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import studylock.focus.FocusSession
import studylock.focus.SessionInterruption
import studylock.focus.SessionState

class SessionRepositoryPersistenceTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: SessionRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = SessionRepositoryImpl(database.sessionDao(), database.sessionInterruptionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saving_a_session_again_keeps_its_recorded_interruptions() = runBlocking {
        val session = FocusSession(
            state = SessionState.ACTIVE,
            targetMinutes = 120,
            startTime = Instant.ofEpochMilli(1_000),
            lastUpdated = Instant.ofEpochMilli(1_000)
        )
        repository.save(session)

        val interruption = SessionInterruption(
            sessionId = session.id,
            startedAt = Instant.ofEpochMilli(2_000),
            endedAt = Instant.ofEpochMilli(12_000),
            durationSeconds = 10,
            reason = "user_pause"
        )
        repository.saveInterruption(interruption)

        repository.save(
            session.copy(
                state = SessionState.PAUSED,
                interruptionCount = 1,
                interruptionSeconds = 10,
                lastUpdated = Instant.ofEpochMilli(12_000)
            )
        )

        val saved = repository.getInterruptions(session.id)
        assertEquals(1, saved.size)
        assertEquals(interruption.id, saved.single().id)
        assertEquals(10, saved.single().durationSeconds)
    }
}
