package studylock.focus

/**
 * Honest enforcement states. None of these claim universal app blocking.
 * They describe only what is verified on the current device and permission
 * state, per docs/06.
 */
enum class EnforcementState {
    /** Monitoring and notification quieting both verified and active. */
    FULL_SUPPORTED,
    /** Controls exist but are not all currently active. */
    LIMITED,
    /** Signals available; notification control not granted. */
    MONITORING_ONLY,
    /** Setup incomplete: required access is missing. */
    PERMISSION_REQUIRED,
    /** A previously granted control was revoked or interrupted. */
    ENFORCEMENT_INTERRUPTED
}

data class EnforcementSnapshot(
    val usageAccessGranted: Boolean,
    val notificationPolicyAccessGranted: Boolean,
    val quietModeActive: Boolean,
    val sessionActive: Boolean,
    val interrupted: Boolean
)

object EnforcementResolver {

    fun resolve(snapshot: EnforcementSnapshot): EnforcementState {
        if (snapshot.interrupted) return EnforcementState.ENFORCEMENT_INTERRUPTED
        if (snapshot.sessionActive &&
            !snapshot.usageAccessGranted && !snapshot.notificationPolicyAccessGranted
        ) {
            return EnforcementState.PERMISSION_REQUIRED
        }
        return when {
            snapshot.usageAccessGranted && snapshot.notificationPolicyAccessGranted &&
                snapshot.quietModeActive -> EnforcementState.FULL_SUPPORTED
            snapshot.usageAccessGranted && snapshot.notificationPolicyAccessGranted ->
                EnforcementState.LIMITED
            snapshot.usageAccessGranted -> EnforcementState.MONITORING_ONLY
            else -> EnforcementState.PERMISSION_REQUIRED
        }
    }

    /** User-facing wording that never overstates what is enforced. */
    fun describe(state: EnforcementState): String = when (state) {
        EnforcementState.FULL_SUPPORTED ->
            "Monitoring on; distracting notifications silenced during sessions on this device."
        EnforcementState.LIMITED ->
            "Controls are available but not all are active right now. No blocking is claimed."
        EnforcementState.MONITORING_ONLY ->
            "Monitoring only. Notification control is not granted; nothing is blocked."
        EnforcementState.PERMISSION_REQUIRED ->
            "Setup needed. Grant access in Settings to enable monitoring and notification quieting."
        EnforcementState.ENFORCEMENT_INTERRUPTED ->
            "A control was interrupted or revoked. StudyLock is showing its real, reduced state."
    }
}
