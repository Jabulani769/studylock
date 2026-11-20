package studylock.app.settings

/**
 * Appearance choices from GitHub issue #6. SYSTEM keeps the previous behaviour
 * of following the device, while LIGHT and DARK override it for this app only.
 */
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK;

    /** Pure so it can be tested without an Activity or system configuration. */
    fun resolvesToDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** Unknown or missing stored values degrade safely to SYSTEM. */
        fun fromStored(value: String?): ThemePreference =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
