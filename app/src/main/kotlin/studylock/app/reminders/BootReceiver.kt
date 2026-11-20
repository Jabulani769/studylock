package studylock.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import studylock.app.StudyLockApplication

/** Restores reminders after reboot or a clock/time-zone change. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in HANDLED_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val settings = (appContext as StudyLockApplication).container.settingsRepository
                ReminderScheduler(appContext).reschedule(
                    enabled = settings.reminderEnabled.first(),
                    startMinutes = settings.reminderStartMinutes.first()
                )
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
