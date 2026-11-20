package studylock.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AppViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(
            academicRepository = container.academicRepository,
            sessionRepository = container.sessionRepository,
            focusStateMachine = container.focusStateMachine,
            scheduler = container.scheduler,
            settingsRepository = container.settingsRepository,
            profilePhotoStore = container.profilePhotoStore,
            reminderScheduler = container.reminderScheduler,
            documentStore = container.documentStore,
            backupRepository = container.backupRepository,
            enforcement = container.enforcement,
            clock = container.clock
        ) as T
    }
}
