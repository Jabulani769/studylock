package studylock.app.profile

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileIdentityTest {

    @Test
    fun initial_comes_from_the_entered_name() {
        assertEquals("T", ProfileIdentity.initial("Thandi"))
        assertEquals("T", ProfileIdentity.initial("  thandi mhlongo  "))
    }

    @Test
    fun initial_handles_unicode_names() {
        assertEquals("É", ProfileIdentity.initial("élan"))
        assertEquals("З", ProfileIdentity.initial("Зоя"))
    }

    @Test
    fun missing_names_get_a_neutral_initial() {
        assertEquals("?", ProfileIdentity.initial(null))
        assertEquals("?", ProfileIdentity.initial(""))
        assertEquals("?", ProfileIdentity.initial("   "))
    }

    @Test
    fun display_label_falls_back_without_inventing_identity() {
        assertEquals("Student", ProfileIdentity.displayLabel(null))
        assertEquals("Student", ProfileIdentity.displayLabel("   "))
        assertEquals("Thandi", ProfileIdentity.displayLabel("  Thandi  "))
    }

    @Test
    fun profile_exposes_derived_identity() {
        val profile = UserProfile(displayName = "  Jabu  ")
        assertEquals("J", profile.initial)
        assertEquals("Jabu", profile.displayLabel)
    }
}
