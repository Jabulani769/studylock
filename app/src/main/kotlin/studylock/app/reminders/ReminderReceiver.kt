package studylock.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import studylock.app.R
import studylock.app.StudyLockApplication

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        postReminder(context)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val settings = (appContext as StudyLockApplication).container.settingsRepository
                val enabled = settings.reminderEnabled.first()
                val minutes = settings.reminderStartMinutes.first()
                ReminderScheduler(appContext).reschedule(enabled, minutes)
            } finally {
                pending.finish()
            }
        }
    }

    private fun postReminder(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Study reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Gentle reminders to begin a study session." }
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to study")
            .setContentText("Your StudyLock session is ready when you are.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "study_reminders"
        const val NOTIFICATION_ID = 71
    }
}
