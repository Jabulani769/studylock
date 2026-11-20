package studylock.app.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("studylock_settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    /** Whether the first-launch permission explanations have been shown. */
    val permissionsPrompted: Flow<Boolean> =
        dataStore.data.map { it[PERMISSIONS_PROMPTED] ?: false }

    suspend fun setPermissionsPrompted(prompted: Boolean) {
        dataStore.edit { it[PERMISSIONS_PROMPTED] = prompted }
    }

    val themePreference: Flow<ThemePreference> =
        dataStore.data.map { preferences ->
            ThemePreference.fromStored(preferences[THEME_PREFERENCE])
        }

    suspend fun setThemePreference(preference: ThemePreference) {
        dataStore.edit { it[THEME_PREFERENCE] = preference.name }
    }

    val reminderEnabled: Flow<Boolean> =
        dataStore.data.map { it[REMINDER_ENABLED] ?: false }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[REMINDER_ENABLED] = enabled }
    }

    val reminderStartMinutes: Flow<Int> =
        dataStore.data.map { it[REMINDER_START_MINUTES] ?: DEFAULT_REMINDER_MINUTES }

    suspend fun setReminderStartMinutes(minutes: Int) {
        dataStore.edit { it[REMINDER_START_MINUTES] = minutes.coerceIn(0, 1439) }
    }

    val planningMode: Flow<PlanningMode> =
        dataStore.data.map { preferences ->
            PlanningMode.fromStored(preferences[PLANNING_MODE])
        }

    suspend fun setPlanningMode(mode: PlanningMode) {
        dataStore.edit { it[PLANNING_MODE] = mode.name }
    }

    val redesignChoice: Flow<RedesignChoice> =
        dataStore.data.map { preferences ->
            RedesignChoice.fromStored(preferences[REDESIGN_CHOICE])
        }

    suspend fun setRedesignChoice(choice: RedesignChoice) {
        dataStore.edit { it[REDESIGN_CHOICE] = choice.name }
    }

    val profileName: Flow<String> =
        dataStore.data.map { it[PROFILE_NAME] ?: "" }

    suspend fun setProfileName(name: String) {
        val normalized = name.trim()
        dataStore.edit { preferences ->
            if (normalized.isEmpty()) preferences.remove(PROFILE_NAME)
            else preferences[PROFILE_NAME] = normalized
        }
    }

    val profilePhotoName: Flow<String?> =
        dataStore.data.map { it[PROFILE_PHOTO_NAME] }

    suspend fun setProfilePhotoName(fileName: String?) {
        dataStore.edit { preferences ->
            if (fileName == null) preferences.remove(PROFILE_PHOTO_NAME)
            else preferences[PROFILE_PHOTO_NAME] = fileName
        }
    }

    private companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PERMISSIONS_PROMPTED = booleanPreferencesKey("permissions_prompted")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_START_MINUTES = intPreferencesKey("reminder_start_minutes")
        const val DEFAULT_REMINDER_MINUTES = 18 * 60
        val PLANNING_MODE = stringPreferencesKey("planning_mode")
        val REDESIGN_CHOICE = stringPreferencesKey("redesign_choice")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_PHOTO_NAME = stringPreferencesKey("profile_photo_name")
    }
}
