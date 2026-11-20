package studylock.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {

    @Test
    fun system_follows_device() {
        assertEquals(true, ThemePreference.SYSTEM.resolvesToDark(systemDark = true))
        assertEquals(false, ThemePreference.SYSTEM.resolvesToDark(systemDark = false))
    }

    @Test
    fun light_and_dark_override_device() {
        assertEquals(false, ThemePreference.LIGHT.resolvesToDark(systemDark = true))
        assertEquals(false, ThemePreference.LIGHT.resolvesToDark(systemDark = false))
        assertEquals(true, ThemePreference.DARK.resolvesToDark(systemDark = false))
        assertEquals(true, ThemePreference.DARK.resolvesToDark(systemDark = true))
    }

    @Test
    fun stored_values_round_trip() {
        ThemePreference.entries.forEach { preference ->
            assertEquals(preference, ThemePreference.fromStored(preference.name))
        }
    }

    @Test
    fun missing_or_unknown_values_default_to_system() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStored(null))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStored("MIDNIGHT"))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStored(""))
    }
}
