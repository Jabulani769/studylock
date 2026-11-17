package studylock.app

import studylock.focus.FocusSession
import studylock.focus.SessionState

/**
 * The repository no longer returns a completed session as "current", so the
 * timer service checks the latest recorded session when its active session
 * disappears. Only sessions that were actually completed should notify.
 */
internal fun shouldNotifyCompletion(
    current: FocusSession?,
    latest: FocusSession?,
    sawActiveSession: Boolean
): Boolean =
    current == null &&
        sawActiveSession &&
        latest?.state == SessionState.COMPLETED
