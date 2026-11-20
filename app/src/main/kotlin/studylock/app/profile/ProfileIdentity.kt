package studylock.app.profile

import java.util.Locale

/** Pure name-to-identity rules so they can be tested without Android. */
object ProfileIdentity {

    /** First leading character of the user's own name, never a random initial. */
    fun initial(displayName: String?): String {
        val trimmed = displayName?.trim().orEmpty()
        if (trimmed.isEmpty()) return "?"
        val first = trimmed.codePointAt(0)
        return String(Character.toChars(first)).uppercase(Locale.ROOT)
    }

    /** Neutral label for greetings before the user has entered a name. */
    fun displayLabel(displayName: String?): String =
        displayName?.trim()?.takeIf { it.isNotEmpty() } ?: "Student"
}
