package studylock.app

import android.content.Context
import studylock.academic.engine.AcademicScheduler
import studylock.academic.engine.DefaultAcademicScheduler
import studylock.app.documents.DocumentStore
import studylock.app.enforcement.EnforcementController
import studylock.app.profile.ProfilePhotoStore
import studylock.app.reminders.ReminderScheduler
import studylock.app.settings.SettingsRepository
import studylock.common.clock.StudyClock
import studylock.common.clock.SystemClock
import studylock.data.local.AcademicRepositoryImpl
import studylock.data.local.AppDatabase
import studylock.data.local.BackupRepository
import studylock.data.local.SessionRepositoryImpl
import studylock.domain.AcademicRepository
import studylock.focus.FocusStateMachine
import studylock.focus.SessionRepository

/**
 * Manual dependency wiring per owner-approved D-11 (no DI framework).
 */
class AppContainer(context: Context) {
    val clock: StudyClock = SystemClock
    val scheduler: AcademicScheduler = DefaultAcademicScheduler()
    val focusStateMachine = FocusStateMachine(clock)
    val settingsRepository = SettingsRepository(context)
    val profilePhotoStore = ProfilePhotoStore(context.applicationContext)
    val reminderScheduler = ReminderScheduler(context.applicationContext)
    val documentStore = DocumentStore(context.applicationContext)
    val enforcement = EnforcementController(context.applicationContext)

    private val database = AppDatabase.getDatabase(context)

    val academicRepository: AcademicRepository = AcademicRepositoryImpl(database)
    val sessionRepository: SessionRepository = SessionRepositoryImpl(
        database.sessionDao(),
        database.sessionInterruptionDao()
    )
    val backupRepository = BackupRepository(context.applicationContext, database)
}
