package studylock.app

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import studylock.focus.FocusSession
import studylock.focus.SessionState

class SessionCompletionTest {

    @Test
    fun `notifies when an observed active session is completed`() {
        val latest = FocusSession(state = SessionState.COMPLETED)
        assertTrue(shouldNotifyCompletion(current = null, latest = latest, sawActiveSession = true))
    }

    @Test
    fun `does not notify for an early exit`() {
        val latest = FocusSession(state = SessionState.EARLY_EXIT)
        assertFalse(shouldNotifyCompletion(current = null, latest = latest, sawActiveSession = true))
    }

    @Test
    fun `does not notify if the service never saw an active session`() {
        val latest = FocusSession(state = SessionState.COMPLETED)
        assertFalse(shouldNotifyCompletion(current = null, latest = latest, sawActiveSession = false))
    }

    @Test
    fun `does not notify while a session is still active`() {
        val current = FocusSession(state = SessionState.ACTIVE)
        val latest = FocusSession(state = SessionState.COMPLETED)
        assertFalse(shouldNotifyCompletion(current = current, latest = latest, sawActiveSession = true))
    }
}
