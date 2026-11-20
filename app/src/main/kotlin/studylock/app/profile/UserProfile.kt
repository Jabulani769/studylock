package studylock.app.profile

/**
 * One local identity on this device. No account, no sync (D-18, D-22).
 * The photo is a file name inside app-private storage, never a remote URL.
 */
data class UserProfile(
    val displayName: String = "",
    val photoFileName: String? = null
) {
    val initial: String get() = ProfileIdentity.initial(displayName)
    val displayLabel: String get() = ProfileIdentity.displayLabel(displayName)
}
